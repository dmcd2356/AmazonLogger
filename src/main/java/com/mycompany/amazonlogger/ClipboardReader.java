/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

/**
 *
 * @author dan
 */
public class ClipboardReader {
    
    private static final String CLASS_NAME = ClipboardReader.class.getSimpleName();

    private static StringTokenizer clipReader = null;
    private static BufferedReader  fileReader = null;
    
    public ClipboardReader () {
        // read the contents of the clipboard so we can parse it line by line
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Object clipObject = clipboard .getData(DataFlavor.stringFlavor);
            if (clipObject != null) {
                String strClipboard = clipboard .getData(DataFlavor.stringFlavor).toString();
                clipReader = new StringTokenizer(strClipboard,"\n");
            }
        }
        catch (UnsupportedFlavorException | IOException ex) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "ClipboardReader: " + ex);
            frame.disableAllButton();
        }
    }
    
    public ClipboardReader (File clipFile) {
        // read the contents from a file (for command line testing)
        try {
            if (clipFile != null) {
                FileReader fReader = new FileReader(clipFile);
                fileReader = new BufferedReader(fReader);
            }
        }
        catch (IOException ex) {
            frame.outputInfoMsg(UIFrame.STATUS_ERROR, "ClipboardReader: " + ex);
            frame.disableAllButton();
        }
    }
    
    /*********************************************************************
    ** reads the next line from the web text file (or clipboard).
    * 
    *  @return the next line of text
    * 
    * @throws IOException
    */
    public String getLine() throws IOException {
        String line = null;
        
        if (fileReader != null) {
            try {
                line = fileReader.readLine();
            } catch (IOException ex) {
                throw new IOException("ClipboardReader.getLine: " + ex);
            }
        } else if (clipReader != null) {
            if (clipReader.hasMoreTokens()) {
                line = clipReader.nextToken();
                if (! clipReader.hasMoreTokens())
                    clipReader = null;
            }
        }
        if (line == null) {
            this.close();
        }

        return line;
    }
    
    /*********************************************************************
    ** closes the Amazon web page file (or clipboard).
    * 
    * @throws java.io.IOException
    */
    public void close() throws IOException {
        if (fileReader != null) {
            try {
                fileReader.close();
            } catch (IOException ex) {
                throw new IOException("ClipboardReader.close: " + ex);
            }
        }
        clipReader = null;
        fileReader = null;
    }

}
