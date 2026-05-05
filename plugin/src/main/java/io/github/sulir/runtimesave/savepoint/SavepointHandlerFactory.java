package io.github.sulir.runtimesave.savepoint;

import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.JavaBreakpointHandler;
import com.intellij.debugger.engine.JavaBreakpointHandlerFactory;

public class SavepointHandlerFactory implements JavaBreakpointHandlerFactory {
    @Override
    public JavaBreakpointHandler createHandler(DebugProcessImpl process) {
        process.getProject().getService(InvalidSavepointService.class);
        return new JavaBreakpointHandler(SavepointType.class, process) {};
    }
}
