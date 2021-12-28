package com.ryan.plugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.ryan.plugin.checker.BeanMappingChecker;
import com.ryan.plugin.factory.BeanMappingHandlerFactory;
import org.jetbrains.annotations.NotNull;

public class BeanMappingAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        // check
        if (!BeanMappingChecker.isSupport(e)) {
            return;
        }
        BeanMappingHandlerFactory.process(e);

    }

}
