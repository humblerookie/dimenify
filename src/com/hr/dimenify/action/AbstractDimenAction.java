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
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import static com.hr.dimenify.util.Constants.MESSAGES;
import static com.hr.dimenify.util.Constants.MIGRATION_FLAG;
import static com.hr.dimenify.util.Constants.SAVE_PREFIX_V2;

public abstract class AbstractDimenAction extends AnAction {

    protected ArrayList<Dimen> data;
    protected Project project;
    protected AtomicInteger fileCreationCount = new AtomicInteger(0);
    protected int currentBucketIndex;
    protected PsiFile psiFile;
    protected XmlFile xmlFile;

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

    protected void createDirectoriesAndFilesIfNeeded(PsiDirectory psiParent) {
        for (Dimen datum : data) {
            if (datum.isSelected()) {
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
                        fileCreationCompleteAndCheck();

                    } else {
                        fileCreationCompleteAndCheck();
                    }
                });
            }
        }
    }

    protected void fileCreationCompleteAndCheck() {
        int value = fileCreationCount.incrementAndGet();
        if (value == getSelectedCount(data)) {
            calculateAndWriteScaledValueToFiles();
        }
    }

    private int getSelectedCount(ArrayList<Dimen> data) {
        int selected = 0;
        for (Dimen d : data) {
            if (d.isSelected()) {
                selected++;
            }
        }
        return selected;
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

    protected abstract void calculateAndWriteScaledValueToFiles();

    protected XmlTag[] getDimenValuesInFile(XmlFile xmlFile) {
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

    protected HashMap<String, Float>[] normalizeToHashMap(XmlTag[] dimens, int bucketIndex) {
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
                                                , getFormattedValue(floatDimen[0].get(name) * data.get(bucketIndex).getFactorDp())
                                                , Constants.DP);
                                        stringBuilder.append(dimenFormattedString);
                                        setDp.remove(name);
                                    } else if (floatDimen[1].containsKey(name)) {
                                        String dimenFormattedString = MessageFormat.format(Constants.PLACEHOLDER_DIMEN, name
                                                , getFormattedValue(floatDimen[1].get(name) * data.get(bucketIndex).getFactorSp())
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
                                            , getFormattedValue(floatDimen[0].get(name) * data.get(bucketIndex).getFactorDp())
                                            , Constants.DP);
                                    stringBuilder.append(dimenFormattedString);
                                }
                                for (String name : setSp) {
                                    String dimenFormattedString = MessageFormat.format(Constants.PLACEHOLDER_DIMEN, name
                                            , getFormattedValue(floatDimen[1].get(name) * data.get(bucketIndex).getFactorSp())
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

    protected String getFormattedValue(float v) {

        DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.getDefault());
        otherSymbols.setDecimalSeparator('.');
        otherSymbols.setGroupingSeparator(',');
        DecimalFormat formatter = new DecimalFormat("#.#", otherSymbols);
        formatter.setGroupingUsed(false);
        String s = formatter.format(v);
        float floatedValue = Float.parseFloat(s);
        int intValue = (int) floatedValue;
        return intValue == floatedValue ? String.valueOf(intValue) : s;

    }
}
