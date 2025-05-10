/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.UIFrame.STATUS_DEBUG;
import static com.mycompany.amazonlogger.UIFrame.STATUS_PROGRAM;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

/**
 *
 * @author dan
 */
public class Subroutine {
    
    private static final String CLASS_NAME = Subroutine.class.getSimpleName();
    
    // contains a list of the subroutines and their corresponding command index locations
    private static final HashMap<String, Integer> subroutines = new HashMap<>();
    
    // contains the running list of command index locations to return to after sub call
    private final static Stack<SubCall> subStack = new Stack<>();
    
    // this holds the script line numbers associated with the subroutines (during COMPILE)
    private static final HashMap<String, Bounds> boundary = new HashMap<>();
    private static String lastSubName = null;
    
    // this holds the calls to subs to verify at the end of compilation that they are defined
    private static final ArrayList<String> subCallList = new ArrayList<>();
    
    private static Integer endOfMainLine = null; // the last line of the main program

    /**
     * this class is used for defining the bounds of the script file for the subroutines.
     * 
     * This allows us to determine whether a particular line of the script is
     *   contained in the MAIN section of the script or in a subroutine, and which
     *   subroutine. This is only used for the COMPILER as it uses raw line numbers
     *   rather than program indices.
     */
    private class Bounds {
        private int start;
        private int end;
        private int cmdIx;
        
        Bounds (int start, int cmdIx) {
            this.start = start;
            this.end = 0; // set to an invalid value
            this.cmdIx = cmdIx;
        }
        
        public boolean setEnd (int end) {
            if (this.end <= 0) {
                this.end = end;
                return true;
            }
            return false;
        }
        
        public boolean isContained(int lineNum) {
            return (this.start <= lineNum && lineNum <= this.end);
        }
        
        public int getCmdIx() {
            return this.cmdIx;
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
        private static final String CLASS_NAME = SubCall.class.getSimpleName();
    
        private final int     index;      // command index of return location
        private final String  name;       // name of current subroutine
        private final ArrayList<ParameterStruct> args;
        
        SubCall (int index, String name, ArrayList<ParameterStruct> argList) {
            this.index = index;
            this.name  = name;
            this.args  = argList;
        }
        
        String getName() {
            return this.name;
        }
        
        int getIndex() {
            return this.index;
        }
        
        int getArgCount() {
            if (args != null) {
                return args.size();
            }
            return 0;
        }
        
        ParameterStruct getArg(int index) throws ParserException {
            String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

            if (args != null) {
                return args.get(index);
            }
            throw new ParserException(functionId + "Subroutine " + this.name + " arg index exceeded: " + index + " (max = " + getArgCount() + ")");
        }
    }

    /**
     * returns the subroutine name the line is currently contained in.
     * 
     * @param lineNum - the current line number
     * 
     * @return the name of the current subroutine (MAIN if not in a subroutine)
     */
    public static String getSubName (int lineNum) {
        if (endOfMainLine == null || lineNum <= endOfMainLine) {
            return "MAIN";
        }
        if (! boundary.isEmpty()) {
            for (String name : boundary.keySet()) {
                if (boundary.get(name).isContained(lineNum)) {
                    return name;
                }
            }
        }
        return "MAIN";
    }

    /**
     * returns the specified parameter for the current subroutine.
     * 
     * @param index - the subroutine argument selection
     * 
     * @return the parameter selection for the current subroutine
     * 
     * @throws ParserException
     */
    public static ParameterStruct getSubArgument (int index) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        SubCall current = subStack.peek();
        if (index < 0 || index >= current.getArgCount()) {
            throw new ParserException(functionId + "Invalid index value: " + index);
        }
        if (index == 0) {
            // $0 indicates we return the name of the subroutine
            return new ParameterStruct(current.getName(),
                    ParameterStruct.ParamClass.Discrete, ParameterStruct.ParamType.String);
        }
        
        return current.getArg(index - 1);
    }
    
