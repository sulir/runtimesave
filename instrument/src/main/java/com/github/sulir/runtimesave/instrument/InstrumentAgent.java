package com.github.sulir.runtimesave.instrument;

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
                if (className == null)
                    return null;
                String javaClass = className.replace('/', '.');

                if (included.matcher(javaClass).matches() && !excluded.matcher(javaClass).matches())
                    return instrumentClass(javaClass, classFileBuffer);
                else
                    return null;
            }
        });
    }

    public byte[] instrumentClass(String className, byte[] bytes) {
        System.out.printf("Instrumenting %s %d %d\n", className, everyNthLine, firstTExecutions);
        return null;
    }
}
