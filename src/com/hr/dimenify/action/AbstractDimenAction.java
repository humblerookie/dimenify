package com.hr.dimenify.action;

import com.hr.dimenify.model.Dimen;
import com.hr.dimenify.util.Constants;
import com.hr.dimenify.util.ModelUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static com.hr.dimenify.util.Constants.*;

public abstract class AbstractDimenAction extends AnAction {

    protected ArrayList<Dimen> data;
    protected Project project;
    protected String values[];
    protected AtomicInteger fileCreationCount = new AtomicInteger(0);
    protected int currentBucketIndex;

    @Override
    public void actionPerformed(AnActionEvent e) {
        Locale.setDefault(new Locale("pt", "BR"));
        project = e.getRequiredData(CommonDataKeys.PROJECT);
        data = ModelUtil.fromJson(PropertiesComponent.getInstance().getValue(Constants.SAVE_PREFIX_V2, Constants.INIT_MODEL_JSON));
        fileCreationCount.set(0);
    }

    protected void migrateData() {
        if (!PropertiesComponent.getInstance().getBoolean(MIGRATION_FLAG)) {
            for (Dimen datum : data) {
                datum.setFactorDp(PropertiesComponent.getInstance().getFloat(Constants.SAVE_PREFIX + Constants.DP + datum.getBucket(), datum.getFactorDp()));
                datum.setFactorSp(PropertiesComponent.getInstance().getFloat(Constants.SAVE_PREFIX + Constants.SP + datum.getBucket(), datum.getFactorSp()));
            }
            PropertiesComponent.getInstance().setValue(MIGRATION_FLAG, true);
        }
        saveValues(data);
    }

    protected void saveValues(ArrayList<Dimen> data) {
        String value = ModelUtil.toJson(data);
        PropertiesComponent.getInstance().setValue(SAVE_PREFIX_V2, value);
    }


    protected int getBucketIndex(PsiFile psiFile) {

        PsiDirectory psiDirectory = psiFile.getParent();

        String value = PropertiesComponent.getInstance().getValue(SAVE_PREFIX_V2, Constants.INIT_MODEL_JSON);
        ArrayList<Dimen> data = ModelUtil.fromJson(value);
        for (int i = 0; i < data.size(); i++) {
            if (psiDirectory.getName().equals(data.get(i).getDirectory())) {
                return i;
            }
        }
        return -1;
    }

    protected void showAlert(int errorIndex) {
        JOptionPane optionPane = new JOptionPane(ERROR_MESSAGES[errorIndex], JOptionPane.WARNING_MESSAGE);
        JDialog dialog = optionPane.createDialog(Constants.ERROR_TITLE);
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true);
    }

    protected void createDirectoriesAndFilesIfNeeded(PsiDirectory psiParent) {
        for (Dimen datum : data) {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                PsiDirectory subDirectory = psiParent.findSubdirectory(datum.getDirectory());
                if (subDirectory == null) {
                    subDirectory = psiParent.createSubdirectory(datum.getDirectory());
                }
                PsiFile file = subDirectory.findFile(Constants.FILE_NAME);
                if (file == null) {
                    PsiFile psiFile = subDirectory.createFile(Constants.FILE_NAME);


                    Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
                    document.setText(Constants.RESOURCES_TEXT);
                    fileCreationCompleteAndCheck(psiParent);


                } else {
                    fileCreationCompleteAndCheck(psiParent);
                }
            });
        }
    }

    protected void fileCreationCompleteAndCheck(PsiDirectory psiDirectory) {
        int value = fileCreationCount.incrementAndGet();
        if (value == data.size()) {
            writeScaledValuesToFiles(psiDirectory, currentBucketIndex, values);
        }
    }

    protected void writeScaledValuesToFiles(PsiDirectory directory, int currentBucketIndex, String[] values) {
        for (int i = 0; i < values.length; i++) {
            if (i != currentBucketIndex && data.get(i).isSelected()) {
                PsiFile file = directory.findSubdirectory(data.get(i).getDirectory()).findFile(Constants.FILE_NAME);


                final int x = i;
                WriteCommandAction.runWriteCommandAction(project, new Runnable() {
                    @Override
                    public void run() {
                        Document document = PsiDocumentManager.getInstance(project).getDocument(file);
                        document.setReadOnly(false);
                        String text = document.getText();
                        int index = text.indexOf(">") + 1;
                        StringBuilder stringBuilder = new StringBuilder(text);
                        document.setText(stringBuilder.insert(index, "\n" + values[x]).toString());
                    }
                });


            }
        }
    }
}
