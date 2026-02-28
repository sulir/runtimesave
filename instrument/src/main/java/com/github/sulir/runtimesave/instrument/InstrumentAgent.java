package com.github.sulir.runtimesave.instrument;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.regex.Pattern;

public class InstrumentAgent {
    private static final String[] EXCLUDED_PACKAGES = {
            "com.sun.*", "java.*", "javax.*", "jdk.*", "sun.*",
            "com.intellij.execution.*", "com.intellij.rt.*", "org.jetbrains.capture.*",
            "com.github.sulir.runtimesave.*"
    };
    public static final Pattern excluded = Pattern.compile(String.join("|", EXCLUDED_PACKAGES)
            .replace(".", "\\.").replace("*", ".*"));
    public static final boolean DEBUG = System.getenv("RS_DEBUG") != null;

    private final int everyNthLine;
    private final int firstTExecutions;
    private final Pattern included;

    public InstrumentAgent(int everyNthLine, int firstTExecutions, Pattern included) {
        this.everyNthLine = everyNthLine;
        this.firstTExecutions = firstTExecutions;
        this.included = included;
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        int everyNthLine = Integer.parseInt(System.getProperty("runtimesave.n"));
        int firstTExecutions = Integer.parseInt(System.getProperty("runtimesave.t"));
        Pattern included = Pattern.compile(System.getProperty("runtimesave.include"));

        InstrumentAgent agent = new InstrumentAgent(everyNthLine, firstTExecutions, included);
        agent.setup(inst);
    }

    public void setup(Instrumentation inst) {
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain, byte[] classFileBuffer) {
                try {
                    if (className == null)
                        return null;
                    String javaClass = className.replace('/', '.');

                    if (included.matcher(javaClass).matches() && !excluded.matcher(javaClass).matches())
                        return rewriteClass(classFileBuffer);
                    else
                        return null;
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                    return null;
                }
            }
        });
    }

    public byte[] rewriteClass(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);

        if (classNode.methods.isEmpty() || (classNode.access & Opcodes.ACC_SYNTHETIC) != 0)
            return null;

        new ClassInstrumentation(classNode).instrument(everyNthLine, firstTExecutions);

        ClassWriter writer = new ClassWriter(reader, 0);
        classNode.accept(writer);
        return writer.toByteArray();
    }
}
