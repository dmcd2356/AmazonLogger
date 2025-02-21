package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.props;
import com.mycompany.amazonlogger.PropertiesFile.Property;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

/**
 * this is the frame class for the user interface
 * 
 * @author dan
 */
public final class UIFrame extends JFrame implements ActionListener {

    // type of text characteristics to print with 'outputInfoMsg'
    public static final int STATUS_ERROR  = 0;  // fatal errors
    public static final int STATUS_WARN   = 1;  // non-fatal warnings
    public static final int STATUS_NORMAL = 2;  // output written to spreadsheet
    public static final int STATUS_PARSER = 3;  // parser status
    public static final int STATUS_SSHEET = 4;  // spreadsheet status
    public static final int STATUS_INFO   = 5;  // processing of data from web clip and from PDF file
    public static final int STATUS_DEBUG  = 6;  // low-level detailed messages
    public static final int STATUS_PROPS  = 7;  // low-level properties interface messages
    
    private static boolean bMsgNormal;
    private static boolean bMsgParser;
    private static boolean bMsgSsheet;
    private static boolean bMsgInfo;
    private static boolean bMsgDebug;
    private static boolean bMsgProps;
    
    private PrintWriter    debugFile = null;
    private PrintWriter    testFile = null;
    private final boolean  bUseGUI;
    private static long    elapsedStart = 0;    // hold start of elapsed time for running from file
    private static boolean showElapsed = false; // indicates if elapsed time to be displayed in logs
    
    private enum TextColor {
        Black, DkGrey, DkRed, Red, LtRed, Orange, Brown,
        Gold, Green, Cyan, LtBlue, Blue, Violet, DkVio;
    }
    
    private static final String NEWLINE = System.getProperty("line.separator");

    // Components of the Form
    private final Container c;
    private final JCheckBox cbox_normal;
    private final JCheckBox cbox_parser;
    private final JCheckBox cbox_ssheet;
    private final JCheckBox cbox_info;
    private final JCheckBox cbox_debug;
    private final JCheckBox cbox_props;
    private final ButtonGroup btn_group;
    private final JButton btn_select;
    private final JButton btn_clipboard;
    private final JButton btn_update;
    private final JButton btn_balance;
    private final JButton btn_copy;
    private final JButton btn_print;
    private final JLabel lbl_select;
    private final JLabel lbl_clipboard;
    private final JLabel lbl_update;
    private final JLabel lbl_balance;
    private final JLabel lbl_title;
    private final JLabel lbl_order_tab;
    private final JLabel lbl_order_title;
    private final JLabel lbl_orders;
    private final JLabel lbl_orders_num;
    private final JLabel lbl_orders_item;
    private final JLabel lbl_orders_date;
    private final JLabel lbl_detail;
    private final JLabel lbl_detail_num;
    private final JLabel lbl_detail_item;
    private final JLabel lbl_detail_date;
    private final JScrollPane scroll_info;
    private final JPanel txt_panel;
    private final JTextPane txt_info;
    
    // constructor, to initialize the components
    // with default values.
    public UIFrame(boolean bGUI)
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
        lbl_title = new JLabel("Amazon Expenses");
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

        txt_info = new JTextPane();
        txt_info.setText("");
        txt_info.setFont(new Font("Courier", Font.PLAIN, 15));
        txt_info.setSize(x_pane_width, y_pane_height);
        txt_info.setLocation(loc_x, loc_y);
        txt_info.setEditable(false);
        c.add(txt_info);
        // put it in a panel to make it non-wrap mode, so we can scroll horizontally
        txt_panel = new JPanel();
        txt_panel.add(txt_info);
        txt_panel.setLayout(new BoxLayout(txt_panel, BoxLayout.Y_AXIS));
        c.add(txt_panel);
        // need it to be scrollable
        scroll_info = new JScrollPane (txt_panel);
        scroll_info.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scroll_info.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scroll_info.setSize(x_pane_width, y_pane_height);
        scroll_info.setLocation(loc_x, loc_y);
        c.add(scroll_info);

