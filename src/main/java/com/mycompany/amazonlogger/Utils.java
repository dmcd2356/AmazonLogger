/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.AmazonReader.props;
import static com.mycompany.amazonlogger.UIFrame.STATUS_DEBUG;
import static com.mycompany.amazonlogger.UIFrame.STATUS_ERROR;

import java.awt.Color;
import java.io.File;
import java.util.Objects;

/**
 *
 * @author dan
 */
public class Utils {
    
    private static final String CLASS_NAME = Utils.class.getSimpleName();

    public enum PathType {
        PDF,
        Spreadsheet,
        Test,
    }
    
    /*********************************************************************
    ** returns the unsigned integer value from a string of digits.
    * 
    *  @param str    - the string containing the digits to convert
    *  @param offset - the string offset in which to begin extracting
    *  @param maxlen - the max expected string length (0 if read until non-digit)
    * 
    *  @return the corresponding integer value
    * 
    * @throws ParserException if invalid format of input
    */
    public static Integer getIntFromString (String str, int offset, int maxlen) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        Integer value;
        
        if (str == null) {
            throw new ParserException(functionId + "Input string was null");
        }
        if (offset >= str.length()) {
            throw new ParserException(functionId + "Index offset " + offset + " exceeds string length of " + str.length());
        }
        if (maxlen > 0 && str.length() < offset + maxlen) {
            throw new ParserException(functionId + "Index offset " + offset + " + expected length of " + maxlen + " exceeds string length of " + str.length());
        }
        // limit # digits read by specified amount. if unspecified, run until non-digit
        int limit = offset + maxlen;
        if (maxlen == 0)
            limit = str.length();

