/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.dmcd.amazonlogger;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.stream.Stream;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

/**
 *
 * @author dan
 */
public class GUIOrderPanel {
    
    private static final String CLASS_NAME = GUIOrderPanel.class.getSimpleName();

    private static JTextPane    txtPane = null;

    // this holds the font color, type, etc for the message types
    private static final HashMap<Column, MsgControl> msgInfo  = new HashMap<>();

    private final class MsgControl {
        private final Integer   fieldSize;    // size of the field on the display
        private final char      justify;      // justification in field: L, R, C
        private final String    font;         // whether the displayed message is Normal, Bold, Italic, or both
        private final TextColor color;        // color to use for the text on the screen
        
        MsgControl (int size, char just, String font, TextColor color) {
            this.fieldSize = size;
            this.justify   = just;
            this.font      = font;       // N=normal, I=italic, B=Bold, BI=Bold+Italic
            this.color     = color;
        }
        
        public int getFieldSize() {
            return this.fieldSize;
        }
        
        public char getFieldJust() {
            return this.justify;
        }
        
        public String getFont() {
            return this.font;
        }
        
        public TextColor getColor() {
            return this.color;
        }
    }
    
    // this is the spacing used between fireld
    private static final int FIELD_GAP = 4;
    
    // these are similar to the entries in Spreadsheet, since they refer to those
    // entities in some cases, but there are additional ones added for PDF file output.
    // These are used to identify the item being displayed, and are used in
    //  specifying the characteristics (e.g. the field length) for that entry.
    private static enum Column { 
        OrderNumber, 
        DateOrdered,
        DateDelivered,
        DatePaid,
        Total, 
        ItemIndex,
        Qty, 
        Description, 
        ItemPrice, 
        Paid, 
        Pending, 
        Payment, 
        Refund, 
        Tax,
        Seller,
        UserName,
        Row,
    };

    private enum TextColor {
        Black, White, LtGrey, DkGrey, DkRed, Red, LtRed, Orange, Brown,
        Yellow, Gold, Green, Cyan, LtBlue, Blue, Violet, DkVio;
    }
    
    private static final String NEWLINE = System.getProperty("line.separator");

    GUIOrderPanel (JTextPane txt_info) {
        txtPane = txt_info;

        msgInfo.clear();
        // these are gathered by the YOUR ORDERS selection
        msgInfo.put(Column.DateOrdered  , new MsgControl (10, 'C', "N", TextColor.Blue));
        msgInfo.put(Column.OrderNumber  , new MsgControl (19, 'L', "B", TextColor.DkVio));
        msgInfo.put(Column.Total        , new MsgControl ( 7, 'R', "N", TextColor.Black));
        msgInfo.put(Column.DateDelivered, new MsgControl (10, 'C', "N", TextColor.Blue));
        msgInfo.put(Column.ItemIndex    , new MsgControl ( 6, 'L', "N", TextColor.Black));
        msgInfo.put(Column.Qty          , new MsgControl ( 2, 'L', "N", TextColor.Black));
        msgInfo.put(Column.Description  , new MsgControl (50, 'L', "N", TextColor.DkVio));
        // these are gathered by the INVOICE selection
        msgInfo.put(Column.DatePaid     , new MsgControl (10, 'C', "N", TextColor.Blue));
        msgInfo.put(Column.ItemPrice    , new MsgControl ( 7, 'R', "N", TextColor.Green));
        msgInfo.put(Column.Tax          , new MsgControl ( 5, 'R', "N", TextColor.Green));
        msgInfo.put(Column.Seller       , new MsgControl (20, 'L', "N", TextColor.Green));
        // these are gathered or computed by the PDF file
        msgInfo.put(Column.Paid         , new MsgControl ( 7, 'R', "I", TextColor.Blue));
        msgInfo.put(Column.UserName     , new MsgControl (12, 'L', "B", TextColor.Black));
        msgInfo.put(Column.Row          , new MsgControl ( 4, 'L', "N", TextColor.Black));
        // these are read from the spreadsheet file for PDFbalancing
        msgInfo.put(Column.Payment      , new MsgControl ( 7, 'R', "N", TextColor.Green));
        msgInfo.put(Column.Refund       , new MsgControl ( 7, 'R', "B", TextColor.Blue));
        msgInfo.put(Column.Pending      , new MsgControl ( 7, 'R', "I", TextColor.Blue));
        // also from spreadsheet: Total, which is already listed above
    }

