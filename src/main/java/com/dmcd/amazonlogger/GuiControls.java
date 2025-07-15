/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dmcd.amazonlogger;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;

/**
 *
 * @author dan
 */
public class GuiControls {
    /*
    * This class is a library of functions that handle creating the individual
    *  controls for the GUI.
    */
  
    final static private int GAPSIZE = 4; // gap size to place on each side of each widget

    public enum Orient { NONE, LEFT, RIGHT, CENTER }

    private static JFrame         mainFrame;
    private static GridBagLayout  mainLayout;
    private static Dimension      framesize;
    private final static HashMap<String, JPanel>       gPanel = new HashMap();
    private final static HashMap<String, JScrollPane>  gScrollPanel = new HashMap();
    private final static HashMap<String, JTabbedPane>  gTabbedPanel = new HashMap();
    private final static HashMap<String, JTextPane>    gTextPane = new HashMap();
    private final static HashMap<String, JList>        gList = new HashMap();
    private final static HashMap<String, JLabel>       gLabel = new HashMap();
    private final static HashMap<String, JButton>      gButton = new HashMap();
    private final static HashMap<String, JCheckBox>    gCheckbox = new HashMap();
    private final static HashMap<String, JComboBox>    gCombobox = new HashMap();
    private final static HashMap<String, JTextField>   gTextField = new HashMap();
    private final static HashMap<String, JRadioButton> gRadiobutton = new HashMap();
    private final static HashMap<String, JSpinner>     gSpinner = new HashMap();

    public GuiControls() {
        mainFrame = null;
        mainLayout = null;
    }

    public GuiControls(String title, int height, int width) {
        framesize = new Dimension(height, width);
        mainFrame = new JFrame(title);
        mainFrame.setSize(framesize);
        mainFrame.setMinimumSize(framesize);

        // setup the layout for the frame
        mainLayout = new GridBagLayout();
        mainFrame.setFont(new Font("SansSerif", Font.PLAIN, 14));
        mainFrame.setLayout(this.mainLayout);
    }

    public static void newFrame(String title, int width, int height, boolean fixed) {
        if (mainFrame != null) {
            return;
        }
        framesize = new Dimension (width, height);
        mainFrame = new JFrame(title);
        mainFrame.setSize(framesize);
        mainFrame.setMinimumSize(framesize);
        if (fixed) {
            mainFrame.setMaximumSize(framesize);
        }

        // setup the layout for the frame
        mainLayout = new GridBagLayout();
        mainFrame.setFont(new Font("SansSerif", Font.PLAIN, 14));
        mainFrame.setLayout(mainLayout);
    }

    public static void display() {
        if (mainFrame != null) {
            mainFrame.pack();
            mainFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            mainFrame.setLocationRelativeTo(null);
            mainFrame.setVisible(true);
        }
    }

    public static void update() {
        if (mainFrame != null) {
            mainFrame.repaint();
        }
    }

    public static void repack() {
        if (mainFrame != null) {
            mainFrame.pack();
            mainFrame.setSize(framesize);
        }
    }

    public static void close() {
        gPanel.clear();
        gScrollPanel.clear();
        gTabbedPanel.clear();
        gTextPane.clear();
        gList.clear();
        gLabel.clear();
        gButton.clear();
        gCheckbox.clear();
        gCombobox.clear();
        gTextField.clear();
        gRadiobutton.clear();
        gSpinner.clear();

        if (mainFrame != null) {
            mainFrame.dispose();
            mainFrame = null;
        }
    }

    public static JFrame getFrame() {
        return mainFrame;
    }

    public static boolean isValidFrame() {
        return mainFrame != null;
    }

    public static JPanel getPanel(String name) {
        JPanel panel = null;
        if (gPanel != null) {
            panel = gPanel.get(name);
        }
        return panel;
    }

    public static JScrollPane getScrollPanel(String name) {
        JScrollPane panel = null;
        if (gScrollPanel != null) {
            panel = gScrollPanel.get(name);
        }
      return panel;
    }

