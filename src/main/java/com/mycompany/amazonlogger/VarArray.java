/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.ParameterStruct.getLongOrUnsignedValue;
import static com.mycompany.amazonlogger.UIFrame.STATUS_PROGRAM;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/**
 *
 * @author dan
 */
public class VarArray {

    private static final String CLASS_NAME = "VarArray";
    
    // array Variables
    private static final HashMap<String, ArrayList<Long>>    intArrayParams = new HashMap<>();
    private static final HashMap<String, ArrayList<String>>  strArrayParams = new HashMap<>();
    
    // reserved static Variables
    private static final ArrayList<String>  strResponse = new ArrayList<>();    // responses from RUN commands

    // array filter info
    private static ArrayList<Boolean> ixFilter = null;
    
    /**
     * initializes the saved Variables
     */
    public static void initVariables () {
        intArrayParams.clear();
        strArrayParams.clear();
        strResponse.clear();
    }

    //==========================================    
    // Getter functions
    //==========================================
    
    public static ArrayList<String> getResponseValue () {
        return strResponse;
    }
    
    public static ArrayList<Boolean> getFilterArray () {
        return ixFilter;
    }
    
    public static ArrayList<String> getStrArray (String name) {
        return strArrayParams.get(name);
    }
    
    public static ArrayList<Long> getIntArray (String name) {
        return intArrayParams.get(name);
    }

    public static boolean isIntArray (String name) {
        return intArrayParams.containsKey(name);
    }
    
    public static boolean isStrArray (String name) {
        return strArrayParams.containsKey(name);
    }
    
    //==========================================    
    // Putter functions
    //==========================================
    
    /**
     * adds a String value to the $RESPONSE Variable
     * 
     * @param value - value to add to the response Variable
     */
    public static void putResponseValue (String value) {
        strResponse.add(value);
    }
    
    /**
     * adds an array of values to the $RESPONSE Variable
     * 
     * @param value - value to add to the response Variable
     */
    public static void putResponseValue (ArrayList<String> value) {
        strResponse.addAll(value);
    }
    
    /**
     * creates a new entry in the Variable table and sets the initial value.
     * 
     * @param name  - Variable name
     * @param type  - the type of parameter
     */
    public static void allocateVariable (String name, ParameterStruct.ParamType type) {
        switch (type) {
            case ParameterStruct.ParamType.IntArray:
                intArrayParams.put(name, new ArrayList<>());
                break;
            case ParameterStruct.ParamType.StringArray:
                strArrayParams.put(name, new ArrayList<>());
                break;
            default:
                break;
        }
    }
    
    /**
     * removes the specified array variable entry.
     * 
     * @param name  - Variable name
     * @param type  - the type of parameter
     */
    public static void removeArrayEntry (String name, ParameterStruct.ParamType type) {
        switch (type) {
            case ParameterStruct.ParamType.IntArray:
                intArrayParams.remove(name);
                break;
            case ParameterStruct.ParamType.StringArray:
                strArrayParams.remove(name);
                break;
            default:
                break;
        }
    }
    
    /**
     * get the number of elements in an existing Array Variable.
     * Indicates if the name was not found (does NOT create a new entry).
     * 
     * @param name   - Variable name
     * 
     * @return number of entries in array
     * 
     * @throws ParserException
     */
    public static int getIntArraySize (String name) throws ParserException {
        String functionId = CLASS_NAME + ".getArraySize: ";

        if (name.contentEquals("RESPONSE")) {
            return strResponse.size();
        } else if (intArrayParams.containsKey(name)) {
            ArrayList<Long> entry = intArrayParams.get(name);
            return entry.size();
        } else if (strArrayParams.containsKey(name)) {
            ArrayList<String> entry = strArrayParams.get(name);
            return entry.size();
        }
        throw new ParserException(functionId + "Array Variable " + name + " not found");
    }
    
