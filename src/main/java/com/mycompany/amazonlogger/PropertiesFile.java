/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

/**
 * This class handles reading from and writing to entries in the properties file.
 *   The file is a hidden file that is created in the directory of where
 *   the application is run. This way each directory that it runs from can have
 *   its own values. The entries are hard-coded for the project and describe
 *   some value that you would like to be changeable, yet also have the
 *   program remember the settings from the last time.
 * When PropertiesFile() is instantiated it checks for an existing
 *   site.properties file (hidden) in the current directory. If not found,
 *   it creates a blank one.
 * When retrieving a value from the file using the 'getPropertiesItem' method,
 *   if the entry is not found in the file, the default value passed in the call
 *   will be used and the property will be added to the file with that default
 *   value for the next time. Entries can also be added/modified directly
 *   using the 'putPropertiesItem' method.
 * 
 * @author dan
 */

import static com.mycompany.amazonlogger.AmazonReader.frame;

import java.io.File;
import java.io.FileInputStream;
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

    // these are the properties defined for this project
    public static enum Property {
        // these are startup settings to use that are saved from last run
        PdfPath,                // initial path selection for PDF file
        SpreadsheetPath,        // initial path selection for spreadsheet file
        MsgEnable,              // debug message enable flags
        MaxLenDescription,      // max length of description to display
        DebugFileOut,           // name of the output file to copy debug info to
        TestPath,               // test directory path
        TestFileOut,            // name of the output file to copy test report to
        SpreadsheetFile,        // name of the spreadsheet file selection
        SpreadsheetTab,         // name of the spreadsheet tab selection
    };

    /**
     * Constructor - opens the file for read/write and creates the file if one does not exist.
     */
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
    
    /**
     * get a property from the file defined as an String value.
     * 
     * @param tag  - the property tag (name of the property)
     * @param dflt - the default value to use if either the file is not found
     *                or the tag is not found in the file
     * 
     * @return the value associated with the property
     */
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
  
    /**
     * get a property from the file defined as an Integer value.
     * Note that all entries are stored as Strings, but you can define
     *   some properties as Integer, so that the get and put methods will
     *   do the proper conversions and flag an error if not in the correct format.
     * 
     * @param tag  - the property tag (name of the property)
     * @param dflt - the default value to use if either the file is not found
     *                or the tag is not found in the file
     * 
     * @return the value associated with the property
     */
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

    /**
     * sets a String value to be saved for a property in the file.
     * If the property is not found in the file, it will be created.
     * 
     * @param tag   - the property tag (name of the property)
     * @param value - the value to set the property to
     */
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
  
    /**
     * sets an Integer value to be saved for a property in the file.
     * If the property is not found in the file, it will be created.
     * Note that all entries are stored as Strings, but you can define
     *   some properties as Integer, so that the get and put methods will
     *   do the proper conversions and flag an error if not in the correct format.
     * 
     * @param tag   - the property tag (name of the property)
     * @param value - the value to set the property to
     */
    public void setPropertiesItem (Property tag, Integer value) {
        setPropertiesItem (tag, Integer.toString(value));
    }

    /**
     * write the properties data to the properties file
     */    
    private void writePropertiesFile () {
        try {
            FileOutputStream out = new FileOutputStream(PROPERTIES_PATH + PROPERTIES_FILE);
            props.store(out, "---No Comment---");
            out.close();
        } catch (IOException ex) {
            errorMsg(ex + "- site.properties");
        }
    }
    
    /**
     * the local function for interfacing with the method for writing a
     * non-error message.
     */    
    private void outputMsg (String msg) {
        if (frame != null) {
            frame.outputInfoMsg(UIFrame.STATUS_PROPS, msg);
        }
    }
    
    /**
     * the local function for interfacing with the method for writing an
     * error message.
     */    
    private void errorMsg (String msg) {
        if (frame != null) {
            frame.outputInfoMsg(UIFrame.STATUS_WARN, msg);
        }
    }
    
}