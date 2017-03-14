package com.hr.dimenify;

import com.intellij.ide.util.PropertiesComponent;
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

import javax.swing.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static com.hr.dimenify.Constants.*;


public class GenerateAction extends AnAction {

    AtomicInteger fileCreationCount = new AtomicInteger(0);
    int currentBucketIndex;
    Project project;
    String values[];

    @Override
    public void actionPerformed(AnActionEvent e) {
        Locale.setDefault(new Locale("pt","BR"));
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
            values = getInsertionValuesElement(psiElement);
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
        for (int i = 0; i < Constants.DIRECTORIES.length; i++) {
            if (psiDirectory.getName().equals(Constants.DIRECTORIES[i])) {
                return i;
            }
        }
        return 0;
    }

    private void writeScaledValuesToFiles(PsiDirectory directory, int currentBucketIndex, String[] values) {
        for (int i = 0; i < values.length; i++) {
            if (i != currentBucketIndex) {
                PsiFile file = directory.findSubdirectory(Constants.DIRECTORIES[i]).findFile(Constants.FILE_NAME);


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
                Constants.DIRECTORIES) {

            WriteCommandAction.runWriteCommandAction(project, () -> {
                PsiDirectory subDirectory = psiParent.findSubdirectory(directory);
                if (subDirectory == null) {
                    subDirectory = psiParent.createSubdirectory(directory);
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

    private void fileCreationCompleteAndCheck(PsiDirectory psiDirectory) {
        int value = fileCreationCount.incrementAndGet();
        if (value == Constants.DIRECTORIES.length) {
            writeScaledValuesToFiles(psiDirectory, currentBucketIndex, values);
        }
    }

    private String[] getInsertionValuesElement( PsiElement psiElement) {
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
                if (((XmlTagImpl) subNode).getName().equals(Constants.RESOURCES_TAG) && currentNode.getName().equals(Constants.DIMEN_TAG)) {
                    XmlAttributeImpl attribute = null;
                    XmlTextImpl value = null;
                    for (PsiElement element : currentNode.getChildren()) {
                        if (element instanceof XmlAttributeImpl && ((XmlAttributeImpl) element).getLocalName().equals(Constants.NAME_TAG)) {
                            attribute = (XmlAttributeImpl) element;
                        } else if (element instanceof XmlTextImpl) {
                            value = (XmlTextImpl) element;
                        }
                    }

                    if (attribute != null) {
                        String attributeName = attribute.getValue();
                        String val = value.getValue().toLowerCase().trim();

                        if (val.endsWith(Constants.DP) || val.endsWith(Constants.SP)) {

                            return showScaleDialog(attributeName,val,val.endsWith(Constants.DP));
                        }

                    }
                }
            }
            return null;

        } else {
            return null;
        }
    }

    private String[] showScaleDialog(String attributeName, String val, boolean isDp) {
        GenerateDialog generateDialog = new GenerateDialog(project,isDp);
        generateDialog.show();
        if(generateDialog.isOK()) {
            DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.getDefault());
            otherSymbols.setDecimalSeparator('.');
            otherSymbols.setGroupingSeparator(',');
            DecimalFormat formatter = new DecimalFormat("#0.0",otherSymbols);
           saveValues(generateDialog,isDp);

            float scaleFactor[] = new float[BUCKETS.length];
            for (int i = 0; i < BUCKETS.length; i++) {
                scaleFactor[i]=PropertiesComponent.getInstance().getFloat(SAVE_PREFIX+(isDp? DP:SP)+BUCKETS[i], 0);
            }
            float mdpiValue = Float.parseFloat(val.substring(0, val.length() - 2)) / scaleFactor[currentBucketIndex];
            float scaledValues[] = new float[scaleFactor.length];
            String elementsScaled[] = new String[scaleFactor.length];

            for (int i = 0; i < scaledValues.length; i++) {
                scaledValues[i] = mdpiValue * scaleFactor[i];
                elementsScaled[i] = MessageFormat.format(Constants.PLACEHOLDER_DIMEN, attributeName
                        , formatter.format(scaledValues[i])
                        , val.endsWith(Constants.DP) ? Constants.DP : Constants.SP);
            }
            return  elementsScaled;
        }
        return  null;
    }

    private void saveValues(GenerateDialog generateDialog, boolean isDp) {
        for (int i = 0; i < generateDialog.getTextFields().length; i++) {
            JTextField textField = generateDialog.getTextFields()[i];
            float factor=isDp? SCALE_FACTORS_DP[i]: SCALE_FACTORS_SP[i];
            if(textField.getText().trim().length() !=0) {
                try {
                    float f= Float.parseFloat(textField.getText().trim());
                    if(f >0) {
                        factor = f;
                    }
                }catch (NumberFormatException ex) {
                }
            }
            PropertiesComponent.getInstance().setValue(SAVE_PREFIX+(isDp? DP:SP)+BUCKETS[i], factor,-1);
        }
    }


}
