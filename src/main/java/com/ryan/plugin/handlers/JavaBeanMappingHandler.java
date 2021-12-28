package com.ryan.plugin.handlers;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTypesUtil;
import com.ryan.plugin.utils.ClipboardUtil;
import com.ryan.plugin.utils.IntellijNotifyUtil;
import com.ryan.plugin.utils.StringUtil;
import com.ryan.plugin.utils.TabUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class JavaBeanMappingHandler implements BeanMappingHandler {

    private static final String INFO_MSG = "PrettyBeanTool : 已复制到粘贴板 (Copied to pasteboard) !";
    private static final String ERROR_MSG = "PrettyBeanTool : 当前所选对象不支持 (The currently selected object is not supported)";
    private Boolean needNotify = false;

    @Override
    public void exec(AnActionEvent e) {

        PsiElement element = e.getData(LangDataKeys.PSI_ELEMENT);

        if (element instanceof PsiMethod) {

            doPsiMethod((PsiMethod) element);

        } else if (element instanceof PsiClass) {

            doPsiClass((PsiClass) element, StringUtils.EMPTY);
            needNotify = true;

        } else if (element instanceof PsiLocalVariable) {

            doPsiLocalVariable((PsiLocalVariable) element);
            needNotify = true;

        } else {

            doErrorNotify(e.getProject());
        }

        if (needNotify) {
            doInfoNotify(e.getProject());
        }
    }

    private void doPsiMethod(PsiMethod psiMethod) {
        if (isNotAvailablePsiMethod(psiMethod)) {
            return;
        }
        Map<String, Pair<String, String>> fromFieldMap = new HashedMap();
        Arrays.stream(psiMethod.getParameterList().getParameters()).forEach(e -> {
            PsiClass from = PsiTypesUtil.getPsiClass(e.getType());
            Arrays.stream(from.getAllMethods()).filter(m -> m.getName().startsWith("get")).forEach(
                    x -> fromFieldMap.put(StringUtil.getFieldName(x.getName()), Pair.of(x.getName(), e.getName())));
        });
        PsiClass to = PsiTypesUtil.getPsiClass(psiMethod.getReturnType());
        StringBuilder context = new StringBuilder();

        if (Arrays.stream(to.getInnerClasses()).anyMatch(c -> c.getName().contains("Builder"))) {
            // builder model
            context.append(TabUtil.getDoubleTabSpace()).append("return ").append(to.getName()).append(".builder()\n");
            Arrays.stream(to.getAllFields()).forEach(f -> {
                Pair<String, String> pair = fromFieldMap.get(f.getName());
                context.append(TabUtil.getDoubleTabSpace()).append(TabUtil.getDoubleTabSpace())
                        .append(".").append(f.getName()).append("(");
                if (pair != null) {
                    context.append(pair.getRight()).append(".").append(pair.getLeft()).append("()");
                }
                context.append(")\n");
            });
            context.append(TabUtil.getDoubleTabSpace()).append(TabUtil.getDoubleTabSpace()).append(".build();");

        } else {
            // set model
            String localVar = StringUtil.captureName(to.getName());
            context.append(TabUtil.getDoubleTabSpace()).append(TabUtil.getDoubleTabSpace())
                    .append(to.getName()).append(" ").append(localVar)
                    .append(" = new ").append(to.getName()).append("();\n");
            Arrays.stream(to.getAllFields()).forEach(f -> {
                Pair<String, String> pair = fromFieldMap.get(f.getName());
                if (pair != null) {
                    context.append(TabUtil.getDoubleTabSpace()).append(TabUtil.getDoubleTabSpace())
                            .append(localVar).append(".").append("set")
                            .append(String.valueOf(f.getName().charAt(0)).toUpperCase())
                            .append(f.getName().substring(1))
                            .append("(").append(pair.getRight()).append(".").append(pair.getLeft()).append("());\n");
                }
            });
            context.append(TabUtil.getDoubleTabSpace()).append("return ").append(localVar).append(";");
        }

        Document document = FileDocumentManager.getInstance()
                .getDocument(psiMethod.getContainingFile().getVirtualFile());
        WriteCommandAction.runWriteCommandAction(psiMethod.getProject(),
                () -> document.insertString(psiMethod.getBody().getTextOffset() + 1, "\n" + context));
    }

    private void doPsiClass(PsiClass psiClass, String localVar) {
        if (isNotAvailablePsiClass(psiClass)) {
            return;
        }

        doSetField(psiClass, localVar);

        Arrays.stream(psiClass.getInnerClasses()).forEach(c -> {
            if (c.getName().endsWith("Builder")) {
                doBuildField(c, psiClass.getName());
            }
        });
    }

    private void doBuildField(PsiClass psiClass, String className) {

        StringBuilder context = new StringBuilder();
        context.append("return ").append(className).append(".builder()\n");

        Arrays.stream(psiClass.getAllFields()).forEach(f -> context.append(".").append(f.getName()).append("()\n"));

        context.append(".build();");
        ClipboardUtil.setClipboard(context.toString());
    }

    private void doSetField(PsiClass psiClass, String localVar) {

        StringBuilder context = new StringBuilder();
        if (StringUtils.isBlank(localVar)) {
            localVar = String.valueOf(psiClass.getName().charAt(0)).toLowerCase() + psiClass.getName().substring(1);
            context.append(psiClass.getName()).append(" ").append(localVar).append(" = new ")
                    .append(psiClass.getName()).append("();\n");
        }

        String var = localVar;
        Arrays.stream(psiClass.getAllMethods()).forEach(m -> {
            if (m.getName().startsWith("set")) {
                context.append(TabUtil.getTabSpace()).append(var).append(".").append(m.getName()).append("();\n");
            }
        });

        ClipboardUtil.setClipboard(context.toString());
    }

    private void doPsiLocalVariable(PsiLocalVariable psiVariable) {
        PsiElement element = psiVariable.getParent();
        if (element instanceof PsiClass) {
            doPsiClass((PsiClass) element, psiVariable.getName());
        } else {
            doErrorNotify(psiVariable.getProject());
        }
    }

    private boolean isNotAvailablePsiClass(PsiClass psiClass) {
        if (psiClass.isInterface()) {
            doErrorNotify(psiClass.getProject());
            return true;
        }
        return false;
    }

    private boolean isNotAvailablePsiMethod(PsiMethod psiMethod) {
        if (psiMethod.getParameterList().isEmpty() || PsiType.VOID.equals(psiMethod.getReturnType())) {
            doErrorNotify(psiMethod.getProject());
            return true;
        }
        return false;
    }

    private void doInfoNotify(Project project) {
        IntellijNotifyUtil.notifyInfo(project, INFO_MSG);
    }

    private void doErrorNotify(Project project) {
        IntellijNotifyUtil.notifyError(project, ERROR_MSG);
    }


}
