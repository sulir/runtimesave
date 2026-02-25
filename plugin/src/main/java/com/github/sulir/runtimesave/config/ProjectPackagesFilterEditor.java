package com.github.sulir.runtimesave.config;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.classFilter.ClassFilterEditor;

import javax.swing.*;
import java.util.ResourceBundle;

public class ProjectPackagesFilterEditor extends ClassFilterEditor {
    private static final ResourceBundle bundle = ResourceBundle.getBundle("messages.Bundle");

    private final Project project;

    public ProjectPackagesFilterEditor(Project project) {
        super(project);
        this.project = project;
        getEmptyText().setText(bundle.getString("sampling.include.default"));
    }

    @Override
    protected String getAddButtonText() {
        return "Add All Project Packages";
    }

    @Override
    protected Icon getAddButtonIcon() {
        return AllIcons.Nodes.Package;
    }

    @Override
    protected void addClassFilter() {
        PsiPackage root = JavaPsiFacade.getInstance(project).findPackage("");
        if (root != null)
            addPackages(root);
    }

    private void addPackages(PsiPackage pkg) {
        GlobalSearchScope inProject = GlobalSearchScope.projectScope(project);
        PsiClass[] classes = pkg.getClasses(inProject);

        if (pkg.getName() == null) {
            for (PsiClass clazz : classes)
                addPattern(clazz.getQualifiedName());
        } else if (classes.length > 0) {
            addPattern(pkg.getQualifiedName() + ".*");
            return;
        }

        for (PsiPackage sub : pkg.getSubPackages(inProject))
            addPackages(sub);
    }
}
