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
import com.intellij.openapi.vfs.VirtualFile;
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
        boolean hasZeroValues = false;
        for (Dimen datum : data) {
            if (datum.getFactorSp() == 0) {
                datum.setFactorSp(1);
                hasZeroValues = true;
            }

            if (datum.getFactorDp() == 0) {
                datum.setFactorDp(1);
                hasZeroValues = true;
            }

        }
        if (hasZeroValues) {
            saveValues(data);
        }
        fileCreationCount.set(0);
        migrateData();
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
        JOptionPane optionPane = new JOptionPane(MESSAGES[errorIndex], JOptionPane.WARNING_MESSAGE);
        JDialog dialog = optionPane.createDialog(Constants.ERROR_TITLE);
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true);
    }

    protected void createDirectoriesAndFilesIfNeeded(PsiDirectory psiParent, Mode mode) {
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
                    fileCreationCompleteAndCheck(psiParent, mode);

                } else {
                    fileCreationCompleteAndCheck(psiParent, mode);
                }
            });
        }
    }

    protected void fileCreationCompleteAndCheck(PsiDirectory psiDirectory, Mode mode) {
        int value = fileCreationCount.incrementAndGet();
        if (value == data.size()) {
            if (mode == Mode.SINGLE) {
                writeScaledValuesToFiles(psiDirectory, currentBucketIndex, values);
            } else if (mode == Mode.BULK) {
                writeBulkValuesToFiles();
            }
        }
    }

    @Override
    public void update(AnActionEvent e) {
        final VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(e.getDataContext());
        final boolean isDimensXml = isDimenFile(file);
        e.getPresentation().setEnabled(isDimensXml);
        e.getPresentation().setVisible(isDimensXml);
    }

    private boolean isDimenFile(VirtualFile file) {
        return file != null && file.getName().endsWith(".xml") && file.getParent().getName().startsWith("values");
    }

    protected abstract void writeBulkValuesToFiles();

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
                        int indexStart = text.indexOf("<resources");
                        if (indexStart != -1) {
                            int index = text.indexOf(">", indexStart) + 1;
                            StringBuilder stringBuilder = new StringBuilder(text);
                            document.setText(stringBuilder.insert(index, "\n" + values[x]).toString());
                        }
                    }
                });


            }
        }
    }
}
