package com.github.sulir.runtimesave.savepoints;

import com.intellij.debugger.ui.breakpoints.LineBreakpoint;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties;

import javax.swing.*;

public class Savepoint extends LineBreakpoint<JavaLineBreakpointProperties> {
    protected Savepoint(Project project, XBreakpoint xBreakpoint) {
        super(project, xBreakpoint);
    }

    @Override
    protected Icon getVerifiedIcon(boolean isMuted) {
        return AllIcons.Actions.MenuSaveall;
    }
}
