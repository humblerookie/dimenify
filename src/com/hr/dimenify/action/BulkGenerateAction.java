package com.hr.dimenify.action;

import com.hr.dimenify.dialogs.BulkDimenDialog;
import com.hr.dimenify.model.Dimen;
import com.hr.dimenify.util.Constants;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static com.hr.dimenify.util.Constants.*;

public class BulkGenerateAction extends AbstractDimenAction {

    private static final String TAG = "BulkGenerateAction";

    int currentBucketIndex;

    PsiFile psiFile;
    XmlFile xmlFile;

    @Override
    public void actionPerformed(AnActionEvent e) {
        super.actionPerformed(e);
        psiFile = e.getData(LangDataKeys.PSI_FILE);

        if (psiFile instanceof XmlFile) {
            xmlFile = (XmlFile) psiFile;
            String folderName = psiFile.getParent().getName();
            String bucket = null;
            if (folderName.startsWith(VALUES_PREFIX)) {
                bucket = folderName.substring(VALUES_PREFIX.length());
            } else if (folderName.equalsIgnoreCase(VALUES_PREFIX.substring(0, VALUES_PREFIX.length() - 1))) {
                bucket = Constants.MDPI;
            }


            if (bucket != null) {
                currentBucketIndex = getBucketIndex(psiFile);
                showScaleDialog(bucket, currentBucketIndex != -1);

            }
        }

    }

    private String[] showScaleDialog(String bucket, boolean isAdded) {
        BulkDimenDialog bulkDimenDialog = new BulkDimenDialog(project, data);
        if (!isAdded) {
            bulkDimenDialog.prefillNewBucket(bucket);
        }
        bulkDimenDialog.show();
        int invalidIndex = bulkDimenDialog.invalidBucketIndex();
        if (bulkDimenDialog.isOK() && (invalidIndex == -1 || (invalidIndex == ERROR_CODE[1] && data.size() == Constants.MAX_DIMENS))) {
            saveValues(data);
            int index = indexOfBucket(data, bucket);
            if (index != -1) {
                currentBucketIndex = index;
                createDirectoriesAndFilesIfNeeded(psiFile.getParent().getParent(), Constants.Mode.BULK);
            } else {
                bulkDimenDialog.showAlert(MESSAGES.length);
            }
        } else if (bulkDimenDialog.isOK()) {
            bulkDimenDialog.showAlert(invalidIndex);
        }
        return null;
    }

    private int indexOfBucket(ArrayList<Dimen> data, String bucket) {
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).getBucket().equals(bucket)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void writeBulkValuesToFiles() {
        XmlTag[] dimens = getDimenValuesInFile(xmlFile);
        if (dimens != null && dimens.length > 0) {
            HashMap<String, Float>[] floatDimen = normalizeToHashMap(dimens, currentBucketIndex);
            writeScaledValuesToFiles(psiFile.getParent().getParent(), floatDimen);
        }

    }

    private HashMap<String, Float>[] normalizeToHashMap(XmlTag[] dimens, int bucketIndex) {
        HashMap<String, Float> dpHashMap = new HashMap<>();
        HashMap<String, Float> spHashMap = new HashMap<>();
        for (XmlTag tag : dimens) {
            String val = tag.getValue().getText().toString().toLowerCase();
            try {
                if (val.endsWith(Constants.DP)) {
                    dpHashMap.put(tag.getAttribute("name").getValue(),
                            Float.parseFloat(val.substring(0, val.length() - 2)) / data.get(bucketIndex).getFactorDp());
                } else if (val.endsWith(Constants.SP)) {
                    spHashMap.put(tag.getAttribute("name").getValue(),
                            Float.parseFloat(val.substring(0, val.length() - 2)) / data.get(bucketIndex).getFactorSp());
                }
            } catch (NumberFormatException | ArithmeticException e) {
                e.printStackTrace();
            }

        }
        return new HashMap[]{dpHashMap, spHashMap};
    }

