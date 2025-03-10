package com.mycompany.amazonlogger;

/**
 * This handles the loop parameter processing
 * 
 * @author dan
 */
public class LoopStruct {

    private static final String CLASS_NAME = "LoopStruct";
    
    private final String          name;       // parameter name for the loop
    private       Integer         value;      // current parameter value
    private final ParameterStruct valStart;   // loop start value
    private final ParameterStruct valEnd;     // loop end   value
    private final ParameterStruct valStep;    // value to increment value by on each loop
    private final String          comparator; // the comparison symbols used for checking against valEnd
    private final Integer         ixBegin;    // command index of start of loop (where it returns to)
    private       Integer         ixEnd;      // command index of ENDFOR (end of loop or break reached)
    private       Integer         ifLevel;    // IF nest level (to make sure loop def doesn't exceed the boundaries)
    
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
    LoopStruct (String name, ParameterStruct start,
                             ParameterStruct end,
                             ParameterStruct step,
                             String comp, int index, int ifLev) throws ParserException {
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
            ParameterStruct.isValidLoopName(name, index);
        } catch (ParserException exMsg) {
            throw new ParserException(functionId + "FOR param @ " + index + ": name error - " + exMsg);
        }
        
        this.name     = name;
        this.value    = start.unpackIntegerValue();
        this.valStart = start;
        this.valEnd   = end;
        this.valStep  = step;
        this.comparator = comp;
        this.ixBegin  = index;
        this .ixEnd   = null;
        this.ifLevel  = ifLev;
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
        value = valStart.unpackIntegerValue();
        
        // just in case the loop is set to not run, perform the exit comparison
        boolean bResult = Utils.compareParameterValues (valStart, valEnd, comparator);
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
        value += valStep.unpackIntegerValue();
        
        ParameterStruct newValue = new ParameterStruct(value);
        boolean bResult = Utils.compareParameterValues (newValue, valEnd, comparator);
        if (! bResult) {
            return ixEnd;   // loop completed
        }

        // not done yet, start on the line following the FOR command
        return ixBegin + 1;
    }
}
