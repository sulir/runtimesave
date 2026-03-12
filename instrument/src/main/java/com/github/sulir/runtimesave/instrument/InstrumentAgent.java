package com.github.sulir.runtimesave.instrument;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

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

    public static void premain(String agentArgs, Instrumentation inst) {
        new InstrumentAgent().setup(inst);
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

                    if (Settings.INCLUDE.matcher(javaClass).matches() && !excluded.matcher(javaClass).matches())
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
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        ClassTransformer transformer = new ClassTransformer(writer);

        try {
            reader.accept(transformer, ClassReader.EXPAND_FRAMES);
            return writer.toByteArray();
        } catch (ExcludedClassException e) {
            return null;
        }
    }
}
