/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.UIFrame.STATUS_DEBUG;
import static com.mycompany.amazonlogger.UIFrame.STATUS_VARS;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author dan
 */
public class LoopParam {
    
    private static final String CLASS_NAME = LoopParam.class.getSimpleName();
    
    Integer value;          // the current value of the loop parameter
    String  paramName;      // the name of the reference Variable (null if no ref param)
        
    // for loops, the loopParams will find the loop parameter for the loop at the
    // specified command index. In order to determine if we have a nested loop
    // using the same param name, we use loopNames that contains an array of
    // all the loop params having the same name. When a name is being reused,
    // we must verify that all the occurrances of FOR loops using that name
    // are all completely defined, meaning that the ENDFOR has already been found
    // for each one. When compiling, we simply proceed through the instructions
    // sequentially, so if all current uses of the FOR parameter indicate they
    // are complete (i.e. ENDFOR has been found), we are safe to reuse the loop name.
    private static final HashMap<LoopId, LoopStruct> loopParams = new HashMap<>();
    private static final HashMap<String, ArrayList<LoopId>> loopNames = new HashMap<>();

    
    public LoopParam (Integer value) {
        this.value = value;
        this.paramName = null;
    }
        
    public LoopParam (String name) throws ParserException {
        String functionId = CLASS_NAME + " (new): ";
       
        try {
            Integer iVal = Utils.getHexValue(name);
            if (iVal == null) {
                iVal = Utils.getIntValue(name).intValue();
            }
            this.value = iVal;
            this.paramName = null;
        } catch (ParserException ex) {
            boolean bValid;
            try {
                bValid = Variables.isValidVariableName(Variables.VarCheck.REFERENCE, name);
            } catch (ParserException exMsg) {
                throw new ParserException(exMsg + "\n  -> " + functionId);
            }
            if (!name.startsWith("$") || ! bValid) {
                throw new ParserException(functionId + "reference Variable " + this.paramName + " is not valid Variable name");
            }
            this.value = null;
            this.paramName = name;
        }
    }
        
    public void update (Integer value) {
        this.value = value;
    }
        
    public Integer getIntValue () throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
       
        if (paramName != null) {
            // it is a Variable, get the current value
            Long numValue = Variables.getNumericValue(paramName, null, true);
            if (numValue == null) {
                throw new ParserException(functionId + "reference Variable " + paramName + " is not an Integer: " + paramName);
            }
            value = numValue.intValue();
        }

