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
import com.intellij.psi.JavaCodeFragmentFactory;
import com.intellij.psi.PsiElement;
import com.intellij.xdebugger.breakpoints.SuspendPolicy;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.sun.jdi.Value;
import com.sun.jdi.event.LocatableEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties;

import javax.swing.*;
import java.util.Objects;

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

        SuspendContextImpl context = Objects.requireNonNull(action.getSuspendContext());
        JavaCodeFragmentFactory factory = JavaCodeFragmentFactory.getInstance(context.getDebugProcess().getProject());
        PsiElement fragment = factory.createExpressionCodeFragment(COLLECT_EXPRESSION, null, null, true);
        ExpressionEvaluator evaluator = ReadAction.computeBlocking(() ->
                uncheck(() -> EvaluatorBuilderImpl.getInstance().build(fragment, null)));
        StackFrameProxyImpl frame = context.getFrameProxy();
        EvaluationContextImpl evalContext = new EvaluationContextImpl(context, frame, (Value) null);
        uncheck(() -> evaluator.evaluate(evalContext));

        return true;
    }

    @Override
    protected Icon getVerifiedIcon(boolean isMuted) {
        return AllIcons.Actions.MenuSaveall;
    }
}