    public static JTabbedPane getTabbedPanel(String name) {
        return gTabbedPanel.get(name);
    }

    public static JTextPane getTextPane(String name) {
        if (gTextPane == null) {
            return null;
        }
        return gTextPane.get(name);
    }

    public static JList getList(String name) {
        if (gList == null) {
            return null;
        }
        return gList.get(name);
    }

    public static JLabel getLabel(String name) {
        if (gLabel == null) {
            return null;
        }
        return gLabel.get(name);
    }

    public static JButton getButton(String name) {
        if (gButton == null) {
            return null;
        }
        return gButton.get(name);
    }

    public static JCheckBox getCheckBox(String name) {
        if (gCheckbox == null) {
            return null;
        }
        return gCheckbox.get(name);
    }

    public static JComboBox getComboBox(String name) {
        if (gCombobox == null) {
            return null;
        }
        return gCombobox.get(name);
    }

    public static JTextField getTextField(String name) {
        if (gTextField == null) {
            return null;
        }
        return gTextField.get(name);
    }

    public static JRadioButton getRadioButton(String name) {
        if (gRadiobutton == null) {
            return null;
        }
        return gRadiobutton.get(name);
    }

    public static JSpinner getSpinner(String name) {
        if (gSpinner == null) {
            return null;
        }
        return gSpinner.get(name);
    }

    /**
     * this sets up the GridBag constraints for a single panel to fill the container
     * 
     * @return the constraints
     */
    private static GridBagConstraints setGbagConstraintsPanel() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(GAPSIZE, GAPSIZE, GAPSIZE, GAPSIZE);

