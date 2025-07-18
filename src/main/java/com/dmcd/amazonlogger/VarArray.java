/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.dmcd.amazonlogger;

import com.dmcd.amazonlogger.GUILogPanel.MsgType;
import java.util.ArrayList;
import java.util.Objects;

/**
 *
 * @author dan
 */
public class VarArray {

    private static final String CLASS_NAME = VarArray.class.getSimpleName();
    private static final String INDENT = "    ";
    
    // array filter info
    private static ArrayList<Boolean> ixFilter = null;


    public static void initVariables () {
        ixFilter = null;
    }
    
    private static boolean isStrArray (String name) throws ParserException {
        ParameterStruct.ParamType ptype = Variables.getVariableTypeFromName(name);
        return ptype == ParameterStruct.ParamType.StrArray;
    }
    
    private static boolean isIntArray (String name) throws ParserException {
        ParameterStruct.ParamType ptype = Variables.getVariableTypeFromName(name);
        return ptype == ParameterStruct.ParamType.IntArray;
    }
    
    public static String getStrArrayEntry (String name, int ix) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        int size = Variables.getStrArray(name).size();
        if (ix >= size) {
            throw new ParserException(functionId + "Index exceeded max size of Array " + name + " (ix = " + ix + ", size = " + size + ")");
        }
        return Variables.getStrArray(name).get(ix);
    }
    
    public static Long getIntArrayEntry (String name, int ix) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        int size = Variables.getIntArray(name).size();
        if (ix >= size) {
            throw new ParserException(functionId + "Index exceeded max size of Array " + name + " (ix = " + ix + ", size = " + size + ")");
        }
        return Variables.getIntArray(name).get(ix);
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
    public static int getArraySize (String name) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name == null || name.isEmpty()) {
            throw new ParserException(functionId + "Array Variable is missing name");
        }
        if (VarReserved.isArray(name)) {
            return VarReserved.getArraySize(name);
        } else if (isIntArray(name)) {
            return Variables.getIntArray(name).size();
        } else if (isStrArray(name)) {
            return Variables.getStrArray(name).size();
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
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name == null || name.isEmpty()) {
            throw new ParserException(functionId + "Array Variable is missing name");
        }
        if (!isStrArray(name)) {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
        Variables.setStrArray(name, value);
        GUILogPanel.outputInfoMsg(MsgType.VARS, INDENT + "- Saved StrArray param: " + name);
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
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name == null || name.isEmpty()) {
            throw new ParserException(functionId + "Array Variable is missing name");
        }
        if (!isIntArray(name)) {
            throw new ParserException(functionId + "Variable " + name + " not found");
        }
        Variables.setIntArray(name, value);
        GUILogPanel.outputInfoMsg(MsgType.VARS, INDENT + "- Saved IntArray param: " + name);
    }

    /**
     * clears all entries of an existing Array Variable.
     * (DOES NOT DELETE THE VARIABLE, JUST CLEARS ITS DATA)
     * Indicates if the name was not found (does NOT create a new entry).
     * 
     * @param name  - Variable name
     * 
     * @return true if successful, false if the Variable was not found
     * 
     * @throws ParserException
     */
    public static boolean arrayClearAll (String name) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name == null || name.isEmpty()) {
            throw new ParserException(functionId + "Array Variable is missing name");
        }
        if (VarReserved.isArray(name)) {
            int size = VarReserved.getArraySize(name);
            VarReserved.resetVar(name);
            GUILogPanel.outputInfoMsg(MsgType.VARS, INDENT + "- Deleted " + size + " entries in Array param: " + name);
            return true;
        }
        else if (isIntArray(name)) {
            ArrayList<Long> entry = Variables.getIntArray(name);
            int size = entry.size();
            entry.clear();
            Variables.setVarChange (name);
            GUILogPanel.outputInfoMsg(MsgType.VARS, INDENT + "- Deleted " + size + " entries in Array param: " + name);
            return true;
        }
        else if (isStrArray(name)) {
            ArrayList<String> entry = Variables.getStrArray(name);
            int size = entry.size();
            entry.clear();
            Variables.setVarChange (name);
            GUILogPanel.outputInfoMsg(MsgType.VARS, INDENT + "- Deleted " + size + " entries in List param: " + name);
            return true;
        }
        return false;
    }

    /**
     * clears selected entries of an existing Array Variable.
     * Indicates if the name was not found (does NOT create a new entry).
     * (DOES NOT DELETE THE VARIABLE, JUST CLEARS ITS DATA)
     * 
     * @param name   - Variable name
     * @param iStart - index of starting entry in array to delete
     * @param iCount - number of entries in array to delete
     * 
     * @return true if successful, false if the Variable was not found
     * 
     * @throws ParserException
     */
    public static boolean arrayClearEntries (String name, int iStart, int iCount) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name == null || name.isEmpty()) {
            throw new ParserException(functionId + "Array Variable is missing name");
        }
        if (iCount < 1 || iStart < 0) {
            throw new ParserException(functionId + "Array Variable " + name + " index range exceeded: " + iStart + " to " + (iStart + iCount) + " is invalid");
        }
        int size;
        String arrayContents;
        if (isIntArray(name)) {
            ArrayList<Long> entry = Variables.getIntArray(name);
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
        } else if (isStrArray(name)) {
            ArrayList<String> entry = Variables.getStrArray(name);
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
        Variables.setVarChange (name);
        GUILogPanel.outputInfoMsg(MsgType.VARS, INDENT + "- Removed " + iCount + " entries from Array param " + name + ": (new size = "+ size  + ")");
        GUILogPanel.outputInfoMsg(MsgType.VARS, INDENT + "- " + name + ": " + arrayContents);
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
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name == null || name.isEmpty()) {
            throw new ParserException(functionId + "Array Variable is missing name");
        }
        try {
            String arrayContents;
            if (isIntArray(name)) {
                ArrayList<Long> entry = Variables.getIntArray(name);
                if (index >= entry.size()) {
                    throw new ParserException(functionId + "Array Variable " + name + " index exceeded: " + index + " (max " + entry.size() + ")");
                }
                Long longVal = Utils.getLongOrUnsignedValue (value);
                entry.set(index, longVal);
                arrayContents = entry.toString();
            }
            else if (isStrArray(name)) {
                ArrayList<String> entry = Variables.getStrArray(name);
                if (index >= entry.size()) {
                    throw new ParserException(functionId + "List Variable " + name + " index exceeded: " + index + " (max " + entry.size() + ")");
                }
                entry.set(index, value);
                arrayContents = entry.toString();
            } else {
                return false;
            }
            Variables.setVarChange (name);
            GUILogPanel.outputInfoMsg(MsgType.VARS, INDENT + "- Modified List param: " + name + " = " + value);
            GUILogPanel.outputInfoMsg(MsgType.VARS, INDENT + "- " + name + ": " + arrayContents);
        } catch (ParserException exMsg) {
            Utils.throwAddendum (exMsg.getMessage(), functionId);
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
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name == null || name.isEmpty()) {
            throw new ParserException(functionId + "Array Variable is missing name");
        }
        try {
            String arrayContents;
            if (isIntArray(name)) {
                ArrayList<Long> entry = Variables.getIntArray(name);
                Long longVal = Utils.getLongOrUnsignedValue (value);
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
            else if (isStrArray(name)) {
                ArrayList<String> entry = Variables.getStrArray(name);
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
            Variables.setVarChange (name);
            GUILogPanel.outputInfoMsg(MsgType.VARS, INDENT + "- Inserted entry[" + index + "] in param: " + name + " = " + value);
            GUILogPanel.outputInfoMsg(MsgType.VARS, INDENT + "- " + name + ": " + arrayContents);
        } catch (ParserException exMsg) {
            Utils.throwAddendum (exMsg.getMessage(), functionId);
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
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name == null || name.isEmpty()) {
            throw new ParserException(functionId + "Array Variable is missing name");
        }
        String arrayContents = "";
        if (isIntArray(name)) {
            ArrayList<Long> entry = Variables.getIntArray(name);
            try {
                Long longVal = Utils.getLongOrUnsignedValue (value);
                entry.addLast(longVal);
                arrayContents = entry.toString();
            } catch (ParserException exMsg) {
                Utils.throwAddendum (exMsg.getMessage(), functionId);
            }
        }
        else if (isStrArray(name)) {
            ArrayList<String> entry = Variables.getStrArray(name);
            entry.addLast(value);
            arrayContents = entry.toString();
        } else {
            return false;
        }
        if (value.length() > 90) {
            int offset = value.length() - 25;
            value = value.substring(0, 60) + " ... " + value.substring(offset);
        }
        Variables.setVarChange (name);
        GUILogPanel.outputInfoMsg(MsgType.VARS, INDENT + "- Appended entry to Variable: " + name + " = " + value);
        GUILogPanel.outputInfoMsg(MsgType.VARS, INDENT + "- " + name + ": " + arrayContents);
        return true;
    }

    //==========================================    
    // Array Filter methods
    //==========================================
    
    /**
     * gets the $FILTER Variable
     * 
     * @return the filter Variable
     */
    public static ArrayList<Boolean> getFilterArray () {
        return ixFilter;
    }
    
    /**
     * resets the array filter.
     */
    public static void arrayFilterReset () {
        ixFilter = null;
    }
    
    /**
     * sets the filter entries based on the matching conditions in the StrArray.
     * 
     * @param varName   - name of the variable to check
     * @param strFilter - the filter match criteria
     * @param opts      - '!' to invert the logic and LEFT or RIGHT for searching only ends
     * 
     * @throws ParserException
     * 
     */
    public static void arrayFilterString (String varName, String strFilter, String opts) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (varName == null || varName.isEmpty()) {
            throw new ParserException(functionId + "Array Variable is missing name");
        }
        if (! isStrArray(varName)) {
            throw new ParserException(functionId + "Array parameter not found: " + varName);
        }
        ArrayList<String> var = Variables.getStrArray(varName);

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
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (varName == null || varName.isEmpty()) {
            throw new ParserException(functionId + "Array Variable is missing name");
        }
        if (! isIntArray(varName)) {
            throw new ParserException(functionId + "Array parameter not found: " + varName);
        }
        ArrayList<Long> var = Variables.getIntArray(varName);
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
