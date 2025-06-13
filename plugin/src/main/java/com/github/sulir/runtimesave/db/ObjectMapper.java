package com.github.sulir.runtimesave.db;

import com.github.sulir.runtimesave.nodes.GraphNode;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

public class ObjectMapper {
    private static final Map<Class<? extends GraphNode>, ObjectMapper> classToMapper = new HashMap<>();
    private static final Map<String, ObjectMapper> stringToMapper = new HashMap<>();

    private final Class<? extends GraphNode> clazz;
    private final String label;
    private final Constructor<? extends GraphNode> constructor;
    private final List<Property> properties = new ArrayList<>();
    private final Map<String, Relation> relations = new HashMap<>();

    public static ObjectMapper getInstance(Class<? extends GraphNode> nodeClass) {
        return classToMapper.computeIfAbsent(nodeClass, ObjectMapper::new);
    }

    public static ObjectMapper getInstance(String nodeLabel) {
        return stringToMapper.computeIfAbsent(nodeLabel, label -> getInstance(findClass(nodeLabel)));
    }

    private static Class<? extends GraphNode> findClass(String nodeLabel) {
        String packageName = GraphNode.class.getPackageName();
        try {
            return Class.forName(packageName + "." + nodeLabel + "Node").asSubclass(GraphNode.class);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public String getLabel() {
        return label;
    }

    public GraphNode createNode(Node node) {
        try {
            Object[] params = new Object[properties.size()];
            for (int i = 0; i < params.length; i++)
                params[i] = node.get(properties.get(i).name()).asObject();
            return constructor.newInstance(params);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> createProperties(GraphNode node) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("id", UUID.randomUUID().toString());

        try {
            for (Property property : this.properties)
                properties.put(property.name(), property.getter().invoke(node));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        return properties;
    }

    public void createEdge(GraphNode from, GraphNode to, Relationship edge) {
        Relation relation = relations.get(edge.type());
        Value keyOrIndex = edge.get(relation.property());

        try {
            if (relation.kind() == RelationKind.MAP)
                relation.setter().invoke(from, keyOrIndex.asString(), to);
            else
                relation.setter().invoke(from, keyOrIndex.asInt(), to);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Map<String, Object>> createOutEdges(GraphNode node, Map<GraphNode, String> nodeToId) {
        List<Map<String, Object>> edges = new ArrayList<>();
        String from = nodeToId.get(node);

        relations.forEach((type, relation) -> {
            try {
                Object items = relation.getter().invoke(node);
                if (items instanceof Map<?,?> map) {
                    map.forEach((key, value) -> edges.add(Map.of("from", from, "type", type,
                            "props", Map.of(relation.property(), key), "to", nodeToId.get((GraphNode) value))));
                } else if (items instanceof List<?> list) {
                    int index = 0;
                    for (var element : list)
                        edges.add(Map.of("from", from, "type", type, "props", Map.of(relation.property(), index++),
                                "to", nodeToId.get((GraphNode) element)));
                }
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        });

        return edges;
    }

    private ObjectMapper(Class<? extends GraphNode> clazz) {
        this.clazz = clazz;
        label = clazz.getSimpleName().substring(0, clazz.getSimpleName().lastIndexOf("Node"));
        try {
            constructor = findConstructor();
            findProperties();
            findRelations();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private Constructor<? extends GraphNode> findConstructor() throws ReflectiveOperationException {
        Constructor<?>[] constructors = clazz.getConstructors();
        if (constructors.length != 1)
            throw new ReflectiveOperationException("Node classes must have one constructor");
        return clazz.getConstructor(constructors[0].getParameterTypes());
    }

    private void findProperties() throws NoSuchMethodException {
        for (Parameter param : constructor.getParameters()) {
            String name = param.getName();
            Method getter = clazz.getMethod("get" + name.substring(0, 1).toUpperCase() + name.substring(1));
            properties.add(new Property(name, getter));
        }
    }

    private void findRelations() throws NoSuchMethodException {
        for (Method setter : clazz.getMethods()) {
            if (!setter.getName().startsWith("set"))
                continue;

            Parameter[] params = setter.getParameters();
            if (params.length != 2 || !GraphNode.class.isAssignableFrom(params[1].getType()))
                continue;

            RelationKind kind = RelationKind.fromSetterParameter(params[0].getType());
            if (kind == null)
                continue;

            String noun = setter.getName().substring(3);
            String type = "HAS_" + noun.toUpperCase();
            String property = params[0].getName();
            Method getter = clazz.getMethod("get" + pluralize(noun));
            relations.put(type, new Relation(kind, property, getter, setter));
        }
    }

    private static String pluralize(String noun) {
        return noun.matches(".*(s|sh|ch|x|z|o)$") ? noun + "es" :
               noun.matches(".*[^aeiou]y$") ? noun.replaceAll("y$", "ies") :
               noun.matches(".*(f|fe)$") ? noun.replaceAll("f?e?$", "ves") :
               noun + "s";
    }

    private record Property(String name, Method getter) { }

    private record Relation(RelationKind kind, String property, Method getter, Method setter) { }

    private enum RelationKind {
        MAP, LIST;

        public static RelationKind fromSetterParameter(Class<?> firstParamType) {
            return Map.of(String.class, RelationKind.MAP, int.class, RelationKind.LIST).get(firstParamType);
        }
    }
}