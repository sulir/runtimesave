package com.github.sulir.runtimesave.savepoints;

import com.intellij.debugger.ui.breakpoints.Breakpoint;
import com.intellij.debugger.ui.breakpoints.JavaLineBreakpointType;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties;

import javax.swing.*;
import java.util.Arrays;
import java.util.EnumSet;

public class SavepointType extends JavaLineBreakpointType {
    public SavepointType() {
        super("savepoint", "Savepoints");
    }

    @Override
    public @Nls String getGeneralDescription(XLineBreakpoint<JavaLineBreakpointProperties> breakpoint) {
        return "Savepoint";
    }

    @Override
    public @NotNull Icon getEnabledIcon() {
        return AllIcons.Actions.MenuSaveall;
    }

    @Override
    public Icon getTemporaryIcon() {
        return AllIcons.Actions.MenuSaveall;
    }

    @Override
    public EnumSet<StandardPanels> getVisibleStandardPanels() {
        return EnumSet.of(StandardPanels.ACTIONS);
    }

    @Override
    public @NotNull Breakpoint<JavaLineBreakpointProperties> createJavaBreakpoint(Project project,
            XBreakpoint<JavaLineBreakpointProperties> breakpoint) {
        return new Savepoint(project, breakpoint);
    }

    public static SavepointType getInstance() {
        return Arrays.stream(XDebuggerUtil.getInstance().getLineBreakpointTypes())
                .filter(SavepointType.class::isInstance)
                .map(SavepointType.class::cast)
                .findFirst().orElseThrow();
    }
}
