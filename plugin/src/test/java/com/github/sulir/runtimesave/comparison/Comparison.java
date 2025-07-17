package com.github.sulir.runtimesave.comparison;

import com.github.sulir.runtimesave.db.Database;
import com.github.sulir.runtimesave.db.DbConnection;
import com.github.sulir.runtimesave.db.DbIndex;
import com.github.sulir.runtimesave.db.HashedDb;
import com.github.sulir.runtimesave.graph.*;
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
import java.util.List;

import static com.github.sulir.runtimesave.UncheckedThrowing.uncheck;

public class Comparison {
    private final DbIndex dbIndex = new DbIndex(DbConnection.getInstance());
    private final ValuePacker packer = ValuePacker.fromServiceLoader();
    private final NodeFactory nodeFactory = new NodeFactory(packer);
    private final PlainDb plainDb = new PlainDb(DbConnection.getInstance(), nodeFactory);
    private final HashedDb hashedDb = new HashedDb(DbConnection.getInstance(), nodeFactory);
    private final GraphHasher hasher = new GraphHasher();
    private final GraphIdHasher idHasher = new GraphIdHasher();

    public static void main(String[] args) {
        new Comparison().perform();
    }

    public void perform() {
        dbIndex.createIndexes();
        dbIndex.createConstraint(PlainDb.INDEX_LABEL, PlainDb.INDEX_PROPERTY);

        Program program = uncheck(this::collectProgramData);

        measureWrites(program.frames(), null, plainDb, "simple");
        measureWrites(program.frames(), packer, plainDb, "packed");
        measureWrites(program.frames(), null, hashedDb, "hashed");
        measureWrites(program.frames(), packer, hashedDb, "pack+h");
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

    private void measureWrites(List<FrameNode> frames, ValuePacker packer, Database db, String message) {
        long time = System.currentTimeMillis();

        for (FrameNode frame : frames)
            write(frame, packer, db);

        System.out.print(message + ": " + (System.currentTimeMillis() - time) + " ms, ");
        System.out.printf("%d nodes, %d edges\n", db.nodeCount(), db.edgeCount());
        db.deleteAll();
    }

    private void write(GraphNode node, ValuePacker packer, Database db) {
        if (packer != null)
            packer.pack(node);

        if (db instanceof HashedDb hashedDatabase) {
            AcyclicGraph dag = AcyclicGraph.multiCondensationOf(node);
            hasher.assignHashes(dag);
            idHasher.assignIdHashes(node);
            hashedDatabase.write(dag);
        } else {
            ((PlainDb) db).write(node);
        }
    }

    private class Program {
        private final ReflectionReader reader = new ReflectionReader();
        private final List<FrameNode> frames = new ArrayList<>();

        void addLine(Object... variables) {
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
}
