/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import com.mycompany.amazonlogger.Spreadsheet.Column;
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
    private static final HashMap<Spreadsheet.Column, MsgControl> msgInfo  = new HashMap<>();

    public final class MsgControl {
        Integer   fieldSize;    // size of the field on the display
        String    font;         // whether the displayed message is Normal, Bold, Italic, or both
        TextColor color;        // color to use for the text on the screen
        
        MsgControl (int size, String font, TextColor color) {
            this.fieldSize = size;
            this.font      = font;       // N=normal, I=italic, B=Bold, BI=Bold+Italic
            this.color     = color;
        }
    }
    
    private enum TextColor {
        Black, DkGrey, DkRed, Red, LtRed, Orange, Brown,
        Gold, Green, Cyan, LtBlue, Blue, Violet, DkVio;
    }
    
    private static final String NEWLINE = System.getProperty("line.separator");

    GUIOrderPanel (JTextPane txt_info) {
        txtPane = txt_info;

        msgInfo.clear();
        // these are gathered by the YOUR ORDERS selection
        msgInfo.put(Column.DateOrdered  , new MsgControl (10, "N", TextColor.Blue));
        msgInfo.put(Column.OrderNumber  , new MsgControl (19, "B", TextColor.Red));
        msgInfo.put(Column.Total        , new MsgControl ( 7, "N", TextColor.Gold));
        msgInfo.put(Column.DateDelivered, new MsgControl (10, "N", TextColor.Blue));
        msgInfo.put(Column.ItemIndex    , new MsgControl ( 6, "N", TextColor.Black));
        msgInfo.put(Column.Qty          , new MsgControl ( 2, "N", TextColor.Black));
        msgInfo.put(Column.Description  , new MsgControl (50, "N", TextColor.DkVio));
        // these are gathered by the INVOICE selection
        msgInfo.put(Column.ItemPrice    , new MsgControl ( 7, "N", TextColor.Green));
        msgInfo.put(Column.PreTaxCost   , new MsgControl ( 7, "N", TextColor.Green));
        msgInfo.put(Column.Tax          , new MsgControl ( 7, "N", TextColor.Green));
        msgInfo.put(Column.Pending      , new MsgControl ( 7, "I", TextColor.Green));
        msgInfo.put(Column.Payment      , new MsgControl ( 7, "I", TextColor.Green));
        msgInfo.put(Column.Refund       , new MsgControl ( 7, "B", TextColor.Green));
        msgInfo.put(Column.Seller       , new MsgControl (15, "N", TextColor.Green));
        msgInfo.put(Column.CreditCard   , new MsgControl (20, "I", TextColor.Black));
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
        printText (Column.DateOrdered  , "Order date"  , true, false);        
        printText (Column.OrderNumber  , "Order number", true, false);        
        printText (Column.Total        , "Tot cost"    , true, false);        
        printText (Column.ItemIndex    , "Index"       , true, false);        
        printText (Column.Qty          , "Qty"         , true, false);        
        printText (Column.DateDelivered, "Del date"    , true, false);        
        printText (Column.Description  , "Description" , true, true);
        print ("_______________________________________________________________________________________________________________________________________" + NEWLINE,
                TextColor.Black, false, false, false);
    }
    
    /**
     * displays the order information.
     * 
     * @param orderInfo - the order information to display
     */
    public static void printOrder (AmazonOrder orderInfo) {
        if (! GUIMain.isGUIMode() || orderInfo == null) {
            return;
        }

        int itemCount = orderInfo.getItemCount();
        for (int ix = 0; ix < itemCount; ix++) {
            printItem (Column.DateOrdered  , orderInfo, ix, false);        
            printItem (Column.OrderNumber  , orderInfo, ix, false);        
            printItem (Column.Total        , orderInfo, ix, false);        
            printItem (Column.ItemIndex    , orderInfo, ix, false);        
            printItem (Column.Qty          , orderInfo, ix, false);        
            printItem (Column.DateDelivered, orderInfo, ix, false);        
            printItem (Column.Description  , orderInfo, ix, true);
        }
    }

    private static String getOrderEntry (AmazonOrder orderInfo, Column colName) {
        String entry = null;
        switch (colName) {
            case DateOrdered:
                LocalDate date = orderInfo.getOrderDate();
                if (date != null) {
                    entry = date.toString();
                }
                break;
            case OrderNumber:
                entry = orderInfo.getOrderNumber();
                break;
            case Total:
                Integer cost = orderInfo.getTotalCost();
                if (cost != null) {
                    entry = Utils.cvtAmountToString(cost);
                }
                entry = Utils.padLeft(entry, 7); // this will align the dec pt
                break;
            default:
                break;
        }
        return entry;
    }
    
    private static String getOrderItemEntry (AmazonOrder orderInfo, Column colName, int ix) {
        String entry = null;
        if (ix >= orderInfo.item.size()) {
            return entry;
        }
        AmazonItem item = orderInfo.item.get(ix);
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
            case Description:
                entry = item.getDescription();
                break;
            default:
                break;
        }
        return entry;
    }
    
    private static String padEntry (Column colName, String entry) {
        MsgControl font = msgInfo.get(colName);
        if (font != null) {
            int fieldLen = font.fieldSize + 4; // 4 is the gap between fields
            entry = Utils.padRight(entry, fieldLen);
        }
        return entry;
    }
    
    /**
     * displays the specified item.
     * 
     * @param colName   - the name of the item we are placing
     * @param orderInfo - the order information to display
     * @param ix        - index of item in order (if more than 1)
     * @param term      - true if end of line
     */
    private static void printItem (Spreadsheet.Column colName, AmazonOrder orderInfo, int ix, boolean term) {
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
            bError = true;
        }
        
        MsgControl font = msgInfo.get(colName);
        String    msgFont  = "N";
        TextColor msgColor = TextColor.Black;
        if (font != null) {
            msgColor = font.color;
            msgFont  = font.font;
        }

        entry = padEntry (colName, entry);
        if (term) {
            entry = entry + NEWLINE;
        }
        
        if (msgFont.contentEquals("B") || msgFont.contentEquals("BI")) {
            bBold = true;
        }
        if (msgFont.contentEquals("I") || msgFont.contentEquals("BI")) {
            bItalic = true;
        }

        print (entry, msgColor, bBold, bItalic, bError);
    }

    /**
     * displays the specified text in selected columns.
     * 
     * @param colName   - the name of the item we are placing
     * @param text      - text to display
     * @param term      - true if end of line
     */
    private static void printText (Spreadsheet.Column colName, String text, boolean bBold, boolean term) {
        text = padEntry (colName, text);
        if (term) {
            text += NEWLINE;
        }
        print (text, TextColor.Black, bBold, false, false);
    }
    
    /**
     * displays a line of text.
     * 
     * @param line    - the line of text to display
     * @param color   - the color to display text in
     * @param bBold   - true if BOLD
     * @param bItalic - true if ITALIC
     */
    private static void print (String line, TextColor color, boolean bBold, boolean bItalic, boolean bHighlight) {
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setFontFamily(attributes,"Courier");
        StyleConstants.setFontSize(attributes, 15);

        // set the text color and font characteristics
        StyleConstants.setForeground(attributes, generateColor (color));
        StyleConstants.setBackground(attributes, bHighlight ? Color.YELLOW : Color.WHITE);
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

}
