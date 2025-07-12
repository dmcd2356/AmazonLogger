/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
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
public class GUILogPanel {

    private static final String CLASS_NAME = GUILogPanel.class.getSimpleName();

    private static JTextPane    txtPane = null;
    private static PrintWriter  testFile = null;        // the log file for network use
    private static String       testFname = "";         // name of the log file in network mode
    private static int          msgEnable;              // the cumulative bits that are enabled for logging
    private static int          logCounter = 0;
    private static boolean      bNetPrintEnable = true; // true if network receives all enabled msgs, false for just errors & warnings

    // this holds the font color, type, etc for the message types
    private static final HashMap<MsgType, MsgControl> fontInfo  = new HashMap<>();

    public final class MsgControl {
        int       bitValue;     // corresponding bit value for the message
        String    msgName;      // name to insert at begining of the msg to identify it
        String    font;         // whether the displayed message is Normal, Bold, Italic, or both
        TextColor color;        // color to use for the text on the screen
        
        MsgControl (int bitValue, String name, String font, TextColor color) {
            this.bitValue = bitValue;
            this.msgName  = name;
            this.font     = font;       // N=normal, I=italic, B=Bold, BI=Bold+Italic
            this.color    = color;
        }
    }
    
    private enum TextColor {
        Black, DkGrey, DkRed, Red, LtRed, Orange, Brown,
        Gold, Green, Cyan, LtBlue, Blue, Violet, DkVio;
    }
    
    // type of text characteristics to print with 'outputInfoMsg'
    public enum MsgType {
        NORMAL,      // output written to spreadsheet
        PARSER,      // parser status
        SSHEET,      // spreadsheet status
        INFO,        // processing of data from web clip and from PDF file
        PROPS,       // properties interface messages
        PROGRAM,     // program interface messages
        COMPILE,     // compiler messages
        VARS,        // compiler messages
        DEBUG,       // low-level detailed messages
        WARN,        // non-fatal warnings
        ERROR,       // fatal errors
    }
    private static final String NEWLINE = System.getProperty("line.separator");

    
    GUILogPanel (JTextPane txt_info) {
        txtPane = txt_info;

        fontInfo.clear();
        fontInfo.put(MsgType.ERROR   , new MsgControl (0x8000, "[ERROR ] ", "B", TextColor.Red));
        fontInfo.put(MsgType.WARN    , new MsgControl (0x4000, "[WARN  ] ", "B", TextColor.Orange));
        fontInfo.put(MsgType.DEBUG   , new MsgControl (0x0800, "[DEBUG ] ", "N", TextColor.Brown));
        fontInfo.put(MsgType.VARS    , new MsgControl (0x0080, "[VARS  ] ", "N", TextColor.DkVio));
        fontInfo.put(MsgType.COMPILE , new MsgControl (0x0040, "[COMPIL] ", "N", TextColor.DkVio));
        fontInfo.put(MsgType.PROGRAM , new MsgControl (0x0020, "[PROG  ] ", "N", TextColor.DkVio));
        fontInfo.put(MsgType.PROPS   , new MsgControl (0x0010, "[PROPS ] ", "I", TextColor.Gold));
        fontInfo.put(MsgType.INFO    , new MsgControl (0x0008, "[INFO  ] ", "N", TextColor.DkVio));
        fontInfo.put(MsgType.SSHEET  , new MsgControl (0x0004, "[SSHEET] ", "I", TextColor.Green));
        fontInfo.put(MsgType.PARSER  , new MsgControl (0x0002, "[PARSER] ", "I", TextColor.Blue));
        fontInfo.put(MsgType.NORMAL  , new MsgControl (0x0001, "[NORMAL] ", "N", TextColor.Black));

        msgEnable = 0;
        testFile = null;
        testFname = "";
        logCounter = 0;
    }

    public static void init() {
        msgEnable = 0;
        testFile = null;
        testFname = "";
        logCounter = 0;
    }
    
    /**
     * resets the GUI state after a script has been run.
     * called when a script has completed when running from network connection
     */
    public static void reset() {
        logCounter = 0;
    }

    /**
     * clear all GUI messages
     */
    public static void clearMessages () {
        txtPane.setText("");
    }

