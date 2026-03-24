package com.github.sulir.runtimesave.instrument;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class AppUnaffectingLogManager extends LogManager {
    private static Pattern agentPackages;
    private boolean appLoggingStarted = false;

    public static void setAgentPackages(Pattern pattern) {
        agentPackages = pattern;
    }

    @Override
    public boolean addLogger(Logger logger) {
        if (agentPackages == null || logger.getName().isEmpty() || logger.getName().equals("global"))
            return super.addLogger(logger);

        if (agentPackages.matcher(logger.getName()).matches()) {
            logger.setLevel(Level.OFF);
        } else if (!appLoggingStarted) {
            appLoggingStarted = true;
            try {
                readConfiguration();
            } catch (IOException ignored) { }
        }
        return super.addLogger(logger);
    }
}