    public static void init() {
    }
    
    /**
     * clear all GUI messages
     */
    public static void clearMessages () {
        if (! GUIMain.isGUIMode()) {
            return;
        }

        txtPane.setText("");
    }

    /**
     * saves the debug log information to the system clipboard.
     */
    public static void saveToClipboard() {
        String textToCopy = txtPane.getText();
        StringSelection stringSelection = new StringSelection(textToCopy);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }
    
    /**
     * saves the debug log information to the debug file selection.
     */
    public static void saveDebugToFile () {
        PrintWriter debugFile = GUIMain.getDebugOutputFile();
        if (debugFile != null) {
            String textToCopy = txtPane.getText();
            Stream<String> lines = textToCopy.lines();
            lines.forEach(debugFile::println);
            debugFile.flush();
            debugFile.close();
        }
    }

    public static void printOrderHeader() {
        if (! GUIMain.isGUIMode()) {
            return;
        }

        clearMessages();
        printText (Column.DateOrdered  , "Order date"  , true);        
        printText (Column.OrderNumber  , "Order number", true);        
        printText (Column.Total        , "Tot cost"    , true);        
        printText (Column.ItemIndex    , "Index"       , true);        
        printText (Column.Qty          , "Qty"         , true);        
        printText (Column.DateDelivered, "Del date"    , true);        
        printText (Column.ItemPrice    , "Cost"        , true);
        printText (Column.Tax          , "Tax"         , true);
        printText (Column.Seller       , "Seller"      , true);
        printText (Column.Description  , "Description" , true);
        printNewLine();
        printSeparator(190, "_");
    }
    
    /**
     * displays the order information.
     * 
     * @param orderInfo - the order information to display
     * @param bInvalid  - true if entry is already listed in the spreadsheet
     */
    public static void printOrder (AmazonOrder orderInfo, boolean bInvalid) {
        if (! GUIMain.isGUIMode() || orderInfo == null) {
            return;
        }

        int itemCount = orderInfo.getItemCount();
        for (int ix = 0; ix < itemCount; ix++) {
            printOrderItem (Column.DateOrdered  , orderInfo, ix, bInvalid);        
            printOrderItem (Column.OrderNumber  , orderInfo, ix, bInvalid);        
            printOrderItem (Column.Total        , orderInfo, ix, bInvalid);        
            printOrderItem (Column.ItemIndex    , orderInfo, ix, bInvalid);        
            printOrderItem (Column.Qty          , orderInfo, ix, bInvalid);        
            printOrderItem (Column.DateDelivered, orderInfo, ix, bInvalid);        
            printOrderItem (Column.ItemPrice    , orderInfo, ix, bInvalid);
            printOrderItem (Column.Tax          , orderInfo, ix, bInvalid);
            printOrderItem (Column.Seller       , orderInfo, ix, bInvalid);
            printOrderItem (Column.Description  , orderInfo, ix, bInvalid);
            printNewLine();
        }
    }

    /**
     * display the header for the PDF Balancing.
     * 
     * @param bClear - true if clear the display before outputting header
     */
    public static void printBalanceHeader(boolean bClear) {
        if (! GUIMain.isGUIMode()) {
            return;
        }

        // if this is the first entry, clear the screen
        if (bClear) {
            clearMessages();
        }
        printText (Column.UserName     , "User name"   , true);
        printText (Column.Row          , "Row"         , true);
        printText (Column.DatePaid     , "Date paid"   , true);
        printText (Column.OrderNumber  , "Order number", true);
        printText (Column.Paid         , "Paid"        , true);
        printText (Column.Total        , "Total"       , true, TextColor.Green);
        printText (Column.Payment      , "Payment"     , true, TextColor.Green);
        printText (Column.Refund       , "Refund"      , true, TextColor.Green);
        printText (Column.Pending      , "Pending"     , true, TextColor.Green);
        printNewLine();
        printSeparator(120, "_");
    }