    /**
     * saves the array in a String Array Variable.
     * Indicates if the name was not found (does NOT create a new entry).
     * 
     * @param name  - Variable name
     * @param value - Variable value
     * 
     * @throws ParserException
     */
    public static void setStrArrayVariable (String name, ArrayList<String> value) throws ParserException {
        String functionId = CLASS_NAME + ".setStrArrayVariable: ";

        if (!strArrayParams.containsKey(name)) {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
        strArrayParams.replace(name, value);
        frame.outputInfoMsg(STATUS_PROGRAM, "   - Saved StrArray param: " + name + " = " + value);
    }

    /**
     * saves the array in a Integer Array Variable.
     * Indicates if the name was not found (does NOT create a new entry).
     * 
     * @param name  - Variable name
     * @param value - Variable value
     * 
     * @throws ParserException
     */
    public static void setIntArrayVariable (String name, ArrayList<Long> value) throws ParserException {
        String functionId = CLASS_NAME + ".setIntArrayVariable: ";

        if (!intArrayParams.containsKey(name)) {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
        intArrayParams.replace(name, value);
        frame.outputInfoMsg(STATUS_PROGRAM, "   - Saved IntArray param: " + name + " = " + value);
    }

    /**
     * clears all entries of an existing Array Variable.
     * Indicates if the name was not found (does NOT create a new entry).
     * 
     * @param name  - Variable name
     * 
     * @return true if successful, false if the Variable was not found
     */
    public static boolean arrayRemoveAll (String name) {
        if (name.contentEquals("RESPONSE")) {
            int size = strResponse.size();
            strResponse.clear();
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Deleted " + size + " entries in Array param: " + name);
            return true;
        }
        else if (intArrayParams.containsKey(name)) {
            ArrayList<Long> entry = intArrayParams.get(name);
            int size = entry.size();
            entry.clear();
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Deleted " + size + " entries in Array param: " + name);
            return true;
        }
        else if (strArrayParams.containsKey(name)) {
            ArrayList<String> entry = strArrayParams.get(name);
            int size = entry.size();
            entry.clear();
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Deleted " + size + " entries in List param: " + name);
            return true;
        }
        return false;
    }

    /**
     * clears selected entries of an existing Array Variable.
     * Indicates if the name was not found (does NOT create a new entry).
     * 
     * @param name   - Variable name
     * @param iStart - index of starting entry in array to delete
     * @param iCount - number of entries in array to delete
     * 
     * @return true if successful, false if the Variable was not found
     * 
     * @throws ParserException
     */
    public static boolean arrayRemoveEntries (String name, int iStart, int iCount) throws ParserException {
        String functionId = CLASS_NAME + ".arrayRemoveEntries: ";

        if (iCount < 1 || iStart < 0) {
            throw new ParserException(functionId + "Array Variable " + name + " index range exceeded: " + iStart + " to " + (iStart + iCount) + " is invalid");
        }
        int size;
        String arrayContents;
        if (intArrayParams.containsKey(name)) {
            ArrayList<Long> entry = intArrayParams.get(name);
            size = entry.size();
            if (iStart + iCount > size) {
                throw new ParserException(functionId + "Array Variable " + name + " index range exceeded: " + iStart + " to " + (iStart + iCount) + " (max " + entry.size() + ")");
            }
            if (iCount == entry.size()) {
                entry.clear();
            } else {
                for (int ix = 0; ix < iCount; ix++) {
                    entry.remove(iStart);
                }
            }
            arrayContents = entry.toString();
        } else if (strArrayParams.containsKey(name)) {
            ArrayList<String> entry = strArrayParams.get(name);
            size = entry.size();
            if (iStart + iCount > size) {
                throw new ParserException(functionId + "Array Variable " + name + " index range exceeded: " + iStart + " to " + (iStart + iCount) + " (max " + entry.size() + ")");
            }
            if (iCount == entry.size()) {
                entry.clear();
            } else {
                for (int ix = 0; ix < iCount; ix++) {
                    entry.remove(iStart);
                }
            }
            arrayContents = entry.toString();
        } else {
            return false;
        }
        frame.outputInfoMsg(STATUS_PROGRAM, "   - Removed " + iCount + " entries from Array param " + name + ": (new size = "+ size  + ")");
        frame.outputInfoMsg(STATUS_PROGRAM, "   - " + name + ": " + arrayContents);
        return true;
    }

    /**
     * modifies the value of an existing entry in the Array or List Variable table.
     * Indicates if the name was not found (does NOT create a new entry).
     * 
     * @param name  - Variable name
     * @param index - index of entry in list to change
     * @param value - Variable value
     * 
     * @return true if successful, false if the Variable was not found
     * 
     * @throws ParserException
     */
    public static boolean arrayModifyEntry (String name, int index, String value) throws ParserException {
        String functionId = CLASS_NAME + ".arrayModifyEntry: ";

        try {
            String arrayContents;
            if (intArrayParams.containsKey(name)) {
                ArrayList<Long> entry = intArrayParams.get(name);
                if (index >= entry.size()) {
                    throw new ParserException(functionId + "Array Variable " + name + " index exceeded: " + index + " (max " + entry.size() + ")");
                }
                Long longVal = getLongOrUnsignedValue (value);
                entry.set(index, longVal);
                arrayContents = entry.toString();
            }
            else if (strArrayParams.containsKey(name)) {
                ArrayList<String> entry = strArrayParams.get(name);
                if (index >= entry.size()) {
                    throw new ParserException(functionId + "List Variable " + name + " index exceeded: " + index + " (max " + entry.size() + ")");
                }
                entry.set(index, value);
                arrayContents = entry.toString();
            } else {
                return false;
            }
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Modified List param: " + name + " = " + value);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - " + name + ": " + arrayContents);
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId);
        }
        return true;
    }

