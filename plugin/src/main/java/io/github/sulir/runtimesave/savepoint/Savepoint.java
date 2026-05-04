package io.github.sulir.runtimesave.savepoint;

import com.intellij.debugger.engine.SuspendContextImpl;
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl;
import com.intellij.debugger.engine.evaluation.expression.EvaluatorBuilderImpl;
import com.intellij.debugger.engine.evaluation.expression.ExpressionEvaluator;
import com.intellij.debugger.engine.events.SuspendContextCommandImpl;
import com.intellij.debugger.jdi.StackFrameProxyImpl;
import com.intellij.debugger.ui.breakpoints.LineBreakpoint;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderEx;
import com.intellij.psi.JavaCodeFragmentFactory;
import com.intellij.psi.PsiElement;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.breakpoints.SuspendPolicy;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.sun.jdi.Value;
import com.sun.jdi.event.LocatableEvent;
import io.github.sulir.runtimesave.config.RuntimeSaveSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties;

import javax.swing.*;

import static io.github.sulir.runtimesave.misc.UncheckedThrowing.uncheck;

public class Savepoint extends LineBreakpoint<JavaLineBreakpointProperties> {
    private static final String COLLECT_EXPRESSION = "((Runnable) Class.forName(\"io.github.sulir.runtimesave.rt."
            + "Collector$SavepointCollector\").getDeclaredConstructor().newInstance()).run()";

    protected Savepoint(Project project, XBreakpoint xBreakpoint) {
        super(project, xBreakpoint);
        xBreakpoint.setSuspendPolicy(SuspendPolicy.NONE);
    }

    @Override
    public boolean processLocatableEvent(@NotNull SuspendContextCommandImpl action, LocatableEvent event)
            throws EventProcessingException {
        if (!super.processLocatableEvent(action, event))
            return false;

        SuspendContextImpl context = action.getSuspendContext();
        if (context == null || !savepointsEnabled(context))
            return false;

        collectData(context);
        return true;
    }

    private boolean savepointsEnabled(SuspendContextImpl context) {
        XDebugSession session = context.getDebugProcess().getSession().getXDebugSession();
        if (session == null || ! (session.getRunProfile() instanceof UserDataHolderEx data))
            return false;
        return RuntimeSaveSettings.getOrDefault(data).isSavepointEnabled();
    }

    private static void collectData(SuspendContextImpl context) {
        JavaCodeFragmentFactory factory = JavaCodeFragmentFactory.getInstance(context.getDebugProcess().getProject());
        PsiElement fragment = factory.createExpressionCodeFragment(COLLECT_EXPRESSION, null, null, true);
        ExpressionEvaluator evaluator = ReadAction.computeBlocking(() ->
                uncheck(() -> EvaluatorBuilderImpl.getInstance().build(fragment, null)));
        StackFrameProxyImpl frame = context.getFrameProxy();
        EvaluationContextImpl evalContext = new EvaluationContextImpl(context, frame, (Value) null);
        uncheck(() -> evaluator.evaluate(evalContext));
    }

    @Override
    protected Icon getVerifiedIcon(boolean isMuted) {
        return AllIcons.Actions.MenuSaveall;
    }
}