    /**
     * displays the order information.
     * 
     * @param tabName   - name of the tab selection
     * @param entry     - the card transaction info to display
     */
    public static void printBalance (String tabName, CardTransaction entry) {
        if (! GUIMain.isGUIMode() || entry == null) {
            return;
        }

        printBalanceItem (Column.UserName     , entry);        
        printBalanceItem (Column.Row          , entry);        
        printBalanceItem (Column.DatePaid     , entry);        
        printBalanceItem (Column.OrderNumber  , entry);        
        printBalanceItem (Column.Paid         , entry);        
        printBalanceItem (Column.Total        , entry);        
        printBalanceItem (Column.Payment      , entry);
        printBalanceItem (Column.Refund       , entry);        
        printBalanceItem (Column.Pending      , entry);        
        printNewLine();
    }

    /**
     * gets the string value for the specified order.
     * 
     * @param orderInfo - the order contents
     * @param colName   - the selected entry desired
     * 
     * @return the string value of the selected order entry
     */
    private static String getOrderEntry (AmazonOrder orderInfo, Column colName) {
        String entry = null;
        switch (colName) {
            case OrderNumber:
                entry = orderInfo.getOrderNumber();
                break;
            case DateOrdered:
                LocalDate date = orderInfo.getOrderDate();
                if (date != null) {
                    entry = date.toString();
                }
                break;
            case DateDelivered:
                date = orderInfo.getDeliveryDate();
                if (date != null) {
                    entry = date.toString();
                }
                break;
            case Total:
                entry = Utils.cvtAmountToString(orderInfo.getTotalCost());
                break;
            case Tax:
                entry = Utils.cvtAmountToString(orderInfo.getTaxCost());
                break;
            default:
                break;
        }
        return entry;
    }
    
    /**
     * gets the string value for the specified item in the order.
     * 
     * @param orderInfo - the order contents
     * @param colName   - the selected entry desired
     * @param ix        - the item number within the order
     * 
     * @return the string value of the selected order entry
     */
    private static String getOrderItemEntry (AmazonOrder orderInfo, Column colName, int ix) {
        String entry = null;
        if (ix >= orderInfo.getItemCount()) {
            return entry;
        }
        AmazonItem item = orderInfo.getItem(ix);
        switch (colName) {
            case ItemIndex:
                Integer itemCount = orderInfo.getItemCount();
                if (itemCount > 1) {
                    entry = ix + " of " + itemCount;
                } else {
                    entry = "  -";
                }
                break;
            case Qty:
                Integer qty = item.getQuantity();
                if (qty != null) {
                    entry = qty.toString();
                }
                break;
            case DateDelivered:
                LocalDate date = item.getDeliveryDate();
                if (date != null) {
                    entry = date.toString();
                }
                break;
            case ItemPrice:
                entry = Utils.cvtAmountToString(item.getItemCost());
                break;
            case Seller:
                entry = item.getSeller();
                break;
            case Description:
                entry = item.getDescription();
                break;
            default:
                break;
        }
        return entry;
    }

    /**
     * gets the string value for the specified PDF transaction item.
     * 
     * @param orderInfo - the transaction contents
     * @param colName   - the selected entry desired
     * 
     * @return the string value of the selected transaction entry
     */
    private static String getBalanceEntry (CardTransaction transInfo, Column colName) {
        String entry = null;
        switch (colName) {
            case Row:
                Integer row = transInfo.getRow();
                if (row != null) {
                    // the displayed row is based on 1st line = 1, but the index is zero-based,
                    //  so add 1 for the deiplay.
                    entry = Integer.toString(row + 1);
                }
                break;
            case OrderNumber:
                entry = transInfo.getOrderNumber();
                break;
            case DatePaid:
                entry = transInfo.getTransDate();
                break;
            case Paid:
                entry = Utils.cvtAmountToString(transInfo.getAmount());
                break;
            case Payment:
                entry = transInfo.getPayment();
                break;
            case Pending:
                entry = transInfo.getPending();
                break;
            case Total:
                entry = transInfo.getTotal();
                break;
            case Refund:
                entry = transInfo.getRefund();
                break;
            default:
                break;
        }
        return entry;
    }

