package com.github.sulir.runtimesave.graph;

import com.github.sulir.runtimesave.nodes.*;
import com.github.sulir.runtimesave.packing.Packer;
import com.github.sulir.runtimesave.packing.ValuePacker;
import org.neo4j.driver.Value;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

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

        for (Packer packer : valuePacker.getPackers())
            for (Class<?> nested : packer.getClass().getClasses())
                if (GraphNode.class.isAssignableFrom(nested))
                    registerMapping(nested.asSubclass(GraphNode.class));
    }

    public GraphNode createNode(String label, Map<String, Value> nodeProperties) {
        Mapping mapping = labelToMapping.get(label);
        if (mapping == null)
            throw new IllegalArgumentException("Unknown node label: " + label);

        Mapping.PropertySpec[] propertySpecs = mapping.properties();
        Object[] params = new Object[propertySpecs.length];
        for (int i = 0; i < params.length; i++)
            params[i] = nodeProperties.get(propertySpecs[i].key()).as(propertySpecs[i].type());
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
}