        c.fill = GridBagConstraints.BOTH;
        c.gridwidth  = GridBagConstraints.REMAINDER;
        // since only 1 component, these both have to be non-zero for grid bag to work
        c.weightx = 1.0;
        c.weighty = 1.0;
        return c;
    }

    /**
     * This sets up the GridBag constraints for a simple element
     * 
     * @param pos - the orientation on the line
     * @param end - true if this is the last (or only) entry on the line
     * @return the constraints
     */
    private static GridBagConstraints setGbagConstraints(Orient pos, boolean end) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(GAPSIZE, GAPSIZE, GAPSIZE, GAPSIZE);

        switch(pos) {
            case LEFT   -> c.anchor = GridBagConstraints.BASELINE_LEADING;
            case RIGHT  -> c.anchor = GridBagConstraints.BASELINE_TRAILING;
            case CENTER -> c.anchor = GridBagConstraints.CENTER;
            case NONE -> {
                c.fill = GridBagConstraints.BOTH;
                c.gridwidth  = GridBagConstraints.REMAINDER;
                // since only 1 component, these both have to be non-zero for grid bag to work
                c.weightx = 1.0;
                c.weighty = 1.0;
                return c;
            }
        }
        c.fill = GridBagConstraints.NONE;

        c.gridheight = 1;
        if (end) {
            c.gridwidth = GridBagConstraints.REMAINDER; //end row
        }
        return c;
    }

    /**
     * This sets up the GridBag constraints for an element on a line and places a label to the left
     * 
     * @param panel     - the panel to place the element in (null if place in frame)
     * @param gridbag   - the GridBag layout
     * @param pos       - the orientation on the line
     * @param end       - true if this is the last (or only) entry on the line
     * @param fullline  - true if take up entire line with item
     * @param title     - name of label to add
     * @return the constraints
     */
    private static GridBagConstraints setGbagInsertLabel(JPanel panel, GridBagLayout gridbag,
                               Orient pos, boolean end, boolean fullline, String name, String title) {
        if (mainFrame == null) {
            System.err.println("mainFrame not found!");
            System.exit(1);
        }
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(GAPSIZE, GAPSIZE, GAPSIZE, GAPSIZE);

        switch(pos) {
            case LEFT   -> c.anchor = GridBagConstraints.BASELINE_LEADING;
            case RIGHT  -> c.anchor = GridBagConstraints.BASELINE_TRAILING;
            case CENTER -> c.anchor = GridBagConstraints.CENTER;
            case NONE -> {
            }
        }
        c.fill = GridBagConstraints.NONE;
        JLabel label = new JLabel(title);
        gridbag.setConstraints(label, c);
        if (panel != null) {
            panel.add(label);
        } else {
            mainFrame.add(label);
        }

        if (fullline) {
            c.weightx = 1.0;
            c.fill = GridBagConstraints.HORIZONTAL;
        } else {
            c.weightx = 50.0;
        }
        if (end) {
            c.gridwidth = GridBagConstraints.REMAINDER; //end row
        }
        if (name != null && !name.isEmpty()) {
            gLabel.put(name, label);
        }

        return c;
    }

    private static JPanel getSelectedPanel(String panelname) {
        // get container panel if specified
        JPanel panel = null;
        if (panelname != null && !panelname.isEmpty()) {
            panel = gPanel.get(panelname);
            if (panel == null) {
                System.err.println("'" + panelname + "' panel not found!");
                System.exit(1);
            }
        }
        return panel;
    }

    /**
     * This adds the specified panel component to the main frame.
     * 
     * @param component - component to place the main frame
     */
    public static void addToPanelFrame(JComponent component) {
        if (mainFrame == null || mainLayout == null) {
            return;
        }
        GridBagLayout gridbag = mainLayout;
        gridbag.setConstraints(component, setGbagConstraintsPanel());
        mainFrame.add(component);
    }

    /**
     * This creates a JLabel and places it in the container.
     * 
     * @param panelname - the name of the jPanel container to place the component in (null if use main frame)
     * @param name    - the name id of the component (optional)
     * @param title   - the name to display as a label preceding the widget
     * @param pos     - orientation on the line: LEFT, RIGHT or CENTER
     * @param end     - true if this is last widget in the line
     */
    public static void makeLabel(String panelname, String name, String title, Orient pos, boolean end) {
        if (mainFrame == null || mainLayout == null) {
            return;
        }
        if (name != null && !name.isEmpty() && gLabel.containsKey(name)) {
            System.err.println("'" + name + "' label already added to container!");
            System.exit(1);
        }

        // get container panel if specified & get corresponding layout
        JPanel panel = getSelectedPanel(panelname);
        GridBagLayout gridbag = mainLayout;
        if (panel != null) {
            gridbag = (GridBagLayout) panel.getLayout();
        }

        // create the component
        JLabel label = new JLabel(title);
        gridbag.setConstraints(label, setGbagConstraints(pos, end));

        // place component in container & add entry to components list
        if (panel != null) {
            panel.add(label);
        } else {
            mainFrame.add(label);
        }

        if (name != null && !name.isEmpty()) {
            gLabel.put(name, label);
        }
    }

    /**
     * This creates a JLabel and places it in the container.
     * 
     * @param panelname - the name of the jPanel container to place the component in (null if use main frame)
     * @param pos     - orientation on the line: LEFT, RIGHT or CENTER
     * @param end     - true if this is last widget in the line
     */
    public static void makePlaceholder(String panelname, Orient pos, boolean end) {
      makeLabel(panelname, "", "    ", pos, end);
    }

    /**
     * This creates a JButton and places it in the container.
     * 
     * @param panelname - the name of the jPanel container to place the component in (null if use main frame)
     * @param name    - the name id of the component
     * @param title   - the name to display as a label preceding the widget
     * @param pos     - orientation on the line: LEFT, RIGHT or CENTER
     * @param end     - true if this is last widget in the line
     * @return the button widget
     */
    public static JButton makeButton(String panelname, String name, String title, Orient pos, boolean end) {
        if (mainFrame == null || mainLayout == null) {
            return null;
        }
        if (gButton.containsKey(name)) {
            System.err.println("'" + name + "' button already added to container!");
            System.exit(1);
        }

        // get the layout for the container
        GridBagLayout gridbag = mainLayout;
        JPanel panel = null;
        if (panelname != null && !panelname.isEmpty()) {
            panel = getPanel(panelname);
            if (panel == null) {
                System.err.println("'" + panelname + "' panel not found!");
                System.exit(1);
            }
            gridbag = (GridBagLayout) panel.getLayout();
        }

        // create the component
        JButton button = new JButton(title);
        gridbag.setConstraints(button, setGbagConstraints(pos, end));

        // place component in container & add entry to components list
        if (panel != null) {
            panel.add(button);
        } else {
            mainFrame.add(button);
        }

        gButton.put(name, button);
        return button;
    }

    /**
     * This creates a JCheckBox and places it in the container.
     * 
     * @param panelname - the name of the jPanel container to place the component in (null if use main frame)
     * @param name    - the name id of the component
     * @param title   - the name to display as a label preceding the widget
     * @param pos     - orientation on the line: LEFT, RIGHT or CENTER
     * @param end     - true if this is last widget in the line
     * @param value   - 0 to have checkbox initially unselected, any other value for selected
     * @return the checkbox widget
     */
    public static JCheckBox makeCheckbox(String panelname, String name, String title, Orient pos,
                boolean end, int value) {
        if (mainFrame == null || mainLayout == null) {
            return null;
        }
        if (gCheckbox.containsKey(name)) {
            System.err.println("'" + name + "' checkbox already added to container!");
            System.exit(1);
        }

        // get the layout for the container
        GridBagLayout gridbag = mainLayout;
        JPanel panel = null;
        if (panelname != null && !panelname.isEmpty()) {
            panel = getPanel(panelname);
            if (panel == null) {
                System.err.println("'" + panelname + "' panel not found!");
                System.exit(1);
            }
            gridbag = (GridBagLayout) panel.getLayout();
        }

        // create the component
        JCheckBox cbox = new JCheckBox(title);
        cbox.setSelected(value != 0);
        gridbag.setConstraints(cbox, setGbagConstraints(pos, end));

        // place component in container & add entry to components list
        if (panel != null) {
            panel.add(cbox);
        } else {
            mainFrame.add(cbox);
        }

        gCheckbox.put(name, cbox);
        return cbox;
    }

    /**
     * This creates a JTextField and places it in the container.
     * These are single line String displays.
     * 
     * @param panelname - the name of the jPanel container to place the component in (null if use main frame)
     * @param name    - the name id of the component
     * @param title   - the name to display as a label preceding the widget
     * @param pos     - orientation on the line: LEFT, RIGHT or CENTER
     * @param end     - true if this is last widget in the line
     * @param length  - length of text field
     * @param value   - initial text to display
     * @param writable - true if field is writable by user, false if display only
     * @return the checkbox widget
     */
    public static JTextField makeTextField(String panelname, String name, String title, Orient pos,
                  boolean end, int length, String value, boolean writable) {
        if (mainFrame == null || mainLayout == null) {
            return null;
        }
        if (gTextField.containsKey(name)) {
            System.err.println("'" + name + "' textfield already added to container!");
            System.exit(1);
        }

        // get the layout for the container
        GridBagLayout gridbag = mainLayout;
        JPanel panel = null;
        if (panelname != null && !panelname.isEmpty()) {
            panel = getPanel(panelname);
            if (panel == null) {
                System.err.println("'" + panelname + "' panel not found!");
                System.exit(1);
            }
            gridbag = (GridBagLayout) panel.getLayout();
        }

        // insert a label before the component
        GridBagConstraints c = setGbagInsertLabel(panel, gridbag, pos, end, true, name, title);

        // create the component
        JTextField field = new JTextField();
        field.setText(value);
        field.setPreferredSize(new Dimension(length, 25));
        field.setMinimumSize(new Dimension(length, 25));
        field.setEditable(writable);
        gridbag.setConstraints(field, c);

        // place component in container & add entry to components list
        if (panel != null) {
            panel.add(field);
        } else {
            mainFrame.add(field);
        }

        gTextField.put(name, field);
        return field;
    }

    /**
     * This creates a JRadioButton and places it in the container.
     * 
     * @param panelname - the name of the jPanel container to place the component in (null if use main frame)
     * @param name    - the name id of the component
     * @param title   - the name to display as a label preceding the widget
     * @param pos     - orientation on the line: LEFT, RIGHT or CENTER
     * @param end     - true if this is last widget in the line
     * @param value   - 0 to have checkbox initially unselected, any other value for selected
     * @return the checkbox widget
     */
    public static JRadioButton makeRadiobutton(String panelname, String name, String title, Orient pos,
                boolean end, int value) {
        if (mainFrame == null || mainLayout == null) {
            return null;
        }
        if (gRadiobutton.containsKey(name)) {
            System.err.println("'" + name + "' radiobutton already added to container!");
            System.exit(1);
        }

        // get the layout for the container
        GridBagLayout gridbag = mainLayout;
        JPanel panel = null;
        if (panelname != null && !panelname.isEmpty()) {
            panel = getPanel(panelname);
            if (panel == null) {
                System.err.println("'" + panelname + "' panel not found!");
                System.exit(1);
            }
            gridbag = (GridBagLayout) panel.getLayout();
        }

        // create the component
        JRadioButton rbutton = new JRadioButton(title);
        rbutton.setSelected(value != 0);
        gridbag.setConstraints(rbutton, setGbagConstraints(pos, end));

        // place component in container & add entry to components list
        if (panel != null) {
            panel.add(rbutton);
        } else {
            mainFrame.add(rbutton);
        }

        gRadiobutton.put(name, rbutton);
        return rbutton;
    }

    /**
     * This creates a JComboBox and places it in the container.
     * 
     * @param panelname - the name of the jPanel container to place the component in (null if use main frame)
     * @param name    - the name id of the component
     * @param title   - the name to display as a label preceding the widget
     * @param pos     - orientation on the line: LEFT, RIGHT or CENTER
     * @param end     - true if this is last widget in the line
     * @return the combo widget
     */
    public static JComboBox makeCombobox(String panelname, String name, String title, Orient pos, boolean end) {
        if (mainFrame == null || mainLayout == null) {
          return null;
        }
        if (gCombobox.containsKey(name)) {
            System.err.println("'" + name + "' combobox already added to container!");
            System.exit(1);
        }

        // get the layout for the container
        GridBagLayout gridbag = mainLayout;
        JPanel panel = null;
        if (panelname != null && !panelname.isEmpty()) {
            panel = getPanel(panelname);
            if (panel == null) {
                System.err.println("'" + panelname + "' panel not found!");
                System.exit(1);
            }
            gridbag = (GridBagLayout) panel.getLayout();
        }

        // insert a label before the component
        GridBagConstraints c = setGbagInsertLabel(panel, gridbag, pos, end, true, name, title);

        // create the component
        JComboBox combobox = new JComboBox();
        gridbag.setConstraints(combobox, c);

        // place component in container & add entry to components list
        if (panel != null) {
            panel.add(combobox);
        } else {
            mainFrame.add(combobox);
        }

        gCombobox.put(name, combobox);
        return combobox;
    }

    /**
     * This creates an integer JSpinner and places it in the container.
     * Step size (increment/decrement value) is always set to 1.
     * 
     * @param panelname - the name of the jPanel container to place the component in (null if use main frame)
     * @param name    - the name id of the component
     * @param title   - the name to display as a label preceding the widget
     * @param pos     - orientation on the line: LEFT, RIGHT or CENTER
     * @param end     - true if this is last widget in the line
     * @param minval  - the min range limit for the spinner
     * @param maxval  - the max range limit for the spinner
     * @param step    - step size for the spinner
     * @param curval  - the current value for the spinner
     * @return the spinner widget
     */
    public static JSpinner makeSpinner(String panelname, String name, String title, Orient pos, boolean end,
            int minval, int maxval, int step, int curval) {
      if (mainFrame == null || mainLayout == null) {
        return null;
      }
        if (gSpinner.containsKey(name)) {
            System.err.println("'" + name + "' spinner already added to container!");
            System.exit(1);
        }

        // get the layout for the container
        GridBagLayout gridbag = mainLayout;
        JPanel panel = null;
        if (panelname != null && !panelname.isEmpty()) {
            panel = getPanel(panelname);
            if (panel == null) {
                System.err.println("'" + panelname + "' panel not found!");
                System.exit(1);
            }
            gridbag = (GridBagLayout) panel.getLayout();
        }

        // insert a label before the component
        GridBagConstraints c = setGbagInsertLabel(panel, gridbag, pos, end, true, name, title);

        // create the component
        JSpinner spinner = new JSpinner();
        spinner.setModel(new SpinnerNumberModel(curval, minval, maxval, step));
        gridbag.setConstraints(spinner, c);

        // place component in container & add entry to components list
        if (panel != null) {
            panel.add(spinner);
        } else {
            mainFrame.add(spinner);
        }

        gSpinner.put(name, spinner);
        return spinner;
    }

    /**
     * This creates an empty JPanel and places it in the container.
     * 
     * @param panelname - the name of the jPanel container to place the component in (null if use main frame)
     * @param name    - the name id of the component
     * @param title   - the name to display as a label preceding the widget (null if no border)
     * @param pos     - orientation on the line: LEFT, RIGHT or CENTER
     * @param end     - true if this is last widget in the line
     * @return the panel
     */
    public static JPanel makePanel(String panelname, String name, String title, Orient pos, boolean end) {
        if (mainFrame == null || mainLayout == null) {
            return null;
        }
        if (gPanel.containsKey(name)) {
            System.err.println("'" + name + "' panel already added to container!");
            System.exit(1);
        }

        // get the layout for the container
        GridBagLayout gridbag = mainLayout;
        JPanel panel = null;
        if (panelname != null && !panelname.isEmpty()) {
            panel = getPanel(panelname);
            if (panel == null) {
                System.err.println("'" + panelname + "' container panel not found!");
                System.exit(1);
            }
            gridbag = (GridBagLayout) panel.getLayout();
        }

        // create the panel and apply constraints
        JPanel newpanel = new JPanel();
        if (title != null) {
            newpanel.setBorder(BorderFactory.createTitledBorder(title));
        }

        // create a layout for inside the panel
        GridBagLayout gbag = new GridBagLayout();
        newpanel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        newpanel.setLayout(gbag);

        // place component in container & add entry to components list
        if (panel != null) {
            panel.add(newpanel);
        } else {
            mainFrame.add(newpanel);
        }

        gridbag.setConstraints(newpanel, setGbagConstraints(pos, end));
        gPanel.put(name, newpanel);
        return newpanel;
    }

    /**
     * This creates an empty JTabbedPanel and places it in the container.
     * 
     * @param panelname - the name of the JTabbedPanel container to place the component in (null if use main frame)
     * @param name    - the name id of the component
     * @param title   - the name to display as a label preceding the widget (null if no border)
     * @param pos     - orientation on the line: LEFT, RIGHT or CENTER
     * @param end     - true if this is last widget in the line
     * @return the panel
     */
    public static JTabbedPane makeTabbedPanel(String panelname, String name, String title, Orient pos, boolean end) {
        if (mainFrame == null || mainLayout == null) {
            return null;
        }
        if (gTabbedPanel.containsKey(name)) {
            System.err.println("'" + name + "' panel already added to container!");
            System.exit(1);
        }

        // get the layout for the container
        GridBagLayout gridbag = mainLayout;
        JPanel panel = null;
        if (panelname != null && !panelname.isEmpty()) {
            panel = getPanel(panelname);
            if (panel == null) {
                System.err.println("'" + panelname + "' container panel not found!");
                System.exit(1);
            }
            gridbag = (GridBagLayout) panel.getLayout();
        }

        // create the panel and apply constraints
        JTabbedPane newpanel = new JTabbedPane();
        if (title != null) {
            newpanel.setBorder(BorderFactory.createTitledBorder(title));
        }

        // place component in container & add entry to components list
        if (panel != null) {
            panel.add(newpanel);
        } else {
            mainFrame.add(newpanel);
        }

        gridbag.setConstraints(newpanel, setGbagConstraintsPanel());
        gTabbedPanel.put(name, newpanel);
      return newpanel;
    }


    /**
     * This creates a JScrollPane containing a JList of Strings and places it in the container.
     * A List of String entries is passed to it that can be manipulated (adding & removing entries
     * that will be automatically reflected in the scroll pane.
     * 
     * @param panelname - the name of the jPanel container to place the component in (null if use main frame)
     * @param name    - the name id of the component
     * @param title   - the name to display as a label preceding the widget
     * @param list    - the list of entries to associate with the panel
     * @return the JList corresponding to the list passed
     */
    public static JList makeScrollList(String panelname, String name, String title, DefaultListModel list) {
        if (mainFrame == null || mainLayout == null) {
            return null;
        }
        if (gScrollPanel.containsKey(name) || gList.containsKey(name)) {
            System.err.println("'" + name + "' scrolling list panel already added to container!");
            System.exit(1);
        }

        // get the layout for the container
        GridBagLayout gridbag = mainLayout;
        JPanel panel = null;
        if (panelname != null && !panelname.isEmpty()) {
            panel = getPanel(panelname);
            if (panel == null) {
                System.err.println("'" + panelname + "' container panel not found!");
                System.exit(1);
            }
            gridbag = (GridBagLayout) panel.getLayout();
        }

        // create the scroll panel and apply constraints
        JScrollPane spanel = new JScrollPane();
        spanel.setBorder(BorderFactory.createTitledBorder(title));
        gridbag.setConstraints(spanel, setGbagConstraintsPanel());

        // create a list component for the scroll panel and assign the list model to it
        JList scrollList = new JList();
        spanel.setViewportView(scrollList);
        scrollList.setModel(list);

        // place component in container & add entry to components list
        if (panel != null) {
            panel.add(spanel);
        } else {
            mainFrame.add(spanel);
        }

        gScrollPanel.put(name, spanel);
        gList.put(name, scrollList);
        return scrollList;
    }

    /**
     * This creates a JScrollPane containing a JTextPane for text and places it in the container.
     * 
     * @param panelname - the name of the jPanel container to place the component in (null if use main frame)
     * @param name    - the name id of the component
     * @param title     - the name to display as a label preceding the widget
     * @return the text panel contained in the scroll panel
     */
    public static JTextPane makeScrollText(String panelname, String name, String title) {
        if (mainFrame == null || mainLayout == null) {
          return null;
        }
        if (gScrollPanel.containsKey(name) || gTextPane.containsKey(name)) {
            System.err.println("'" + name + "' scrolling textpanel already added to container!");
            System.exit(1);
        }

        // get the layout for the container
        GridBagLayout gridbag = mainLayout;
        JPanel panel = null;
        if (panelname != null && !panelname.isEmpty()) {
            panel = getPanel(panelname);
            if (panel == null) {
                System.err.println("'" + panelname + "' container panel not found!");
                System.exit(1);
            }
            gridbag = (GridBagLayout) panel.getLayout();
        }

        // create a text panel component
        JTextPane tpanel = new JTextPane();

        // create the scroll panel and apply constraints
        JScrollPane spanel = new JScrollPane(tpanel);
        spanel.setBorder(BorderFactory.createTitledBorder(title));
        gridbag.setConstraints(spanel, setGbagConstraintsPanel());

        // place component in container & add entry to components list
        if (panel != null) {
            panel.add(spanel);
        } else {
            mainFrame.add(spanel);
        }

        gScrollPanel.put(name, spanel);
        gTextPane.put(name, tpanel);
        return tpanel;
    }
    
}
