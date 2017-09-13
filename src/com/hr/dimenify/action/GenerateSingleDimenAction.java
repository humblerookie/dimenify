package com.hr.dimenify.action;

import com.hr.dimenify.dialogs.SingleDimenDialog;
import com.hr.dimenify.model.Dimen;
import com.hr.dimenify.util.Constants;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.xml.XmlAttributeImpl;
import com.intellij.psi.impl.source.xml.XmlDocumentImpl;
import com.intellij.psi.impl.source.xml.XmlTagImpl;
import com.intellij.psi.impl.source.xml.XmlTextImpl;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;

import static com.hr.dimenify.util.Constants.ERROR_CODE;


public class GenerateSingleDimenAction extends AbstractDimenAction {

    String attributeName;


    @Override
    public void actionPerformed(AnActionEvent e) {
        super.actionPerformed(e);
        psiFile = e.getData(LangDataKeys.PSI_FILE);
        Editor editor = PlatformDataKeys.EDITOR.getData(e.getDataContext());

        if (psiFile == null || editor == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        int offset = editor.getCaretModel().getOffset();
        PsiElement psiElement = psiFile.findElementAt(offset);
        if (psiFile.getFileType() == StdFileTypes.XML) {
            xmlFile = (XmlFile) psiFile;
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
            initialize(psiElement);
        } else {
            e.getPresentation().setEnabled(false);
            return;
        }
    }

    protected void calculateAndWriteScaledValueToFiles() {
        XmlTag[] dimens = getDimenValuesInFile(xmlFile);
        for (XmlTag tag : dimens) {
            try {
                if (tag.getAttribute("name").getValue().equalsIgnoreCase(attributeName)) {
                    dimens = new XmlTag[]{tag};
                    break;
                }
            } catch (Exception e) {
                return;
            }
        }

        if (dimens != null && dimens.length > 0) {
            HashMap<String, Float>[] floatDimen = normalizeToHashMap(dimens, currentBucketIndex);
            writeScaledValuesToFiles(psiFile.getParent().getParent(), floatDimen);
        }
    }


    private void initialize(PsiElement psiElement) {
        PsiElement selectedNode = psiElement;
        PsiElement rootParent;
        PsiElement subNode;
        if (psiElement != null && psiElement.getParent() != null && psiElement.getParent().getParent() != null) {
            subNode = psiElement.getParent();
            rootParent = subNode.getParent();

            while (!(rootParent instanceof XmlDocumentImpl) && (!(selectedNode instanceof XmlTagImpl) || !((XmlTagImpl) selectedNode).getName().equals(Constants.DIMEN_TAG))) {
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
                        attributeName = attribute.getValue();
                        String val = value.getValue().toLowerCase().trim();
                        if (val.endsWith(Constants.DP) || val.endsWith(Constants.SP)) {
                            showScaleDialog(val.endsWith(Constants.DP));
                        }

                    }
                }
            }

        }
    }

    private void showScaleDialog(boolean isDp) {
        SingleDimenDialog singleDimenDialog = new SingleDimenDialog(project, isDp, data);
        singleDimenDialog.show();
        int invalidIndex = singleDimenDialog.invalidBucketIndex();
        if (singleDimenDialog.isOK() && (invalidIndex == -1 || (invalidIndex == ERROR_CODE[1] && data.size() == Constants.MAX_DIMENS))) {

            ArrayList<Dimen> data = singleDimenDialog.getConversionValues();
            saveValues(data);
            createDirectoriesAndFilesIfNeeded(psiFile.getParent().getParent());

        } else if (singleDimenDialog.isOK()) {
            singleDimenDialog.showAlert(invalidIndex);
        }
    }


}
