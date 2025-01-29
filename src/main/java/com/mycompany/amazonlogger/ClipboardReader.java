package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.StringTokenizer;

/**
 *
 * @author dan
 */
public class ClipboardReader {
    
    private static StringTokenizer clippy = null;
    
    /*********************************************************************
    ** opens the clipboard UI entity for reading web text file input from.This reads the contents of the current system clipboard and sends
    it to a tokenizer to be read off one line at a time.
    * 
    *  @return true if successful
    */
    public static boolean webClipOpen() {
        boolean bSuccess = false;
        
        try {
            // read the contents of the clipboard so we can parse it line by line
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Object clipObject = clipboard .getData(DataFlavor.stringFlavor);
            if (clipObject != null) {
                String strClipboard = clipboard .getData(DataFlavor.stringFlavor).toString();
                clippy = new StringTokenizer(strClipboard,"\n");
                bSuccess = true;
            }
        }
        catch (UnsupportedFlavorException ex) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "*** webClipOpen: UnsupportedFlavorException");
            frame.disableAllButton();
        }
        catch (IOException ex) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "*** webClipOpen: IOException");
            frame.disableAllButton();
        }
        return bSuccess;
    }
    
    /*********************************************************************
    ** reads the next line from the web text file (or clipboard).
    * 
    *  @return the next line of text
    */
    public static String webClipGetLine() {
        String line = null;
        
        if (clippy != null && clippy.hasMoreTokens()) {
            line = clippy.nextToken();
            if (line == null || !clippy.hasMoreTokens())
                clippy = null;
        }

        return line;
    }
    
    /*********************************************************************
    ** closes the Amazon web page file (or clipboard).
    */
    public static void webClipClose() {
        clippy = null;
    }

}
