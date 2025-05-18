/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.UIFrame.STATUS_DEBUG;
import static com.mycompany.amazonlogger.UIFrame.STATUS_PROGRAM;
import static com.mycompany.amazonlogger.UIFrame.STATUS_WARN;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

/**
 *
 * @author dan
 */
public class Subroutine {
    
    private static final String CLASS_NAME = Subroutine.class.getSimpleName();
    
    private static final String MAIN_FCTN = "*MAIN*";
    private static final String INDENT = "     ";
    
    // contains a list of the subroutines and their corresponding command index locations
    // (for EXECUTION only)
    private static final HashMap<String, Integer> subroutines = new HashMap<>();
    
    // contains the running list of command index locations to return to after sub call
    // (for EXECUTION only)
    private final static Stack<SubCall> subStack = new Stack<>();
    
    // this holds the script line numbers associated with the subroutines (during COMPILE)
    private static String lastSubName = null;
    
    // this holds the info for subs during COMPILE to verify at the end of compilation
    private static final ArrayList<SubInfo> subCallList = new ArrayList<>();
    private static final ArrayList<String>  subUsed = new ArrayList<>();
    
    // the current script line number
    private static Integer curLineNum  = 1;

    /**
     * this class is used for defining the bounds of the script file for the subroutines.
     * 
     * This allows us to determine whether a particular line of the script is
     *   contained in the MAIN section of the script or in a subroutine, and which
     *   subroutine. This is only used for the COMPILER .
     */
    private class SubInfo {
        private final String subName;
        private final int    startIx;
        private int          endIx;
        
        SubInfo (String name, int ix) {
            this.subName = name;
            this.startIx = ix;
            this.endIx   = -1;
        }
        
        public void setEndIx (int ix) {
            this.endIx = ix;
        }
        
        public boolean isDefined() {
            return this.startIx >= 0;
        }
        
        public boolean isComplete() {
            return this.startIx >= 0 && this.endIx >= 0;
        }
    }
    
    /**
     * this class defines subroutine call information that is placed in the subStack.
     * 
     * This allows us to get the line to return to upon exit and the parameters
     *  that the subroutine was passed. This is used exclusively in the EXECUTION stage.
     * 
     * The new SubCall is performed when the GOSUB command is executed.
     */
    private class SubCall {
        private final int     cmdIndex;   // command index of return location
        private final String  name;       // name of current subroutine
        
        SubCall (int index, String name) {
            this.cmdIndex = index;
            this.name  = name;
        }
        
        String getName() {
            return this.name;
        }
        
        int getIndex() {
            return this.cmdIndex;
        }
    }

    Subroutine() {
        curLineNum = 1;
    }
    
    /**
     * sets the current command index location during compile and execution.
     * 
     * @param lineNum - the current command index
     */
    public static void setCurrentIndex (int lineNum) {
        curLineNum = lineNum;
    }

    /**
     * sets the current command index location during compile and execution.
     * 
     * @return the current command index
     */
    public static int getCurrentIndex () {
        return curLineNum;
    }

    /**
     * determines if code is currently from the MAIN section of the script.
     * 
     * @return true if running in MAIN (or compiler is verifying the MAIN section)
     */
    public static boolean isMainFunction() {
        return getSubName().contentEquals(MAIN_FCTN);
    }

    /**
     * returns the subroutine name for the  current command index.
     * This uses line numbers when in compile mode and the stack when executing.
     * 
     * @return the name of the current subroutine (or MAIN)
     */
    public static String getSubName () {
        String subName = MAIN_FCTN;
        if (AmazonReader.isRunModeCompile()) {
            if (! subCallList.isEmpty()) {
                for (int ix = subCallList.size() - 1; ix >= 0; ix--) {
                    SubInfo info = subCallList.get(ix);
                    if (curLineNum >= info.startIx  && info.startIx >= 0) {
                        subName = info.subName;
                        break;
                    }
                }
            }
        } else {
            if (! subStack.empty()) {
                SubCall info = subStack.peek();
                if (info != null) {
                    subName = info.getName();
                }
            }
        }
        return subName;
    }

    /****************************************
    * THESE ARE PRE-COMPILER METHODS ONLY !
    *****************************************/
    
