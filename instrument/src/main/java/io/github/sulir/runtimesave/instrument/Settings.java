package io.github.sulir.runtimesave.instrument;

import java.util.regex.Pattern;

public class Settings {
    public static final int LINE = Integer.parseInt(System.getProperty("runtimesave.line", "1"));
    public static final int HITS = Integer.parseInt(System.getProperty("runtimesave.hits", "1"));
    public static final Pattern INCLUDE = Pattern.compile(System.getProperty("runtimesave.include", ".*"));
    public static final boolean DEBUG = System.getenv("RS_DEBUG") != null;
}
