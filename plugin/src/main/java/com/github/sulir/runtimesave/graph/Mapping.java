package com.github.sulir.runtimesave.graph;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public record Mapping(String label,
                      PropertySpec[] properties,
                      RelationSpec relation,
                      Function<GraphNode, SortedMap<?, ? extends GraphNode>> edgeMap,
                      Function<Object[], ? extends GraphNode> constructor) {
    public record PropertySpec(String key,
                               Class<?> type,
                               Function<GraphNode, ?> getter) {
    }

    public record RelationSpec(String type,
                               String propertyName,
                               Class<?> propertyType,
                               Class<? extends GraphNode> targetType) {
    }

    @SuppressWarnings("unchecked")
    public static class Builder<T extends GraphNode> {
        private final String label;
        private final List<PropertySpec> properties = new ArrayList<>();
        private RelationSpec relations = new RelationSpec("", "", Void.class, GraphNode.class);
        private Function<GraphNode, SortedMap<?, ? extends GraphNode>> edgeMap = n -> Collections.emptySortedMap();

        public Builder(Class<T> nodeClass) {
            label = nodeClass.getSimpleName().substring(0, nodeClass.getSimpleName().lastIndexOf("Node"));
        }

        public <U> Builder<T> property(String key, Class<U> type, Function<T, U> getter) {
            properties.add(new PropertySpec(key, type, (Function<GraphNode, ?>) getter));
            return this;
        }

        public <K extends Comparable<K>, V extends GraphNode>
        Builder<T> edges(String relationType,
                         String relationPropertyName,
                         Class<K> relationPropertyType,
                         Class<V> targetType,
                         Function<T, SortedMap<K, V>> edgeMap) {
            relations = new RelationSpec(relationType, relationPropertyName, relationPropertyType, targetType);
            this.edgeMap = (Function<GraphNode, SortedMap<?, ? extends GraphNode>>) (Function<?, ?>) edgeMap;
            return this;
        }

        public Mapping constructor(Supplier<T> noArgConstructor) {
            return argsConstructor(props -> noArgConstructor.get());
        }

        public <A> Mapping constructor(Function<A, T> oneArgConstructor) {
            return argsConstructor(props -> oneArgConstructor.apply((A) props[0]));
        }

        public Mapping argsConstructor(Function<Object[], T> anyConstructor) {
            PropertySpec[] propertiesArray = properties.toArray(PropertySpec[]::new);
            return new Mapping(label, propertiesArray, relations, edgeMap, anyConstructor);
        }
    }
}
