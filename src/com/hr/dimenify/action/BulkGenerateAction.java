package com.hr.dimenify.action;

import com.hr.dimenify.dialogs.BulkDimenDialog;
import com.hr.dimenify.model.Dimen;
import com.hr.dimenify.util.Constants;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;

import java.util.ArrayList;
import java.util.HashMap;

import static com.hr.dimenify.util.Constants.*;

public class BulkGenerateAction extends AbstractDimenAction {

    private static final String TAG = "BulkGenerateAction";

    @Override
    public void actionPerformed(AnActionEvent e) {
        super.actionPerformed(e);
        psiFile = e.getData(LangDataKeys.PSI_FILE);

        if (psiFile.getFileType() == StdFileTypes.XML) {
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
                createDirectoriesAndFilesIfNeeded(psiFile.getParent().getParent());
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
    protected void calculateAndWriteScaledValueToFiles() {
        XmlTag[] dimens = getDimenValuesInFile(xmlFile);
        if (dimens != null && dimens.length > 0) {
            HashMap<String, Float>[] floatDimen = normalizeToHashMap(dimens, currentBucketIndex);
            writeScaledValuesToFiles(psiFile.getParent().getParent(), floatDimen);
        }

    }


}