    /**
     * finds the subroutine name in the gosub array.
     * 
     * @param subName - name of subroutine to check
     * 
     * @return the index to the info (null if not found)
     */
    private static boolean isSubUsed (String subName) {
        for (int ix = 0; ix < subUsed.size(); ix++) {
            if (subUsed.get(ix).contentEquals(subName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * replaces the specified entry in compiler info array with new data.
     * 
     * @param ix   - index to replace
     * @param info - the value to replace it with
     * 
     * @throws ParserException 
     */
    private void replaceCompileInfo (int ix, SubInfo info) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        String tblSub = subCallList.get(ix).subName;
        if (! tblSub.contentEquals(info.subName)) {
            throw new ParserException(functionId + "Invalid index replacement: " + tblSub + " with " + info.subName);
        }
        subCallList.remove(ix);
        subCallList.add(ix, info);
    }
    
    /**
     * finds the subroutine name in the compiler info array.
     * 
     * @param subName - name of subroutine to check
     * 
     * @return the index to the info (null if not found)
     */
    private static Integer getCompileInfo (String subName) {
        for (int ix = 0; ix < subCallList.size(); ix++) {
            SubInfo info = subCallList.get(ix);
            if (info.subName.contentEquals(subName)) {
                return ix;
            }
        }
        return null;
    }

     /**
     * indicates if a subroutine name is found in the listing.
     * 
     * @param name  - the subroutine name
     * 
     * @return true if sub name found in list
     * 
     * @throws ParserException
     */
    private static boolean isValidSubName (String name) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name == null || name.isBlank()) {
            throw new ParserException(functionId + "Subroutine name is empty");
        }
        // first char must be a letter, the rest can be letter, number or _
        if (! Character.isLetter(name.charAt(0))) {
            return false;
        }
        for (int ix = 0; ix < name.length(); ix++) {
            char curch = name.charAt(ix);
            if ( (curch != '_') && ! Character.isLetterOrDigit(curch) ) {
                return false;
            }
        }
        return true;
    }

    /**
     * checks if any subroutines were called that were not defined.
     * This is done at the end of the PRE-COMPILE phase.
     * 
     * @throws ParserException 
     */
    public static void checkSubroutineMissing() throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        String missingList = "";
        String missingEnd  = "";
        String unusedList  = "";
        for (int ix = 0; ix < subCallList.size(); ix++) {
            SubInfo info = subCallList.get(ix);
            if (info.isDefined() && ! info.isComplete()) {
                missingEnd += ", " + info.subName;
            }
            if (info.isDefined() && ! isSubUsed(info.subName) && ! info.subName.contentEquals(MAIN_FCTN)) {
                unusedList += ", " + info.subName;
            }
        }
        for (int ix = 0; ix < subUsed.size(); ix++) {
            String subName = subUsed.get(ix);
            if (getCompileInfo(subName) == null) {
                missingList += ", " + subName;
            }
        }
        if (! missingList.isEmpty()) {
            throw new ParserException(functionId + "Missing subroutine definitions that were called: " + missingList.substring(2));
        }
        if (! missingEnd.isEmpty()) {
            throw new ParserException(functionId + "Missing ENDSUB commands for the following subroutines: " + missingEnd.substring(2));
        }
        if (! unusedList.isEmpty()) {
            frame.outputInfoMsg(STATUS_WARN, INDENT + "Unused functions: " + unusedList.substring(2));
        }
    }
    
