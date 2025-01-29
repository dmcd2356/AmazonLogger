package com.mycompany.amazonlogger;

/**
 *
 * @author dan
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import static com.mycompany.amazonlogger.AmazonReader.frame;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author dmcd2356
 */
public class PropertiesFile {
    // the location of the properties file for this application
    final static private String PROPERTIES_PATH = ".amazonreader/";
    final static private String PROPERTIES_FILE = "site.properties";

    private Properties   props;

    public static enum Property {
        // these are startup settings to use that are saved from last run
        PdfPath,                // initial path selection for PDF file
        SpreadsheetPath,        // initial path selection for spreadsheet file
        MsgParser,              // debug message enable flag for STATUS_PARSER
        MsgSpreadsheet,         // debug message enable flag for STATUS_SSHEET
        MsgInfo,                // debug message enable flag for STATUS_INFO
        MsgDebug,               // debug message enable flag for STATUS_DEBUG
        MsgProperties,          // debug message enable flag for STATUS_PROPS
        MaxLenDescription,      // max length of description to display
    };

    PropertiesFile () {
      
        props = null;
        FileInputStream in = null;
        File propfile = new File(PROPERTIES_PATH + PROPERTIES_FILE);
        if (propfile.exists()) {
            try {
                // property file exists, read it in
                in = new FileInputStream(PROPERTIES_PATH + PROPERTIES_FILE);
                props = new Properties();
                props.load(in);
                outputMsg("site.properties file found at: " + propfile.getAbsolutePath());
                return; // success!
            } catch (IOException ex) {
                errorMsg(ex + " <" + PROPERTIES_PATH + PROPERTIES_FILE + ">");
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        errorMsg(ex + " <" + PROPERTIES_PATH + PROPERTIES_FILE + ">");
                    }
                }
            }
        }

        // property file does not exist - create a default (empty) one
        props = new Properties();
        try {
            // first, check if properties directory exists
            File proppath = new File (PROPERTIES_PATH);
            if (!proppath.exists()) {
                proppath.mkdir();
            }

            // now save properties file
            outputMsg("Creating new empty site.properties file at: " + propfile.getAbsolutePath());
            File file = new File(PROPERTIES_PATH + PROPERTIES_FILE);
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                props.store(fileOut, "Initialization");
            }
        } catch (IOException ex) {
            errorMsg(ex + " <" + PROPERTIES_PATH + PROPERTIES_FILE + ">");
            props = null;
        }
    }
    
    public String getPropertiesItem (Property tag, String dflt) {
        if (props == null) {
            return dflt;
        }

        String value = props.getProperty(tag.name());
        if (value == null || value.isEmpty()) {
            // update properties file with default value, if given
            if (dflt != null && !dflt.isBlank()) {
                errorMsg("site.properties String <" + tag + "> : not found, setting to " + dflt);
                props.setProperty(tag.name(), dflt);
                writePropertiesFile ();
            }
            return dflt;
        }

        outputMsg("site.properties String <" + tag + "> = " + value);
        return value;
    }
  
    public Integer getPropertiesItem (Property tag, Integer dflt) {
        // if properties file does not exist, return the default value
        if (props == null) {
            return dflt;
        }

        // check if tag not found, or has an empty value
        // if so, update properties file with default value, if given, and return it
        String strValue = props.getProperty(tag.name());
        if (strValue == null || strValue.isEmpty()) {
            if (dflt != null) {
                errorMsg("site.properties Integer <" + tag + "> : not found, setting to " + dflt);
                props.setProperty(tag.name(), Integer.toString(dflt));
                writePropertiesFile ();
            }
            return dflt;
        }

        // tag is valid and has an non-null string value, check if it is an integer
        Integer iValue = 0;
        for (int ix = 0; ix < strValue.length(); ix++) {
            // if so, convert to integer and return it
            if (strValue.charAt(ix) >= '0' && strValue.charAt(ix) <= '9')
                iValue = (10 * iValue) + strValue.charAt(ix) - '0';
            else {
                // if not, return default value
                errorMsg("site.properties Integer <" + tag + "> : invalid integer value: " + strValue);
                return dflt;
            }
        }

        outputMsg("site.properties Integer <" + tag + "> = " + iValue);
        return iValue;
    }

    public void setPropertiesItem (Property tag, String value) {
        // save changes to properties file
        if (props == null) {
            return;
        }

        // make sure the properties file exists
        File propsfile = new File(PROPERTIES_PATH + PROPERTIES_FILE);
        if (propsfile.exists()) {
            if (value == null) {
                value = "";
            }
            String old_value = props.getProperty(tag.name());
            if (old_value == null) {
                old_value = "";
            }
            if (!old_value.equals(value)) {
                outputMsg("site.properties <" + tag + "> set to " + value);
            }
            props.setProperty(tag.name(), value);
            writePropertiesFile ();
        }
    } 
  
    public void setPropertiesItem (Property tag, Integer value) {
        setPropertiesItem (tag, Integer.toString(value));
    }
    
    private void writePropertiesFile () {
        try {
            FileOutputStream out = new FileOutputStream(PROPERTIES_PATH + PROPERTIES_FILE);
            props.store(out, "---No Comment---");
            out.close();
        } catch (IOException ex) {
            errorMsg(ex + "- site.properties");
        }
    }
    
    private void outputMsg (String msg) {
        if (frame != null) {
            frame.outputInfoMsg(UIFrame.STATUS_PROPS, msg);
        }
    }
    
    private void errorMsg (String msg) {
        if (frame != null) {
            frame.outputInfoMsg(UIFrame.STATUS_WARN, msg);
        }
    }
    
}