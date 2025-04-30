/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import java.util.Stack;

/**
 * This handles the loop parameter processing
 * 
 * @author dan
 */
public class LoopStruct {

    private static final String CLASS_NAME = LoopStruct.class.getSimpleName();
    
    private final String    name;       // parameter name for the loop
    private       Integer   value;      // current parameter value
    private final LoopParam valStart;   // loop start value
    private final LoopParam valEnd;     // loop end   value
    private final LoopParam valStep;    // value to increment value by on each loop
    private final String    comparator; // the comparison symbols used for checking against valEnd
    private final Integer   ixBegin;    // command index of start of loop (where it returns to)
    private       Integer   ixEnd;      // command index of ENDFOR (end of loop or break reached)
    private       Integer   ifLevel;    // IF nest level (to make sure loop def doesn't exceed the boundaries)

    // loop stack for keeping track of current nesting of loops as program runs
    private static final Stack<LoopId> loopStack = new Stack<>();


    /**
     * Initializes the loop structure.
     * This is called when the the FOR command is first parsed during compile.
     * 
     * @param name  - name of the parameter
     * @param start - starting value of the parameter
     * @param end   - ending value of the parameter
     * @param step  - amount to increase the parameter on each iteration
     * @param comp  - the type of comparison for the loop 
     * @param index - the index of the command list to return to on each loop (index of FOR)
     * @param ifLev - the IF nest level at start of LOOP def
     * 
     * @throws ParserException
     */
    LoopStruct (String name, String start, String end, String step, String comp, int index, int ifLev) throws ParserException {
        String functionId = CLASS_NAME + " (new): ";
       
        // check for invalid input
        if (name  == null) throw new ParserException(functionId + "FOR param @ " + index + ": 'name' entry is null");
        if (start == null) throw new ParserException(functionId + "FOR param @ " + index + ": 'start' entry is null");
        if (end   == null) throw new ParserException(functionId + "FOR param @ " + index + ": 'end' entry is null");
        if (step  == null) throw new ParserException(functionId + "FOR param @ " + index + ": 'step' entry is null");
        switch (comp) {
            case "<", "<=", ">", ">=", "!=", "=", "==" -> {
            }
            default -> throw new ParserException(functionId + "FOR param @ " + index + ": Invalid comparison chars: " + comp);
        }
        try {
            isValidLoopName(name, index);
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId + " [FOR param @ " + index + "]");
        }

        try {
            this.name     = name;
            this.valStart = new LoopParam (start);
            this.valEnd   = new LoopParam (end);
            this.valStep  = new LoopParam (step);
            this.value    = valStart.getIntValue(); // set to the current start value if this is a ref param
            this.comparator = comp;
            this.ixBegin  = index;
            this .ixEnd   = null;
            this.ifLevel  = ifLev;
        } catch (ParserException exMsg) {
            throw new ParserException(exMsg + "\n  -> " + functionId);
        }
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
     * indicates if the loop has been completely defined (matching ENDFOR found)
     * 
     * @return true if the loop has been fully defined (FOR and ENDFOR have been parsed)
     */
    public Integer getLoopValue () {
        return value;
    }
    
    /**
     * indicates if the loop exceeds the bounds of the IF block it is in.
     * This should be called when any of the commands NEXT, BREAK, ENDFOR are called
     * during compile.
     * 
     * @param level - the current level of the instruction
     * 
     * @return true if the loop is fully defined within same IF block (or not within IF at all
     */
    public boolean isLoopIfLevelValid (int level) {
        return ifLevel == level;
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
        
        // just in case the loop is set to not run, perform the exit comparison
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
        // increment param by the step value and check if we have completed
        value += valStep.getIntValue();
        
        boolean bResult = Utils.compareParameterValues (value, valEnd.getIntValue(), comparator);
        if (! bResult) {
            return ixEnd;   // loop completed
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
                if (loopId.name.contentEquals(name)) {
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
    public static void isValidLoopName (String name, int index) throws ParserException {
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
            Variables.VarClass type = Variables.getVariableClass(name);
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
            throw new ParserException(exMsg + "\n  -> " + functionId);
        }
    }
    
}
