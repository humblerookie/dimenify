package com.hr.dimenify;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.xml.XmlAttributeImpl;
import com.intellij.psi.impl.source.xml.XmlDocumentImpl;
import com.intellij.psi.impl.source.xml.XmlTagImpl;
import com.intellij.psi.impl.source.xml.XmlTextImpl;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.concurrent.atomic.AtomicInteger;



public class GenerateAction extends AnAction {
    public static final String RESOURCES_TAG = "resources";
    public static final String RESOURCES_TEXT = "<resources>\n</resources>";
    public static final String DIMEN_TAG = "dimen";
    private static final String NAME_TAG = "name";
    private static final String PLACEHOLDER_DIMEN = "<dimen name=\"{0}\">{1}{2}</dimen>";
    private static final float SCALE_FACTORS_DP[] = {1.0f, 1.2f, 1.8f, 2.4f, 3.0f};
    private static final float SCALE_FACTORS_SP[] = {1.0f, 1.2f, 1.8f, 2.4f, 3.0f};
    public static final String FILE_NAME = "dimens.xml";
    public static final String DIRECTORIES[] = {"values", "values-hdpi", "values-xhdpi", "values-xxhdpi", "values-xxxhdpi"};
    private static final String DP = "dp";
    private static final String SP = "sp";


    AtomicInteger fileCreationCount = new AtomicInteger(0);
    int currentBucketIndex;
    Project project;
    String values[];

    @Override
    public void actionPerformed(AnActionEvent e) {

        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);

        project = e.getRequiredData(CommonDataKeys.PROJECT);
        Editor editor = PlatformDataKeys.EDITOR.getData(e.getDataContext());

        if (psiFile == null || editor == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        fileCreationCount.set(0);
        int offset = editor.getCaretModel().getOffset();
        PsiElement psiElement = psiFile.findElementAt(offset);
        if (psiFile.getFileType() == StdFileTypes.XML) {
            currentBucketIndex = getBucketIndex(psiFile);
            values = getInsertionValuesElement(currentBucketIndex, psiElement);
            if (values == null) {
                e.getPresentation().setEnabled(false);
                return;
            } else {
                if (psiFile.getParent().getParent() != null) {
                    PsiDirectory psiDirectory = psiFile.getParent().getParent();
                    createDirectoriesAndFilesIfNeeded(psiDirectory);

                }
            }
        } else {
            e.getPresentation().setEnabled(false);
            return;
        }
    }

    private int getBucketIndex(PsiFile psiFile) {

        PsiDirectory psiDirectory = psiFile.getParent();
        for (int i = 0; i < DIRECTORIES.length; i++) {
            if (psiDirectory.getName().equals(DIRECTORIES[i])) {
                return i;
            }
        }
        return 0;
    }

    private void writeScaledValuesToFiles(PsiDirectory directory, int currentBucketIndex, String[] values) {
        for (int i = 0; i < values.length; i++) {
            if (i != currentBucketIndex) {
                PsiFile file = directory.findSubdirectory(DIRECTORIES[i]).findFile(FILE_NAME);


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

    private void createDirectoriesAndFilesIfNeeded(PsiDirectory psiParent) {
        for (String directory :
                DIRECTORIES) {

            WriteCommandAction.runWriteCommandAction(project, () -> {
                PsiDirectory subDirectory = psiParent.findSubdirectory(directory);
                if (subDirectory == null) {
                    subDirectory = psiParent.createSubdirectory(directory);
                }
                PsiFile file = subDirectory.findFile(FILE_NAME);
                if (file == null) {
                    PsiFile psiFile = subDirectory.createFile(FILE_NAME);


                    Document document = PsiDocumentManager.getInstance(project).getDocument(psiFile);
                    document.setText(RESOURCES_TEXT);
                    fileCreationCompleteAndCheck(psiParent);


                } else {
                    fileCreationCompleteAndCheck(psiParent);
                }
            });
        }
    }

    private void fileCreationCompleteAndCheck(PsiDirectory psiDirectory) {
        int value = fileCreationCount.incrementAndGet();
        if (value == DIRECTORIES.length) {
            writeScaledValuesToFiles(psiDirectory, currentBucketIndex, values);
        }
    }

    private String[] getInsertionValuesElement(int currentBucketIndex, PsiElement psiElement) {
        PsiElement selectedNode = psiElement;
        PsiElement rootParent;
        PsiElement subNode;
        if (psiElement.getParent() != null && psiElement.getParent().getParent() != null) {
            subNode = psiElement.getParent();
            rootParent = subNode.getParent();

            while (!(rootParent instanceof XmlDocumentImpl)) {
                rootParent = rootParent.getParent();
                subNode = subNode.getParent();
                selectedNode = selectedNode.getParent();
            }

            if (subNode instanceof XmlTagImpl && selectedNode instanceof XmlTagImpl) {
                XmlTagImpl currentNode = (XmlTagImpl) selectedNode;
                if (((XmlTagImpl) subNode).getName().equals(RESOURCES_TAG) && currentNode.getName().equals(DIMEN_TAG)) {
                    XmlAttributeImpl attribute = null;
                    XmlTextImpl value = null;
                    for (PsiElement element : currentNode.getChildren()) {
                        if (element instanceof XmlAttributeImpl && ((XmlAttributeImpl) element).getLocalName().equals(NAME_TAG)) {
                            attribute = (XmlAttributeImpl) element;
                        } else if (element instanceof XmlTextImpl) {
                            value = (XmlTextImpl) element;
                        }
                    }

                    if (attribute != null ) {
                        String attributeName = attribute.getValue();
                        String val = value.getValue().toLowerCase().trim();

                        if(val.endsWith(DP) || val.endsWith(SP)) {

                            float scaleFactor[] = val.endsWith(DP) ? SCALE_FACTORS_DP : SCALE_FACTORS_SP;
                            float mdpiValue = Float.parseFloat(val.substring(0, val.length() - 2)) / scaleFactor[currentBucketIndex];
                            float scaledValues[] = new float[scaleFactor.length];
                            String elementsScaled[] = new String[scaleFactor.length];
                            NumberFormat formatter = new DecimalFormat("#0.0");
                            for (int i = 0; i < scaledValues.length; i++) {
                                scaledValues[i] = mdpiValue * scaleFactor[i];
                                elementsScaled[i] = MessageFormat.format(PLACEHOLDER_DIMEN, attributeName
                                        , formatter.format(scaledValues[i])
                                        , val.endsWith(DP) ? DP : SP);
                            }
                            return elementsScaled;
                        }

                    }
                }
            }
            return null;

        } else {
            return null;
        }
    }

}
