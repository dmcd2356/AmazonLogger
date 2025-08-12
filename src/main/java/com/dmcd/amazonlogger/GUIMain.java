/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.dmcd.amazonlogger;

import com.dmcd.amazonlogger.GUILogPanel.MsgType;
import static com.dmcd.amazonlogger.GUILogPanel.MsgType.DEBUG;
import static com.dmcd.amazonlogger.GUILogPanel.MsgType.INFO;
import static com.dmcd.amazonlogger.GUILogPanel.MsgType.NORMAL;
import static com.dmcd.amazonlogger.GUILogPanel.MsgType.PARSER;
import static com.dmcd.amazonlogger.GUILogPanel.MsgType.PROPS;
import static com.dmcd.amazonlogger.GUILogPanel.MsgType.SSHEET;
import com.dmcd.amazonlogger.PropertiesFile.Property;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

/**
 * this is the frame class for the user interface
 * 
 * @author dan
 */
public final class GUIMain extends JFrame implements ActionListener {

    private static final String CLASS_NAME = GUIMain.class.getSimpleName();

    private static boolean bUseGUI = false;
    
    private static long         elapsedStart = 0;       // hold start of elapsed time for running from file
    private static long         prevElapsed = 0;        // hold current elapsed time for pause durations
    private static boolean      showElapsed = false;    // indicates if elapsed time to be displayed in logs
    private static PrintWriter  debugFile = null;       // the log file for non-network mode

    // Components of the Form
    private static Container c;
    private static JCheckBox cbox_normal, cbox_parser, cbox_ssheet;
    private static JCheckBox cbox_info, cbox_debug, cbox_props;
    private static ButtonGroup btn_group;
    private static JButton btn_select, btn_clipboard, btn_update;
    private static JButton btn_pdf, btn_balance;
    private static JButton btn_clear, btn_copy, btn_print;
    private static JLabel lbl_error_msg, lbl_select, lbl_lastbal;
    private static JLabel lbl_order_tab, lbl_orders_num, lbl_orders_item, lbl_orders_date;
    private static JLabel lbl_lastline1, lbl_lastline2, lbl_lastline3;
    private static JTextPane log_txtpane, order_txtpane;
    private static JTabbedPane tab_panel;
    private static final ArrayList<Tabs> panelId = new ArrayList<>();

    private static final int BUTTON_WIDTH    = 130;     // width of buttons
    private static final int BUTTON_COL_GAP  = 30 + BUTTON_WIDTH; // spacing between side by side buttons
    private static final int LABEL_WIDTH_NUM = 40;      // width of a label with numeric contents
    private static final int CBOX_WIDTH      = 200;     // width of checkboxes
    private static final int TEXT_HEIGHT     = 20;      // height of buttons, labels and checkboxes
    private static final int LINE_GAP        = 10;      // amount of vertical gap between lines
    private static final int LINE_SPACING    = TEXT_HEIGHT + LINE_GAP; // spacing between lines (rows)
    private static final int TAB_PANE_WIDTH  = 1400;    // width  of the tabbed panel
    private static final int TAB_PANE_HEIGHT = 600;     // height of the tabbed panel
    private static final int BORDER_SIZE     = 50;      // top, bottom, left, right border size
    
    // the IDs for the tabbed panels
    private enum Tabs {
        LOG,
        ORDER,
    }
    
    // actions to perform on the tabbed panels
    private enum TabAction {
        CLEAR,
        COPY,
        PRINT,
    }
    
    /**
     * initializes the GUI state.
     * called prior to compiling a file
     */
    public static void init () {
        GUILogPanel.init();
        GUILogPanel.closeTestFile();
        PropertiesFile.setPropertiesItem(Property.TestFileOut, "");
    }

    public static boolean isGUIMode() {
        return bUseGUI;
    }
    
