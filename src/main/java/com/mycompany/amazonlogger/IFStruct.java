/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.UIFrame.STATUS_PROGRAM;
import java.util.ArrayList;

/**
 *
 * @author dan
 */
public class IFStruct {
    
    private static final String CLASS_NAME = "IFStruct";
    
    Integer ixIf;       // command index for IF statement
    ArrayList<Integer> ixElse;  // command index for ELSE & ELSEIF statement(s)
    boolean bFinalElse; // true if last entry in ixElse list was an ELSE, so there can be no more
    Integer ixEndIf;    // command index for ENDIF statement
    Integer loopLevel;  // loop nest level for start of IF statement
    boolean bCondMet;   // set to the IF level condition being executed (null if none)
        
    IFStruct (int index, int loopLevel) {
        this.ixIf    = index;
        this.ixElse  = new ArrayList<>();
        this.ixEndIf = null;
        this.bFinalElse = false;
        this.bCondMet = false;
            
        // save the loop level for testing whether ELSE and ENDIF are at same level
        this.loopLevel = loopLevel;

        String cmdId = "line " + index + " IF ";
        String nestLevel = " (nest level " + loopLevel + ")";
        frame.outputInfoMsg(STATUS_PROGRAM, "    " + cmdId + " @ " + this.ixIf + nestLevel);
    }

    /**
     * verifies that the IF statement command has the required entries.
     * 
     * @return true if valid
     */
    boolean isValid () {
        return this.ixIf != null && this.ixEndIf != null && this.ixElse != null && this.loopLevel != null;
    }
        
    /**
     * determines if one of the IF/ELSEIF conditions has been met.
     * This is used to determine when the ELSE or ELSEIF command is the next
     * command to run whether it was being jumped to because the previous condition
     * was not met or because the previous condition WAS met and has completed its
     * execution. If the first case, it should handle the ELSE or ELSEIF statement,
     * but if the last case, it should jump to the next ENDIF statement.
     * 
     * @return true if condition was met
     */
    boolean isConditionMet () {
        return this.bCondMet;
    }
        
    /**
     * sets the flag to indicate the IF condition has been met.
     */
    void setConditionMet () {
        this.bCondMet = true;
    }
        
    /**
     * get the command index of the next ELSE, ELSEIF or ENDIF statement.
     * 
     * @param index - the current command index
     * 
     * @return index of next branch if the IF is not true
     * 
     * @throws ParserException
     */
    int getElseIndex (int index) throws ParserException {
        String functionId = CLASS_NAME + ".getElseIndex: ";

        if (this.ixElse != null && !this.ixElse.isEmpty()) {
            // return the location of the next ELSE or ELSEIF statement
            for (int ix = 0; ix < this.ixElse.size(); ix++) {
                if (index < this.ixElse.get(ix))
                    return this.ixElse.get(ix);
            }
        }
        // no ELSE case matches, go to end of IF
        if (this.ixEndIf == null) {
            throw new ParserException(functionId + "Missing ENDIF command index for IF @ " + this.ixIf);
        }
        return this.ixEndIf;
    }

    /**
     * get the command index of the next ENDIF statement.
     * 
     * @param index - the current command index
     * 
     * @throws ParserException
     */
    int getEndIndex () throws ParserException {
        return this.ixEndIf;
    }

    /**
     * sets the IF conditions when an ELSE or ELSEIF command is found.
     * 
     * @param index     - the current command line index
     * @param bElseIf   - true if ELSEIF command, false if ELSE command
     * @param loopLevel - the current FOR loop nest level
     * 
     * @throws ParserException 
     */
    void setElseIndex (int index, boolean bElseIf, int loopLevel) throws ParserException {
        String functionId = CLASS_NAME + ".setElseIndex: ";

        String command = (bElseIf) ? "ELSEIF" : "ELSE";
        String cmdId = "line " + index + " " + command + " ";
        String nestLevel = " (nest level " + loopLevel + ")";
        
        // if ELSE statement was previously defined, can't have any more ELSEIF or another ELSE
        if (this.bFinalElse && !this.ixElse.isEmpty()) {
            throw new ParserException(functionId + cmdId + "cmd already set for IF cmd @ " + this.ixIf + " on line : " + this.ixElse.getLast());
        }
            
        // set flag to final when an ELSE startement is used - can't have any other ELSE or ELSEIF
        this.bFinalElse = !bElseIf;
        if (this.loopLevel != loopLevel) {
            throw new ParserException(functionId + cmdId + "cmd outside of loop level " + this.loopLevel + nestLevel);
        }
        this.ixElse.add(index);
        frame.outputInfoMsg(STATUS_PROGRAM, "    " + cmdId + "    entry " + this.ixElse.size() + " for IF @ " + this.ixIf + nestLevel);
    }
        
    /**
     * sets the IF conditions when an ENDIF command is found.
     * 
     * @param index     - the current command line index
     * @param loopLevel - the current FOR loop nest level
     * 
     * @throws ParserException 
     */
    void setEndIfIndex (int index, int loopLevel) throws ParserException {
        String functionId = CLASS_NAME + ".setEndIfIndex: ";

        String cmdId = "line " + index + " ENDIF ";
        String nestLevel = " (nest level " + loopLevel + ")";
        
        if (this.ixEndIf != null) {
            throw new ParserException(functionId + cmdId + "cmd already set for IF cmd @ " + this.ixIf + " on line : " + this.ixEndIf);
        }
        if (this.loopLevel != loopLevel) {
            throw new ParserException(functionId + cmdId + "cmd outside of loop level " + this.loopLevel + nestLevel);
        }
        this.ixEndIf = index;
        frame.outputInfoMsg(STATUS_PROGRAM, "    " + cmdId + "    for IF @ " + this.ixIf + nestLevel);
    }

}
