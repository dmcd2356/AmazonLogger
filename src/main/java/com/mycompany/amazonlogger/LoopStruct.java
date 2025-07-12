/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import com.mycompany.amazonlogger.GUILogPanel.MsgType;
import java.util.Stack;

/**
 * This handles the loop parameter processing
 * 
 * @author dan
 */
public class LoopStruct {

    private static final String CLASS_NAME = LoopStruct.class.getSimpleName();
    
    private static final String LOOP_FOREVER = "#FOREVER#";
    
    // the chars used to seperate entries in reporting variable contents to the client
    private static final String DATA_SEP = "::";
    
    private final String    name;       // parameter name for the loop
    private       Integer   value;      // current parameter value
    private       LoopParam valStart;   // loop start value
    private       LoopParam valEnd;     // loop end   value
    private       LoopParam valStep;    // value to increment value by on each loop
    private final boolean   bInclEnd;   // true if include valEnd limit before exit, false if exit on = valEnd
    private       String    comparator; // the comparison symbols used for checking against valEnd
    private final Integer   ixBegin;    // command index of start of loop (where it returns to)
    private       Integer   ixEnd;      // command index of ENDFOR (end of loop or break reached)
    private       Integer   ifLevel;    // IF nest level (to make sure loop def doesn't exceed the boundaries)
    private       LoopId    loopId;     // the loop ID value
    private       Integer   maxLoops;   // the max number of loops to run in FOREVER mode (null if no safety)
    private       boolean   bUpdate;    // true when a value has been written to

    // loop stack for keeping track of current nesting of loops as program runs
    private static final Stack<LoopId> loopStack = new Stack<>();


    /**
     * Initializes the loop structure.
     * This is called when the the FOR EVER command is first parsed during compile.
     * 
     * @param maxLoops - the max number of loops to run before terminating program (safety switch - null to omit)
     * @param index - the index of the command list to return to on each loop (index of FOR)
     * @param ifLev - the IF nest level at start of LOOP def
     * 
     * @throws ParserException
     */
    LoopStruct (Integer maxLoops, int index, int ifLev) throws ParserException {
        String functionId = CLASS_NAME + " (new FOREVER): ";
       
        this.name     = LOOP_FOREVER;
        this.bUpdate  = false;
        this.loopId   = new LoopId(name, index);
        this.valStart = new LoopParam(0L);
        this.valEnd   = new LoopParam(0L);
        this.valStep  = new LoopParam(1L);
        this.bInclEnd = false;
        this.value    = 0;
        this.ixBegin  = index;
        this.ixEnd    = null;
        this.ifLevel  = ifLev;
        this.maxLoops = maxLoops;
        if (maxLoops == null) {
            GUILogPanel.outputInfoMsg(MsgType.WARN, functionId + "Loop " + name + " @" + index + " level " + LoopStruct.getStackSize() + " is run without safety limit");
        }

        // add entry to the current loop stack
        LoopStruct.pushStack(this.loopId);
        GUILogPanel.outputInfoMsg(MsgType.DEBUG, functionId + "Loop " + name + " @" + index + " level " + LoopStruct.getStackSize() + " starts with IF level " + ifLev);
    }

