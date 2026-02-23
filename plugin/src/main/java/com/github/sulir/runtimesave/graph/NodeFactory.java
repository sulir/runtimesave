package com.github.sulir.runtimesave.graph;

import com.github.sulir.runtimesave.nodes.*;
import com.github.sulir.runtimesave.pack.Packer;
import com.github.sulir.runtimesave.pack.ValuePacker;
import org.neo4j.driver.Value;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

import static com.github.sulir.runtimesave.UncheckedThrowing.uncheck;

public class NodeFactory {
    private static final List<Class<? extends GraphNode>> nodeClasses = List.of(
            ArrayNode.class,
            FrameNode.class,
            NullNode.class,
            ObjectNode.class,
            PrimitiveNode.class,
            StringNode.class
    );

    private final Map<String, Mapping> labelToMapping = new HashMap<>();

    public NodeFactory(ValuePacker valuePacker) {
        nodeClasses.forEach(this::registerMapping);

        for (Packer packer : valuePacker.getAllPackers())
            for (Class<?> nested : packer.getClass().getClasses())
                if (GraphNode.class.isAssignableFrom(nested))
                    registerMapping(nested.asSubclass(GraphNode.class));
    }

    public GraphNode createNode(Iterable<String> labels, Map<String, Value> properties) {
        Mapping mapping = findMapping(labels);

        Mapping.PropertySpec[] propertySpecs = mapping.properties();
        Object[] params = new Object[propertySpecs.length];
        for (int i = 0; i < params.length; i++)
            params[i] = properties.get(propertySpecs[i].key()).as(propertySpecs[i].type());
        return mapping.constructor().apply(params);
    }

    public GraphNode createNode(String label, NodeProperty[] properties) {
        Mapping mapping = findMapping(List.of(label));
        Object[] params = Arrays.stream(properties).map(NodeProperty::value).toArray(Object[]::new);
        return mapping.constructor().apply(params);
    }

    private void registerMapping(Class<?> nodeClass) {
        for (Field field : nodeClass.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType().equals(Mapping.class)) {
                Mapping mapping = (Mapping) uncheck(() -> field.get(null));
                String label = mapping.label();
                labelToMapping.put(label, mapping);
                return;
            }
        }
        throw new NoSuchElementException("No Mapping field in node class " + nodeClass.getName());
    }

    private Mapping findMapping(Iterable<String> labels) {
        for (String label : labels) {
            Mapping mapping = labelToMapping.get(label);
            if (mapping != null)
                return  mapping;
        }
        throw new IllegalArgumentException("Unknown node labels: " + String.join(", ", labels));
    }
}
