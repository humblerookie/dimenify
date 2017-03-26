package com.hr.dimenify.action;

import com.hr.dimenify.dialogs.GenerateDialog;
import com.hr.dimenify.model.Dimen;
import com.hr.dimenify.util.Constants;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.psi.PsiDirectory;
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
import java.util.ArrayList;
import java.util.Locale;


public class GenerateAction extends AbstractDimenAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        super.actionPerformed(e);
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        migrateData();
        Editor editor = PlatformDataKeys.EDITOR.getData(e.getDataContext());

        if (psiFile == null || editor == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        int offset = editor.getCaretModel().getOffset();
        PsiElement psiElement = psiFile.findElementAt(offset);
        if (psiFile.getFileType() == StdFileTypes.XML) {
            currentBucketIndex = getBucketIndex(psiFile);
            if (currentBucketIndex == -1) {
                if (data.size() == 10) {
                    showAlert(1);
                    return;
                }
                String directoryName = psiFile.getParent().getName();
                if (directoryName.startsWith(Constants.VALUES_PREFIX)) {
                    String bucket = directoryName.substring(Constants.VALUES_PREFIX.length());
                    String val = JOptionPane.showInputDialog(null, Constants.SCALE_TEXT_PREFIX + bucket, Constants.NEW_BUCKET, JOptionPane.INFORMATION_MESSAGE);
                    try {
                        float value = Float.parseFloat(val);
                        Dimen dimen = new Dimen().setBucket(bucket).setFactorDp(value).setFactorSp(value).setMandatory(false).setSelected(true);
                        data.add(dimen);
                        saveValues(data);
                        currentBucketIndex = data.size() - 1;
                    } catch (NullPointerException ex) {
                        return;
                    } catch (NumberFormatException ex) {
                        showAlert(3);
                        return;
                    }
                } else {
                    showAlert(2);
                    return;
                }

            }
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


    private String[] getInsertionValuesElement(PsiElement psiElement) {
        PsiElement selectedNode = psiElement;
        PsiElement rootParent;
        PsiElement subNode;
        if (psiElement != null && psiElement.getParent() != null && psiElement.getParent().getParent() != null) {
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

                            return showScaleDialog(attributeName, val, val.endsWith(Constants.DP));
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
        GenerateDialog generateDialog = new GenerateDialog(project, isDp, data);
        generateDialog.show();
        int invalidIndex = generateDialog.invalidBucketIndex();
        if (generateDialog.isOK() && invalidIndex == 0) {
            DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.getDefault());
            otherSymbols.setDecimalSeparator('.');
            otherSymbols.setGroupingSeparator(',');
            DecimalFormat formatter = new DecimalFormat("#0.0", otherSymbols);
            ArrayList<Dimen> data = generateDialog.getConversionValues();
            saveValues(data);


            float scaleFactor[] = new float[data.size()];
            for (int i = 0; i < data.size(); i++) {
                scaleFactor[i] = isDp ? data.get(i).getFactorDp() : data.get(i).getFactorSp();
            }
            float mdpiValue = 0;
            try {
                mdpiValue = Float.parseFloat(val.substring(0, val.length() - 2)) / scaleFactor[currentBucketIndex];
            } catch (NullPointerException | NumberFormatException e) {
                return null;
            }
            float scaledValues[] = new float[scaleFactor.length];
            String elementsScaled[] = new String[scaleFactor.length];

            for (int i = 0; i < scaledValues.length; i++) {
                scaledValues[i] = mdpiValue * scaleFactor[i];
                elementsScaled[i] = MessageFormat.format(Constants.PLACEHOLDER_DIMEN, attributeName
                        , formatter.format(scaledValues[i])
                        , val.endsWith(Constants.DP) ? Constants.DP : Constants.SP);
            }
            return elementsScaled;
        } else if (invalidIndex != 0) {
            generateDialog.showAlert(invalidIndex);
        }
        return null;
    }


}
