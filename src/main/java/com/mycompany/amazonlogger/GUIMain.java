/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.props;
import com.mycompany.amazonlogger.GUILogPanel.MsgType;
import com.mycompany.amazonlogger.PropertiesFile.Property;

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
    private static JCheckBox cbox_normal;
    private static JCheckBox cbox_parser;
    private static JCheckBox cbox_ssheet;
    private static JCheckBox cbox_info;
    private static JCheckBox cbox_debug;
    private static JCheckBox cbox_props;
    private static ButtonGroup btn_group;
    private static JButton btn_select;
    private static JButton btn_clipboard;
    private static JButton btn_update;
    private static JButton btn_balance;
    private static JButton btn_clear;
    private static JButton btn_copy;
    private static JButton btn_print;
    private static JLabel lbl_error_msg;
    private static JLabel lbl_select;
    private static JLabel lbl_clipboard;
    private static JLabel lbl_update;
    private static JLabel lbl_balance;
    private static JLabel lbl_order_tab;
    private static JLabel lbl_order_title;
    private static JLabel lbl_orders;
    private static JLabel lbl_orders_num;
    private static JLabel lbl_orders_item;
    private static JLabel lbl_orders_date;
    private static JLabel lbl_detail;
    private static JLabel lbl_detail_num;
    private static JLabel lbl_detail_item;
    private static JLabel lbl_detail_date;
    private static JTextPane log_txtpane;
    private static JTextPane order_txtpane;
    private static JTabbedPane tab_panel;
    private static ArrayList<Tabs> panelId = new ArrayList<>();

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
        props.setPropertiesItem(Property.TestFileOut, "");
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
        int y_pane_height = 700;        // dimensions of the text pane
        int x_pane_width = 1100;
        
        int y_button_height = 20;       // dimensions of the buttons
        int x_button_width = 130;
        int y_label_height = 20;        // dimensions of the button labels
        int x_label_width = 800;
        int y_cbox_height = 20;         // dimensions of the checkboxes
        int x_cbox_width = 200;

        int y_line_gap = 30;            // y gap between buttons and checkboxes
        int x_label_gap = 20;           // x gap between button and its corresponding label
        int x_cbox_offset = 250;        // x location for checkboxes
        int x_info_offset = 600;        // x location for orders/items information labels
        
        int y_title_offset = 80;        // starting y offset beneath title

        int border_size = 50;
        int panel_width = x_pane_width + (2 * border_size);
        int panel_height = y_pane_height + y_title_offset + (9 * y_line_gap) + y_button_height + (2 * border_size);

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

        // TOP LEFT OF TOP PANEL
        int loc_y = y_title_offset;
        int loc_x = border_size;
        int loc_xlab = loc_x + x_button_width + x_label_gap;
        
        btn_select = new JButton("Select");
        btn_select.setFont(new Font("Arial", Font.BOLD, 15));
        btn_select.setSize(x_button_width, y_button_height);
        btn_select.setLocation(loc_x, loc_y);
        btn_select.addActionListener((ActionListener) this);
        c.add(btn_select);

        lbl_select = new JLabel("Selects the Amazon-list.ods spreadsheet file to work on");
        lbl_select.setFont(new Font("Arial", Font.PLAIN, 15));
        lbl_select.setSize(x_label_width, y_label_height);
        lbl_select.setLocation(loc_xlab, loc_y);
        c.add(lbl_select);

        loc_y += y_line_gap;
        btn_clipboard = new JButton("Clipboard");
        btn_clipboard.setFont(new Font("Arial", Font.BOLD, 15));
        btn_clipboard.setSize(x_button_width, y_button_height);
        btn_clipboard.setLocation(loc_x, loc_y);
        btn_clipboard.addActionListener((ActionListener) this);
        btn_clipboard.setVisible(false);
        c.add(btn_clipboard);

        lbl_clipboard = new JLabel("Loads the clipboard data selection");
        lbl_clipboard.setFont(new Font("Arial", Font.PLAIN, 15));
        lbl_clipboard.setSize(x_label_width, y_label_height);
        lbl_clipboard.setLocation(loc_xlab, loc_y);
        lbl_clipboard.setVisible(false);
        c.add(lbl_clipboard);

        loc_y += y_line_gap;
        btn_update = new JButton("Update");
        btn_update.setFont(new Font("Arial", Font.BOLD, 15));
        btn_update.setSize(x_button_width, y_button_height);
        btn_update.setLocation(loc_x, loc_y);
        btn_update.addActionListener((ActionListener) this);
        btn_update.setVisible(false);
        c.add(btn_update);

        lbl_update = new JLabel("Updates the spreadsheet from clipboard selection");
        lbl_update.setFont(new Font("Arial", Font.PLAIN, 15));
        lbl_update.setSize(x_label_width, y_label_height);
        lbl_update.setLocation(loc_xlab, loc_y);
        lbl_update.setVisible(false);
        c.add(lbl_update);

        loc_y += y_line_gap;
        btn_balance = new JButton("Balance");
        btn_balance.setFont(new Font("Arial", Font.BOLD, 15));
        btn_balance.setSize(x_button_width, y_button_height);
        btn_balance.setLocation(loc_x, loc_y);
        btn_balance.addActionListener((ActionListener) this);
        btn_balance.setVisible(false);
        c.add(btn_balance);

        lbl_balance = new JLabel("Marks the spreadsheet paid items from the credit card summary");
        lbl_balance.setFont(new Font("Arial", Font.PLAIN, 15));
        lbl_balance.setSize(x_label_width, y_label_height);
        lbl_balance.setLocation(loc_xlab, loc_y);
        lbl_balance.setVisible(false);
        c.add(lbl_balance);

        btn_group = new ButtonGroup();
        btn_group.add(btn_select);
        btn_group.add(btn_update);
        btn_group.add(btn_balance);
       
        // MIDDLE PANEL
        // this displays progress and debug msgs
        loc_x = border_size;
        loc_y += y_line_gap;

        // this is the debug log panel
        order_txtpane = new JTextPane();
        order_txtpane.setText("");
        order_txtpane.setFont(new Font("Courier", Font.PLAIN, 15));
        order_txtpane.setSize(x_pane_width, y_pane_height);
        order_txtpane.setLocation(loc_x, loc_y);
        order_txtpane.setEditable(false);
        c.add(order_txtpane);
        // put it in a panel to make it non-wrap mode, so we can scroll horizontally
        JPanel order_panel = new JPanel();
        order_panel.add(order_txtpane);
        order_panel.setLayout(new BoxLayout(order_panel, BoxLayout.Y_AXIS));
        c.add(order_panel);
        // need it to be scrollable
        JScrollPane order_scroll = new JScrollPane (order_panel);
        order_scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        order_scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        order_scroll.setSize(x_pane_width, y_pane_height);
        order_scroll.setLocation(loc_x, loc_y);
        c.add(order_scroll);

        // this is the debug log panel
        log_txtpane = new JTextPane();
        log_txtpane.setText("");
        log_txtpane.setFont(new Font("Courier", Font.PLAIN, 15));
        log_txtpane.setSize(x_pane_width, y_pane_height);
        log_txtpane.setLocation(loc_x, loc_y);
        log_txtpane.setEditable(false);
        c.add(log_txtpane);
        // put it in a panel to make it non-wrap mode, so we can scroll horizontally
        JPanel log_panel = new JPanel();
        log_panel.add(log_txtpane);
        log_panel.setLayout(new BoxLayout(log_panel, BoxLayout.Y_AXIS));
        c.add(log_panel);
        // need it to be scrollable
        JScrollPane log_scroll = new JScrollPane (log_panel);
        log_scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        log_scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        log_scroll.setSize(x_pane_width, y_pane_height);
        log_scroll.setLocation(loc_x, loc_y);
        c.add(log_scroll);

        // create the panel and apply constraints
        tab_panel = new JTabbedPane();
        tab_panel.setBorder(BorderFactory.createTitledBorder(""));
        tab_panel.addTab("Order Info", order_scroll);
        panelId.add(Tabs.ORDER);
        tab_panel.addTab("Log messages", log_scroll);
        panelId.add(Tabs.LOG);
        tab_panel.setSize(x_pane_width, y_pane_height);
        tab_panel.setLocation(loc_x, loc_y);
        c.add(tab_panel);

        // TOP LEFT OF BOTTOM PANEL
        loc_x = border_size;
        loc_y += y_line_gap + y_pane_height;

        // this will display the error status info
        lbl_error_msg = new JLabel();
        lbl_error_msg.setFont(new Font("Arial", Font.BOLD, 15));
        lbl_error_msg.setForeground(Color.red);
        lbl_error_msg.setSize(panel_width, y_label_height);
        lbl_error_msg.setLocation(loc_x, loc_y);
        lbl_error_msg.setVisible(true);
        c.add(lbl_error_msg);

        loc_y += y_line_gap;
        int y_bottom_panel = loc_y;
        
        btn_clear = new JButton("Clear");
        btn_clear.setFont(new Font("Arial", Font.BOLD, 15));
        btn_clear.setSize(x_button_width, y_button_height);
        btn_clear.setLocation(loc_x, loc_y);
        btn_clear.setVisible(true);
        // this provides a way to copy the text to the clipboard
        btn_clear.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                runSelectedTabAction (TabAction.CLEAR);
            }
        });    
        c.add(btn_clear);

        loc_y += y_line_gap;
        btn_copy = new JButton("Copy text");
        btn_copy.setFont(new Font("Arial", Font.BOLD, 15));
        btn_copy.setSize(x_button_width, y_button_height);
        btn_copy.setLocation(loc_x, loc_y);
        btn_copy.setVisible(true);
        // this provides a way to copy the text to the clipboard
        btn_copy.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                runSelectedTabAction (TabAction.COPY);
            }
        });    
        c.add(btn_copy);

        loc_y += y_line_gap;
        btn_print = new JButton("Print text");
        btn_print.setFont(new Font("Arial", Font.BOLD, 15));
        btn_print.setSize(x_button_width, y_button_height);
        btn_print.setLocation(loc_x, loc_y);
        btn_print.setVisible(false);
        // this provides a way to copy the text to the clipboard
        btn_print.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                runSelectedTabAction (TabAction.PRINT);
            }
        });    
        c.add(btn_print);

        // NEXT COLUMN OF BOTTOM PANEL
        loc_x = x_cbox_offset;
        loc_y = y_bottom_panel;
        cbox_normal = new JCheckBox("Normal msgs");
        cbox_normal.setFont(new Font("Arial", Font.BOLD, 15));
        cbox_normal.setSize(x_cbox_width, y_cbox_height);
        cbox_normal.setLocation(loc_x, loc_y);
        cbox_normal.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                setBitMsgEnableProps(MsgType.NORMAL);
            }
        });    
        c.add(cbox_normal);
        
        loc_y += y_line_gap;
        cbox_parser = new JCheckBox("Parser msgs");
        cbox_parser.setFont(new Font("Arial", Font.BOLD, 15));
        cbox_parser.setSize(x_cbox_width, y_cbox_height);
        cbox_parser.setLocation(loc_x, loc_y);
        cbox_parser.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                setBitMsgEnableProps(MsgType.PARSER);
            }
        });    
        c.add(cbox_parser);
        
        loc_y += y_line_gap;
        cbox_ssheet = new JCheckBox("Spreadsheet msgs");
        cbox_ssheet.setFont(new Font("Arial", Font.BOLD, 15));
        cbox_ssheet.setSize(x_cbox_width, y_cbox_height);
        cbox_ssheet.setLocation(loc_x, loc_y);
        cbox_ssheet.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                setBitMsgEnableProps(MsgType.SSHEET);
            }
        });    
        c.add(cbox_ssheet);
        
        loc_y += y_line_gap;
        cbox_info = new JCheckBox("Info msgs");
        cbox_info.setFont(new Font("Arial", Font.BOLD, 15));
        cbox_info.setSize(x_cbox_width, y_cbox_height);
        cbox_info.setLocation(loc_x, loc_y);
        cbox_info.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                setBitMsgEnableProps(MsgType.INFO);
            }
        });    
        c.add(cbox_info);
        
        loc_y += y_line_gap;
        cbox_debug = new JCheckBox("Debug msgs");
        cbox_debug.setFont(new Font("Arial", Font.BOLD, 15));
        cbox_debug.setSize(x_cbox_width, y_cbox_height);
        cbox_debug.setLocation(loc_x, loc_y);
        cbox_debug.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                setBitMsgEnableProps(MsgType.DEBUG);
            }
        });    
        c.add(cbox_debug);
        
        loc_y += y_line_gap;
        cbox_props = new JCheckBox("Properties msgs");
        cbox_props.setFont(new Font("Arial", Font.BOLD, 15));
        cbox_props.setSize(x_cbox_width, y_cbox_height);
        cbox_props.setLocation(loc_x, loc_y);
        cbox_props.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                setBitMsgEnableProps(MsgType.PROPS);
            }
        });    
        c.add(cbox_props);
        
        // TOP RIGHT OF BOTTOM PANEL
        loc_x = x_info_offset;
        loc_y = y_bottom_panel;
        x_label_width = 500;
        int lbl_width;
        int x_order_lbl_width  = 80;
        int x_order_num_width  = 40;
        int x_order_date_width = 200;
        int x_order_gap_width  = 20;
        
        // this will display the tab owner of the clipboard data loaded
        lbl_order_tab = new JLabel();
        lbl_order_tab.setFont(new Font("Arial", Font.BOLD, 15));
        lbl_order_tab.setSize(x_label_width, y_label_height);
        lbl_order_tab.setLocation(loc_x, loc_y);
        lbl_order_tab.setVisible(true);
        c.add(lbl_order_tab);

        // this will display the tab owner of the clipboard data loaded
        loc_x = x_info_offset + x_order_lbl_width + x_order_num_width + x_order_gap_width;
        loc_y += y_line_gap;
        lbl_order_title = new JLabel("Items");
        lbl_order_title.setFont(new Font("Arial", Font.BOLD, 15));
        lbl_order_title.setSize(x_label_width, y_label_height);
        lbl_order_title.setLocation(loc_x, loc_y);
        lbl_order_title.setVisible(true);
        c.add(lbl_order_title);

        // this displays the clipboard stats on what is loaded from the Orders
        loc_x = x_info_offset;
        loc_y += y_line_gap;
        lbl_width = x_order_lbl_width;
        lbl_orders = new JLabel("ORDERS :");
        lbl_orders.setFont(new Font("Arial", Font.BOLD, 15));
        lbl_orders.setSize(lbl_width, y_label_height);
        lbl_orders.setLocation(loc_x, loc_y);
        lbl_orders.setVisible(true);
        c.add(lbl_orders);
        loc_x += lbl_width + x_order_gap_width;
        lbl_width = x_order_num_width;
        lbl_orders_num = new JLabel();
        lbl_orders_num.setFont(new Font("Arial", Font.BOLD, 15));
        lbl_orders_num.setSize(lbl_width, y_label_height);
        lbl_orders_num.setLocation(loc_x, loc_y);
        lbl_orders_num.setForeground(Color.blue);
        lbl_orders_num.setVisible(true);
        c.add(lbl_orders_num);
        loc_x += lbl_width + x_order_gap_width;
        lbl_orders_item = new JLabel();
        lbl_orders_item.setFont(new Font("Arial", Font.BOLD, 15));
        lbl_orders_item.setSize(lbl_width, y_label_height);
        lbl_orders_item.setLocation(loc_x, loc_y);
        lbl_orders_item.setForeground(Color.blue);
        lbl_orders_item.setVisible(true);
        c.add(lbl_orders_item);
        loc_x += lbl_width + x_order_gap_width;
        lbl_width = x_order_date_width;
        lbl_orders_date = new JLabel();
        lbl_orders_date.setFont(new Font("Arial", Font.BOLD, 15));
        lbl_orders_date.setSize(lbl_width, y_label_height);
        lbl_orders_date.setLocation(loc_x, loc_y);
        lbl_orders_date.setForeground(Color.blue);
        lbl_orders_date.setVisible(true);
        c.add(lbl_orders_date);

        // this displays the clipboard stats on what is loaded from the Details
        loc_x = x_info_offset;
        loc_y += y_line_gap;
        lbl_width = x_order_lbl_width;
        lbl_detail = new JLabel("DETAILS:");
        lbl_detail.setFont(new Font("Arial", Font.BOLD, 15));
        lbl_detail.setSize(lbl_width, y_label_height);
        lbl_detail.setLocation(loc_x, loc_y);
        lbl_detail.setVisible(true);
        c.add(lbl_detail);
        loc_x += lbl_width + x_order_gap_width;
        lbl_width = x_order_num_width;
        lbl_detail_num = new JLabel();
        lbl_detail_num.setFont(new Font("Arial", Font.BOLD, 15));
        lbl_detail_num.setSize(lbl_width, y_label_height);
        lbl_detail_num.setLocation(loc_x, loc_y);
        lbl_detail_num.setForeground(Color.blue);
        lbl_detail_num.setVisible(true);
        c.add(lbl_detail_num);
        loc_x += lbl_width + x_order_gap_width;
        lbl_detail_item = new JLabel();
        lbl_detail_item.setFont(new Font("Arial", Font.BOLD, 15));
        lbl_detail_item.setSize(lbl_width, y_label_height);
        lbl_detail_item.setLocation(loc_x, loc_y);
        lbl_detail_item.setForeground(Color.blue);
        lbl_detail_item.setVisible(true);
        c.add(lbl_detail_item);
        loc_x += lbl_width + x_order_gap_width;
        lbl_width = x_order_date_width;
        lbl_detail_date = new JLabel();
        lbl_detail_date.setFont(new Font("Arial", Font.BOLD, 15));
        lbl_detail_date.setSize(lbl_width, y_label_height);
        lbl_detail_date.setLocation(loc_x, loc_y);
        lbl_detail_date.setForeground(Color.blue);
        lbl_detail_date.setVisible(true);
        c.add(lbl_detail_date);

        // init the values in the clipboard info
        clearTabOwner();
        clearOrderCount ();
        clearDetailCount ();
        
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
    public void actionPerformed(ActionEvent e)
    {
        clearErrorMsg();
        try {
            if (e.getSource() == btn_select) {
                outputSeparatorLine("LOAD SPREADSHEET");
                Spreadsheet.selectSpreadsheet(null);
                Spreadsheet.loadSheets(2, true);
             }
            else if (e.getSource() == btn_clipboard) {
                outputSeparatorLine("PARSE CLIPBOARD");
                AmazonParser amazonParser = new AmazonParser();
                amazonParser.parseWebData();
            }
            else if (e.getSource() == btn_update) {
                outputSeparatorLine("UPDATE FROM CLIPS");
                AmazonParser.updateSpreadsheet();
            }
            else if (e.getSource() == btn_balance) {
                outputSeparatorLine("BALANCE FROM PDF");
                PdfReader pdfReader = new PdfReader();
                pdfReader.readPdfContents(null);
                pdfReader.processData();
            }
        } catch (ParserException | IOException | SAXException | TikaException ex) {
            String msg = ex.getMessage();
            String header = "com.mycompany.amazonlogger.";
            int offset = msg.lastIndexOf(header);
            if (offset >= 0) {
                msg = msg.substring(offset + header.length());
            }
            GUILogPanel.outputInfoMsg (MsgType.ERROR, msg);
            disableAllButton();
        }
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
     * disable all the GUI execute buttons except the SELECT button.
     * Forces a spreadsheet file selection prior to doing anything else.
     */
    public static void disableAllButton () {
        if (!bUseGUI)
            return;
        
        btn_clipboard.setVisible(false);
        lbl_clipboard.setVisible(false);
        btn_balance.setVisible(false);
        lbl_balance.setVisible(false);
        btn_update.setVisible(false);
        lbl_update.setVisible(false);
    }

    /**
     * clears the error message
     */
    private static void clearErrorMsg () {
        lbl_error_msg.setText("");
    }
    
    /**
     * displays the error message.
     * 
     * @param msg - the message to display
     */
    public static void showErrorMsg (String msg) {
        lbl_error_msg.setText(msg);
    }
    
    /**
     * enables/disables the Clipboard button.
     * 
     * @param status - true to enable
     */
    public static void enableClipboardButton (boolean status) {
        if (!bUseGUI)
            return;
        
        btn_clipboard.setVisible(status);
        lbl_clipboard.setVisible(status);
    }

    public static void enablePrintButton (boolean status) {
        if (!bUseGUI)
            return;
        
        btn_print.setVisible(status);
    }
    
    /**
     * enables/disables the Balance button.
     * 
     * @param status - true to enable
     */
    public static void enableCheckBalanceButton (boolean status) {
        if (!bUseGUI)
            return;
        
        btn_balance.setVisible(status);
        lbl_balance.setVisible(status);
    }
    
    /**
     * enables/disables the Update button.
     * 
     * @param status - true to enable
     */
    public static void enableUpdateButton (boolean status) {
        if (!bUseGUI)
            return;
        
        btn_update.setVisible(status);
        lbl_update.setVisible(status);
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
        
        lbl_order_tab.setText("Clipboard Selection:  " + tab);
        lbl_order_tab.setForeground(Color.blue);
    }
    
    /**
     * clears the displayed clipboard tab selection.
     */
    public static void clearTabOwner () {
        if (!bUseGUI)
            return;
        
        lbl_order_tab.setText("Clipboard Selection:  <none>");
        lbl_order_tab.setForeground(Color.black);
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
        
        lbl_orders_num.setText ("0");
        lbl_orders_item.setText("0");
        lbl_orders_date.setText("");
    }
    
    /**
     * displays the Detailed Orders information loaded from the clipboard.
     * 
     * @param orders    - number of orders
     * @param items     - number of items in all the orders
     * @param startDate - earliest date in the orders
     * @param endDate   - most recent date in the orders
     */
    public static void setDetailCount (int orders, int items, LocalDate startDate, LocalDate endDate) {
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
        lbl_detail_num.setText (orders + "");
        lbl_detail_item.setText(items + "");
        lbl_detail_date.setText(dateRange);
    }

    /**
     * clears the Detailed Orders information.
     */
    public static void clearDetailCount () {
        if (!bUseGUI)
            return;
        
        lbl_detail_num.setText ("0");
        lbl_detail_item.setText("0");
        lbl_detail_date.setText("");
    }

    /**
     * reads the Property setting for 'MsgEnable'.
     * The value is read as a string entry and converted from hex format
     * if it starts with either an 'x' or '0x', or as an integer value otherwise.
     * 
     * @param msgType - the message to enable/disable
     */
    private static int getPropsMsgEnable () {
        String strFlags = props.getPropertiesItem(Property.MsgEnable, "0");
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
            props.setPropertiesItem(Property.MsgEnable, strFlags);
        } catch (ParserException exMsg) {
            // ignore the error
        }
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
            GUILogPanel.outputInfoMsg (MsgType.WARN, functionId + "Debug file name missing from PropertiesFile - disabling Print to debug file");
            debugFile = null;
            return false;
        }
        // we always put the file in the same location as where the spreadsheet file is
        String ssPath = Utils.getPathFromPropertiesFile(PropertiesFile.Property.SpreadsheetPath);
        if (ssPath == null) {
            GUILogPanel.outputInfoMsg (MsgType.WARN, functionId + "Spreadsheet path missing from PropertiesFile - disabling Print to debug file");
            debugFile = null;
            return false;
        }
        fname = ssPath + "/" + fname;
        File newFile = new File(fname);
        if (newFile.isDirectory()) {
            GUILogPanel.outputInfoMsg (MsgType.WARN, functionId + "Debug file name invalid - disabling Print to debug file");
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
     * sets all debug message flags at one time - ONLY FOR COMMAND LINE USE (NON-GUI)!!!
     * 
     * @param debugFlags
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
            String testName = props.getPropertiesItem(Property.TestFileOut, "");
            Integer testAppend = props.getPropertiesItem(Property.TestFileAppend, 0);
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
