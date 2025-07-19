package com.github.sulir.runtimesave.comparison;

import com.github.sulir.runtimesave.db.*;
import com.github.sulir.runtimesave.graph.NodeFactory;
import com.github.sulir.runtimesave.graph.ReflectionReader;
import com.github.sulir.runtimesave.graph.TestUtils;
import com.github.sulir.runtimesave.graph.ValueNode;
import com.github.sulir.runtimesave.hash.AcyclicGraph;
import com.github.sulir.runtimesave.hash.GraphHasher;
import com.github.sulir.runtimesave.hash.GraphIdHasher;
import com.github.sulir.runtimesave.nodes.FrameNode;
import com.github.sulir.runtimesave.pack.ValuePacker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.github.sulir.runtimesave.UncheckedThrowing.uncheck;

public class Comparison {
    private static final int WARMUP_ROUNDS = 20;
    private static final int MEASUREMENT_ROUNDS = 20;

    private final ValuePacker packer = ValuePacker.fromServiceLoader();
    private final NodeFactory nodeFactory = new NodeFactory(packer);
    private final GraphHasher hasher = new GraphHasher();
    private final GraphIdHasher idHasher = new GraphIdHasher();
    private final DbIndex dbIndex = new DbIndex(DbConnection.getInstance());
    private final Metadata metadata = new Metadata(DbConnection.getInstance());
    private PlainDb plainDb;
    private HashedDb hashedDb;

    public static void main(String[] args) {
        new Comparison().perform();
    }

    public void perform() {
        Program program = uncheck(this::collectProgramData);

        measure(() -> write(program.frames(), null, plainDb), "plain");
        measure(() -> write(program.frames(), packer, plainDb), "packed");
        measure(() -> write(program.frames(), null, hashedDb), "hashed");
        measure(() -> write(program.frames(), packer, hashedDb), "packed and hashed");
    }

    public void measure(Runnable action, String message) {
        System.out.println("--- " + message + " ---");
        for (int i = 0; i < WARMUP_ROUNDS; i++) {
            setUp();
            action.run();
            tearDown();
        }

        long[] times = new long[MEASUREMENT_ROUNDS];
        for (int i = 0; i < MEASUREMENT_ROUNDS; i++) {
            setUp();
            long start = System.nanoTime();
            action.run();
            long end = System.nanoTime();
            times[i] = end - start;
            tearDown();
        }

        System.out.println(Arrays.stream(times)
                .mapToObj(time -> String.valueOf(TimeUnit.NANOSECONDS.toMillis(time)))
                .collect(Collectors.joining(" ")) + " ms");
        System.out.printf("Median: %.0f ms\n", median(times) / 1_000_000d);
    }

    private void setUp() {
        dbIndex.createIndexes();
        dbIndex.createConstraint(PlainDb.INDEX_LABEL, PlainDb.INDEX_PROPERTY);
        plainDb = new PlainDb(DbConnection.getInstance());
        hashedDb = new HashedDb(DbConnection.getInstance(), nodeFactory);
    }

    private void tearDown() {
        System.out.printf("%d nodes, %d edges\n", plainDb.nodeCount(), plainDb.edgeCount());
        plainDb.deleteAll();
        hashedDb.deleteAll();
        plainDb = null;
        hashedDb = null;
    }

    private Program collectProgramData() throws Exception {
        Program program = new Program();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        program.addLine(factory);
        DocumentBuilder builder = factory.newDocumentBuilder();
        program.addLine(factory, builder);
        InputSource source = new InputSource(new StringReader("<person><name>Ed</name></person>"));
        program.addLine(factory, builder, source);
        Document document = builder.parse(source);
        program.addLine(factory, builder, source, document);
        Element element = document.createElement("age");
        program.addLine(factory, builder, source, document, element);
        element.setTextContent("1");
        program.addLine(factory, builder, source, document, element);
        document.getDocumentElement().appendChild(element);
        program.addLine(factory, builder, source, document, element);

        TransformerFactory tFactory = TransformerFactory.newInstance();
        program.addLine(factory, builder, source, document, element, tFactory);
        Transformer transformer = tFactory.newTransformer();
        program.addLine(factory, builder, source, document, element, tFactory, transformer);
        Result output = new StreamResult(OutputStream.nullOutputStream());
        program.addLine(factory, builder, source, document, element, tFactory, transformer, output);
        transformer.transform(new DOMSource(document), output);
        program.addLine(factory, builder, source, document, element, tFactory, transformer, output);

        return program;
    }

    private void write(List<FrameNode> frames, ValuePacker packer, Database db) {
        int line = 1;

        for (FrameNode frame : frames) {
            if (packer != null)
                packer.pack(frame);

            if (db instanceof HashedDb hashedDatabase) {
                AcyclicGraph dag = AcyclicGraph.multiCondensationOf(frame);
                hasher.assignHashes(dag);
                idHasher.assignIdHashes(frame);
                hashedDatabase.write(dag);
                metadata.addNote(frame.idHash(), "Line " + line++);
            } else {
                PlainDb plainDb = ((PlainDb) db);
                String id = plainDb.write(frame);
                plainDb.addNote(id, "Line " + line++);
            }
        }
    }

    private class Program {
        private final List<FrameNode> frames = new ArrayList<>();

        void addLine(Object... variables) {
            ReflectionReader reader = new ReflectionReader();

            FrameNode frame = new FrameNode();
            for (int i = 0; i < variables.length; i++) {
                ValueNode value = reader.read(variables[i]);
                frame.setVariable("variable" + i, value);
            }
            frames.add(frame);
        }

        public List<FrameNode> frames() {
            return frames.stream().map(frame -> TestUtils.copyGraph(frame, nodeFactory)).toList();
        }
    }

    private double median(long[] values) {
        Arrays.sort(values);
        return values.length % 2 == 0
                ? (values[values.length / 2 - 1] + values[values.length / 2]) / 2.0
                : values[values.length / 2];
    }
}
