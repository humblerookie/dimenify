package com.hr.dimenify.dialogs;

import com.hr.dimenify.model.Dimen;
import com.hr.dimenify.util.Constants;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.ui.components.JBScrollPane;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import static com.hr.dimenify.util.Constants.ERROR_CODE;
import static com.hr.dimenify.util.Constants.MESSAGES;
import static com.hr.dimenify.util.Constants.TITLE_SIZE;

public class BulkDimenDialog extends DialogWrapper {
    private JPanel controlPanel;
    private JBScrollPane scrollPane;
    private List<Component> bucketLabels = new ArrayList<>();
    private List<JCheckBox> selectionValues = new ArrayList<>();
    private List<JTextField> bucketScaleFactors = new ArrayList<>();
    private List<JButton> removeButtons = new ArrayList<>();
    private List<JLabel> labels = new ArrayList<>();
    private ArrayList<Dimen> data = new ArrayList<>();
    GroupLayout layout;

    LabeledComponent<JBScrollPane> component;


    public BulkDimenDialog(@Nullable Project project, ArrayList<Dimen> data) {
        super(project);
        this.data = data;
        setTitle(Constants.BULK_TITLE);
        initializePanel();
        init();
    }

    private void addInitialFields() {
        JLabel jLabel = new JLabel("Bucket");
        jLabel.setFont(jLabel.getFont().deriveFont(TITLE_SIZE));
        labels.add(jLabel);
        jLabel = new JLabel("DP");
        jLabel.setFont(jLabel.getFont().deriveFont(TITLE_SIZE));
        labels.add(jLabel);
        jLabel = new JLabel("SP");
        jLabel.setFont(jLabel.getFont().deriveFont(TITLE_SIZE));
        labels.add(jLabel);
        jLabel = new JLabel("Include");
        jLabel.setFont(jLabel.getFont().deriveFont(TITLE_SIZE));
        labels.add(jLabel);
        jLabel = new JLabel("");
        jLabel.setFont(jLabel.getFont().deriveFont(TITLE_SIZE));
        labels.add(jLabel);
        for (int i = 0; i < data.size(); i++) {
            final Dimen dimen = data.get(i);
            JLabel bucketLabel = new JLabel();
            bucketLabels.add(bucketLabel);
            bucketLabel.setText(dimen.getBucket());
            final JTextField scaleFactorDp = new JTextField();
            bucketScaleFactors.add(scaleFactorDp);
            scaleFactorDp.setColumns(6);
            scaleFactorDp.getDocument().addDocumentListener(new DocumentListener() {
                private void setData() {
                    float val = 0;
                    try {
                        val = Float.parseFloat(scaleFactorDp.getText());
                    } catch (NullPointerException | NumberFormatException ex) {

                    }
                    dimen.setFactorDp(val);
                }

                @Override
                public void insertUpdate(DocumentEvent e) {
                    setData();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    setData();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    setData();
                }
            });
            bucketScaleFactors.get(i * 2).setText(dimen.getFactorDp() + "");
            final JTextField scaleFactorSp = new JTextField();
            bucketScaleFactors.add(scaleFactorSp);
            scaleFactorSp.setColumns(6);
            scaleFactorSp.getDocument().addDocumentListener(new DocumentListener() {
                private void setData() {
                    float val = 0;
                    try {
                        val = Float.parseFloat(scaleFactorSp.getText());
                    } catch (NullPointerException | NumberFormatException ex) {

                    }
                    dimen.setFactorSp(val);
                }

                @Override
                public void insertUpdate(DocumentEvent e) {
                    setData();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    setData();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    setData();
                }
            });
            bucketScaleFactors.get(i * 2 + 1).setText(dimen.getFactorSp() + "");

            JCheckBox selectedCheckBox = new JCheckBox();
            selectedCheckBox.setSelected(dimen.isSelected());
            selectedCheckBox.addChangeListener(e -> dimen.setSelected(selectedCheckBox.isSelected()));
            if (!dimen.isMandatory()) {
                JButton removeButton = new JButton("-");
                removeButton.addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        removeLayoutValues(dimen, bucketLabel, scaleFactorDp, scaleFactorSp, selectedCheckBox, removeButton);
                        setLayoutConstraints();
                        layout.invalidateLayout(controlPanel);
                    }
                });
                removeButtons.add(removeButton);

            }
            selectionValues.add(selectedCheckBox);
        }

    }

    private void initializePanel() {
        controlPanel = new JPanel();
        scrollPane = new JBScrollPane(controlPanel, JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        addInitialFields();
        layout = new GroupLayout(controlPanel);
        controlPanel.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        setLayoutConstraints();
        scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> e.getAdjustable().setValue(e.getAdjustable().getMaximum()));
        component = LabeledComponent.create(scrollPane, "");
    }

    private void setLayoutConstraints() {
        GroupLayout.SequentialGroup hGroup = layout.createSequentialGroup();

        GroupLayout.ParallelGroup group1 = layout.createParallelGroup();
        GroupLayout.ParallelGroup group2 = layout.createParallelGroup();
        GroupLayout.ParallelGroup group3 = layout.createParallelGroup();
        GroupLayout.ParallelGroup group4 = layout.createParallelGroup();
        GroupLayout.ParallelGroup group5 = layout.createParallelGroup();
        group1.addComponent(labels.get(0));
        group2.addComponent(labels.get(1));
        group3.addComponent(labels.get(2));
        group4.addComponent(labels.get(3));
        if (removeButtons.size() > 0) {
            group5.addComponent(labels.get(4));
        }
        for (int i = 0; i < bucketLabels.size(); i++) {
            group1.addComponent(bucketLabels.get(i));
            group2.addComponent(bucketScaleFactors.get(2 * i));
            group3.addComponent(bucketScaleFactors.get(2 * i + 1));
            group4.addComponent(selectionValues.get(i));
            if (i < removeButtons.size()) {
                group5.addComponent(removeButtons.get(i));
            }
        }
        hGroup.addGroup(group1);
        hGroup.addGroup(group2);
        hGroup.addGroup(group3);
        hGroup.addGroup(group4);
        if (removeButtons.size() > 0) {
            hGroup.addGroup(group5);
        }
        layout.setHorizontalGroup(hGroup);

        GroupLayout.SequentialGroup vGroup = layout.createSequentialGroup();

        GroupLayout.ParallelGroup parallelGroup = layout.createParallelGroup(GroupLayout.Alignment.BASELINE);
        parallelGroup.addComponent(labels.get(0))
                .addComponent(labels.get(1))
                .addComponent(labels.get(2))
                .addComponent(labels.get(3));
        if (removeButtons.size() > 0) {
            parallelGroup.addComponent(labels.get(4));
        }
        vGroup.addGroup(parallelGroup);


        for (int i = 0, k = 0; i < bucketLabels.size(); i++) {

            parallelGroup = layout.createParallelGroup(GroupLayout.Alignment.BASELINE);
            parallelGroup.addComponent(bucketLabels.get(i)).addComponent(bucketScaleFactors.get(2 * i)).addComponent(bucketScaleFactors.get(2 * i + 1)).addComponent(selectionValues.get(i));
            if (!data.get(i).isMandatory()) {
                parallelGroup.addComponent(removeButtons.get(k++));
            }
            vGroup.addGroup(parallelGroup);
        }
        layout.setVerticalGroup(vGroup);

    }

    @NotNull
    @Override
    protected Action[] createActions() {
        Action[] actions = super.createActions();
        Action[] actionsAdd = new Action[actions.length + 1];
        for (int i = 0; i < actions.length; i++) {
            actionsAdd[i] = actions[i];
        }
        actionsAdd[actionsAdd.length - 1] = new DialogWrapperAction("Add Bucket") {
            @Override
            protected void doAction(ActionEvent actionEvent) {

                addNewBucket(null);

            }

        };
        return actionsAdd;
    }

    private void addNewBucket(String prefill) {
        int invalidIndex = invalidBucketIndex();
        if (invalidIndex == -1) {
            JTextField bucketValue = new JTextField();
            bucketValue.setColumns(15);
            bucketValue.setText(prefill == null ? Constants.DEFAULT_BUCKET : prefill);
            bucketValue.selectAll();


            bucketLabels.add(bucketValue);
            JTextField scaleFactorDp = new JTextField();
            scaleFactorDp.setColumns(6);
            scaleFactorDp.setText("1");
            bucketScaleFactors.add(scaleFactorDp);
            JTextField scaleFactorSp = new JTextField();
            scaleFactorSp.setColumns(6);
            scaleFactorSp.setText("1");
            bucketScaleFactors.add(scaleFactorSp);
            final JCheckBox selectedBucket = new JCheckBox();
            selectedBucket.setSelected(true);
            selectionValues.add(selectedBucket);
            Dimen dimen = new Dimen()
                    .setBucket(prefill == null ? Constants.DEFAULT_BUCKET : prefill)
                    .setFactorDp(Constants.DEFAULT_SCALE_FACTOR)
                    .setFactorSp(Constants.DEFAULT_SCALE_FACTOR)
                    .setMandatory(false)
                    .setSelected(true);
            data.add(dimen);
            bucketValue.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    dimen.setBucket(bucketValue.getText());

                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    dimen.setBucket(bucketValue.getText());
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    dimen.setBucket(bucketValue.getText());

                }
            });
            scaleFactorDp.getDocument().addDocumentListener(new DocumentListener() {
                private void setData() {
                    float val = 0;
                    try {
                        val = Float.parseFloat(scaleFactorDp.getText());
                    } catch (NullPointerException | NumberFormatException ex) {

                    }
                    if (val <= 0) {
                        val = 1;
                        scaleFactorDp.setText(val + "");
                    }

                    dimen.setFactorDp(val);
                }

                @Override
                public void insertUpdate(DocumentEvent e) {
                    setData();

                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    setData();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    setData();
                }
            });

            scaleFactorSp.getDocument().addDocumentListener(new DocumentListener() {
                private void setData() {
                    float val = 0;
                    try {
                        val = Float.parseFloat(scaleFactorSp.getText());
                    } catch (NullPointerException | NumberFormatException ex) {

                    }
                    if (val <= 0) {
                        val = 1;
                        scaleFactorSp.setText(val + "");
                    }
                    dimen.setFactorSp(val);

                }

                @Override
                public void insertUpdate(DocumentEvent e) {
                    setData();

                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    setData();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    setData();
                }
            });
            JButton removeButton = new JButton("-");
            removeButton.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    removeLayoutValues(dimen, bucketValue, scaleFactorDp, scaleFactorSp, selectedBucket, removeButton);
                    setLayoutConstraints();
                    layout.invalidateLayout(controlPanel);
                }
            });
            removeButtons.add(removeButton);
            setLayoutConstraints();
            layout.invalidateLayout(controlPanel);
        } else {
            showAlert(invalidIndex);
        }
    }

    private void removeLayoutValues(Dimen dimen, Component bucketValue, JTextField scaleFactorDp, JTextField scaleFactorSp, JCheckBox selectedBucket, JButton removeButton) {
        data.remove(dimen);
        bucketValue.setVisible(false);
        bucketLabels.remove(bucketValue);
        scaleFactorDp.setVisible(false);
        bucketScaleFactors.remove(scaleFactorDp);
        scaleFactorSp.setVisible(false);
        bucketScaleFactors.remove(scaleFactorSp);
        selectedBucket.setVisible(false);
        selectionValues.remove(selectedBucket);
        removeButton.setVisible(false);
        removeButtons.remove(removeButton);
        if (removeButtons.size() == 0) {
            labels.get(labels.size() - 1).setVisible(false);
        }
    }

    public int invalidBucketIndex() {
        HashMap<String, Boolean> containedBuckets = new HashMap<>();
        //Check for duplicates
        for (Dimen dimen : data) {
            if (containedBuckets.containsKey(dimen.getBucket())) {
                return ERROR_CODE[0];
            }
            containedBuckets.put(dimen.getBucket(), true);
        }

        return -1;
    }


    public void showAlert(int option) {
        JOptionPane optionPane = new JOptionPane(MESSAGES[option - 1], JOptionPane.WARNING_MESSAGE);
        JDialog dialog = optionPane.createDialog(Constants.ERROR_TITLE);
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return component;
    }

    public ArrayList<Dimen> getConversionValues() {
        return data;
    }

    public void prefillNewBucket(String prefill) {
        addNewBucket(prefill);
    }
}
