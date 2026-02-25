package com.github.sulir.runtimesave.starter;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class StarterAgent {
    public static final boolean DEBUG = System.getenv("RUNTIMESAVE_DEBUG") != null;
    @SuppressWarnings("unused")
    public static boolean JUMPED_TO_LINE = false;

    private final String targetClass;
    private final String methodName;
    private final String descriptor;
    private final int line;

    public static void premain(String agentArgs, Instrumentation inst) {
        String[] commandLine = System.getProperty("sun.java.command").split(" ");
        String targetClass = commandLine[1];
        String methodName = commandLine[2];
        String descriptor = commandLine[3];
        int line = Integer.parseInt(commandLine[4]);

        new StarterAgent(targetClass, methodName, descriptor, line).setup(inst);
    }

    public StarterAgent(String targetClass, String methodName, String descriptor, int line) {
        this.targetClass = targetClass;
        this.methodName = methodName;
        this.descriptor = descriptor;
        this.line = line;
    }

    public void setup(Instrumentation inst) {
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain, byte[] classFileBuffer) {
                if (targetClass.replace('.', '/').equals(className))
                    return instrumentClass(classFileBuffer);
                else
                    return null;
            }
        });
    }

    public byte[] instrumentClass(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, ClassReader.SKIP_FRAMES);

        for (var method : classNode.methods) {
            if (method.name.equals(methodName) && method.desc.equals(descriptor)) {
                new LineJumpInstrumentation(method).generate(line);

                ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                classNode.accept(writer);
                return writer.toByteArray();
            }
        }

        return null;
    }
}
