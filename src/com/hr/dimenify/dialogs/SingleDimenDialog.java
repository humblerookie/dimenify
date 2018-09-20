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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import static com.hr.dimenify.util.Constants.ERROR_CODE;
import static com.hr.dimenify.util.Constants.MESSAGES;

public class SingleDimenDialog extends DialogWrapper {
    private JPanel controlPanel;
    private JBScrollPane scrollPane;
    private List<Component> bucketLabels = new ArrayList<>();
    private List<JCheckBox> selectionValues = new ArrayList<>();
    private List<JTextField> bucketScaleFactors = new ArrayList<>();
    private List<JButton> removeButtons = new ArrayList<>();
    private ArrayList<Dimen> data;
    GroupLayout layout;

    LabeledComponent<JBScrollPane> component;

    boolean isDp;

    public SingleDimenDialog(@Nullable Project project, boolean isDp, ArrayList<Dimen> data) {
        super(project);
        this.isDp = isDp;
        this.data = data;
        setTitle(Constants.TITLE + (isDp ? Constants.DP : Constants.SP) + Constants.METRIC);
        initializePanel(isDp);
        init();
    }

    private void addInitialFields(boolean isDp) {
        for (int i = 0; i < data.size(); i++) {
            final Dimen dimen = data.get(i);
            JLabel bucketLabel = new JLabel();
            bucketLabels.add(bucketLabel);
            bucketLabel.setText(dimen.getBucket());
            final JTextField scaleFactor = new JTextField();
            bucketScaleFactors.add(scaleFactor);
            scaleFactor.setColumns(20);
            scaleFactor.getDocument().addDocumentListener(new DocumentListener() {
                private void setData() {
                    float val = 0;
                    try {
                        val = Float.parseFloat(scaleFactor.getText());
                    } catch (NullPointerException | NumberFormatException ex) {

                    }
                    if (isDp) {
                        dimen.setFactorDp(val);

                    } else {
                        dimen.setFactorSp(val);
                    }
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
            bucketScaleFactors.get(i).setText((isDp ? dimen.getFactorDp() : dimen.getFactorSp()) + "");

            JCheckBox selectedCheckBox = new JCheckBox();
            selectedCheckBox.setSelected(dimen.isSelected());
            selectedCheckBox.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    dimen.setSelected(selectedCheckBox.isSelected());
                }
            });
            if (!dimen.isMandatory()) {
                JButton removeButton = new JButton("-");
                removeButton.addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        removeLayoutValues(dimen, bucketLabel, scaleFactor, selectedCheckBox, removeButton);
                        setLayoutConstraints();
                        layout.invalidateLayout(controlPanel);
                    }
                });
                removeButtons.add(removeButton);

            }
            selectionValues.add(selectedCheckBox);
        }

    }

    private void initializePanel(boolean isDp) {
        controlPanel = new JPanel();
        scrollPane = new JBScrollPane(controlPanel, JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        addInitialFields(isDp);
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
        for (int i = 0; i < bucketLabels.size(); i++) {
            group1.addComponent(bucketLabels.get(i));
            group2.addComponent(bucketScaleFactors.get(i));
            group3.addComponent(selectionValues.get(i));
            if (i < removeButtons.size()) {

                group4.addComponent(removeButtons.get(i));
            }
        }
        hGroup.addGroup(group1);
        hGroup.addGroup(group2);
        hGroup.addGroup(group3);
        if (removeButtons.size() > 0) {
            hGroup.addGroup(group4);
        }
        layout.setHorizontalGroup(hGroup);

        GroupLayout.SequentialGroup vGroup = layout.createSequentialGroup();
        for (int i = 0, k = 0; i < bucketLabels.size(); i++) {
            GroupLayout.ParallelGroup parallelGroup = layout.createParallelGroup(GroupLayout.Alignment.BASELINE);
            parallelGroup.addComponent(bucketLabels.get(i)).addComponent(bucketScaleFactors.get(i)).addComponent(selectionValues.get(i));
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

                int invalidIndex = invalidBucketIndex();
                if (invalidIndex == -1) {
                    JTextField bucketValue = new JTextField();
                    bucketValue.setColumns(15);
                    bucketValue.setText(Constants.DEFAULT_BUCKET);
                    bucketValue.selectAll();

                    bucketLabels.add(bucketValue);
                    JTextField scaleFactor = new JTextField();
                    scaleFactor.setColumns(20);
                    scaleFactor.setText("1");
                    bucketScaleFactors.add(scaleFactor);
                    final JCheckBox selectedBucket = new JCheckBox();
                    selectedBucket.setSelected(true);
                    selectionValues.add(selectedBucket);
                    Dimen dimen = new Dimen()
                            .setBucket(Constants.DEFAULT_BUCKET)
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
                    scaleFactor.getDocument().addDocumentListener(new DocumentListener() {
                        private void setData() {
                            float val = 0;
                            try {
                                val = Float.parseFloat(scaleFactor.getText());
                            } catch (NullPointerException | NumberFormatException ex) {

                            }
                            if (val <= 0) {
                                val = 1;
                                scaleFactor.setText(val + "");
                            }

                            dimen.setFactorDp(val);
                            if (isDp) {
                                dimen.setFactorDp(val);

                            } else {
                                dimen.setFactorSp(val);
                            }
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
                            removeLayoutValues(dimen, bucketValue, scaleFactor, selectedBucket, removeButton);
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

        };
        return actionsAdd;
    }

    private void removeLayoutValues(Dimen dimen, Component bucketValue, JTextField scaleFactor, JCheckBox selectedBucket, JButton removeButton) {
        data.remove(dimen);
        bucketValue.setVisible(false);
        bucketLabels.remove(bucketValue);
        scaleFactor.setVisible(false);
        bucketScaleFactors.remove(scaleFactor);
        selectedBucket.setVisible(false);
        selectionValues.remove(selectedBucket);
        removeButton.setVisible(false);
        removeButtons.remove(removeButton);
    }

    public int invalidBucketIndex() {
        HashMap<String, Boolean> containedBuckets = new HashMap<>();
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
}
