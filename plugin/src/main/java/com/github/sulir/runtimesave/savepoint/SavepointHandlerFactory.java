package com.github.sulir.runtimesave.savepoint;

import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.JavaBreakpointHandler;
import com.intellij.debugger.engine.JavaBreakpointHandlerFactory;

public class SavepointHandlerFactory implements JavaBreakpointHandlerFactory {
    @Override
    public JavaBreakpointHandler createHandler(DebugProcessImpl process) {
        return new JavaBreakpointHandler(SavepointType.class, process) { };
    }
}