    /**
     * adds the required padding to fill the field size of the entry.
     * 
     * @param colName - identifies the data being passed
     * @param entry   - the entry value
     * 
     * @return the entry value padded to the field size, with an extra gap for spacing
     */
    private static String padEntry (Column colName, String entry) {
        if (entry == null) {
            entry = "null";
        }
        MsgControl font = msgInfo.get(colName);
        if (font != null) {
            switch (font.getFieldJust()) {
                case 'R':
                    // right-justify dollar amounts to align dec pt, so pad to left.
                    entry = Utils.padLeft (entry, font.getFieldSize());
                    break;
                case 'L':
                    // do left-justify, so pad to right.
                    entry = Utils.padRight(entry, font.getFieldSize());
                    break;
                default:
                    // else, must be center-justify, pad half to the left
                    entry = Utils.padCenter(entry, font.getFieldSize());
                    break;
            }

            // the spacing between fields always goes to the right.
            entry = Utils.padRight(entry, font.getFieldSize() + FIELD_GAP);
        }
        return entry;
    }
    
    /**
     * displays the specified item.
     * 
     * @param colName   - the name of the item we are placing
     * @param orderInfo - the order information to display
     * @param ix        - index of item in order (if more than 1)
     * @param bInvalid  - true if entry is already listed in the spreadsheet
     */
    private static void printOrderItem (Column colName, AmazonOrder orderInfo, int ix, boolean bInvalid) {
        if (! GUIMain.isGUIMode()) {
            return;
        }

        boolean bError = false;
        boolean bBold = false;
        boolean bItalic = false;

        String entry = getOrderEntry (orderInfo, colName);
        if (entry == null) {
            entry = getOrderItemEntry (orderInfo, colName, ix);
        }
        if (entry == null) {
            entry = "null";
            switch (colName) {
                // non-essential entries (these are supplied by invoice or details clips, which are not required)
                case DateDelivered:
                case ItemPrice:
                case Tax:
                case Seller:
                    break;
                default:
                    bError = true;
                    break;
            }
        } else if (colName == Column.Qty && entry.contentEquals("0")) {
            bError = true;
        }
        
        MsgControl font = msgInfo.get(colName);
        String    msgFont  = "N";
        TextColor msgColor = TextColor.Black;
        if (font != null) {
            msgColor = font.getColor();
            msgFont  = font.getFont();
        }

        // limit the field data to the max field size + field gap size - 1
        int maxlen = msgInfo.get(colName).getFieldSize();
        if (entry.length() > maxlen) {
            entry = entry.substring(0, maxlen);
        }
        entry = padEntry (colName, entry);
        
        if (msgFont.contentEquals("B") || msgFont.contentEquals("BI")) {
            bBold = true;
        }
        if (msgFont.contentEquals("I") || msgFont.contentEquals("BI")) {
            bItalic = true;
        }

        // get background color
        Color bkColor = Color.WHITE;
        if (bInvalid) {
            bkColor = Color.LIGHT_GRAY;
        } else if (bError) {
            bkColor = Color.YELLOW;
        }
        print (entry, msgColor, bBold, bItalic, bkColor);
    }