    /**
     * inserts a value into an existing Array Variable table.
     * Indicates if the name was not found (does NOT create a new entry).
     * 
     * @param name  - Variable name
     * @param index - index of where to insert the value
     *                (moves current index value and all following values back 1 entry)
     * @param value - Variable value
     * 
     * @return true if successful, false if the Variable was not found
     * 
     * @throws ParserException
     */
    public static boolean arrayInsertEntry (String name, int index, String value) throws ParserException {
        String functionId = CLASS_NAME + ".arrayInsertEntry: ";

        try {
            String arrayContents;
            if (intArrayParams.containsKey(name)) {
                ArrayList<Long> entry = intArrayParams.get(name);
                Long longVal = getLongOrUnsignedValue (value);
                if (index >= entry.size() || entry.isEmpty()) {
                    throw new ParserException(functionId + "Variable " + name + " index exceeded: " + index + " (max " + entry.size() + ")");
                }
                // bump current entries back 1
                entry.addLast(entry.getLast());
                for (int ix = entry.size()-2; ix >= index; ix--) {
                    entry.set(ix+1, entry.get(ix));
                }
                entry.set(index, longVal);
                arrayContents = entry.toString();
            }
            else if (strArrayParams.containsKey(name)) {
                ArrayList<String> entry = strArrayParams.get(name);
                if (index >= entry.size() || entry.isEmpty()) {
                    throw new ParserException(functionId + "Variable " + name + " index exceeded: " + index + " (max " + entry.size() + ")");
                }
                // bump current entries back 1
                entry.addLast(entry.getLast());
                for (int ix = entry.size()-2; ix >= index; ix--) {
                    entry.set(ix+1, entry.get(ix));
                }
                entry.set(index, value);
                arrayContents = entry.toString();
            } else {
                return false;
            }
            frame.outputInfoMsg(STATUS_PROGRAM, "   - Inserted entry[" + index + "] in param: " + name + " = " + value);
            frame.outputInfoMsg(STATUS_PROGRAM, "   - " + name + ": " + arrayContents);
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId);
        }
        return true;
    }

    /**
     * appends a value to the end of an existing Array Variable table.
     * Indicates if the name was not found (does NOT create a new entry).
     * 
     * @param name  - Variable name
     * @param value - Variable value
     * 
     * @return true if successful, false if the Variable was not found
     * 
     * @throws ParserException
     */
    public static boolean arrayAppendEntry (String name, String value) throws ParserException {
        String functionId = CLASS_NAME + ".arrayAppendEntry: ";

        String arrayContents;
        if (intArrayParams.containsKey(name)) {
            ArrayList<Long> entry = intArrayParams.get(name);
            try {
                Long longVal = getLongOrUnsignedValue (value);
                entry.addLast(longVal);
                arrayContents = entry.toString();
            } catch (ParserException exMsg) {
                throw new ParserException(exMsg + "\n  -> " + functionId);
            }
        }
        else if (strArrayParams.containsKey(name)) {
            ArrayList<String> entry = strArrayParams.get(name);
            entry.addLast(value);
            arrayContents = entry.toString();
        } else {
            return false;
        }
        frame.outputInfoMsg(STATUS_PROGRAM, "   - Appended entry to Variable: " + name + " = " + value);
        frame.outputInfoMsg(STATUS_PROGRAM, "   - " + name + ": " + arrayContents);
        return true;
    }

    /**
     * resets the array filter.
     */
    public static void arrayFilterReset () {
        ixFilter = null;
    }
    
    /**
     * sets the filter entries based on the matching conditions in the StringArray.
     * 
     * @param varName   - name of the variable to check
     * @param strFilter - the filter match criteria
     * @param opts      - '!' to invert the logic and LEFT or RIGHT for searching only ends
     * 
     * @throws ParserException
     * 
     */
    public static void arrayFilterString (String varName, String strFilter, String opts) throws ParserException {
        String functionId = CLASS_NAME + ".arrayFilterString: ";

        if (! strArrayParams.containsKey(varName)) {
            throw new ParserException(functionId + "Array parameter not found: " + varName);
        }
        ArrayList<String> var = strArrayParams.get(varName);

        // if this is 1st filter being performed, init entries to all true
        if (ixFilter == null) {
            ixFilter = new ArrayList<>();
            for (String var1 : var) {
                ixFilter.add(true);
            }
        } else if (var.size() != ixFilter.size()) {
            throw new ParserException(functionId + "Filter Array size mismatch: " + varName + " = " + var.size() + ", filter = " + ixFilter.size());
        }

        // check for leading or trailing filter checks
        boolean bInvert = false;
        if (opts.charAt(0) == '!') {
            bInvert = true;
            opts = opts.substring(1);
        }
        
        // now we only mark the entries that do not match the criteria, so that filters
        // can be stacked upon each other.
        for (int ix = 0; ix < var.size(); ix++) {
            String entry = var.get(ix);

            // search for matching chars anywhere in string
            boolean bMatch = entry.contains(strFilter);
            if (bMatch) {
                // the pattern was found somewhere in the string.
                //  If we are matching LEFT or RIGHT only, we need to check
                //  if these cases matched.
                int endix = entry.length() - strFilter.length();
                int offset = entry.lastIndexOf(strFilter);
                switch (opts) {
                    case "LEFT" -> {
                        if (offset != 0) {      // the match did not occur at the start
                            bMatch = false;
                        }
                    }
                    case "RIGHT" -> {
                        if (offset != endix) {  // the match did not occur at the end
                            bMatch = false;
                        }
                    }
                    default -> {}
                }
            }

            // now we should eliminate the entry if we are not inverting and we did not have a math,
            // or if we are inverting and we did have a match.
            if (bMatch == bInvert) {
                ixFilter.set(ix, false);
            }
        }
    }
    
    /**
     * sets the filter entries based on the matching conditions in the IntArray.
     * 
     * @param varName   - name of the variable to check
     * @param compSign  - the comparison sign
     * @param value     - value to compare the entries to
     * 
     * @throws ParserException
     * 
     */
    public static void arrayFilterInt (String varName, String compSign, Long value) throws ParserException {
        String functionId = CLASS_NAME + ".arrayFilterInt: ";

        if (! intArrayParams.containsKey(varName)) {
            throw new ParserException(functionId + "Array parameter not found: " + varName);
        }
        ArrayList<Long> var = intArrayParams.get(varName);
        for (int ix = 0; ix < var.size(); ix++) {
            Long entry = var.get(ix);
            boolean bMatch = false;
            switch (compSign) {
                default:
                case "==": bMatch = (Objects.equals(entry, value));   break;
                case "!=": bMatch = (!Objects.equals(entry, value));  break;
                case ">=": bMatch = (entry >= value);   break;
                case "<=": bMatch = (entry <= value);   break;
                case ">":  bMatch = (entry > value);    break;
                case "<":  bMatch = (entry < value);    break;
            }
            
            if (!bMatch) {
                ixFilter.set(ix, false);
            }
        }
    }
    
}