    /**
     * Initializes the loop structure.
     * This is called when the the FOR command is first parsed during compile.
     * 
     * @param name  - name of the parameter
     * @param start - starting value of the parameter
     * @param end   - ending value of the parameter
     * @param step  - amount to increase the parameter on each iteration
     * @param index - the index of the command list to return to on each loop (index of FOR)
     * @param ifLev - the IF nest level at start of LOOP def
     * 
     * @throws ParserException
     */
    LoopStruct (String name, ParameterStruct start, ParameterStruct end, ParameterStruct step, boolean bIncl, int index, int ifLev) throws ParserException {
        String functionId = CLASS_NAME + " (new): ";
       
        // check for invalid input
        if (name  == null) throw new ParserException(functionId + "FOR param @ " + index + ": 'name' entry is null");
        if (start == null) throw new ParserException(functionId + "FOR param @ " + index + ": 'start' entry is null");
        if (end   == null) throw new ParserException(functionId + "FOR param @ " + index + ": 'end' entry is null");
        try {
            isValidLoopName(name, index);
        } catch (ParserException exMsg) {
            Utils.throwAddendum (exMsg.getMessage(), functionId + " [FOR param @ " + index + "]");
        }

        // now verify types of params and check whether they are discreet or reference
        this.valStart = null;
        try {
            if (checkIfValidInteger ("START", start))
                this.valStart = new LoopParam (start.getIntegerValue());
            else
                this.valStart = new LoopParam (start.getStringValue());

            if (checkIfValidInteger ("END", end))
                this.valEnd = new LoopParam (end.getIntegerValue());
            else
                this.valEnd = new LoopParam (end.getStringValue());

            if (step != null) {
                if (checkIfValidInteger ("STEP", step))
                    this.valStep = new LoopParam (step.getIntegerValue());
                else
                    this.valStep = new LoopParam (step.getStringValue());
            } else {
                this.valStep = new LoopParam (1L); // set to default value
            }
        } catch (ParserException exMsg) {
            Utils.throwAddendum (exMsg.getMessage(), functionId);
        }
        
        this.bUpdate  = false;
        this.name     = name;
        this.loopId   = new LoopId(name, index);
        this.bInclEnd = bIncl;
        this.value    = 0; // this is called during compile, so if the start param is a ref, we don't know the value
        this.ixBegin  = index;
        this.ixEnd    = null;
        this.ifLevel  = ifLev;
        
        // add entry to the current loop stack
        LoopStruct.pushStack(this.loopId);
        GUILogPanel.outputInfoMsg(MsgType.DEBUG, functionId + "Loop " + name + " @" + index + " level " + LoopStruct.getStackSize() + " starts with IF level " + ifLev);
    }

    /**
     * this resets the flag that indicates the loop value has changed.
     * It is called at the start of a run or a step in Network mode.
     */
    public static void resetUpdate() {
        if (! loopStack.empty()) {
            for (int ix = 0; ix < loopStack.size(); ix++) {
                LoopId id = loopStack.get(ix);
                LoopStruct loop = LoopParam.getLoopStruct (id);
                if (loop != null) {
                    loop.bUpdate = false;
                }
            }
        }
    }

    public static String getLoopInfo (LoopId id, LoopStruct loop) {
        if (loop != null) {
            String start = "?";
            String end   = "?";
            String step  = "?";
            try {
                start = loop.valStart.getIntValue().toString();
            } catch (ParserException exMsg) {  }
            try {
                end = loop.valEnd.getIntValue().toString();
            } catch (ParserException exMsg) {  }
            try {
                step = loop.valStep.getIntValue().toString();
            } catch (ParserException exMsg) {  }
            return "<name> " + id.getName() + " @ " + ScriptCompile.getLineNumber(id.getIndex())
                    + " " + DATA_SEP + " <owner> "   + Subroutine.findSubName(loop.ixBegin)
                    + " " + DATA_SEP + " <type> Integer"
                    + " " + DATA_SEP + " <value> "   + loop.value
                    + " " + DATA_SEP + " <start> "   + start
                    + " " + DATA_SEP + " <end> "     + end
                    + " " + DATA_SEP + " <step> "    + step
                    + " " + DATA_SEP + " <incl> "    + loop.bInclEnd
                    + " " + DATA_SEP + " <comp> "    + loop.comparator ;
        }
        return null;
    }
    
    /**
     * sends the LoopId for the current running loops to the client.
     */
    public static void sendVarChange () {
        if (! AmazonReader.isOpModeNetwork()) {
            return;
        }
        for (int ix = 0; ix < loopStack.size(); ix++) {
            LoopId id = loopStack.get(ix);
            LoopStruct loop = LoopParam.getLoopStruct (id);
            if (loop != null && loop.bUpdate) {
                String entry = getLoopInfo (id, loop);
                entry = "[<section> LOOP " + DATA_SEP + " " + entry + "]";
                TCPServerThread.sendVarInfo(entry);
            }
        }
    }

