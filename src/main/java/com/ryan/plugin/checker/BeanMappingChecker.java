package com.ryan.plugin.checker;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import com.ryan.plugin.enums.SupportedFileType;
import org.apache.commons.lang3.StringUtils;

public class BeanMappingChecker {

    public static Boolean isSupport(AnActionEvent e) {

        PsiFile file = e.getData(PlatformDataKeys.PSI_FILE);
        if (file == null || !SupportedFileType.contains(file.getFileType().getName())) {
            return false;
        }

        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        if (editor == null) {
            return false;
        }

        String selectedText = editor.getSelectionModel().getSelectedText(true);

        return !StringUtils.isBlank(selectedText);
    }
}