        // TOP LEFT OF BOTTOM PANEL
        loc_x = border_size;
        loc_y += y_line_gap + y_pane_height;
        int y_bottom_panel = loc_y;
        btn_copy = new JButton("Copy text");
        btn_copy.setFont(new Font("Arial", Font.BOLD, 15));
        btn_copy.setSize(x_button_width, y_button_height);
        btn_copy.setLocation(loc_x, loc_y);
        btn_copy.setVisible(true);
        // this provides a way to copy the text to the clipboard
        btn_copy.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e){
                String textToCopy = txt_info.getText();
                StringSelection stringSelection = new StringSelection(textToCopy);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
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
                if (debugFile != null) {
                    debugFile.println("=== " + getCurrentDateTime() + " ============================================================");
                    String textToCopy = txt_info.getText();
                    Stream<String> lines = textToCopy.lines();
                    lines.forEach(debugFile::println);
                    debugFile.flush();
                    debugFile.close();
                }
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
                props.setPropertiesItem(Property.MsgNormal, cbox_normal.isSelected() ? "1" : "0");
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
                props.setPropertiesItem(Property.MsgParser, cbox_parser.isSelected() ? "1" : "0");
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
                props.setPropertiesItem(Property.MsgSpreadsheet, cbox_ssheet.isSelected() ? "1" : "0");
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
                props.setPropertiesItem(Property.MsgInfo, cbox_info.isSelected() ? "1" : "0");
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
                props.setPropertiesItem(Property.MsgDebug, cbox_debug.isSelected() ? "1" : "0");
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
                props.setPropertiesItem(Property.MsgProperties, cbox_props.isSelected() ? "1" : "0");
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
        this.clearTabOwner();
        this.clearOrderCount ();
        this.clearDetailCount ();

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
            }
        } catch (ParserException | IOException | SAXException | TikaException ex) {
            outputInfoMsg (STATUS_ERROR, ex.getMessage());
            disableAllButton();
        }
    }

    public void clearMessages () {
        if (!bUseGUI)
            return;
        
        txt_info.setText("");
    }

    public void disableAllButton () {
        if (!bUseGUI)
            return;
        
        btn_clipboard.setVisible(false);
        lbl_clipboard.setVisible(false);
        btn_balance.setVisible(false);
        lbl_balance.setVisible(false);
        btn_update.setVisible(false);
        lbl_update.setVisible(false);
    }
    
    public void enableClipboardButton (boolean status) {
        if (!bUseGUI)
            return;
        
        btn_clipboard.setVisible(status);
        lbl_clipboard.setVisible(status);
    }
    
    public void enableCheckBalanceButton (boolean status) {
        if (!bUseGUI)
            return;
        
        btn_balance.setVisible(status);
        lbl_balance.setVisible(status);
    }
    
    public void enableUpdateButton (boolean status) {
        if (!bUseGUI)
            return;
        
        btn_update.setVisible(status);
        lbl_update.setVisible(status);
    }

    public void setSpreadsheetSelection (String filepath) {
        if (!bUseGUI)
            return;
        
        lbl_select.setText(filepath);
        lbl_select.setForeground(Color.blue);
    }
    
    public void setTabOwner (String tab) {
        if (!bUseGUI)
            return;
        
        lbl_order_tab.setText("Clipboard Selection:  " + tab);
        lbl_order_tab.setForeground(Color.blue);
    }
    
    public void clearTabOwner () {
        if (!bUseGUI)
            return;
        
        lbl_order_tab.setText("Clipboard Selection:  <none>");
        lbl_order_tab.setForeground(Color.black);
    }

    public void setOrderCount (int orders, int items, LocalDate startDate, LocalDate endDate) {
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

    public void clearOrderCount () {
        if (!bUseGUI)
            return;
        
        lbl_orders_num.setText ("0");
        lbl_orders_item.setText("0");
        lbl_orders_date.setText("");
    }
    
    public void setDetailCount (int orders, int items, LocalDate startDate, LocalDate endDate) {
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

    public void clearDetailCount () {
        if (!bUseGUI)
            return;
        
        lbl_detail_num.setText ("0");
        lbl_detail_item.setText("0");
        lbl_detail_date.setText("");
    }
    
    public void setDebugOutputFile (String fname) {
        if (!bUseGUI)
            return;
        
        if (fname == null || fname.isBlank()) {
            outputInfoMsg (STATUS_WARN, "UIFrame.setDebugOutputFile: Debug file name missing from PropertiesFile - disabling Print to debug file");
            debugFile = null;
            return;
        }
        // we always put the file in the same location as where the spreadsheet file is
        String ssPath = Utils.getPathFromPropertiesFile(Property.SpreadsheetPath);
        if (ssPath == null) {
            outputInfoMsg (STATUS_WARN, "UIFrame.setDebugOutputFile: Spreadsheet path missing from PropertiesFile - disabling Print to debug file");
            debugFile = null;
            return;
        }
        fname = ssPath + "/" + fname;
        File newFile = new File(fname);
        if (newFile.isDirectory()) {
            outputInfoMsg (STATUS_WARN, "UIFrame.setDebugOutputFile: Debug file name invalid - disabling Print to debug file");
            debugFile = null;
            return;
        }
        // create a new file or overwrite the existing one
        try {
            outputInfoMsg (STATUS_NORMAL, "Creating debug file: " + fname);
            newFile.createNewFile();
            debugFile = new PrintWriter(fname);
            btn_print.setVisible(true);
        } catch (IOException ex) {
            // file inaccessible
            outputInfoMsg (STATUS_ERROR, "UIFrame.setDebugOutputFile: for file: " + fname + ", " + ex);
            debugFile = null;
        }
    }

    private String get2DigitString (int value) {
        String strVal = (value < 10) ? "0" + value : "" + value;
        return strVal;
    }
    
    private String getCurrentDateTime () {
        LocalDateTime datetime = LocalDateTime.now();
        String strDate = "" + datetime.getYear();
        strDate += "-" + get2DigitString(datetime.getMonthValue());
        strDate += "-" + get2DigitString(datetime.getDayOfMonth());
        strDate += " " + get2DigitString(datetime.getHour());
        strDate += ":" + get2DigitString(datetime.getMinute());
        strDate += ":" + get2DigitString(datetime.getSecond());
        return strDate;
    }
    
    public void setTestOutputFile (String fname) {
        if (fname != null && !fname.isBlank()) {
            try {
                this.testFile = new PrintWriter(new FileWriter(fname, true));
                this.testFile.println("=== " + getCurrentDateTime() + " ============================================================");

            } catch (IOException ex) {
                System.out.println("UIFrame.setTestOutputFile: on creating file: " + fname + ", " + ex);
                this.testFile = null;
                fname = "";
            }
        } else {
            this.testFile = null;
            fname = "";
        }
        // update the properties file status
        props.setPropertiesItem(Property.TestFileOut, fname);
    }
    
    public void closeTestFile () {
        if (testFile != null) {
            testFile.flush();
            testFile.close();
        }
    }
    
    private void enableMessage (int msgType, boolean bEnable) {
        if (!bUseGUI) {
            Property flag;
            switch (msgType) {
                case STATUS_NORMAL: bMsgNormal = bEnable;  flag = Property.MsgNormal;       break;
                case STATUS_PARSER: bMsgParser = bEnable;  flag = Property.MsgParser;       break;
                case STATUS_SSHEET: bMsgSsheet = bEnable;  flag = Property.MsgSpreadsheet;  break;
                case STATUS_INFO:   bMsgInfo   = bEnable;  flag = Property.MsgInfo;         break;
                case STATUS_DEBUG:  bMsgDebug  = bEnable;  flag = Property.MsgDebug;        break;
                case STATUS_PROPS:  bMsgProps  = bEnable;  flag = Property.MsgProperties;   break;
                default:
                    return;
            }
            // update the properties file
            props.setPropertiesItem(flag, bEnable ? "1" : "0");
        } else {
            switch (msgType) {
                case STATUS_NORMAL: bMsgNormal = bEnable;  cbox_normal.setSelected(bEnable); break;
                case STATUS_PARSER: bMsgParser = bEnable;  cbox_parser.setSelected(bEnable); break;
                case STATUS_SSHEET: bMsgSsheet = bEnable;  cbox_ssheet.setSelected(bEnable); break;
                case STATUS_INFO:   bMsgInfo   = bEnable;  cbox_info  .setSelected(bEnable); break;
                case STATUS_DEBUG:  bMsgDebug  = bEnable;  cbox_debug .setSelected(bEnable); break;
                case STATUS_PROPS:  bMsgProps  = bEnable;  cbox_props .setSelected(bEnable); break;
                default:
                    break;
            }
        }
    }

    public void setMessageFlags (int debugFlags) {
        enableMessage(UIFrame.STATUS_NORMAL, (0 != (debugFlags & 1)));
        enableMessage(UIFrame.STATUS_PARSER, (0 != (debugFlags & 2)));
        enableMessage(UIFrame.STATUS_SSHEET, (0 != (debugFlags & 4)));
        enableMessage(UIFrame.STATUS_INFO  , (0 != (debugFlags & 8)));
        enableMessage(UIFrame.STATUS_DEBUG , (0 != (debugFlags & 16)));
        enableMessage(UIFrame.STATUS_PROPS , (0 != (debugFlags & 32)));
    }
    
    public void setDefaultStatus () {
        setTestOutputFile(props.getPropertiesItem(Property.TestFileOut, ""));

        enableMessage(UIFrame.STATUS_NORMAL, props.getPropertiesItem(Property.MsgNormal     , "0").contentEquals("1"));
        enableMessage(UIFrame.STATUS_PARSER, props.getPropertiesItem(Property.MsgParser     , "0").contentEquals("1"));
        enableMessage(UIFrame.STATUS_SSHEET, props.getPropertiesItem(Property.MsgSpreadsheet, "0").contentEquals("1"));
        enableMessage(UIFrame.STATUS_INFO  , props.getPropertiesItem(Property.MsgInfo       , "0").contentEquals("1"));
        enableMessage(UIFrame.STATUS_DEBUG , props.getPropertiesItem(Property.MsgDebug      , "0").contentEquals("1"));
        enableMessage(UIFrame.STATUS_PROPS , props.getPropertiesItem(Property.MsgProperties , "0").contentEquals("1"));
    }
    
    public void outputInfoMsg (int errLevel, String msg) {
        if (!bUseGUI) {
            boolean bEnableOutput = false;
            String time = elapsedTimerGet();
            switch (errLevel) {
                default:
                case STATUS_ERROR:
                    msg = "[ERROR ] " + msg;
                    bEnableOutput = true;
                    break;
                case STATUS_WARN:
                    msg = "[WARN  ] " + msg;
                    bEnableOutput = true;
                    break;
                case STATUS_NORMAL:
                    msg = "[NORMAL] " + msg;
                    if (bMsgNormal) bEnableOutput = true;
                    break;
                case STATUS_PARSER:
                    msg = "[PARSER] " + msg;
                    if (bMsgParser) bEnableOutput = true;
                    break;
                case STATUS_SSHEET:
                    msg = "[SSHEET] " + msg;
                    if (bMsgSsheet) bEnableOutput = true;
                    break;
                case STATUS_INFO:
                    msg = "[INFO  ] " + msg;
                    if (bMsgInfo)   bEnableOutput = true;
                    break;
                case STATUS_DEBUG:
                    msg = "[DEBUG ] " + msg;
                    if (bMsgDebug)  bEnableOutput = true;
                    break;
                case STATUS_PROPS:
                    msg = "[PROPS ] " + msg;
                    if (bMsgProps)  bEnableOutput = true;
                    break;
            }
            if (bEnableOutput) {
                msg = time + msg;
                if (testFile != null) {
                    testFile.println(msg);
                    // errors and warnings will always go to console, even if reporting to file
                    if (errLevel == STATUS_ERROR || errLevel == STATUS_WARN) {
                        System.out.println(msg);
                    }
                } else {
                    System.out.println(msg);
                }
            }
            return;
        }
        
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setFontFamily(attributes,"Courier");
        StyleConstants.setFontSize(attributes, 15);
        switch (errLevel) {
            default:
            case STATUS_ERROR:
                msg = "[ERROR] " + msg;
                StyleConstants.setForeground(attributes, generateColor (TextColor.Red));
                StyleConstants.setBold(attributes, true);
                StyleConstants.setItalic(attributes, false);
                break;
            case STATUS_WARN:
                msg = "[WARN] " + msg;
                StyleConstants.setForeground(attributes, generateColor (TextColor.Orange));
                StyleConstants.setBold(attributes, true);
                StyleConstants.setItalic(attributes, false);
                break;
            case STATUS_NORMAL:
                if (!bMsgNormal) {
                    return;
                }
                StyleConstants.setForeground(attributes, generateColor (TextColor.Black));
                StyleConstants.setBold(attributes, false);
                StyleConstants.setItalic(attributes, false);
                break;
            case STATUS_PARSER:
                if (!bMsgParser) {
                    return;
                }
                StyleConstants.setForeground(attributes, generateColor (TextColor.Blue));
                StyleConstants.setBold(attributes, false);
                StyleConstants.setItalic(attributes, true);
                break;
            case STATUS_SSHEET:
                if (!bMsgSsheet) {
                    return;
                }
                StyleConstants.setForeground(attributes, generateColor (TextColor.Green));
                StyleConstants.setBold(attributes, false);
                StyleConstants.setItalic(attributes, true);
                break;
            case STATUS_INFO:
                if (!bMsgInfo) {
                    return;
                }
                StyleConstants.setForeground(attributes, generateColor (TextColor.DkVio));
                StyleConstants.setBold(attributes, false);
                StyleConstants.setItalic(attributes, false);
                break;
            case STATUS_DEBUG:
                if (!bMsgDebug) {
                    return;
                }
                StyleConstants.setForeground(attributes, generateColor (TextColor.Brown));
                StyleConstants.setBold(attributes, false);
                StyleConstants.setItalic(attributes, false);
                break;
            case STATUS_PROPS:
                if (!bMsgProps) {
                    return;
                }
                StyleConstants.setForeground(attributes, generateColor (TextColor.Gold));
                StyleConstants.setBold(attributes, false);
                StyleConstants.setItalic(attributes, true);
                break;
        }

        txt_info.setCharacterAttributes(attributes, false);
        Document doc = txt_info.getDocument();
        try {
            doc.insertString(doc.getLength(), msg + NEWLINE, attributes);
            
            // scroll the text to the bottom of the page
            txt_info.setCaretPosition(txt_info.getDocument().getLength());
        } catch (BadLocationException ex) {
            Logger.getLogger(UIFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void elapsedTimerEnable() {
        elapsedStart = System.currentTimeMillis();
        showElapsed = true;
    }
    
    public void elapsedTimerDisable() {
        showElapsed = false;
    }
    
    private String elapsedTimerGet() {
        if (!showElapsed) {
            return "";
        }
        long elapsedTime = System.currentTimeMillis() - elapsedStart;
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
    
    private static Color cvtHSBtoColor (int h, int s, int b) {
        double hue    = (double) h / 360.0;
        double sat    = (double) s / 100.0;
        double bright = (double) b / 100.0;
        return Color.getHSBColor((float)hue, (float)sat, (float)bright);
    }
    
    /**
     * generates the specified text color for the debug display.
     * 
     * @param colorName - name of the color to generate
     * @return corresponding Color value representation
     */
    private static Color generateColor (TextColor colorName) {
        switch (colorName) {
            default:
            case Black:   return Color.BLACK;
            case DkGrey:  return Color.DARK_GRAY;
            case DkRed:   return cvtHSBtoColor (0,   100, 66);
            case Red:     return cvtHSBtoColor (0,   100, 90);
            case LtRed:   return cvtHSBtoColor (0,   60,  100);
            case Orange:  return cvtHSBtoColor (20,  100, 100);
            case Brown:   return cvtHSBtoColor (20,  80,  66);
            case Gold:    return cvtHSBtoColor (40,  100, 90);
            case Green:   return cvtHSBtoColor (128, 100, 45);
            case Cyan:    return cvtHSBtoColor (190, 80,  45);
            case LtBlue:  return cvtHSBtoColor (210, 100, 90);
            case Blue:    return cvtHSBtoColor (240, 100, 100);
            case Violet:  return cvtHSBtoColor (267, 100, 100);
            case DkVio:   return cvtHSBtoColor (267, 100, 66);
        }
    }

    private void outputSeparatorLine (String heading) {
        heading = "=====" + heading + "======================================================================";
        heading = heading.substring(0, 75);
        outputInfoMsg (STATUS_NORMAL, heading);
    }
}