    /**
     * checks if any subroutines were called that were not defined.
     * This is done at the end of the COMPILE phase.
     * 
     * @throws ParserException 
     */
    public static void checkSubroutineMissing() throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (! subCallList.isEmpty()) {
            throw new ParserException(functionId + "Missing subroutine definitions that were called: " + subCallList);
        }
    }
    
    /**
     * saves indication of end of MAIN routine (marked by EXIT command).
     * 
     * @param lineNum - line number of last line of MAIN
     * 
     * @throws ParserException
     */
    public void compileEndOfMain (int lineNum) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (endOfMainLine != null) {
            throw new ParserException(functionId + "Duplicate EXIT command (prev at line " + endOfMainLine + ")");
        }
        endOfMainLine = lineNum;
    }

    /**
     * saves info for marking start of subroutine in script.
     * Used during COMPILE stage only.
     *
     * @param name    - name of subroutine
     * @param lineNum - line number of SUB command
     * @param cmdIx   - the command index of the SUB command
     * 
     * @throws ParserException
     */
    public void compileSubStart (String name, int lineNum, int cmdIx) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (endOfMainLine == null) {
            throw new ParserException(functionId + "Subroutine defined insided of MAIN");
        }
        // verify name is correct format
        if (! isValidSubName(name)) {
            throw new ParserException(functionId + "Invalid subroutine name: " + name);
        }
        // verify the name wasn't repeated
        Bounds bounds = boundary.get(name);
        if (bounds != null) {
            throw new ParserException(functionId + "Duplicate subroutine definition: " + name);
        }
        lastSubName = name;
        boundary.put(name, new Bounds(lineNum, cmdIx));
        addSubroutine (name, cmdIx);
    }
    
    /**
     * saves info for marking end of subroutine in script.
     * Used during COMPILE stage only.
     *
     * @param lineNum - line number of ENDSUB command
     * 
     * @throws ParserException
     */
    public void compileSubEnd (int lineNum) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (endOfMainLine == null) {
            throw new ParserException(functionId + "Subroutine defined insided of MAIN");
        }
        if (lastSubName == null) {
            throw new ParserException(functionId + "No subroutines have been defined");
        }
        Bounds bounds = boundary.get(lastSubName);
        if (bounds == null) {
            throw new ParserException(functionId + "Subroutine not found: " + lastSubName);
        }
        boolean bSuccess = bounds.setEnd(lineNum);
        if (! bSuccess) {
            throw new ParserException(functionId + "Duplicate ENDSUB on line " + bounds.end);
        }
        // eliminate current sub name from list of calls
        if (subCallList.contains(lastSubName)) {
            subCallList.remove(lastSubName);
        }
        boundary.replace(lastSubName, bounds);
    }

    public void compileSubGosub (String name) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        // verify name is correct format
        if (! isValidSubName(name)) {
            throw new ParserException(functionId + "Invalid subroutine name: " + name);
        }
        // add to list of subroutines called
        if (! subCallList.contains(name)) {
            subCallList.add(name);
        }
    }
    
    public void compileSubReturn () throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (lastSubName == null) {
            throw new ParserException(functionId + "RETURN encountered when not in a subroutine");
        }
    }
    
    /**
     * adds a subroutine name to the list, along with the command index it resides at.
     * 
     * @param name  - the subroutine name
     * @param index - the command index of the subroutine
     * 
     * @throws ParserException
     */
    public static void addSubroutine (String name, int index) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name == null || name.isBlank()) {
            throw new ParserException(functionId + "Subroutine name is empty");
        }
        if (index < 0) {
            throw new ParserException(functionId + "Invalid index value: " + index);
        }
        if (subroutines.containsKey(name)) {
            throw new ParserException(functionId + "Subroutine already defined: " + name);
        }
        subroutines.put(name, index);
        frame.outputInfoMsg(STATUS_DEBUG, "Added subroutine: " + name + " at location " + index);
    }

    /**
     * returns the index of the subroutine.
     * 
     * @param name  - the subroutine name
     * 
     * @return the command index of the subroutine
     * 
     * @throws ParserException
     */
    public static int getSubroutine (String name) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name == null || name.isBlank()) {
            throw new ParserException(functionId + "Subroutine name is empty");
        }
        Integer index = subroutines.get(name);
        if (index ==  null) {
            throw new ParserException(functionId + "Subroutine not found: " + name);
        }
        return index;
    }

    /**
     * returns the subroutine command index.
     * 
     * @param name    - the subroutine name
     * @param argList - the list of any arguments to pass
     * @param index   - the command index of the current command
     * 
     * @return the new command index to execute
     * 
     * @throws ParserException
     */
    public int subBegin (String name, ArrayList<ParameterStruct> argList, int index) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (name == null || name.isBlank()) {
            throw new ParserException(functionId + "Subroutine name is empty");
        }
        if (! subroutines.containsKey(name)) {
            throw new ParserException(functionId + "Subroutine name not found: " + name);
        }
        
        // push the command index location to return to
        int argSize = (argList == null) ? 0 : argList.size();
        frame.outputInfoMsg(STATUS_PROGRAM, "      Subroutine " + name + " index " + index +
                " entered at level " + (1 + subStack.size()) + ", " + argSize + " args passed");
        SubCall info = new SubCall(index, name, argList);
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
        Variables.putSubRetValue (param);
        
        // get the command index of the calling function to return to
        SubCall info = subStack.pop();
        int index = info.getIndex();
        frame.outputInfoMsg(STATUS_PROGRAM, "      Subroutine returned to index: " + index);
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

     /**
     * indicates if a subroutine name is found in the listing.
     * 
     * @param name  - the subroutine name
     * 
     * @return true if sub name found in list
     * 
     * @throws ParserException
     */
    public static boolean isValidSubName (String name) throws ParserException {
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

}
