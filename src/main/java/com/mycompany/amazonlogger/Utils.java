package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.AmazonReader.props;

import java.awt.Color;
import java.io.File;

/**
 *
 * @author dan
 */
public class Utils {
    
    /*********************************************************************
    ** returns the unsigned integer value from a string of digits.
    * 
    *  @param str    - the string containing the digits to convert
    *  @param offset - the string offset in which to begin extracting
    *  @param maxlen - the max expected string length
    * 
    *  @return the corresponding integer value (null if invalid format)
    */
    public static Integer getIntFromString (String str, int offset, int maxlen) {
        Integer value;
        
        if (str == null || str.length() < offset + maxlen || offset >= str.length()) {
            return null;
        }
        value = 0;
        for (int ix = offset; ix < offset + maxlen; ix++) {
            if (str.charAt(ix) >= '0' && str.charAt(ix) <= '9') {
                value = value * 10 + str.charAt(ix) - '0';
            } else {
                return null;
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
        // check for empty string
        if (str == null) {
            throw new ParserException("null string");
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
            throw new ParserException("getAmountValue: invalid string length (" + str.length() + "): " + str);
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
                throw new ParserException("getAmountValue: invalid character (" + str.charAt(ix) + "): " + str);
            }
        }
        if (iDecCnt != 2) {
            throw new ParserException("getAmountValue: invalid number of decimal characters (" + iDecCnt + "): " + str);
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
        int offset;
        for (offset = 0; offset < line.length() && line.charAt(offset) == ' '; offset++) { }
        line = line.substring(offset);
        if (line.length() == 0) {
            throw new ParserException("getNextWord: no word found", line);
        }
        if (line.length() < minlen) {
            throw new ParserException("getNextWord: word < minimum length", line);
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
    */
    public static Color getColorOfTheMonth (int month) {
        Color color;
        
        switch(month) {
            case 1: color  = Color.cyan;      break;
            case 2: color  = Color.green;     break;
            case 3: color  = Color.lightGray; break;
            case 4: color  = Color.yellow;    break;
            case 5: color  = Color.pink;      break;
            case 6: color  = Color.orange;    break;
            case 7: color  = Color.cyan;      break;
            case 8: color  = Color.green;     break;
            case 9: color  = Color.lightGray; break;
            case 10: color = Color.yellow;    break;
            case 11: color = Color.pink;      break;
            default:
            case 12: color = Color.orange;    break;
        }

        return color;
    }

    public static String getFilePath (File file) {
        if (file == null)
            return "";
        String filePath = file.getAbsolutePath();
        int offset = filePath.lastIndexOf('/');
        if (offset > 0) {
            filePath = filePath.substring(0, offset);
        }
        return filePath;
    }

    public static String getFileRootname (File file) {
        if (file == null)
            return "";
        String fileName = file.getName();
        int offset = fileName.lastIndexOf('.');
        if (offset > 0)
            fileName = fileName.substring(0, offset);
        return fileName;
    }

    public static String getFileExtension (File file) {
        if (file == null)
            return "";
        String fileName = file.getName();
        int offset = fileName.lastIndexOf('.');
        if (offset <= 0)
            return "";
        return fileName.substring(offset); // include the leading '.'
    }

    public static String getPathFromPropertiesFile (PropertiesFile.Property tag) {
        String validPath = null;
        String pathName = props.getPropertiesItem(tag, "");
        if (!pathName.isEmpty()) {
            File tempDir = new File(pathName);
            if (tempDir.exists() && tempDir.isDirectory()) {
                validPath = pathName;
            } else {
                frame.outputInfoMsg(UIFrame.STATUS_WARN, "Properties file '" + tag.name() + "' entry not a valid directory: " + pathName);
            }
        }
        return validPath;
    }

}