    /**
     * sets the flag to enable/disable log messages to the network.
     * If disabled, only ERROR and WARN messages will be sent.
     * 
     * @param enable - true to enable
     */
    public static void setNetworkDebugEnable (boolean enable) {
        bNetPrintEnable = enable;
    }
    
    /**
     * closes the test file output (used in non-GUI mode).
     */
    public static void closeTestFile () {
        if (testFile != null) {
            testFile.flush();
            testFile.close();
        }
    }

    public static void setMsgEnable (int value) {
        msgEnable = value;
    }
    
    /**
     * gets the bit value for the specified message type.
     * 
     * @param msgSelect - the message type
     * 
     * @return the font information
     */
    public static int getMsgEnableValue (MsgType msgSelect) {
        int bitValue = 1; // default
        MsgControl msgInfo = fontInfo.get(msgSelect);
        if (msgInfo != null) {
            bitValue = msgInfo.bitValue;
        }
        return bitValue;
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
            debugFile.println("=== " + GUILogPanel.getCurrentDateTime() + " ============================================================");
            String textToCopy = txtPane.getText();
            Stream<String> lines = textToCopy.lines();
            lines.forEach(debugFile::println);
            debugFile.flush();
            debugFile.close();
        }
    }
    
    /**
     * opens the specified test output file (non-GUI use) and places an initial
     *  header line in it.
     * 
     * @param fname - name of the test file
     * @param bAppend - true to append to existing file, false to create new file
     */
    public static void setTestOutputFile (String fname, boolean bAppend) {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        if (GUIMain.isGUIMode())
            return;
        
        String absPath = fname;
        if (absPath != null && !absPath.isBlank()) {
            if (absPath.charAt(0) != '/') {
                absPath = Utils.getDefaultPath (Utils.PathType.Test) + "/" + absPath;
            }
            PropertiesFile.setPropertiesItem(PropertiesFile.Property.TestFileAppend,  bAppend ? 1 : 0);
            if (testFname.contentEquals(absPath)) {
                String time = GUIMain.elapsedTimerGet();
                testFile.println(time + "[DEBUG ] " + functionId + "No change in output file setting");
                return;
            }
            
            // update the properties file status if we were successful
            PropertiesFile.setPropertiesItem(PropertiesFile.Property.TestFileOut, fname);
            testFname = absPath;

            // if the file already exists and we are not appending, delete it first
            File file = new File(absPath);
            if (file.isFile() && ! bAppend) {
                file.delete();
            }
                
            // if a file isn't already open, do it now
            closeTestFile ();
            try {
                testFile = new PrintWriter(new FileWriter(absPath, true));
                testFile.println("\n=== " + getCurrentDateTime() + " ============================================================");
            } catch (IOException ex) {
                System.out.println(functionId + "creating file: " + absPath + ", " + ex);
                testFile = null;
            }
        } else {
            testFile = null;
            PropertiesFile.setPropertiesItem(PropertiesFile.Property.TestFileOut, "");
        }
    }
    
    /**
     * outputs the specified message based on the message type reported.
     * 
     * Some messages can be enabled/disabled based on the 'MsgEnable' flag
     *  settings from the PropertiesFile, and some of these can also be selected
     *  from the GUI checkbox controls. The Error and Warning levels will always
     *  be reported. This will determine where the messages are to be output:
     *  when running the program from the GUI, the messages will be sent to
     *  the GUI display area. When run from the command line (also from a script file)
     *  the messages will be sent to a file, if one is specified in the 
     *  PropertiesFile as 'TestFileOut' or to stdout if not. For non-GUI use,
     *  the Error and Warning messages will always be sent to stdout, even if
     *  a test file output is specified.
     * A prefix is added to the message specifying the type of message, and
     *  for non-GUI use, this is preceded with a timestamp value as well.
     * 
     * @param msgType - the message type
     * @param msg     - the message to display
     */
    public static void outputInfoMsg (MsgType msgType, String msg) {
        if (msg == null || msg.isEmpty()) {
            return;
        }
        
        String    msgPrefix = "";
        String    msgFont  = "N";
        TextColor msgColor = TextColor.Black;
        MsgControl msgInfo = fontInfo.get(msgType);
        if (msgInfo != null) {
            msgPrefix = msgInfo.msgName;
            msgColor  = msgInfo.color;
            msgFont   = msgInfo.font;
        }

        // determine if the message is enabled
        boolean bErrorMsg  = msgType == MsgType.ERROR;
        boolean bErrOrWarn = bErrorMsg || (msgType == MsgType.WARN);
        boolean bIsEnabled = (getMsgEnableValue(msgType) & msgEnable) != 0;
        if (! bErrOrWarn && ! bIsEnabled) {
            return;
        }

        // show errors and warnings on display
        if (bErrOrWarn) {
            GUIMain.showErrorMsg(msg);
        }
        
        // if this contains any Exceptions, remove the extraneous header portion of them.
        if (bErrorMsg) {
            String header = "com.mycompany.amazonlogger.";
            int offset = msg.lastIndexOf(header);
            if (offset >= 0) {
                msg = msg.substring(offset + header.length());
            }
        }
        
        // affix prefix to message identifying the type of message
        msg = msgPrefix + msg;
        
        if (! GUIMain.isGUIMode()) {
            // MESSAGE OUTPUT FOR NON-GUI MODES:
            // add the timestamp to the begining of each message
            String time = GUIMain.elapsedTimerGet();
            msg = time + msg;

            // for error and warning messages, check for inclusion of call trace
            //  and separate into individual lines.
            if (!bErrOrWarn) {
                // not an error or warning, just print or save the line
                printLine (bErrOrWarn, msg);
            } else {
                ArrayList<String> array = new ArrayList<>(Arrays.asList(msg.split(" -> ")));
                printLine (bErrOrWarn, array.get(0).stripTrailing());
                for (int ix = 1; ix < array.size(); ix++) {
                    printLine (bErrOrWarn, time + msgPrefix + "    -> " + array.get(ix).stripLeading());
                }
            }
        } else {
            // MESSAGE OUTPUT FOR GUI MODE:
            // determine if printing in bold or italic
            boolean bBold = false;
            boolean bItalic = false;
            if (msgFont.contentEquals("B") || msgFont.contentEquals("BI")) {
                bBold = true;
            }
            if (msgFont.contentEquals("I") || msgFont.contentEquals("BI")) {
                bItalic = true;
            }

            SimpleAttributeSet attributes = new SimpleAttributeSet();
            StyleConstants.setFontFamily(attributes,"Courier");
            StyleConstants.setFontSize(attributes, 15);

            // set the text color and font characteristics
            StyleConstants.setForeground(attributes, generateColor (msgColor));
            StyleConstants.setBold(attributes, bBold);
            StyleConstants.setItalic(attributes, bItalic);

            txtPane.setCharacterAttributes(attributes, false);
            Document doc = txtPane.getDocument();
            try {
                doc.insertString(doc.getLength(), msg + NEWLINE, attributes);
                // scroll the text to the bottom of the page
                txtPane.setCaretPosition(txtPane.getDocument().getLength());
            } catch (BadLocationException ex) {
                Logger.getLogger(GUIMain.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private static void printLine (boolean bError, String msg) {
        if (testFile != null) {
            testFile.println(msg);
            // errors and warnings will always go to console, even if reporting to file
            if (bError) {
                System.out.println(msg);
            }
        } else if (AmazonReader.isOpModeCommmandLine()) {
            System.out.println(msg);
        }

        // if network connection, send to client
        if (AmazonReader.isOpModeNetwork()) {
            if (bNetPrintEnable || bError) {
                TCPServerThread.sendLogMessage(logCounter, msg);
                logCounter++;
            }
        }
    }
    
    /**
     * get the current date and time formatted as a String
     * 
     * @return the date/time formatted as: "YYYY-MM-DD HH:MM:SS"
     */
    private static String getCurrentDateTime () {
        LocalDateTime datetime = LocalDateTime.now();
        String strDate = "" + datetime.getYear();
        strDate += "-" + get2DigitString(datetime.getMonthValue());
        strDate += "-" + get2DigitString(datetime.getDayOfMonth());
        strDate += " " + get2DigitString(datetime.getHour());
        strDate += ":" + get2DigitString(datetime.getMinute());
        strDate += ":" + get2DigitString(datetime.getSecond());
        return strDate;
    }
    
    /**
     * converts an integer to a 2-digit decimal String.
     * 
     * @param value - the integer value (range 0 to 99)
     * 
     * @return a 2 character String: 00 - 99
     */
    private static String get2DigitString (int value) {
        String strVal = (value < 10) ? "0" + value : "" + value;
        return strVal;
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
