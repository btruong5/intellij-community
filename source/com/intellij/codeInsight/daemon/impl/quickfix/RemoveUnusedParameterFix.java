package com.intellij.codeInsight.daemon.impl.quickfix;

import com.intellij.codeInsight.CodeInsightUtil;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.changeSignature.ChangeSignatureProcessor;
import com.intellij.refactoring.changeSignature.ParameterInfo;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class RemoveUnusedParameterFix implements IntentionAction {
  private final PsiParameter myParameter;

  public RemoveUnusedParameterFix(PsiParameter parameter) {
    myParameter = parameter;
  }

  public String getText() {
    final String text = MessageFormat.format("Remove Parameter ''{0}''", new Object[]{myParameter.getName(), });
    return text;
  }

  public String getFamilyName() {
    return "Remove unused parameter";
  }

  public boolean isAvailable(Project project, Editor editor, PsiFile file) {
    return
        myParameter.getManager().isInProject(myParameter)
        && myParameter != null
        && myParameter.isValid()
        && myParameter.getDeclarationScope() instanceof PsiMethod;
  }

  public void invoke(Project project, Editor editor, PsiFile file) {
    if (!CodeInsightUtil.prepareFileForWrite(myParameter.getContainingFile())) return;
    removeReferences(myParameter);
  }

  private static void removeReferences(PsiParameter parameter) {
    final PsiMethod method = (PsiMethod) parameter.getDeclarationScope();
    ChangeSignatureProcessor processor = new ChangeSignatureProcessor(parameter.getProject(),
                                                                      method,
        false, null,
        method.getName(),
        method.getReturnType(),
        getNewParametersInfo(method, parameter));

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      processor.testRun();
    }
    else {
      processor.run();
    }
  }

  public static ParameterInfo[] getNewParametersInfo(PsiMethod method, PsiParameter parameterToRemove) {
    List<ParameterInfo> result = new ArrayList<ParameterInfo>();
    final PsiParameter[] parameters = method.getParameterList().getParameters();
    for (int i = 0; i < parameters.length; i++) {
      PsiParameter parameter = parameters[i];
      if (!Comparing.equal(parameter, parameterToRemove)) {
        result.add(new ParameterInfo(i, parameter.getName(), parameter.getType()));
      }
    }
    return result.toArray(new ParameterInfo[result.size()]);
  }

  public boolean startInWriteAction() {
    return true;
  }


}
