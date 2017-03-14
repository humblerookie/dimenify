package com.hr.dimenify;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.LabeledComponent;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.hr.dimenify.Constants.*;

public class GenerateDialog extends DialogWrapper {

    private List<Field> fields;
    private JPanel controlPanel;
    private JLabel labels[];
    private JTextField textFields[];

    LabeledComponent<JPanel> component;
    protected GenerateDialog(@Nullable Project project, boolean isDp) {
        super(project);

        setTitle(Constants.TITLE);
        fields = new ArrayList();
        labels= new JLabel[BUCKETS.length];
        textFields= new JTextField[BUCKETS.length];
        initializePanel(isDp);
        init();
    }

    private void initializePanel(boolean isDp) {
        controlPanel= new JPanel();
        GroupLayout layout = new GroupLayout(controlPanel);
        controlPanel.setLayout(layout);
        // Turn on automatically adding gaps between components
        layout.setAutoCreateGaps(true);
        // Create a sequential group for the horizontal axis.
        GroupLayout.SequentialGroup hGroup = layout.createSequentialGroup();
        GroupLayout.Group yLabelGroup = layout.createParallelGroup(GroupLayout.Alignment.TRAILING);
        hGroup.addGroup(yLabelGroup);
        GroupLayout.Group yFieldGroup = layout.createParallelGroup();
        hGroup.addGroup(yFieldGroup);
        layout.setHorizontalGroup(hGroup);
        // Create a sequential group for the vertical axis.
        GroupLayout.SequentialGroup vGroup = layout.createSequentialGroup();
        layout.setVerticalGroup(vGroup);

        int preferredSize = GroupLayout.PREFERRED_SIZE;
        // add the components to the group

        for (int i = 0; i < BUCKETS.length; i++) {
            String bucket= BUCKETS[i];
            labels[i]= new JLabel();
            labels[i].setText(bucket);
            textFields[i]= new JTextField();
            textFields[i].setColumns(20);
            float value = PropertiesComponent.getInstance().getFloat(SAVE_PREFIX+(isDp? DP:SP)+BUCKETS[i],isDp? SCALE_FACTORS_DP[i] :SCALE_FACTORS_SP[i]);
            textFields[i].setText(value+"");

            yLabelGroup.addComponent(labels[i]);
            yFieldGroup.addComponent(textFields[i],preferredSize,preferredSize,preferredSize);
        }

        for (int ii = 0; ii < labels.length; ii++) {
            vGroup.addGroup(layout.createParallelGroup().
                    addComponent(labels[ii]).
                    addComponent(textFields[ii], preferredSize, preferredSize, preferredSize));
        }
        component = LabeledComponent.create(controlPanel,"");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return component;
    }

    public JTextField[] getTextFields() {
        return textFields;
    }
}
