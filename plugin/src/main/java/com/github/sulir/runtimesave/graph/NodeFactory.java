package com.github.sulir.runtimesave.graph;

import com.github.sulir.runtimesave.nodes.FrameNode;
import com.github.sulir.runtimesave.packing.Packer;
import com.github.sulir.runtimesave.packing.ValuePacker;
import org.neo4j.driver.Value;

import java.util.HashMap;
import java.util.Map;

public class NodeFactory {
    private final ValuePacker valuePacker;
    private final Map<String, Mapping> labelToMapping = new HashMap<>();

    public NodeFactory(ValuePacker valuePacker) {
        this.valuePacker = valuePacker;
    }

    public GraphNode createNode(String label, Map<String, Value> nodeProperties) {
        Mapping mapping = labelToMapping.get(label);
        if (mapping == null) {
            Class<? extends GraphNode> nodeClass = findClass(label);
            mapping = Mapping.forClass(nodeClass);
            labelToMapping.put(label, mapping);
        }

        Mapping.PropertySpec[] propertySpecs = mapping.properties();
        Object[] params = new Object[propertySpecs.length];
        for (int i = 0; i < params.length; i++)
            params[i] = nodeProperties.get(propertySpecs[i].key()).as(propertySpecs[i].type());
        return mapping.constructor().apply(params);
    }

    private Class<? extends GraphNode> findClass(String label) {
        String className = label + "Node";
        try {
            return Class.forName(FrameNode.class.getPackageName() + "." + className).asSubclass(GraphNode.class);
        } catch (ClassNotFoundException e) {
            Packer[] packers = valuePacker.getPackers();
            for (Packer packer : packers) {
                for (Class<?> nested : packer.getClass().getClasses())
                    if (nested.getSimpleName().equals(className) && GraphNode.class.isAssignableFrom(nested))
                        return nested.asSubclass(GraphNode.class);
            }
            throw new RuntimeException(new ClassNotFoundException("Node class not found: " + className));
        }
    }
}