    private XmlTag[] getDimenValuesInFile(XmlFile xmlFile) {
        XmlTag[] dimens = null;
        if (xmlFile.getDocument() != null && xmlFile.getDocument().getRootTag() != null) {
            String name = xmlFile.getDocument().getRootTag().getName();

            switch (name) {
                case "xml":
                    XmlTag resourcesTag = xmlFile.getDocument().getRootTag().findFirstSubTag("resources");
                    if (resourcesTag != null) {
                        dimens = resourcesTag.findSubTags("dimen");
                    }
                    break;
                case "resources":
                    dimens = xmlFile.getDocument().getRootTag().findSubTags("dimen");
                    break;

            }
        }
        return dimens;
    }

    protected void writeScaledValuesToFiles(PsiDirectory directory, HashMap<String, Float>[] floatDimen) {

        for (int i = 0; i < data.size(); i++) {
            if (i != currentBucketIndex && data.get(i).isSelected()) {
                PsiFile file = directory.findSubdirectory(data.get(i).getDirectory()).findFile(Constants.FILE_NAME);
                if (file instanceof XmlFile) {
                    XmlFile xmlFile = (XmlFile) file;
                    XmlTag[] tags = getDimenValuesInFile(xmlFile);
                    final int bucketIndex = i;
                    WriteCommandAction.runWriteCommandAction(project, new Runnable() {
                        @Override
                        public void run() {
                            StringBuilder stringBuilder = new StringBuilder();
                            Document document = PsiDocumentManager.getInstance(project).getDocument(file);
                            document.setReadOnly(false);
                            String text = document.getText();
                            int indexStart = text.indexOf("<resources");
                            if (indexStart != -1) {
                                int index = text.indexOf(">", indexStart) + 1;
                                stringBuilder.append(text.substring(0, index));
                                stringBuilder.append("\n");
                                Set<String> setDp = new HashSet<String>(floatDimen[0].keySet());
                                Set<String> setSp = new HashSet<String>(floatDimen[1].keySet());
                                for (int j = 0; tags != null && j < tags.length; j++) {
                                    XmlTag tag = tags[j];
                                    String name = tag.getAttribute("name").getValue();
                                    if (floatDimen[0].containsKey(name)) {
                                        String dimenFormattedString = MessageFormat.format(Constants.PLACEHOLDER_DIMEN, name
                                                , String.valueOf(floatDimen[0].get(name) * data.get(bucketIndex).getFactorDp())
                                                , Constants.DP);
                                        stringBuilder.append(dimenFormattedString);
                                        setDp.remove(name);
                                    } else if (floatDimen[1].containsKey(name)) {
                                        String dimenFormattedString = MessageFormat.format(Constants.PLACEHOLDER_DIMEN, name
                                                , String.valueOf(floatDimen[1].get(name) * data.get(bucketIndex).getFactorSp())
                                                , Constants.SP);
                                        stringBuilder.append(dimenFormattedString);
                                        setSp.remove(name);
                                    } else {
                                        String dimenFormattedString = MessageFormat.format(Constants.PLACEHOLDER_DIMEN, name
                                                , tag.getValue().getText().toString()
                                                , "");
                                        stringBuilder.append(dimenFormattedString);
                                    }
                                }

                                for (String name : setDp) {
                                    String dimenFormattedString = MessageFormat.format(Constants.PLACEHOLDER_DIMEN, name
                                            , String.valueOf(floatDimen[0].get(name) * data.get(bucketIndex).getFactorDp())
                                            , Constants.DP);
                                    stringBuilder.append(dimenFormattedString);
                                }
                                for (String name : setSp) {
                                    String dimenFormattedString = MessageFormat.format(Constants.PLACEHOLDER_DIMEN, name
                                            , String.valueOf(floatDimen[1].get(name) * data.get(bucketIndex).getFactorSp())
                                            , Constants.SP);
                                    stringBuilder.append(dimenFormattedString);
                                }

                                int suffixIndex = text.indexOf("</resources>");
                                if (suffixIndex != -1) {
                                    stringBuilder.append(text.substring(suffixIndex));
                                }
                                document.setText(stringBuilder.toString());
                            }
                        }
                    });
                }
            }
        }


    }
}

