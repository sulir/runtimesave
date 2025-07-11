package com.github.sulir.runtimesave;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StartupActivity implements ProjectActivity {
    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Indexing Neo4j DB") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                RuntimeStorageService.getInstance().createIndexes(indicator::isCanceled);
            }
        });
        return null;
    }
}
