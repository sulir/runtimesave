package com.github.sulir.runtimesave.comparison;

import com.github.sulir.runtimesave.db.DbConnection;
import com.github.sulir.runtimesave.db.DbIndex;
import com.github.sulir.runtimesave.db.HashedDb;
import com.github.sulir.runtimesave.db.Metadata;
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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.github.sulir.runtimesave.UncheckedThrowing.uncheck;

public class Comparison {
    private static final int WARMUP_ROUNDS = 20;
    private static final int MEASUREMENT_ROUNDS = 20;
    private static final String XML = "https://sample-files.com/downloads/data/xml/complex-nested.xml";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ValuePacker packer = ValuePacker.fromServiceLoader();
    private final NodeFactory nodeFactory = new NodeFactory(packer);
    private final GraphHasher hasher = new GraphHasher();
    private final GraphIdHasher idHasher = new GraphIdHasher();
    private final DbIndex dbIndex = new DbIndex(DbConnection.getInstance());
    private final Metadata metadata = new Metadata(DbConnection.getInstance());
    private final PlainDb plainDb = new PlainDb(DbConnection.getInstance());
    private final HashedDb hashedDb = new HashedDb(DbConnection.getInstance(), nodeFactory);
    private Program program;
    private List<FrameNode> frames;

    public static void main(String[] args) {
        new Comparison().perform();
    }

    public void perform() {
        dbIndex.createIndexes();
        dbIndex.createConstraint(PlainDb.INDEX_LABEL, PlainDb.INDEX_PROPERTY);

        program = uncheck(this::collectProgramData);

        measure(() -> write(false, false), "plain");
        measure(() -> write(true, false), "packed");
        measure(() -> write(false, true), "hashed");
        measure(() -> write(true, true), "packed and hashed");

        executor.shutdown();
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
        plainDb.deleteAll();
        frames = program.frames();
    }

    private void tearDown() {
        System.out.printf("%d nodes, %d edges\n", plainDb.nodeCount(), plainDb.edgeCount());
        plainDb.deleteAll();
        frames = null;
    }

    private Program collectProgramData() throws Exception {
        Program program = new Program();
        
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        program.addLine(builder);
        Document doc = builder.parse(XML);
        program.addLine(builder, doc);

        Element book=doc.createElement("book");
        program.addLine(builder, doc, book);
        book.setAttribute("category","fiction");
        program.addLine(builder, doc, book);
        Element title=doc.createElement("title");
        program.addLine(builder, doc, book, title);
        title.setTextContent("Just an Example");
        program.addLine(builder, doc, book, title);
        book.appendChild(title);
        program.addLine(builder, doc, book, title);
        Element publisher=doc.createElement("publisher");
        program.addLine(builder, doc, book, title, publisher);
        Element name=doc.createElement("name");
        program.addLine(builder, doc, book, title, publisher, name);
        name.setTextContent("Examples Publishing");
        program.addLine(builder, doc, book, title, publisher, name);
        publisher.appendChild(name);
        program.addLine(builder, doc, book, title, publisher, name);
        book.appendChild(publisher);
        program.addLine(builder, doc, book, title, publisher, name);
        doc.getDocumentElement().appendChild(book);
        program.addLine(builder, doc, book, title, publisher, name);

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        program.addLine(builder, doc, book, title, publisher, name, transformer);
        StringWriter writer = new StringWriter();
        program.addLine(builder, doc, book, title, publisher, name, transformer, writer);
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        program.addLine(builder, doc, book, title, publisher, name, transformer, writer);
        String result = writer.toString();
        program.addLine(builder, doc, book, title, publisher, name, transformer, writer, result);

        return program;
    }

    private void write(boolean pack, boolean hash) {
        Future<?> dbOperations = null;
        var data = new Object() {
            int line;
        };

        for (FrameNode frame : frames) {
            if (pack)
                packer.pack(frame);

            if (hash) {
                AcyclicGraph dag = AcyclicGraph.multiCondensationOf(frame);
                hasher.assignHashes(dag);
                idHasher.assignIdHashes(frame);

                dbOperations = executor.submit(() -> {
                    hashedDb.write(dag);
                    metadata.addNote(frame.idHash(), "Line " + data.line++);
                });
            } else {
                dbOperations = executor.submit(() -> {
                    String id = plainDb.write(frame);
                    plainDb.addNote(id, "Line " + data.line++);
                });
            }
        }

        if (dbOperations != null)
            uncheck(dbOperations::get);
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