    /**
     * adds a subroutine name to the list, along with the command index it resides at.
     * Used during PRE-COMPILE stage only.
     * 
     * @param name  - the subroutine name
     * @param cmdIx - the command index of the subroutine
     * 
     * @throws ParserException
     */
    private static void compileAddSubroutine (String name, int cmdIx) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name == null || name.isBlank()) {
            throw new ParserException(functionId + "Subroutine name is empty");
        }
        if (cmdIx < 0) {
            throw new ParserException(functionId + "Invalid index value: " + cmdIx);
        }
        if (subroutines.containsKey(name)) {
            throw new ParserException(functionId + "Subroutine already defined: " + name);
        }
        subroutines.put(name, cmdIx);
        frame.outputInfoMsg(STATUS_DEBUG, INDENT + "Added subroutine: " + name + " at location " + cmdIx);
    }

    /**
     * saves indication of end of MAIN routine (marked by ENDMAIN command).
     * Used during PRE-COMPILE stage only.
     * 
     * @param cmdIx - command index of last line of MAIN
     * 
     * @throws ParserException
     */
    public void compileEndOfMain (int cmdIx) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (! subCallList.isEmpty()) {
            throw new ParserException(functionId + "Duplicate ENDMAIN command");
        }

        SubInfo info = new SubInfo(MAIN_FCTN, 0);
        info.setEndIx(cmdIx);
        subCallList.add(info);
    }

    /**
     * saves info for marking start of subroutine in script.
     * Used during PRE-COMPILE stage only.
     *
     * @param name    - name of subroutine
     * @param cmdIx   - command index of the SUB command
     * 
     * @throws ParserException
     */
    public void compileSubStart (String name, int cmdIx) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (subCallList.size() < 1) {
            throw new ParserException(functionId + "Subroutine defined inside of MAIN");
        }
        // verify name is correct format
        if (! isValidSubName(name)) {
            throw new ParserException(functionId + "Invalid subroutine name: " + name);
        }
        // insert the range for MAIN, if we haven't listed it yet
        if (subCallList.isEmpty()) {
            SubInfo info = new SubInfo(MAIN_FCTN, 0);
            info.setEndIx(cmdIx - 1);
            subCallList.add(info);
        }
        
        // verify the name wasn't repeated
        SubInfo info;
        Integer ix = getCompileInfo(name);
        if (ix == null) {
            info = new SubInfo(name, cmdIx);
            subCallList.add(info);
        } else {
            throw new ParserException(functionId + "Duplicate subroutine definition: " + name);
        }
        lastSubName = name;
    }
    
    /**
     * saves info for marking end of subroutine in script.
     * Used during COMPILE stage only.
     *
     * @param cmdIx - command index of the ENDSUB command
     * 
     * @throws ParserException
     */
    public void compileSubEnd (int cmdIx) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (subCallList.size() <= 1) {
            throw new ParserException(functionId + "Subroutine defined insided of MAIN");
        }
        if (lastSubName == null) {
            throw new ParserException(functionId + "No subroutines have been defined");
        }

        // update status of subroutine
        Integer ix = getCompileInfo(lastSubName);
        if (ix == null) {
            throw new ParserException(functionId + "Subroutine not found: " + lastSubName);
        }
        SubInfo info = subCallList.get(ix);
        if (! info.isComplete()) {
            info.setEndIx(cmdIx);
            replaceCompileInfo (ix, info);
        } else {
            throw new ParserException(functionId + "Duplicate ENDSUB for subroutine: " + lastSubName);
        }
    }

    /****************************************
    * THESE ARE COMPILER METHODS ONLY !
    *****************************************/
    
    /**
     * saves EXECUTION info for marking start of subroutine in script.
     * Used during COMPILE stage only (NOT PRE-COMPILE because it doesn't know command indeices).
     *
     * @param name    - name of subroutine
     * @param cmdIx   - command index of the SUB command
     * 
     * @throws ParserException
     */
    public void compileSubStartCmdIx (String name, int cmdIx) throws ParserException {
        compileAddSubroutine (name, cmdIx);
    }
    
    /**
     * saves info for subroutine name in script when GOSUB command found.
     * Used during COMPILE stage only.
     *
     * @param name - name of subroutine
     * 
     * @throws ParserException
     */
    public void compileSubGosub (String name) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        // verify name is correct format
        if (! isValidSubName(name)) {
            throw new ParserException(functionId + "Invalid subroutine name: " + name);
        }
        // add to list of subroutines called, if not already defined
        if (! isSubUsed(name)) {
            subUsed.add(name);
        }
    }
    
    /**
     * checks if a previous SUB start command was defined when RETURN command found.
     * Used during COMPILE stage only.
     *
     * @throws ParserException
     */
    public void compileSubReturn () throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (lastSubName == null) {
            throw new ParserException(functionId + "RETURN encountered when not in a subroutine");
        }
    }
    
    /****************************************
    * THESE ARE EXECUTION METHODS ONLY !
    *****************************************/
    
    /**
     * finds the subroutine listing and returns the command index it starts at.
     * 
     * @param name  - the subroutine name
     * @param cmdIx - the command index of the current command
     * 
     * @return the new command index to execute
     * 
     * @throws ParserException
     */
    public int subBegin (String name, int cmdIx) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name == null || name.isBlank()) {
            throw new ParserException(functionId + "Subroutine name is empty");
        }
        if (! subroutines.containsKey(name)) {
            throw new ParserException(functionId + "Subroutine name not found: " + name);
        }
        
        // push the command index location to return to
        frame.outputInfoMsg(STATUS_PROGRAM, INDENT + "Subroutine " + name + " index " + cmdIx +
                " entered at level " + (1 + subStack.size()));
        SubCall info = new SubCall(cmdIx, name);
        subStack.push(info);
        return subroutines.get(name);
    }

    /**
     * returns the line after the last subroutine call.
     * 
     * @param param - the parameter value to return to the caller (null if none)
     * 
     * @return the next command index to execute
     * 
     * @throws ParserException
     */
    public static int subReturn (String param) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (subStack.isEmpty()) {
            throw new ParserException(functionId + "RETURN when not in subroutine");
        }
        
        // return the parameter value (null if none)
        VarReserved.putSubRetValue (param);
        
        // get the command index of the calling function to return to
        SubCall info = subStack.pop();
        int index = info.getIndex();
        frame.outputInfoMsg(STATUS_PROGRAM, INDENT + "Subroutine returned to index: " + index);
        return index;
    }

    /**
     * returns the current subroutine nesting level.
     * 
     * @return the current subroutine level
     */
    public static int getSubroutineLevel () {
        return subStack.size();
    }

}