    /**
     * resets the stack.
     * This should be done prior to compiling or running a second time, since the
     * previous run might not have completed and left the stack with old entries.
     */
    public static void resetStack() {
        loopStack.clear();
    }
    
    /**
     * checks if the loop index values are valid integers or variable references.
     * 
     * @param which - name of the loop parameter setting (START, END, STEP)
     * @param param - the parameter value
     * 
     * @return true if value is a discreet integer value
     * 
     * @throws ParserException 
     */
    private static boolean checkIfValidInteger (String which, ParameterStruct param) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
       
        ParameterStruct.ParamClass refType = param.getParamClass();
        switch (refType) {
            case Discrete:
                ParameterStruct.ParamType type = param.getParamType();
                Long value = param.getIntegerValue();
                if (value == null) {
                    throw new ParserException(functionId + "Invalid entry for " + which + " loop - type: " + type + " (value = " + param.getStringValue());
                }
                return true;
            case Reference:
                String refName = param.getVariableRefName();
                if (refName == null) {
                    throw new ParserException(functionId + "Invalid reference for " + which + " loop - name: " + param.getStringValue());
                }
                return false;
            default:
                throw new ParserException(functionId + "Invalid class of parameter for loop: " + refType);
        }
    }
    
    /**
     * returns the loop name for the loop structure.
     * 
     * @return variable name associated with the loop
     */
    public String getLoopName () {
        return this.name;
    }
    
    /**
     * returns the loop ID for the loop structure.
     * 
     * @return unique loop ID associated with the loop
     */
    public LoopId getLoopId () {
        return this.loopId;
    }
    
    /**
     * determines if the loop parameter indicates it is looping forever.
     * 
     * @return true if name indicates loop forever
     */
    public boolean isForever () {
        return LOOP_FOREVER.contentEquals(this.name);
    }
    
    /**
     * sets the index of the command list for the corresponding ENDFOR command.
     * This is called when the ENDFOR command is parsed during compile.
     * 
     * @param index - the index of the ENDFOR command
     */
    public void setLoopEnd (int index) {
        this.ixEnd = index;
    }
    
    /**
     * indicates if the loop has been completely defined (matching ENDFOR found)
     * 
     * @return true if the loop has been fully defined (FOR and ENDFOR have been parsed)
     */
    public boolean isLoopComplete () {
        return ixEnd != null;
    }
    
    /**
     * gets the current loop index value.
     * 
     * @return the current loop index value
     */
    public Integer getLoopValue () {
        return value;
    }

    /**
     * indicates if the loop exceeds the bounds of the IF block it is in.
     * This should be called when any of the commands NEXT, BREAK, ENDFOR are called
     * during compile.
     * 
     * @param command - the FOR loop command executed
     * @param level   - the current level of the instruction
     * 
     * @throws ParserException
     */
    public void checkLoopIfLevelValid (String command, int level) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        if (level != this.ifLevel) {
            throw new ParserException(functionId + command + " exceeded bounds of enclosing IF block: IF level = " + level + ", should be: " + this.ifLevel);
        }
    }
    
    /**
     * resets the loop value.
     * This is called when the FOR command is parsed during the execution stage.
     * 
     * @param index - the current command index
     * 
     * @return the next command index to run
     * 
     * @throws ParserException
     */
    public int startLoop (int index) throws ParserException {
        value = valStart.getIntValue();
        bUpdate = true;
        
        // if we are looping without end, we always proceed to the next command
        if (isForever()) {
            return index + 1;
        }
        
        // just in case the loop is set to not run, perform the exit comparison
        if (valStep.getIntValue() >= 1) {
            comparator = bInclEnd ? "<=" : "<";
        } else {
            comparator = bInclEnd ? ">=" : ">";
        }
        boolean bResult = Utils.compareParameterValues (valStart.getIntValue(), valEnd.getIntValue(), comparator);
        if (! bResult) {
            return ixEnd;
        }
        return index + 1;
    }

    /**
     * returns the command index to exit the loop.
     * This should be called when the BREAK command is parsed during the execution stage.
     * 
     * @return the index of the next command to execute
     */
    public int loopBreak () {
        return ixEnd;
    }
    
    /**
     * performs the increment of the loop parameter and determines if the loop
     * should be exited. It returns the command index of either the start of
     * the loop or the end of the loop.
     * This should be called when either the NEXT or the CONTINUE command is
     *  parsed during the execution stage.
     * 
     * @return the index of the next command to execute
     * 
     * @throws ParserException
     */
    public int loopNext () throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        // increment param by the step value and check if we have completed
        value += valStep.getIntValue();
        bUpdate = true;
        GUILogPanel.outputInfoMsg(MsgType.DEBUG, "     LOOP " + loopId.printLoopId() + " value = " + value);
        
        // skip checking for exit if running forever
        if (! isForever()) {
            boolean bResult = Utils.compareParameterValues (value, valEnd.getIntValue(), comparator);
            if (! bResult) {
                return ixEnd;   // loop completed
            }
        } else if (maxLoops != null && value >= maxLoops) {
            throw new ParserException(functionId + "SAFETY CHECK ON LOOP: MAX LOOPS EXCEEDED!");
        }

        // not done yet, start on the line following the FOR command
        return ixBegin + 1;
    }
    
    // THESE HANDLE THE LOOP STACK ACCESS

    /**
     * gets the current stack size.
     * 
     * @return the number of entries in the stack
     */    
    public static int getStackSize () {
        return loopStack.size();
    }
    
    /**
     * pushes the next loop id onto the stack.
     * 
     * @param loopId - the name/index id value to identify the loop
     */    
    public static void pushStack (LoopId loopId) {
        loopStack.push(loopId);
    }
    
    /**
     * pops the next entry off the stack.
     * 
     * @return the name/index loop id value to use
     */    
    public static LoopId popStack () {
        return loopStack.pop();
    }
    
    /**
     * returns the next entry that is on the stack.
     * 
     * @return the name/index loop id value to use
     */    
    public static LoopId peekStack () {
//        return loopStack.firstElement();
        return loopStack.peek();
    }
    
    /**
     * checks for a loop parameter that is currently active and returns its current value.
     * 
     * @param name - name of the loop parameter
     * 
     * @return the current value of the loop parameter (null if parameter not currently active)
     */
    public static Integer getCurrentLoopValue (String name) {
        if (!loopStack.empty()) {
            for (int ix = 0; ix < loopStack.size(); ix++) {
                LoopId loopId = loopStack.get(ix);
                if (loopId.getName().contentEquals(name)) {
                    return LoopParam.getLoopValue(loopId);
                }
            }
        }
        return null;
    }
    
    /**
     * checks if a Loop Variable name is valid.
     * 
     * @param name - the name to check
     *               name must be only alphanumeric or '_' chars,
     *               cannot be a reserved name (RESPONSE, STATUS, ...)
     *               or a String or Integer Variable name.
     * @param index - the command index for the FOR command
     * 
     * @throws ParserException - if not valid
     */
    private static void isValidLoopName (String name, int index) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        try {
            if (name.startsWith("$")) {
                name = name.substring(1);
            }

            // verify the formaat of the Variable name
            Variables.verifyVariableFormat(name);

            // make sure it is not the name of a command
            if (CommandStruct.isValidCommand(name) != null) {
                throw new ParserException(functionId + "using Reserved command name: " + name);
            }

            // make sure its not the name of a defined or reserved Variable
            Variables.VarClass type = PreCompile.variables.getVariableClass(name);
            if (type != Variables.VarClass.UNKNOWN && type != Variables.VarClass.LOOP) {
                throw new ParserException(": using " + type.toString() + " Variable name: " + name);
            }

            // now check if this loop name is nested in a loop having same name
            // get the list of loops using this Variable name (if any)
            Integer loopIx = LoopParam.checkLoopNesting(name);
            if (loopIx != null) {
                throw new ParserException(functionId + ": Loop param " + name + " @ " + index + " is nested in same name at " + loopIx);
            }
        } catch (ParserException exMsg) {
            Utils.throwAddendum (exMsg.getMessage(), functionId);
        }
    }
    
}
