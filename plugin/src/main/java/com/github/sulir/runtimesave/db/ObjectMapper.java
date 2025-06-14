package com.github.sulir.runtimesave.db;

import com.github.sulir.runtimesave.nodes.GraphNode;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

public class ObjectMapper {
    private static final Map<Class<? extends GraphNode>, ObjectMapper> classToMapper = new HashMap<>();
    private static final Map<String, ObjectMapper> stringToMapper = new HashMap<>();
    private static final MethodHandles.Lookup lookup = MethodHandles.publicLookup();

    private final Class<? extends GraphNode> clazz;
    private final String label;
    private final Constructor<? extends GraphNode> constructor;
    private final List<Property> properties = new ArrayList<>();
    private final Map<String, Relation> relations = new HashMap<>();

    public static ObjectMapper forClass(Class<? extends GraphNode> nodeClass) {
        return classToMapper.computeIfAbsent(nodeClass, ObjectMapper::new);
    }

    public static ObjectMapper forLabel(String nodeLabel) {
        return stringToMapper.computeIfAbsent(nodeLabel, label -> forClass(uncheck(() -> findClass(label))));
    }

    public ObjectMapper(Class<? extends GraphNode> clazz) {
        this.clazz = clazz;
        label = clazz.getSimpleName().substring(0, clazz.getSimpleName().lastIndexOf("Node"));
        constructor = findConstructor();
        findProperties();
        findRelations();
    }

    private static Class<? extends GraphNode> findClass(String nodeLabel) throws ClassNotFoundException {
        String packageName = GraphNode.class.getPackageName();
        return Class.forName(packageName + "." + nodeLabel + "Node").asSubclass(GraphNode.class);
    }

    public String getLabel() {
        return label;
    }

    public GraphNode createNodeObject(Node node) {
        Object[] params = new Object[properties.size()];
        for (int i = 0; i < params.length; i++)
            params[i] = node.get(properties.get(i).name()).as(properties.get(i).type());
        return uncheck(() -> constructor.newInstance(params));
    }

    public void connectNodeObjects(GraphNode from, GraphNode to, Relationship edge) {
        Relation relation = relations.get(edge.type());
        Value keyOrIndex = edge.get(relation.property());
        uncheck(() -> relation.setter().invoke(from, keyOrIndex.as(relation.keyType()), to));
    }

    public Map<String, Object> createProperties(GraphNode node) {
        return properties.stream().collect(Collectors.toMap(
                Property::name,
                p -> uncheck(() -> p.getter().invoke(node)))
        );
    }

    public List<Map<String, Object>> createOutEdges(GraphNode node, Map<GraphNode, String> nodeToId) {
        List<Map<String, Object>> edges = new ArrayList<>();
        String from = nodeToId.get(node);

        relations.forEach((type, relation) -> {
            Map<?, ?> items = (Map<?, ?>) uncheck(() -> relation.getter().invoke(node));
            items.forEach((key, value) -> edges.add(Map.of("from", from, "type", type,
                    "props", Map.of(relation.property(), key), "to", nodeToId.get((GraphNode) value))));
        });

        return edges;
    }

    private Constructor<? extends GraphNode> findConstructor() {
        Constructor<?>[] constructors = clazz.getConstructors();
        if (constructors.length != 1)
            throw new IllegalArgumentException("Node classes must have one constructor");
        return uncheck(() -> clazz.getConstructor(constructors[0].getParameterTypes()));
    }

    private void findProperties() {
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
            if (!method.getName().startsWith("set"))
                continue;

            Parameter[] params = method.getParameters();
            if (params.length != 2 || !GraphNode.class.isAssignableFrom(params[1].getType()))
                continue;

            String noun = method.getName().substring(3);
            String type = "HAS_" + noun.toUpperCase();
            String property = params[0].getName();
            MethodHandle getter = uncheck(() -> lookup.unreflect(clazz.getMethod("get" + pluralize(noun))));
            MethodHandle setter = uncheck(() -> lookup.unreflect(method));
            Class<?> keyType = params[0].getType();
            relations.put(type, new Relation(property, getter, setter, keyType));
        }
    }

    private static String pluralize(String noun) {
        return noun.matches(".*(s|sh|ch|x|z|o)$") ? noun + "es" :
               noun.matches(".*[^aeiou]y$") ? noun.replaceAll("y$", "ies") :
               noun.matches(".*(f|fe)$") ? noun.replaceAll("f?e?$", "ves") :
               noun + "s";
    }

    private record Property(String name, MethodHandle getter, Class<?> type) { }

    private record Relation(String property, MethodHandle getter, MethodHandle setter, Class<?> keyType) { }

    private interface Throwing<T> {
        T call() throws Throwable;
    }

    private static <T> T uncheck(Throwing<T> callable) {
        try {
            return callable.call();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
