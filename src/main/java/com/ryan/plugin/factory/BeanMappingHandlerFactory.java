package com.ryan.plugin.factory;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.psi.PsiFile;
import com.ryan.plugin.enums.SupportedFileType;
import com.ryan.plugin.handlers.BeanMappingHandler;
import com.ryan.plugin.handlers.JavaBeanMappingHandler;


import java.util.HashMap;
import java.util.Map;

public class BeanMappingHandlerFactory {

    private static final Map<SupportedFileType, BeanMappingHandler> handlers = new HashMap<>();

    static {
        handlers.put(SupportedFileType.JAVA, new JavaBeanMappingHandler());
    }

    public static void process(AnActionEvent e) {
        PsiFile file = e.getData(PlatformDataKeys.PSI_FILE);
        if (file != null) {
            BeanMappingHandler handler = handlers.get(SupportedFileType.get(file.getFileType().getName()));
            if (handler != null) {
                handler.exec(e);
            }
        }
    }
}
