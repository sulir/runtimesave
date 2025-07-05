package com.github.sulir.runtimesave;

import com.github.sulir.runtimesave.nodes.GraphNode;
import com.github.sulir.runtimesave.packers.Packer;
import org.neo4j.driver.Value;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

import static com.github.sulir.runtimesave.UncheckedThrowing.uncheck;

public class ObjectMapper {
    private static final Map<Class<? extends GraphNode>, ObjectMapper> classToMapper = new HashMap<>();
    private static final Map<String, ObjectMapper> stringToMapper = new HashMap<>();
    private static final MethodHandles.Lookup lookup = MethodHandles.publicLookup();

    private final Class<? extends GraphNode> clazz;
    private final String label;
    private final MethodHandle constructorHandle;
    private final List<Property> properties = new ArrayList<>();
    private Relation relation;

    public static ObjectMapper forClass(Class<? extends GraphNode> nodeClass) {
        return classToMapper.computeIfAbsent(nodeClass, ObjectMapper::new);
    }

    public static ObjectMapper forLabel(String nodeLabel) {
        return stringToMapper.computeIfAbsent(nodeLabel, label -> forClass(uncheck(() -> findClass(label))));
    }

    public ObjectMapper(Class<? extends GraphNode> clazz) {
        this.clazz = clazz;
        label = clazz.getSimpleName().substring(0, clazz.getSimpleName().lastIndexOf("Node"));
        Constructor<? extends GraphNode> constructor = findConstructor();
        constructorHandle = uncheck(() -> lookup.unreflectConstructor(constructor));
        findProperties(constructor);
        findRelations();
    }

    private static Class<? extends GraphNode> findClass(String nodeLabel) throws ClassNotFoundException {
        String className = nodeLabel + "Node";
        try {
            return Class.forName(GraphNode.class.getPackageName() + "." + className).asSubclass(GraphNode.class);
        } catch (ClassNotFoundException e) {
            Packer[] packers = RuntimePersistenceService.getInstance().getValuePackers();
            for (Packer packer : packers) {
                for (Class<?> nested : packer.getClass().getClasses())
                    if (nested.getSimpleName().equals(className) && GraphNode.class.isAssignableFrom(nested))
                        return nested.asSubclass(GraphNode.class);
            }
            throw new ClassNotFoundException("Node class not found: " + className);
        }
    }

    public String getLabel() {
        return label;
    }

    public GraphNode createNodeObject(Map<String, Value> nodeProperties) {
        Object[] params = new Object[properties.size()];
        for (int i = 0; i < params.length; i++)
            params[i] = nodeProperties.get(properties.get(i).name()).as(properties.get(i).type());
        return uncheck(() -> (GraphNode) constructorHandle.invokeWithArguments(params));
    }

    public void connectNodeObjects(GraphNode from, GraphNode to, Map<String, Value> edgeProperties) {
        Value key = edgeProperties.get(relation.propertyName());
        @SuppressWarnings("unchecked")
        SortedMap<Object, GraphNode> edges = (SortedMap<Object, GraphNode>) from.outEdges();
        edges.put(key.as(relation.propertyType()), to);
    }

    public SortedMap<String, Object> getProperties(GraphNode node) {
        SortedMap<String, Object> map = new TreeMap<>();
        for (Property p : properties)
            map.put(p.name(), uncheck(() -> p.getter().invoke(node)));
        return map;
    }

    public List<Map<String, Object>> getRelationships(GraphNode from) {
        List<Map<String, Object>> edges = new ArrayList<>(from.outEdges().size());

        from.outEdges().forEach((key, to) -> edges.add(Map.of(
                "from", from.idHash().toString(),
                "to", to.idHash().toString(),
                "type", relation.type(),
                "props", Map.of(relation.propertyName(), key))));

        return edges;
    }

    private Constructor<? extends GraphNode> findConstructor() {
        Constructor<?>[] constructors = clazz.getConstructors();
        if (constructors.length != 1)
            throw new IllegalArgumentException("Node classes must have one constructor");
        return uncheck(() -> clazz.getConstructor(constructors[0].getParameterTypes()));
    }

    private void findProperties(Constructor<? extends GraphNode> constructor) {
        for (Parameter param : constructor.getParameters()) {
            uncheck(() -> {
                String name = param.getName();
                Method getter = clazz.getMethod("get" + name.substring(0, 1).toUpperCase() + name.substring(1));
                return properties.add(new Property(name, lookup.unreflect(getter), param.getType()));
            });
        }
    }

    private void findRelations() {
        for (Method method : clazz.getMethods()) {
            Parameter[] params = method.getParameters();
            if (!method.getName().startsWith("set") || params.length != 2
                    || !GraphNode.class.isAssignableFrom(params[1].getType()))
                continue;

            if (relation != null)
                throw new IllegalArgumentException("Multiple relation types are unsupported");

            String type = "HAS_" + method.getName().substring(3).toUpperCase();
            relation = new Relation(type, params[0].getName(), params[0].getType());
        }
    }

    private record Property(String name, MethodHandle getter, Class<?> type) { }

    private record Relation(String type, String propertyName, Class<?> propertyType) { }
}