    // constructor, to initialize the components
    // with default values.
    public GUIMain(boolean bGUI)
    {
        bUseGUI = bGUI;
    
        // setup the control sizes
        int x_info_offset = 600;        // x location for orders/items information labels
        int y_title_offset = 80;        // starting y offset beneath title

        int panel_width  = TAB_PANE_WIDTH + (2 * BORDER_SIZE);
        int panel_height = TAB_PANE_HEIGHT + y_title_offset + (13 * LINE_SPACING) + (2 * BORDER_SIZE);

        setTitle("Amazon shopping expenditures");
        setBounds(300, 150, panel_width, panel_height);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        c = getContentPane();
        c.setLayout(null);

        int title_height = 30;
        int title_width = 300;
        JLabel lbl_title = new JLabel("Amazon Expenses");
        lbl_title.setFont(new Font("Arial", Font.PLAIN, 30));
        lbl_title.setSize(title_width, title_height);
        lbl_title.setLocation((panel_width - title_width) / 2, title_height);
        c.add(lbl_title);

        //==========================================
        // FIRST (TOP) PANEL
        //==========================================
        // starting locations for this section
        int section1_start  = y_title_offset;
        int section1_height = 3 * LINE_SPACING;

        int y_row = section1_start;
        int x_col1 = BORDER_SIZE;               // left-most column of buttons
        int x_col2 = x_col1 + BUTTON_COL_GAP;   // next column of buttons
        int x_col3 = x_col2 + BUTTON_COL_GAP;   // column offset for text info to right of buttons
        
        // these are the command buttons for executing the operations on the psreadsheet
        btn_select    = addButton("Select"     , x_col1, y_row, true, null);
        lbl_select    = addLabel("", x_col2, y_row, 500, true);
        y_row += LINE_SPACING;
        btn_clipboard = addButton("Read Clip"  , x_col1, y_row, false, null);
        btn_update    = addButton("Update File", x_col2, y_row, false, null);
        y_row += LINE_SPACING;
        btn_pdf       = addButton("Read Pdf"   , x_col1, y_row, false, null);
        btn_balance   = addButton("Balance"    , x_col2, y_row, false, null);
        
        // this shows the information of the spreadsheet that was loaded
        lbl_lastbal   = addLabel("", x_col3, y_row, 500, true);
        
        // init the select label to indicate what to do
        lbl_select.setText("Select the spreadsheet file to work on");

        btn_group = new ButtonGroup();
        btn_group.add(btn_select);
        btn_group.add(btn_clipboard);
        btn_group.add(btn_update);
        btn_group.add(btn_pdf);
        btn_group.add(btn_balance);
       
        //==========================================
        // SECOND PANEL
        //==========================================
        // starting locations for this section
        int section2_start  = section1_start + section1_height + LINE_GAP;
        int section2_height = TAB_PANE_HEIGHT;

        x_col1 = BORDER_SIZE;
        y_row = section2_start;

        // create the panel and add the Orders and Debug panels to it
        tab_panel = addTabbedPane (x_col1, y_row, TAB_PANE_WIDTH, TAB_PANE_HEIGHT);
        order_txtpane = addScrollTextToTab (tab_panel, "Order Info"  , Tabs.ORDER);
        log_txtpane   = addScrollTextToTab (tab_panel, "Log messages", Tabs.LOG);

        //==========================================
        // THIRD PANEL
        //==========================================
        // starting locations for this section
        int section3_start  = section2_start + section2_height + LINE_GAP;
        int section3_height = 4 * LINE_SPACING; // 4 text lines here

        x_col1 = BORDER_SIZE;
        y_row = section3_start;

        // this will display the last line of the 1st tab of the loaded spreadsheet
        int width = TAB_PANE_WIDTH;
        lbl_lastline1 = addLabel("", x_col1, y_row, width, true, Font.BOLD, false);
        lbl_lastline1.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        y_row += LINE_SPACING;
        lbl_lastline2 = addLabel("", x_col1, y_row, width, true, Font.PLAIN, false);
        y_row += LINE_SPACING;
        lbl_lastline3 = addLabel("", x_col1, y_row, width, true, Font.PLAIN, false);
        y_row += LINE_SPACING;
        
        // this will display the error status info
        lbl_error_msg = addLabel("", x_col1, y_row, width, false, Font.BOLD, true);

        //==========================================
        // FOURTH (BOTTOM) PANEL
        //==========================================
        // starting locations for this section
        int section4_start  = section3_start + section3_height + LINE_GAP;

        y_row += LINE_SPACING;
        x_col1 = BORDER_SIZE;

        btn_clear = addButton("Clear"     , x_col1, y_row, true, TabAction.CLEAR);
        y_row += LINE_SPACING;
        btn_copy  = addButton("Copy text" , x_col1, y_row, true, TabAction.COPY);
        y_row += LINE_SPACING;
        btn_print = addButton("Print text", x_col1, y_row, false, TabAction.PRINT);

        // NEXT COLUMN OF BOTTOM PANEL: CHECKBOX OF LOG MESSAGE ENABLES
        int x_cbox_offset = 250;        // x location for checkboxes
        x_col1 = x_cbox_offset;
        y_row = section4_start;
        cbox_normal = addCheckBox ("Normal msgs"     , x_col1, y_row, CBOX_WIDTH, MsgType.NORMAL);
        y_row += LINE_SPACING;
        cbox_parser = addCheckBox ("Parser msgs"     , x_col1, y_row, CBOX_WIDTH, MsgType.PARSER);
        y_row += LINE_SPACING;
        cbox_ssheet = addCheckBox ("Spreadsheet msgs", x_col1, y_row, CBOX_WIDTH, MsgType.SSHEET);
        y_row += LINE_SPACING;
        cbox_info   = addCheckBox ("Info msgs"       , x_col1, y_row, CBOX_WIDTH, MsgType.INFO);
        y_row += LINE_SPACING;
        cbox_debug  = addCheckBox ("Debug msgs"      , x_col1, y_row, CBOX_WIDTH, MsgType.DEBUG);
        y_row += LINE_SPACING;
        cbox_props  = addCheckBox ("Properties msgs" , x_col1, y_row, CBOX_WIDTH, MsgType.PROPS);
        
        // TOP RIGHT OF BOTTOM PANEL
        int x_order_gap_width  = 20;
        int heading_width      = 100;

        y_row = section4_start;
        x_col1 = x_info_offset;
        x_col2 = x_col1 + heading_width + x_order_gap_width;
        
        // this displays the tab selection of the clipboard data loaded
        int x_width = 150;
        addLabel("Clipboard Selection:", x_col1, y_row, x_width, true);
        lbl_order_tab   = addLabel("", x_col1 + x_width + x_order_gap_width, y_row, x_width, true, Font.BOLD, true);
        // this group displays the summary of the valid clipboard data loaded
        y_row += LINE_SPACING;
        addLabel("ORDERS:", x_col1, y_row, heading_width, true);
        lbl_orders_num   = addLabel("", x_col2, y_row, LABEL_WIDTH_NUM, true, Font.BOLD, true);
        y_row += LINE_SPACING;
        addLabel("ITEMS :", x_col1, y_row, heading_width, true);
        lbl_orders_item  = addLabel("", x_col2, y_row, LABEL_WIDTH_NUM, true, Font.BOLD, true);
        y_row += LINE_SPACING;
        addLabel("DATES :", x_col1, y_row, heading_width, true);
        lbl_orders_date  = addLabel("", x_col2, y_row, 200, true, Font.BOLD, true);

        // init the values in the clipboard info
        setTabOwner(null);
        clearOrderCount ();
        
        // init the log panels
        GUILogPanel   logPanel   = new GUILogPanel(log_txtpane);
        GUIOrderPanel orderPanel = new GUIOrderPanel(order_txtpane);

        // default the message enable flags to on
        cbox_parser.setSelected(true);
        cbox_ssheet.setSelected(true);
        cbox_info  .setSelected(true);
        cbox_debug .setSelected(true);
        cbox_props .setSelected(true);

        if (bUseGUI) {
            setVisible(true);
        }
    }