        value = 0;
        for (int ix = offset; ix < limit; ix++) {
            if (str.charAt(ix) >= '0' && str.charAt(ix) <= '9') {
                value = value * 10 + str.charAt(ix) - '0';
            } else if (maxlen == 0) {
                return value;
            } else {
                throw new ParserException(functionId + "Invalid numeric in string: " + str.substring(offset));
            }
        }
        return value;
    }
    
    /*********************************************************************
    ** returns the unsigned integer value from a string of digits.
    *  Ignores leading spaces and terminates length on trailing space or EOL.
    *  String must be composed of only digits and must be at least as long
    *   as the length specified.
    * 
    *  @param str    - the string containing the digits to convert
    *  @param length - the maximum string length to read (0 means use full string length)
    * 
    *  @return the corresponding integer value (always >= 0)
    *          -1 if invalid format
    */
    public static int getIntegerValue (String str, int length) {
        // check for empty string
        if (str == null || str.length() == 0) {
            return -1;
        }
        // skip any leading and trailing space characters
        int ix;
        for (ix = 0; ix < str.length() && str.charAt(ix) == ' '; ix++) { }
        if (ix > 0)
            str = str.substring(ix);
        int offset = str.indexOf(' ');
        if (offset >= 0)
            str = str.substring(0, offset);
        
        // if passed length is set to 0, it means to use the full length of the string
        // up to the first space character.
        // otherwise, it fails if the string length is less than the specified length.
        if (length == 0) {
            length = str.length();
        } else if (str.length() < length) {
            return -1;
        }
        try {  
            int value = Integer.parseUnsignedInt(str.substring(0, length));  
            return value;
        } catch(NumberFormatException e){  
            return -1;  
        }  
    }

    /*********************************************************************
    ** converts the integer cost in cents into a string in dollar amount.
    * 
    *  @param amtCents - the cost amount in cents
    * 
    *  @return the corresponding dollar amount as a string (e.g. 12.56 or -123.00)
    */
    public static String cvtAmountToString (int amtCents) {
        String strAmt;
        int amtDollers = amtCents / 100;
        amtCents = amtCents % 100;
        if (amtCents < 0) {
            amtCents = -1 * amtCents;
        }
        if (amtCents < 10)
            strAmt = amtDollers + ".0" + amtCents;
        else
            strAmt = amtDollers + "." + amtCents;
        return strAmt;
    }

    /*********************************************************************
    ** returns the amount value in cents.
    * 
    *  @param str - the string containing the digits to convert
    *               characters must be decimal digits, dec pt and optional '$' and minus sign
    *               max value is $99999.99 and must have 2 digits following dec pt.
    * 
    *  @return the corresponding integer amount in cents (can be negative)
    * 
    *  @throws ParserException - if invalid char found
    */
    public static int getAmountValue (String str) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        // check for empty string
        if (str == null) {
            throw new ParserException(functionId + "Input string was null");
        }
        // skip any leading and trailing space characters
        int ix;
        for (ix = 0; ix < str.length() && str.charAt(ix) == ' '; ix++) { }
        if (ix > 0)
            str = str.substring(ix);
        int offset = str.indexOf(' ');
        if (offset >= 0)
            str = str.substring(0, offset);
        
        // check for invalid string length (.01 to -$99999.99)
        if (str.length() < 3 || str.length() > 10) {
            throw new ParserException(functionId + "invalid string length (" + str.length() + "): " + str);
        }
        
        // verify value only contains valid characters
        int iValue = 0;
        boolean bSign = false;
        int iDecCnt = -1;
        for (ix = 0; ix < str.length(); ix++) {
            if (str.charAt(ix) == '-') {
                bSign = true;
            } else if (str.charAt(ix) == '.') {
                iDecCnt = 0;
            } else if (str.charAt(ix) >= '0' && str.charAt(ix) <= '9') {
                iValue = (iValue * 10) + str.charAt(ix) - '0';
                if (iDecCnt >= 0) {
                    iDecCnt++;
                }
            } else if (str.charAt(ix) != '+' && str.charAt(ix) != '$') {
                throw new ParserException(functionId + "invalid character (" + str.charAt(ix) + "): " + str);
            }
        }
        if (iDecCnt != 2) {
            throw new ParserException(functionId + "invalid number of decimal characters (" + iDecCnt + "): " + str);
        }
        
        if (bSign)
            iValue = iValue * -1;
        
        return iValue;
    }

    /*********************************************************************
    ** skips past any leading spaces in the string passed and returns a string
    *  having the specified min and max length.
    * 
    *  @param line   - the line to read in
    *  @param minlen - the minimum string length to extract
    *  @param maxlen - the maximum string length to extract (0 to use remaining length of string)
    * 
    *  @return the specified substring contained in the string passed
    *
    *  @throws ParserException
    */
    public static String getNextWord (String line, int minlen, int maxlen) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        int offset;
        if (line == null) {
            throw new ParserException(functionId + "Input string was null");
        }
        for (offset = 0; offset < line.length() && line.charAt(offset) == ' '; offset++) { }
        line = line.substring(offset);
        if (line.length() == 0) {
            throw new ParserException(functionId + "next word not found in line");
        }
        if (line.length() < minlen) {
            throw new ParserException(functionId + "word < minimum length of " + minlen + "chars: " + line);
        }
        if (maxlen <= 0) { // this indicates we want the entire length of remaining string
            maxlen = line.length();
        }
        else if (line.length() < maxlen) {
            maxlen = line.length(); // limit max length to length of remaining string
        }
        return line.substring(0, maxlen);
    }

    /*********************************************************************
    ** returns the background color assigned for the specified month.
    * 
    *  @param month - the month to look up
    * 
    *  @return the Color selection corresponding to that month
    *          (0 will return white to clear the color selection)
    */
    public static Color getColorOfTheMonth (int month) {
        Color color;
        
        switch(month) {
            case 1:  color = new Color(0x1FFFFF); break;    // cyan
            case 2:  color = new Color(0x1FFF7F); break;    // green
            case 3:  color = new Color(0xDFBFBF); break;    // brown
            case 4:  color = new Color(0xFFFF3F); break;    // yellow
            case 5:  color = new Color(0xFF9FBF); break;    // pink
            case 6:  color = new Color(0xFF9F1F); break;    // orange
            case 7:  color = new Color(0x7FDFFF); break;    // blue
            case 8:  color = new Color(0xFFDFFF); break;    // light pink
            case 9:  color = new Color(0xBFBFFF); break;    // lavender
            case 10: color = new Color(0xFFDF3F); break;    // gold
            case 11: color = new Color(0x9FFFFF); break;    // light blue
            case 12: color = new Color(0xBFFF7F); break;    // light green
            default:
            case 0:  color = Color.white;     break;
            case -1: color = Color.lightGray; break;
        }

        return color;
    }

    /*********************************************************************
    ** returns a color specified by either the RGB palette or the HSB.
    *  The value rgbHSB contains the values of each RGB or HSB component
    *  packed into a single value. Each element (R, G, B and H, S, B) has
    *  a range of 0 to 255. For the HSB, these values are supposed to be in
    *  a range of 0.0 to 1.0, so they are correspondingly divided by 256
    *  to give this range.
    * 
    *  @param type - RGB or HSB (if null or undefined, return white)
    *  @param rgbHSB - the RGB or HSB value
    * 
    *  @return the Color selection
    * 
    *  @throws ParserException
    */
    public static Color getColor (String type, int rgbHSB) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        Color myColor = Color.white;
        
        if (type == null) {
            throw new ParserException(functionId + "Input string was null");
        }
        if (rgbHSB > 0xFFFFFF) rgbHSB = 0xFFFFFF;
        if (rgbHSB < 0)        rgbHSB = 0;

        int R = (rgbHSB & 0xFF0000) >> 16;
        int G = (rgbHSB & 0xFF00) >> 8;
        int B =  rgbHSB & 0xFF;
        
        if (type.contentEquals("HSB")) {
            float fH = (float)R / (float)256;
            float fS = (float)G / (float)256;
            float fB = (float)B / (float)256;
            myColor = Color.getHSBColor(fH, fS, fB);
        } else if (type.contentEquals("RGB")) {
            myColor = new Color(R, G, B);
        }
        
        return myColor;
    }

    /**
     * converts a String value to Boolean.
     * 
     * @param strValue - value to convert (can be true/false or 0/1)
     * 
     * @return true or false
     * 
     * @throws ParserException 
     */    
    public static Boolean getBooleanValue (String strValue) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        if (strValue == null) {
            throw new ParserException(functionId + "Input string was null");
        }
        if (strValue.contentEquals("0") || strValue.compareToIgnoreCase("FALSE") == 0) {
            return false;
        }
        if (strValue.contentEquals("1") || strValue.compareToIgnoreCase("TRUE") == 0) {
            return true;
        }
        throw new ParserException(functionId + "Invalid Boolean value: " + strValue);
    }

    /**
     * converts a String value to an Integer
     * 
     * @param strValue - value to convert
     * 
     * @return the corresponding Integer value
     * 
     * @throws ParserException 
     */
    public static Long getIntValue (String strValue) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        if (strValue == null) {
            throw new ParserException(functionId + "Input string was null");
        }
        if (strValue.isEmpty()) {
            throw new ParserException(functionId + "Zero length input");
        }
        Long intValue;
        try {
            intValue = Long.valueOf(strValue);
        } catch (NumberFormatException ex) {
            throw new ParserException(functionId + "Invalid Integer value: " + strValue);
        }
        return intValue;
    }
    
    /**
     * converts a String hexadecimal value to an Integer
     * 
     * @param strValue - value to convert (must begin with either 'x' or '0x')
     * 
     * @return the corresponding unsigned Integer value
     * 
     * @throws ParserException 
     */
    public static Integer getHexValue (String strValue) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (strValue == null) {
            throw new ParserException(functionId + "Input string was null");
        }
        if (strValue.isEmpty()) {
            throw new ParserException(functionId + "Zero length input");
        }
        Integer retVal;
        int offset;
        if (strValue.charAt(0) == 'x') {
            offset = 1;
        } else if (strValue.startsWith("0x")) {
            offset = 2;
        }
        else {
            return null;
        }
        try {
            retVal = Integer.parseUnsignedInt(strValue.substring(offset), 16);
        } catch (NumberFormatException ex) {
            throw new ParserException(functionId + "Invalid Hexadecimal value: " + strValue);
        }
        return retVal;
    }

    /**
     * converts an Integer value to a 4-digit hexadecimal String
     * 
     * @param intValue - value to convert
     * 
     * @return the corresponding hex value
     * 
     * @throws ParserException
     */
    public static String toHexWordValue (Integer intValue) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (intValue == null) {
            throw new ParserException(functionId + "Input string was null");
        }
        if (intValue < 0x100) {
            return String.format("0x%02X", intValue);
        }
        if (intValue < 0x10000) {
            return String.format("0x%04X", intValue);
        }
        if (intValue < 0x1000000) {
            return String.format("0x%06X", intValue);
        }
        return String.format("0x%08X", intValue);
    }
    
    /**
     * gets the directory path portion of a filename
     * 
     * @param file - the filename (including path)
     * 
     * @return the path only
     */
    public static String getFilePath (File file) {
        if (file == null)
            return "";
        String filePath = file.getAbsolutePath();
        if (filePath == null)
            return "";
        int offset = filePath.lastIndexOf('/');
        if (offset > 0) {
            filePath = filePath.substring(0, offset);
        }
        return filePath;
    }

    /**
     * gets the root filename portion of a filename (excludes file extension)
     * 
     * @param file - the filename (including path)
     * 
     * @return the root filename only
     */
    public static String getFileRootname (File file) {
        if (file == null)
            return "";
        String fileName = file.getName();
        if (fileName == null)
            return "";
        int offset = fileName.lastIndexOf('.');
        if (offset > 0)
            fileName = fileName.substring(0, offset);
        return fileName;
    }

    /**
     * gets the file extension portion of a filename
     * 
     * @param file - the filename (including path)
     * 
     * @return the extension only
     */
    public static String getFileExtension (File file) {
        if (file == null)
            return "";
        String fileName = file.getName();
        if (fileName == null)
            return "";
        int offset = fileName.lastIndexOf('.');
        if (offset <= 0)
            return "";
        return fileName.substring(offset); // include the leading '.'
    }

    /**
     * gets a properties file value that describes a directory path
     * 
     * @param tag - the name of the property desired, that is a path
     * 
     * @return the path specified
     */
    public static String getPathFromPropertiesFile (PropertiesFile.Property tag) {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        String validPath = null;
        String pathName = props.getPropertiesItem(tag, "");
        if (pathName != null && !pathName.isEmpty()) {
            if (pathName.charAt(0) == '~') {
                pathName = System.getProperty("user.home") + pathName.substring(1);
            }
            File tempDir = new File(pathName);
            if (tempDir.exists() && tempDir.isDirectory()) {
                validPath = pathName;
            } else {
                frame.outputInfoMsg(UIFrame.STATUS_WARN, functionId + "Properties file '"
                                + tag.name() + "' entry not a valid directory: " + pathName);
            }
        }
        return validPath;
    }

    /**
     * gets the path used for accessing files from properties file
     * 
     * @param type - type of file
     * 
     * @return the test path
     */
    public static String getDefaultPath (PathType type) {
        String pathname;
        switch (type) {
            case PDF:
                pathname = getPathFromPropertiesFile (PropertiesFile.Property.PdfPath);
                break;
            case Spreadsheet:
                pathname = getPathFromPropertiesFile (PropertiesFile.Property.SpreadsheetPath);
                break;
            default:
            case Test:
                pathname = getPathFromPropertiesFile (PropertiesFile.Property.TestPath);
                break;
        }
        if (pathname == null || pathname.isBlank()) {
            pathname = System.getProperty("user.dir");
        }
        return pathname;
    }

    /**
     * sets the default path used for accessing files from properties file
     * 
     * @param type     - type of file
     * @param pathname - the path to assign as default to the specified file type
     * 
     * @throws ParserException
     */
    public static void setDefaultPath (PathType type, String pathname) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (pathname == null) {
            throw new ParserException(functionId + "Input path name was null");
        }
        // Java doesn't get the '~' char, so change it to the home dir
        if (pathname.charAt(0) == '~') {
            pathname = System.getProperty("user.home") + pathname.substring(1);
        }
        
        switch (type) {
            case PDF:
                props.setPropertiesItem (PropertiesFile.Property.PdfPath, pathname);
                break;
            case Spreadsheet:
                props.setPropertiesItem (PropertiesFile.Property.SpreadsheetPath, pathname);
                break;
            default:
            case Test:
                props.setPropertiesItem (PropertiesFile.Property.TestPath, pathname);
                break;
        }
    }

    /**
     * test if a directory path is valid
     * 
     * @param dirname - name of the directory
     * 
     * @return the directory File
     * 
     * @throws ParserException 
     */
    public static File checkDir (String dirname) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (dirname == null || dirname.isBlank()) {
            throw new ParserException(functionId + "Input path name was null");
        }
        
        File myPath = new File(dirname);
        if (!myPath.isDirectory()) {
            throw new ParserException(functionId + "Path not found: " + dirname);
        }
        frame.outputInfoMsg(UIFrame.STATUS_DEBUG, "  Path param valid: " + dirname);
        return myPath;
    }

    /**
     * test if a file name is valid
     * 
     * @param fname     - name of the file (referenced from base path)
     * @param type      - the file extension allowed (null of blank if don't check)
     * @param filetype  - name of file type (only used for debug msgs & can be null)
     * @param bWritable - true if check if file is writable (else only check if readable)
     * 
     * @return the File
     * 
     * @throws ParserException 
     */
    public static File checkFilename (String fname, String type, PathType filetype, boolean bWritable) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        if (type != null && !type.isBlank() && !fname.endsWith(type)) {
            throw new ParserException(functionId + "Invalid " + filetype + " filename: " + fname);
        }
        if (fname == null || fname.isBlank()) {
            throw new ParserException(functionId + "Invalid " + filetype + " filename is blank");
        }
        
        fname = getDefaultPath(filetype) + "/" + fname;
        File myFile = new File(fname);
        if (!myFile.canRead()) {
            throw new ParserException(functionId + "Invalid " + filetype + " file - no read access: " + fname);
        }
        if (bWritable && !myFile.canWrite()) {
            throw new ParserException(functionId + "Invalid " + filetype + " file - no write access: " + fname);
        }
        if (bWritable) {
            frame.outputInfoMsg(UIFrame.STATUS_DEBUG, "  File exists & is readable and writable: " + fname);
        } else {
            frame.outputInfoMsg(UIFrame.STATUS_DEBUG, "  File exists & is readable: " + fname);
        }
        return myFile;
    }

    /**
     * This performs a comparison of 2 Long parameters.
     * 
     * @param param1   - the first value to compare
     * @param param2   - the parameter to compare it to
     * @param compType - the type of comparison to perform
     * 
     * @return true if condition is true.
     * 
     * @throws ParserException
     */
    public static boolean compareParameterValues (Long param1, Long param2, String compType) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + " (I): ";
        
        boolean bExit = false;
        
        if (param1 == null || param2 == null || compType == null) {
            throw new ParserException(functionId + "Input param was null");
        }
        
        switch (compType) {
            case "<":   if (param1 < param2) bExit = true;
                break;
            case "<=":  if (param1 <= param2) bExit = true;
                break;
            case ">":   if (param1 > param2) bExit = true;
                break;
            case ">=":  if (param1 >= param2) bExit = true;
                break;
            case "!=":  if (!Objects.equals(param1, param2)) bExit = true;
                break;
            case "==":  if (Objects.equals(param1, param2)) bExit = true;
                break;
            default:
                throw new ParserException(functionId + "Invalid comparison sign: " + compType);
        }
        frame.outputInfoMsg(STATUS_DEBUG, functionId + " " + param1 + " " + compType + " " + param2 + " " + bExit);
        return bExit;
    }
    
    /**
     * This performs a comparison of 2 Integer parameters.
     * 
     * @param param1   - the first value to compare
     * @param param2   - the parameter to compare it to
     * @param compType - the type of comparison to perform
     * 
     * @return true if condition is true.
     * 
     * @throws ParserException
     */
    public static boolean compareParameterValues (Integer param1, Integer param2, String compType) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + " (U): ";
        
        boolean bExit = false;
        
        if (param1 == null || param2 == null || compType == null) {
            throw new ParserException(functionId + "Input param was null");
        }
        
        switch (compType) {
            case "<":   if (param1 < param2) bExit = true;
                break;
            case "<=":  if (param1 <= param2) bExit = true;
                break;
            case ">":   if (param1 > param2) bExit = true;
                break;
            case ">=":  if (param1 >= param2) bExit = true;
                break;
            case "!=":  if (!Objects.equals(param1, param2)) bExit = true;
                break;
            case "==":  if (Objects.equals(param1, param2)) bExit = true;
                break;
            default:
                throw new ParserException(functionId + "Invalid comparison sign: " + compType);
        }
        frame.outputInfoMsg(STATUS_DEBUG, functionId + " " + param1 + " " + compType + " " + param2 + " " + bExit);
        return bExit;
    }
    
    /**
     * This performs a comparison of 2 String parameters.
     * 
     * @param param1   - the first value to compare
     * @param param2   - the parameter to compare it to
     * @param compType - the type of comparison to perform
     * 
     * @return true if condition is true.
     * 
     * @throws ParserException
     */
    public static boolean compareParameterValues (String param1, String param2, String compType) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + " (S): ";
        
        boolean bExit = false;
        
        if (param1 == null || param2 == null || compType == null) {
            throw new ParserException(functionId + "Input param was null");
        }
        
        switch (compType) {
            case "<":   if (param1.compareTo(param2) < 0) bExit = true;
                break;
            case "<=":  if (param1.compareTo(param2) <= 0) bExit = true;
                break;
            case ">":   if (param1.compareTo(param2) > 0) bExit = true;
                break;
            case ">=":  if (param1.compareTo(param2) >= 0) bExit = true;
                break;
            case "!=":  if (param1.compareTo(param2) != 0) bExit = true;
                break;
            case "==":  if (param1.compareTo(param2) == 0) bExit = true;
                break;
            default:
                throw new ParserException(functionId + "Invalid comparison sign: " + compType);
        }
        frame.outputInfoMsg(STATUS_DEBUG, functionId + " '" + param1 + "' " + compType + " '" + param2 + "' " + bExit);
        return bExit;
    }

    /**
     * gets the name of the current method.
     * 
     * @return name of the current method
     */
    public static String getCurrentMethodName() {
        return StackWalker.getInstance()
                          .walk(s -> s.skip(1).findFirst())
                          .get()
                          .getMethodName();
    }

    /**
     * gets the name of the calling method.
     * 
     * @return name of the calling method
     */
    public static String getCallerMethodName() {
        return StackWalker.getInstance()
                          .walk(s -> s.skip(2).findFirst())
                          .get()
                          .getMethodName();
    }

    /**
     * prints the call stack trace for debugging
     */
    public static void printCallTrace(String msg) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        StringBuilder traceBuilder = new StringBuilder();
        for (StackTraceElement element : stackTraceElements) {
            traceBuilder.append(element.toString()).append("\n");
        }
        frame.outputInfoMsg(STATUS_ERROR, "\n  -> " + traceBuilder.toString());
    }

}
