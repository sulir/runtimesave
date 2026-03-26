package io.github.sulir.runtimesave.instrument;

import io.github.sulir.runtimesave.rt.SaveService;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class InstrumentAgent {
    private static final String[] SYSTEM_PACKAGES = {
            "com.sun.*", "java.*", "javax.*", "jdk.*", "sun.*",
            "com.intellij.execution.*", "com.intellij.rt.*", "org.jetbrains.capture.*"
    };

    private final String[] agentPackages;
    private final Pattern excluded;
    private final AtomicInteger lineId = new AtomicInteger(0);

    public InstrumentAgent() {
        try (JarFile jar = new JarFile(getClass().getProtectionDomain().getCodeSource().getLocation().getFile())) {
            agentPackages = jar.getManifest().getMainAttributes().getValue("Agent-Packages").split(",");
            String[] packages = Stream.of(SYSTEM_PACKAGES, agentPackages).flatMap(Stream::of).toArray(String[]::new);
            excluded = packagesToRegex(packages);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Pattern packagesToRegex(String[] packages) {
        return Pattern.compile(String.join("|", packages).replace(".", "\\.").replace("*", ".*"));
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        InstrumentAgent agent = new InstrumentAgent();
        agent.setupRuntime();
        agent.startInstrumenting(inst);
    }

    public void setupRuntime() {
        System.setProperty("java.util.logging.manager", AppUnaffectingLogManager.class.getName());
        AppUnaffectingLogManager.setAgentPackages(packagesToRegex(agentPackages));
        SaveService.getInstance().createIndexes();
    }

    public void startInstrumenting(Instrumentation inst) {
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
        ClassTransformer transformer = new ClassTransformer(writer, lineId);

        try {
            reader.accept(transformer, ClassReader.EXPAND_FRAMES);
            return writer.toByteArray();
        } catch (ExcludedClassException e) {
            return null;
        }
    }
}