        return value;
    }

    /**
     * gets the current loop parameter value.
     * 
     * @param name - name of the loop to find
     * 
     * @return the current index value of the loop
     */
    public static Long getLoopCurValue (String name) {
        if (! loopNames.containsKey(name)) {
            return null;
        }
        return LoopStruct.getCurrentLoopValue(name).longValue();
    }

    /**
     * initializes the saved Variables
     */
    public static void initVariables () {
        loopParams.clear();
        loopNames.clear();
    }

    /**
     * determines if a loop Variable has been found with the specified name.
     * 
     * @param name - name of the loop Variable to search for
     * 
     * @return true if the loop Variable was found
     */
    public static boolean isLoopParamDefined (String name) {
        return loopNames.containsKey(name);
    }

    /**
     * gets the LoopStruct entry corresponding to the LoopId value.
     * 
     * @param loopId - the loop name-index combo that uniquely defines a LoopStruct entry
     * 
     * @return the corresponding LoopStruct value from loopParams table
     */
    public static LoopStruct getLoopStruct (LoopId loopId) {
        if (loopParams== null || loopParams.isEmpty()) {
            return null;
        }
        // search for a LoopId match
        for (Map.Entry pair : loopParams.entrySet()) {
            LoopId mapId = (LoopId) pair.getKey();
            LoopStruct mapInfo = (LoopStruct) pair.getValue();
            if (loopId.name.contentEquals(mapId.name) && loopId.index == mapId.index) {
                return mapInfo;
            }
        }
        return null;
    }
    
    /**
     * get the current loop value for the specified loop id
     * 
     * @param loopId - the name-index ID for the currently active loop
     * 
     * @return value of the loop Variable, null if not found
     */    
    public static Integer getLoopValue (LoopId loopId) {
        LoopStruct loopInfo = getLoopStruct (loopId);
        if (loopInfo == null) {
            return null;
        }
        return loopInfo.getLoopValue();
    }
        
    /**
     * checks if the current Loop command is at the same IF level as its corresponding FOR statement.
     * 
     * @param command - the FOR command being run
     * @param level  - current IF nest level for current FOR command
     * @param loopId - the name-index ID for the current loop
     * 
     * @throws ParserException 
     */    
    public static void checkLoopIfLevel (CommandStruct.CommandTable command, int level, LoopId loopId) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        LoopStruct loopInfo = getLoopStruct (loopId);
        if (loopInfo == null) {
            throw new ParserException(functionId + "FOR Loop " + loopId.name + " @ " + loopId.index + " not found");
        }
        if (! loopInfo.isLoopIfLevelValid(level)) {
            throw new ParserException(functionId + command + "exceeded bounds of enclosing IF block: IF level = " + level);
        }
    }
        
    /**
     * sets the location of the end of the loop when the ENDLOOP command is parsed
     * 
     * @param index  - current command index for ENDLOOP command
     * @param loopId - the name-index ID for the current loop
     * 
     * @throws ParserException 
     */    
    public static void setLoopEndIndex (int index, LoopId loopId) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        LoopStruct loopInfo = getLoopStruct (loopId);
        if (loopInfo == null) {
            throw new ParserException(functionId + "FOR Loop " + loopId.name + " @ " + loopId.index + " not found");
        }
        loopInfo.setLoopEnd(index);
    }
        
    /**
     * gets the next command index based on the loop command specified for current loop.
     * This should be called by 'CommandParser.executeProgramCommand' when a FOR
     * loop is in progress and one of the loop commands was found that may change
     * the next location to execute from.
     * 
     * @param command - the loop command to execute
     * @param index   - the current command index
     * @param loopId  - the loop Variable currently running
     * 
     * @return the next command index to run
     * 
     * @throws ParserException
     */
    public static int getLoopNextIndex (CommandStruct.CommandTable command, int index, LoopId loopId) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        int nextIndex = index;
        
        LoopStruct loopInfo = getLoopStruct (loopId);
        if (loopInfo == null) {
            throw new ParserException(functionId + "FOR Loop " + loopId.name + " @ " + loopId.index + " not found");
        }
        
        String action = "";
        switch (command) {
            case CommandStruct.CommandTable.FOR:
                nextIndex = loopInfo.startLoop(index);
                action = "starting";
                break;
            case CommandStruct.CommandTable.BREAK:
                nextIndex = loopInfo.loopBreak();
                action = "exiting";
                break;
            case CommandStruct.CommandTable.NEXT:
            case CommandStruct.CommandTable.CONTINUE:
                nextIndex = loopInfo.loopNext();
                if (nextIndex < index)
                    action = "restarting";
                else
                    action = "exiting";
                break;
            default:
                break;
        }
        
        frame.outputInfoMsg(STATUS_VARS, command.toString() + " command " + action + " at index: " + nextIndex);
        return nextIndex;
    }

    /**
     * adds a new entry in the Loop Variables table.
     * This should only be called by 'CommandParser.compileProgram' when stepping
     * through the commands to verify and create the compiled list of commands.
     * 
     * @param name     - loop Variable name
     * @param loopId   - loop name-index combination to uniquely identify the loop param
     * @param loopInfo - the loop Variable to add
     */
    public static void saveLoopParameter (String name, LoopId loopId, LoopStruct loopInfo) {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        // create a new loop ID (name + command index) for the entry and add it
        // to the list of IDs for the loop parameter name
        ArrayList<LoopId> loopList;
        if (loopNames.isEmpty()) {
            // first loop defined, create an empty array list and add it to the list of names for this name.
            loopList = new ArrayList<>();
            loopNames.put(name, loopList);
        } else {
            // get name from list. if not in list, create new entry
            loopList = loopNames.get(name);
            if (loopList == null) {
                loopList = new ArrayList<>();
                loopNames.put(name, loopList);
            }
        }
        loopList.add(loopId);
        frame.outputInfoMsg(STATUS_DEBUG, functionId + "Number of loops with name " + name + ": " + loopList.size());
        
        // now add loop entry to hashmap based on name/index ID
        frame.outputInfoMsg(STATUS_DEBUG, functionId + "loopParams [" + loopParams.size() + "] " + loopId.name + " @ " + loopId.index);
        loopParams.put(loopId, loopInfo);
    }

    /**
     * checks for loop nesting errors.
     * 
     * @param name - name of loop variable
     * 
     * @return loop index of failed loop, null if no error
     */
    public static Integer checkLoopNesting (String name) {
        ArrayList<LoopId> loopList = loopNames.get(name);
        if (loopList != null && ! loopList.isEmpty()) {
            // we have one or more uses of the same name, check if this is nested in one
            frame.outputInfoMsg(STATUS_VARS, "   - checking previous uses of FOR Loop Variable " + name + " to see if we have a nesting problem");
            for (int ix = 0; ix < loopList.size(); ix++) {
                LoopId loopEntry = loopList.get(ix);
                LoopStruct loopInfo = LoopParam.getLoopStruct (loopEntry);
                if (loopInfo == null || ! loopInfo.isLoopComplete()) {
                    // nesting error
                    return loopEntry.index;
                } else {
                    frame.outputInfoMsg(STATUS_VARS, "   - FOR Loop Variable " + name + " @ " + loopEntry.index + " was complete");
                }
            }
        }
        return null;
    }
}