    /**
     * Get the action performed by the user and act accordingly
     * 
     * @param e
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        clearErrorMsg();
        try {
            if (e.getSource() == btn_select) {
                outputSeparatorLine("LOAD SPREADSHEET");

                // disable the last line info until we have loaded a spreadsheet
                enableLastLineInfo(false);
        
                // load the spreadsheet into memory
                Spreadsheet.selectSpreadsheet(null);
                Spreadsheet.loadSheets(2, true);
        
                // get the name of the file to store debug info to (if defined)
                boolean bSuccess = setDebugOutputFile(PropertiesFile.getPropertiesItem(Property.DebugFileOut, ""));
                if (bSuccess) {
                    btn_print.setVisible(true);
                }
        
                // enable the CLIPBOARD and PDF buttons
                btn_clipboard.setVisible(true);
                btn_pdf.setVisible(true);
             }
            else if (e.getSource() == btn_clipboard) {
                outputSeparatorLine("PARSE CLIPBOARD");
                // make sure we have set the update/undo button to UPDATE and disabled
                btn_update.setText("Update File");
                btn_update.setVisible(false);
                
                // read the clipboard info and parse the data to extract the orders
                AmazonParser amazonParser = new AmazonParser();
                boolean bSuccess = amazonParser.parseWebData();
                if (bSuccess) {
                    // if there is now data in the Orders list, enable the UPDATE button
                    btn_update.setVisible(true);
                }
            }
            else if (e.getSource() == btn_update) {
                if (btn_update.getText().contentEquals("Update File")) {
                    // update the spreadsheet file from the clipboard orders
                    outputSeparatorLine("UPDATE FROM CLIPS");
                    // update the spreadsheet from the orders read from the clipboard
                    boolean bSuccess = AmazonParser.updateSpreadsheet();

                    if (bSuccess && Spreadsheet.checkIfBackupCopy(Spreadsheet.BackupType.Order)) {
                        btn_update.setText("Undo");
                    } else {
                        // erase the UPDATE button until we read in more data
                        btn_update.setVisible(false);
                    }
                } else {
                    // restore the spreadsheet from the backup file
                    outputSeparatorLine("RESTORED FROM BACKUP FILE");
                    Spreadsheet.restoreBackupCopy(Spreadsheet.BackupType.Order);
                    btn_update.setText("Update File");
                    btn_update.setVisible(false);
                }
            }
            else if (e.getSource() == btn_pdf) {
                outputSeparatorLine("PARSE PDF");
                // make sure we have set the balance/undo button to BALANCE and disabled
                btn_balance.setText("Balance");
                btn_balance.setVisible(false);
                
                // read and process the PDF file for orders that are in the spreadsheet
                PdfReader pdfReader = new PdfReader();
                if (pdfReader == null) {
                    throw new ParserException(functionId + "Unable to start PdfReader");
                }
                PdfReader.readPdfContents(null);
                boolean bSuccess = pdfReader.processData();
                if (bSuccess) {
                    // if there is now data to be balanced, enable the BALANCE button
                    btn_balance.setVisible(true);
                }
            }
            else if (e.getSource() == btn_balance) {
                if (btn_balance.getText().contentEquals("Balance")) {
                    // update the spreadsheet from pdf file
                    outputSeparatorLine("BALANCE FROM PDF");
                    // add the balancing info from the PDF file to the spreadsheet
                    boolean bSuccess = PdfReader.balanceSpreadsheet();

                    if (bSuccess && Spreadsheet.checkIfBackupCopy(Spreadsheet.BackupType.Balance)) {
                        btn_balance.setText("Undo");
                    } else {
                        // erase the BALANCE button until we read in more data
                        btn_balance.setVisible(false);
                    }
                } else {
                    // restore the spreadsheet from the backup file
                    outputSeparatorLine("RESTORED FROM BACKUP FILE");
                    Spreadsheet.restoreBackupCopy(Spreadsheet.BackupType.Balance);
                    btn_balance.setText("Balance");
                    btn_balance.setVisible(false);
                }
            }
        } catch (ParserException | IOException | SAXException | TikaException ex) {
            String msg = ex.getMessage();
            String header = "com.dmcd.amazonlogger.";
            int offset = msg.lastIndexOf(header);
            if (offset >= 0) {
                msg = msg.substring(offset + header.length());
            }
            GUILogPanel.outputInfoMsg (MsgType.ERROR, msg);
            disableAllButton();
        }
    }

    /**
     * executes the action specified by the 'action' input and which tab is currently active.
     * 
     * @param action - an action id to specify the action to take.
     */
    private static void runSelectedTabAction (TabAction action) {
        clearErrorMsg();
        int ix = tab_panel.getSelectedIndex();
        if (ix >= 0 && ix < panelId.size()) {
            Tabs tabSelect = panelId.get(ix);
            if (tabSelect == Tabs.ORDER) {
                switch (action) {
                    case CLEAR:
                        GUIOrderPanel.clearMessages();
                        break;
                    case COPY:
                        GUIOrderPanel.saveToClipboard();
                        break;
                    case PRINT:
                        GUIOrderPanel.saveDebugToFile();
                        break;
                    default:
                        break;
                }
            } else {
                switch (action) {
                    case CLEAR:
                        GUILogPanel.clearMessages();
                        break;
                    case COPY:
                        GUILogPanel.saveToClipboard();
                        break;
                    case PRINT:
                        GUILogPanel.saveDebugToFile();
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * clears the error message
     */
    private static void clearErrorMsg () {
        lbl_error_msg.setText("");
    }
    
    private static String formatLastLine (String tab, String line, String order, String date, String desc) {
        int tab1 = 8;
        int tab2 = 7;
        int tab3 = 21;
        int tab4 = 10;
        
        String data = Utils.padRight (tab  , tab1) + "  "
                    + Utils.padRight (line , tab2) + "  "
                    + Utils.padRight (order, tab3) + "  "
                    + Utils.padRight (date , tab4) + "  "
                    + desc;
        return data;
    }
    
    /**
     * reads the Property setting for 'MsgEnable'.
     * The value is read as a string entry and converted from hex format
     * if it starts with either an 'x' or '0x', or as an integer value otherwise.
     * 
     * @param msgType - the message to enable/disable
     */
    private static int getPropsMsgEnable () {
        String strFlags = PropertiesFile.getPropertiesItem(Property.MsgEnable, "0");
        Integer intVal = 0;
        try {
            intVal = Utils.getHexValue (strFlags);
            if (intVal == null) {
                intVal = Utils.getIntValue (strFlags).intValue();
            }
        } catch (ParserException ex) {
            // the Propertiy value was neither Integer or hexadecimal format.
            // we'll just default to 0;
        }

        return intVal;
    }

    /**
     * sets a the Property setting for 'MsgEnable' to the specified value.
     * The value is set in hex format for easier reading
     * 
     * @param msgType - the message to enable/disable
     */
    private static void setPropsMsgEnable (int intValue) {
        try {
            String strFlags = Utils.toHexWordValue (intValue);
            PropertiesFile.setPropertiesItem(Property.MsgEnable, strFlags);
            GUILogPanel.setMsgEnable(intValue);
            // setMessageFlags(intValue);
        } catch (ParserException exMsg) {
            // ignore the error
        }
    }

    /**
     * sets a single bit of the msgEnable flag to either on or off based on the GUI selection
     * 
     * @param msgType - the message to enable/disable
     */
    private void setBitMsgEnableProps (MsgType msgType) {
        int msgBitflag = GUILogPanel.getMsgEnableValue(msgType);
        int flags = getPropsMsgEnable();
        flags &= ~msgBitflag;
        if (getCboxMessage(msgType)) {
            flags |= msgBitflag;
        }
        setPropsMsgEnable (flags);
    }

    /**
     * sets the selected checkbox on the GUI to the specified on/off value.
     * 
     * @param msgType - the checkbox message type selection
     * @param bEnable - the on/off value to set it to
     */
    private static void enableCboxMessage (MsgType msgType, boolean bEnable) {
        switch (msgType) {
            case NORMAL -> cbox_normal.setSelected(bEnable);
            case PARSER -> cbox_parser.setSelected(bEnable);
            case SSHEET -> cbox_ssheet.setSelected(bEnable);
            case INFO   -> cbox_info  .setSelected(bEnable);
            case PROPS  -> cbox_props .setSelected(bEnable);
            case DEBUG  -> cbox_debug .setSelected(bEnable);
            default -> {
            }
        }
    }

    /**
     * gets the on/off status of the selected checkbox on the GUI.
     * 
     * @param msgType - the checkbox message type selection
     * 
     * @return bEnable - the on/off value to set it to
     */
    private static boolean getCboxMessage (MsgType msgType) {
        boolean bEnable = false;
        switch (msgType) {
            case NORMAL -> bEnable = cbox_normal.isSelected();
            case PARSER -> bEnable = cbox_parser.isSelected();
            case SSHEET -> bEnable = cbox_ssheet.isSelected();
            case INFO   -> bEnable = cbox_info  .isSelected();
            case PROPS  -> bEnable = cbox_props .isSelected();
            case DEBUG  -> bEnable = cbox_debug .isSelected();
            default -> {
            }
        }
        return bEnable;
    }
    
    /**
     * adds a button to the frame.
     * 
     * @param name      - name to place in button
     * @param x         - x location to place it
     * @param y         - y location to place it
     * @param visible   - true if make it visible
     * @param eventType - the TabAction associated with it for the function runSelectedTabAction()
     * 
     * @return the JButton definition
     */
    private JButton addButton(String name, int x, int y, boolean visible, TabAction eventType) {
        JButton button = new JButton(name);
        button.setFont(new Font("Arial", Font.BOLD, 15));
        button.setSize(BUTTON_WIDTH, TEXT_HEIGHT);
        button.setLocation(x, y);
        button.setVisible(visible);
        if (eventType == null) {
            button.addActionListener((ActionListener) this);
        } else {
            button.addActionListener(new ActionListener(){
                @Override
                public void actionPerformed(ActionEvent e){
                    runSelectedTabAction(eventType);
                }
            });    
        }
        c.add(button);
        return button;
    }

    /**
     * adds a label to the frame.
     * 
     * @param name      - String to place in label
     * @param x         - x location to place it
     * @param y         - y location to place it
     * @param width     - the width to make it
     * @param visible   - true if make it visible
     * 
     * @return the JLabel definition
     */
    private JLabel addLabel (String name, int x, int y, int width, boolean visible) {
        JLabel label = new JLabel(name);
        label.setFont(new Font("Arial", Font.PLAIN, 15));
        label.setSize(width, TEXT_HEIGHT);
        label.setLocation(x, y);
        label.setVisible(visible);
        c.add(label);
        return label;
    }
    
    /**
     * adds a label to the frame.
     * 
     * @param name      - String to place in label
     * @param x         - x location to place it
     * @param y         - y location to place it
     * @param width     - the width to make it
     * @param courier   - true for Courier font, false for Arial
     * @param style     - style of text (Font.PLAIN, Font.BOLD, Font.ITALIC, etc)
     * @param visible   - true if make it visible
     * 
     * @return the JLabel definition
     */
    private JLabel addLabel (String name, int x, int y, int width, boolean courier, int style, boolean visible) {
        JLabel label = new JLabel(name);
        String fontType = courier ? "Courier" : "Arial";
        label.setFont(new Font(fontType, style, 15));
        label.setSize(width, TEXT_HEIGHT);
        label.setLocation(x, y);
        label.setVisible(visible);
        c.add(label);
        return label;
    }

    /**
     * adds a checkbox to the frame.
     * 
     * @param name      - Name to place next to the checkbox
     * @param x         - x location to place it
     * @param y         - y location to place it
     * @param width     - the width to make it
     * @param eventType - name of the event setting it controls
     * 
     * @return the JCheckBox definition
     */
    private JCheckBox addCheckBox (String name, int x, int y, int width, MsgType eventType) {
        JCheckBox cbox = new JCheckBox(name);
        cbox.setFont(new Font("Arial", Font.BOLD, 15));
        cbox.setSize(width, TEXT_HEIGHT);
        cbox.setLocation(x, y);
        cbox.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                setBitMsgEnableProps(eventType);
            }
        });    
        c.add(cbox);
        return cbox;
    }

    /**
     * adds a tabbed pane to the frame.
     * 
     * @param x      - x location to place it
     * @param y      - y location to place it
     * @param width  - the width to make it
     * @param height - the height to make it
     * 
     * @return the JTabbedPane definition
     */
    private JTabbedPane addTabbedPane (int x, int y, int width, int height) {
        JTabbedPane tabPane = new JTabbedPane();
        tabPane.setBorder(BorderFactory.createTitledBorder(""));
        tabPane.setSize(width, height);
        tabPane.setLocation(x, y);
        c.add(tabPane);
        return tabPane;
    }
    
    /**
     * adds a scrollable text pane to the specified tabbed pane.
     * It gets the size and location info from the tabbed pane.
     * 
     * @param tabPane - the tabbed pane in which to place it
     * @param title   - the title to place in the tab for it
     * @param tabId   - the identifier of which tab it is in
     * 
     * @return the JTextPane definition
     */
    private JTextPane addScrollTextToTab (JTabbedPane tabPane, String title, Tabs tabId) {
        // get location and size from the tab pane we are applying them to
        int x = tabPane.getX();
        int y = tabPane.getY();
        int width  = tabPane.getWidth();
        int height = tabPane.getHeight();
        
        JTextPane txtpane = new JTextPane();
        txtpane.setText("");
        txtpane.setFont(new Font("Courier", Font.PLAIN, 15));
        txtpane.setSize(width, height);
        txtpane.setLocation(x, y);
        txtpane.setEditable(false);
        c.add(txtpane);
        
        // put it in a panel to make it non-wrap mode, so we can scroll horizontally
        JPanel panel = new JPanel();
        panel.add(txtpane);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        c.add(panel);
        
        // need it to be scrollable
        JScrollPane scrollPane = new JScrollPane (panel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setSize(width, height);
        scrollPane.setLocation(x, y);
        c.add(scrollPane);
        
        tabPane.addTab(title, scrollPane);
        panelId.add(tabId);
        return txtpane;
    }
    
    /**
     * outputs a separator line to the output stream
     * 
     * @param heading  - a message to display with the line
     */
    private static void outputSeparatorLine (String heading) {
        heading = "=====" + heading + "======================================================================";
        heading = heading.substring(0, 75);
        GUILogPanel.outputInfoMsg (MsgType.NORMAL, heading);
    }

    /**
     * disable all the GUI execute buttons except the SELECT button.
     * Forces a spreadsheet file selection prior to doing anything else.
     */
    public static void disableAllButton () {
        if (!bUseGUI)
            return;
        
        btn_clipboard.setVisible(false);
        btn_balance.setVisible(false);
        btn_pdf.setVisible(false);
        btn_update.setVisible(false);
    }

    /**
     * displays the error message.
     * 
     * @param msg - the message to display
     */
    public static void showErrorMsg (String msg) {
        lbl_error_msg.setForeground(Color.red);
        lbl_error_msg.setText(msg);
    }

    public static void enableLastLineInfo (boolean status) {
        if (!bUseGUI)
            return;
        
        lbl_lastline1.setVisible(status);
        lbl_lastline2.setVisible(status);
        lbl_lastline3.setVisible(status);
    }

    public static void setLastLineInfo (int ix, String tab, String lineNum, String orderNum, String dateOrd, String descr) {
        if (!bUseGUI)
            return;

        String header = formatLastLine("Tab", "Line #", "Order #", "Ordered", "Description");
        String data   = formatLastLine(tab, lineNum, orderNum, dateOrd, descr);
        lbl_lastline1.setText (header);
        if (ix == 1) {
            lbl_lastline2.setText(data);
        } else {
            lbl_lastline3.setText(data);
        }
        enableLastLineInfo (true);
    }

    public static void showLastBalance (String balance) {
        lbl_lastbal.setText(balance);
    }
    
    /**
     * displays the spreadsheet file location selected.
     * 
     * @param filepath - the spreadsheet file location
     */
    public static void setSpreadsheetSelection (String filepath) {
        if (!bUseGUI)
            return;
        
        lbl_select.setText(filepath);
        lbl_select.setForeground(Color.blue);
    }
    
    /**
     * displays the clipboard tab selected.
     * 
     * @param tab - the tab location
     */
    public static void setTabOwner (String tab) {
        if (!bUseGUI)
            return;
        
        if (tab == null) {
            lbl_order_tab.setText("<none>");
            lbl_order_tab.setForeground(Color.black);
        } else {
            lbl_order_tab.setText(tab);
            lbl_order_tab.setForeground(Color.blue);
        }
    }
    
    /**
     * displays the Orders information loaded from the clipboard.
     * 
     * @param orders    - number of orders
     * @param items     - number of items in all the orders
     * @param startDate - earliest date in the orders
     * @param endDate   - most recent date in the orders
     */
    public static void setOrderCount (int orders, int items, LocalDate startDate, LocalDate endDate) {
        if (!bUseGUI)
            return;
        
        String dateRange = "";
        if (startDate != null && endDate != null) {
            if (startDate.isAfter(endDate)) {
                LocalDate tempDate = startDate;
                startDate = endDate;
                endDate = tempDate;
            }
            dateRange = DateFormat.convertDateToString(startDate, false) + "  to  " +
                        DateFormat.convertDateToString(endDate, false);
        }
        lbl_orders_num.setForeground(Color.blue);
        lbl_orders_item.setForeground(Color.blue);
        lbl_orders_date.setForeground(Color.blue);
        
        lbl_orders_num.setText (orders + "");
        lbl_orders_item.setText(items + "");
        lbl_orders_date.setText(dateRange);
    }

    /**
     * clears the Orders information.
     */
    public static void clearOrderCount () {
        if (!bUseGUI)
            return;
        
        lbl_orders_num.setForeground(Color.black);
        lbl_orders_item.setForeground(Color.black);
        lbl_orders_date.setForeground(Color.black);

        lbl_orders_num.setText ("0");
        lbl_orders_item.setText("0");
        lbl_orders_date.setText("");
    }
    
    /**
     * returns access to the file writer that is used for debug output in GUI mode.
     * 
     * @return the file to use when the PRINT button is pressed in the GUI
     */
    public static PrintWriter getDebugOutputFile() {
        return debugFile;
    }
    
    /**
     * specifies the debug output file to use.
     * This is the file to save the displayed GUI debug information to
     *  when the SAVE key is pressed.
     * 
     * @param fname - debug output file name
     * 
     * @return true if successful
     */    
    public static boolean setDebugOutputFile (String fname) {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (! GUIMain.isGUIMode())
            return false;
        
        if (fname == null || fname.isBlank()) {
            GUILogPanel.outputInfoMsg (MsgType.WARN, functionId + "Debug file failure: DebugFileOut entry missing from PropertiesFile");
            debugFile = null;
            return false;
        }
        int offset = fname.indexOf('/');
        if (offset >= 0) {
            GUILogPanel.outputInfoMsg (MsgType.WARN, functionId + "Debug file failure: DebugFileOut contains path: " + fname + " (must be filename only)");
            debugFile = null;
            return false;
        }
        // we always put the file in the same location as where the spreadsheet file is
        String ssPath = Utils.getPathFromPropertiesFile(PropertiesFile.Property.SpreadsheetPath);
        if (ssPath == null) {
            GUILogPanel.outputInfoMsg (MsgType.WARN, functionId + "Debug file failure: SpreadsheetPath entry missing from PropertiesFile");
            debugFile = null;
            return false;
        }
        File logPath = new File(ssPath);
        if (! logPath.isDirectory()) {
            GUILogPanel.outputInfoMsg (MsgType.WARN, functionId + "Debug file failure: SpreadsheetPath not a directory: " + ssPath);
            debugFile = null;
            return false;
        }
        fname = ssPath + "/" + fname;
        File newFile = new File(fname);
        if (newFile.isDirectory()) {
            GUILogPanel.outputInfoMsg (MsgType.WARN, functionId + "Debug file failure: DebugFileOut entry is a directory");
            debugFile = null;
            return false;
        }
        // create a new file or overwrite the existing one
        try {
            GUILogPanel.outputInfoMsg (MsgType.NORMAL, "Creating debug file: " + fname);
            newFile.createNewFile();
            debugFile = new PrintWriter(fname);
        } catch (IOException ex) {
            // file inaccessible
            GUILogPanel.outputInfoMsg (MsgType.ERROR, functionId + "for file: " + fname + ", " + ex);
            debugFile = null;
            return false;
        }
        return true;
    }

    /**
     * sets all debug message flags
     * 
     * @param debugFlags - the bit value of each message to be enabled is set to 1
     */
    public static void setMessageFlags (int debugFlags) {
        // save the debug settings
        GUILogPanel.setMsgEnable(debugFlags);

        if (bUseGUI) {
            // set the message enable selections on the GUI
            for (GUILogPanel.MsgType type : GUILogPanel.MsgType.values()) {
                int bitval = GUILogPanel.getMsgEnableValue(type);
                boolean bEnable = (bitval & debugFlags) != 0;
                enableCboxMessage (type, bEnable);
            }
        } else {
            // update the properties file for the selections
            setPropsMsgEnable (debugFlags);
        }
    }

    /**
     * sets the default settings for message control
     */
    public static void setDefaultStatus () {
        // only default to PropertiesFile selection for test file out if running from GUI.
        // (for program mode, default to using stdout until selection made)
        if (bUseGUI) {
            String testName = PropertiesFile.getPropertiesItem(Property.TestFileOut, "");
            Integer testAppend = PropertiesFile.getPropertiesItem(Property.TestFileAppend, 0);
            GUILogPanel.setTestOutputFile(testName, testAppend != 0);
        }
        setMessageFlags(getPropsMsgEnable());
    }
    
    /**
     * enable and start the timestamp counter
     */
    public static void elapsedTimerEnable() {
        elapsedStart = System.currentTimeMillis();
        showElapsed = true;
    }
    
    /**
     * save the current elapsed time so we can add it when resumed
     */
    public static void elapsedTimerPause() {
        prevElapsed = System.currentTimeMillis() - elapsedStart;
        showElapsed = false;
    }
    
    /**
     * disable the timestamp counter
     */
    public static void elapsedTimerDisable() {
        prevElapsed = 0;
        showElapsed = false;
    }
    
    /**
     * return a timestamp value.
     * 
     * @return the timestamp value reported as MM:SS.mmm
     */
    public static String elapsedTimerGet() {
        if (!showElapsed) {
            return "";
        }
        long elapsedTime = System.currentTimeMillis() - elapsedStart + prevElapsed;
        long msecs = elapsedTime % 1000;
        long secs = elapsedTime / 1000;
//        long hours = secs / 3600;
        secs = secs % 3600;
        long mins = secs / 60;
        secs = secs % 60;

        String strElapsed = "";
        // ignore the hours, so format is always: MM:SS.m
//        strElapsed =  (hours < 10) ? "0" + hours : "" + hours;
        strElapsed += (mins  < 10) ?  "0" + mins : "" + mins;
        strElapsed += (secs  < 10) ? ":0" + secs : ":" + secs;
        if (msecs < 10)
            strElapsed += ".00" + msecs;
        else if (msecs < 100)
            strElapsed += ".0" + msecs;
        else
            strElapsed += "." + msecs;
        return strElapsed + " ";
    }
    
}