    /**
     * displays the specified item.
     * 
     * @param colName   - the name of the item we are placing
     * @param transInfo - the transaction entry info to display
     */
    private static void printBalanceItem (Column colName, CardTransaction transInfo) {
        if (! GUIMain.isGUIMode()) {
            return;
        }

        boolean bError = false;
        boolean bBold = false;
        boolean bItalic = false;

        String entry;
        if (colName == Column.UserName) {
            entry = transInfo.getTabName();
        } else {
            entry = getBalanceEntry (transInfo, colName);
            if (entry == null) {
                entry = "null";
                bError = true;
            }
        }
        
        MsgControl font = msgInfo.get(colName);
        String    msgFont  = "N";
        TextColor msgColor = TextColor.Black;
        if (font != null) {
            msgColor = font.getColor();
            msgFont  = font.getFont();
        }

        // this item we highlight a debit by setting the text color to red
        if (colName == Column.Paid && entry.indexOf('-') >= 0) {
            msgColor = TextColor.Red;
        }
        
        // limit the field data to the max field size + field gap size - 1
        int maxlen = msgInfo.get(colName).getFieldSize();
        if (entry.length() > maxlen) {
            entry = entry.substring(0, maxlen);
        }
        entry = padEntry (colName, entry);
        
        if (msgFont.contentEquals("B") || msgFont.contentEquals("BI")) {
            bBold = true;
        }
        if (msgFont.contentEquals("I") || msgFont.contentEquals("BI")) {
            bItalic = true;
        }

        // get background color
        Color bkColor = Color.WHITE;
        if (bError) {
            bkColor = Color.YELLOW;
        }
        print (entry, msgColor, bBold, bItalic, bkColor);
    }

    /**
     * displays the specified text in selected columns.
     * 
     * @param colName   - the name of the item we are placing
     * @param text      - text to display
     */
    private static void printText (Column colName, String text, boolean bBold) {
        text = padEntry (colName, text);
        print (text, TextColor.Black, bBold, false, Color.WHITE);
    }

    /**
     * displays the specified text in selected columns.
     * 
     * @param colName   - the name of the item we are placing
     * @param text      - text to display
     */
    private static void printText (Column colName, String text, boolean bBold, TextColor color) {
        text = padEntry (colName, text);
        print (text, color, bBold, false, Color.WHITE);
    }

    /**
     * prints a separator line between entries.
     */
    public static void printNewLine() {
        print (NEWLINE, TextColor.Black, false, false, Color.WHITE);
    }

    /**
     * prints a separator line between entries.
     * 
     * @param count   - the number of times to repeat the line pattern
     * @param pattern - the string pattern of the line to create
     */
    private static void printSeparator (int count, String pattern) {
        String line = "";
        for (int ix = 0; ix < count; ix++) {
            line = line + pattern;
        }
        print (line + NEWLINE, TextColor.Black, false, false, Color.WHITE);
    }
    
    /**
     * displays a line of text.
     * 
     * @param line    - the line of text to display
     * @param color   - the color to display text in
     * @param bBold   - true if BOLD
     * @param bItalic - true if ITALIC
     */
    private static void print (String line, TextColor color, boolean bBold, boolean bItalic, Color bkColor) {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setFontFamily(attributes,"Courier");
        StyleConstants.setFontSize(attributes, 15);

        // set the text color and font characteristics
        StyleConstants.setForeground(attributes, generateColor (color));
        StyleConstants.setBackground(attributes, bkColor);
        StyleConstants.setBold(attributes, bBold);
        StyleConstants.setItalic(attributes, bItalic);

        txtPane.setCharacterAttributes(attributes, false);
        Document doc = txtPane.getDocument();
        try {
            doc.insertString(doc.getLength(), line, attributes);
            // scroll the text to the bottom of the page
            txtPane.setCaretPosition(txtPane.getDocument().getLength());
        } catch (BadLocationException ex) {
            // ignore for now
        }
    }
    
    /**
     * convert Hue Saturation Brightness color value to a RGB Color format.
     * 
     * @param h - the Hue (0 to 360 degrees)
     * @param s - the Saturation (0 to 100 %)
     * @param b - the Brightness (0 to 100 %)
     * 
     * @return the corresponding RGB Color value
     */
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
            case White:   return Color.WHITE;
            case LtGrey:  return Color.LIGHT_GRAY;
            case DkGrey:  return Color.DARK_GRAY;
            case Yellow:  return Color.YELLOW;
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

}
