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
public class Calculation {

    private static final String CLASS_NAME = "Calculation";
    
    
    public enum EntryType {
        ADD,            // addition
        SUB,            // subtraction
        MUL,            // multiplication
        DIV,            // division
        MOD,            // modulus
        Lbracket,       // left  (opening) parenthesis
        Rbracket,       // right (closing) parenthesis
        Param,          // parameter reference value
        Value,          // numeric value
    };
    
    // this holds the array of operands (values/params), operations, and parenthesis
    private static ArrayList<CalcEntry> calcList;
    private static int opCount; // number of operands in calcList
    
    /**
     * Extracts the pertinent parts of a formula into an array of CalcEntry objects.
     * 
     * @param formula - the string containing the calculation formula to extract
     * 
     * @throws ParserException 
     */
    Calculation (String formula) throws ParserException {
        
        calcList = new ArrayList<>();
        while (!formula.isBlank()) {
            // strip off any leading whitespace
            formula = formula.stripLeading();
            
            // seperate the formula into its components of either operation,
            //  parenthesis, or value
            EntryType type = classifyEntry (formula);
            if (type == EntryType.Value || type == EntryType.Param) {
                // if it was a numeric or parameter reference, add it
                String entry = getNextEntry(formula);
                calcList.add(new CalcEntry (type, entry));
                formula = formula.substring(entry.length());
            } else {
                // else, classify and add the paramthesis or operation
                calcList.add(new CalcEntry (type, null));
                formula = formula.substring(1);
            }
        }
        verify();
    }

    /**
     * copies a saved calculation array to this class to be executed.
     * This is used when executing a saved calculation without changing
     *  the saved value, since running the compute() method will modify
     *  the calculation contents.
     * 
     * @param savedValue - the saved calculation value to process
     * 
     * @throws ParserException 
     */
    Calculation (ArrayList<CalcEntry> savedValue) throws ParserException {
        calcList = new ArrayList<>();
        for (int ix = 0; ix < savedValue.size(); ix++) {
            calcList.add(savedValue.get(ix));
        }
        verify();
    }

    /**
     * returns the calculation operand value if there is only 1 operand.
     * 
     * @return the calculation operand value if no calculation to perform, else returns null
     * 
     * @throws ParserException
     */
    public Long getCalcValue() throws ParserException {
        if (opCount == 1) {
            return calcList.getFirst().getValue();
        }
        return null;
    }
    
    /**
     * copies a saved calculation array to this class to be executed.
     * This is used when executing a saved calculation without changing
     *  the saved value, since running the compute() method will modify
     *  the calculation contents.
     * 
     * @return the calculation array
     */
    public ArrayList<CalcEntry> copyCalc () {
        ArrayList<CalcEntry> savedValue = new ArrayList<>();
        for (int ix = 0; ix < calcList.size(); ix++) {
            savedValue.add(calcList.get(ix));
        }
        return savedValue;
    }
    
/**
     * computes the Integer value of the calcList formula.
     * Does any parameter lookup necessary to convert to Integer values.
     * 
     * @return the computed value of the full calculation
     * 
     * @throws ParserException
     */    
    public Long compute () throws ParserException {
        String functionId = CLASS_NAME + ".compute: ";

        // should repeat this process until only 1 entry remains, and it
        //  should be a Value, which is the final calculation result.
        boolean bBrackets = true;
        while (bBrackets) {
            // find the starting locations of each lowest level of parenthesis blocks
            int left = 0;
            bBrackets = false;
            for (int ix = 0; ix < calcList.size(); ix++) {
                CalcEntry entry  = calcList.get(ix);
                if (entry.getType() == EntryType.Lbracket) {
                    bBrackets = true;
                    left = ix;
                } else if (entry.getType() == EntryType.Rbracket) {
                    bBrackets = true;
                    int right = ix;
                    Long value = computeBracket (left, right);
                    CalcEntry param = new CalcEntry(value);
                    // now replace this bracketed section with its computed value
                    // (remove all the entries from the opening to closing parenthesis
                    //  and replace with a single value entry that is the calculation result)
                    calcList.set(left, param);
                    for (int remix = left+1; remix <= right; remix++) {
                        calcList.remove(left+1);
                    }
                    break;
                }
            }
        }
        
        // may still have operations to perform, but should not have anymore parenthesis
        if (calcList.size() != 1) {
            Long value = computeBracket (0, calcList.size()-1);
            CalcEntry param = new CalcEntry(value);
            calcList.set(0, param);
            int count = calcList.size() - 1;
            for (int remix = 0; remix < count; remix++) {
                calcList.remove(1);
            }
        }
        
        // check for errors
        if (calcList.size() != 1) {
            throw new ParserException (functionId + "calc did not complete correctly. " + calcList.size()
                    + " entries still found");
        }
        CalcEntry entry = calcList.getFirst();
        if (entry.getType() != EntryType.Value) {
            throw new ParserException (functionId + "calc did not complete correctly. Entry is of type: " + entry.getType().toString());
        }
        
        return entry.getValue();
    }

    /**
     * computes the value of the bracket in calcList enclosed by the specified indicies.
     * Does any parameter lookup necessary to convert to Integer values.
     * 
     * @param startix - index of left parenthesis of block to compute
     * @param endix   - index of matching right parenthesis of block
     * 
     * @return the computed value of the block
     */    
    private Long computeBracket (int startix, int endix) throws ParserException {
        String functionId = CLASS_NAME + ".computeBracket: ";

        if (endix >= calcList.size()) {
            throw new ParserException (functionId + "invalid index for parenthesis: " + endix + ", array size is " + calcList.size());
        }
        if (endix < startix + 2) {
            throw new ParserException (functionId + "parenthesis end index " + endix + " < start index + 2: " + startix);
        }

        // first get each entry from the bracket range and compile into a list
        //  of values and a list of operations, converting parameter references
        //  to their current values.
        String strDebug = "";
        ArrayList<Long>  bracket = new ArrayList<>();
        ArrayList<EntryType> ops = new ArrayList<>();
        // this may contain the opening and ending parenthesis and may not.
        // compensate accordingly.
        for (int ix = startix; ix <= endix; ix++) {
            CalcEntry entry  = calcList.get(ix);
            switch (entry.getType()) {
                case Param:
                case Value:
                    // add Integer value to array of operands
                    bracket.add(entry.getValue());
                    strDebug += entry.getValue() + " ";
                    break;
                case Lbracket:
                case Rbracket:
                    // do nothing for parenthesis
                    break;
                default:
                    // add operation to array of operations
                    ops.add(entry.getType());
                    strDebug += entry.getType().toString() + " ";
                    break;
            }
        }
        
        // now loop through the operations, executing only the highest priority ones first
        boolean bRanCalc = true;
        while (bRanCalc) {
            // this will find the 1st high priority operation and perform the
            // operation, modifying the list to replace the 2 operands with the
            // result and removing the operation.
            bRanCalc = false;
            for (int ix = 0; ix < ops.size(); ix++) {
                EntryType curOp = ops.get(ix);
                Long value = null;
                Long op1 = bracket.get(ix);
                Long op2 = bracket.get(ix+1);
                switch (curOp) {
                    case EntryType.MUL:
                        value = op1 * op2;
                        frame.outputInfoMsg(STATUS_PROGRAM, "      Calc: " + op1 + " * " + op2 + " = " + value);
                        break;
                    case EntryType.DIV:
                        value = op1 / op2;
                        frame.outputInfoMsg(STATUS_PROGRAM, "      Calc: " + op1 + " / " + op2 + " = " + value);
                        break;
                    case EntryType.MOD:
                        value = op1 % op2;
                        frame.outputInfoMsg(STATUS_PROGRAM, "      Calc: " + op1 + " % " + op2 + " = " + value);
                        break;
                    default:
                        break;
                }
                if (value != null) {
                    // an entry was found, replace the 1st operand with the result
                    // and remove the second operand and the operator.
                    bracket.set(ix, value);
                    bracket.remove(ix+1);
                    ops.remove(ix);
                    bRanCalc = true;
                    break;
                }
            }
        }

        // repeat with the remaining operations
        bRanCalc = true;
        while (bRanCalc) {
            bRanCalc = false;
            for (int ix = 0; ix < ops.size(); ix++) {
                EntryType curOp = ops.get(ix);
                Long value = null;
                Long op1 = bracket.get(ix);
                Long op2 = bracket.get(ix+1);
                switch (curOp) {
                    case EntryType.ADD:
                        value = op1 + op2;
                        frame.outputInfoMsg(STATUS_PROGRAM, "      Calc: " + op1 + " + " + op2 + " = " + value);
                        break;
                    case EntryType.SUB:
                        value = op1 - op2;
                        frame.outputInfoMsg(STATUS_PROGRAM, "      Calc: " + op1 + " - " + op2 + " = " + value);
                        break;
                    default:
                        break;
                }
                if (value != null) {
                    // an entry was found, replace the 1st operand with the result
                    // and remove the second operand and the operator.
                    bracket.set(ix, value);
                    bracket.remove(ix+1);
                    ops.remove(ix);
                    bRanCalc = true;
                    break;
                }
            }
        }
        
        // we should only have a single entry in the bracket array and the ops array
        // should be empty. verify this and if no problems, the result is the last
        // entry in bracket.
        if (!ops.isEmpty() || bracket.size() != 1) {
            throw new ParserException (functionId + "calc did not complete correctly. " + ops.size()
                    + " operations still found and " + bracket.size() + " operands");
        }
        
        frame.outputInfoMsg(STATUS_PROGRAM, "    Calc block (" + strDebug + ") converted to: " + bracket.getFirst());
        return bracket.getFirst();
    }

    /**
     * Verifies the format of the Calculation entries.
     * 
     * @throws ParserException 
     */    
    private void verify () throws ParserException {
        String functionId = CLASS_NAME + ".verify: ";

        // first, verify parenthesis match
        int left = 0;
        int right = 0;
        int valcnt = 0;
        int opcnt = 0;
        boolean isval = false;
        for (int ix = 0; ix < calcList.size(); ix++) {
            EntryType thisType = calcList.get(ix).getType();
            switch (thisType) {
                case EntryType.Lbracket:
                    left++;
                    break;
                case EntryType.Rbracket:
                    right++;
                    if (right > left) {
                        throw new ParserException (functionId + "index " + ix + ": overrun of closing brackets: " + right + " > " + left);
                    }
                    break;
                case EntryType.Value:
                case EntryType.Param:
                    // a value or parameter (must alternate with operations)
                    if (isval) {
                        throw new ParserException (functionId + "index " + ix + ": two values without intervening operation");
                    }
                    isval = true;
                    valcnt++;
                    break;
                default:
                    // any operation (must alternate with values)
                    if (!isval) {
                        throw new ParserException (functionId + "index " + ix + ": two operations without intervening value");
                    }
                    if (valcnt == 0) {
                        throw new ParserException (functionId + "index " + ix + ": operation without preceeding value");
                    }
                    isval = false;
                    opcnt++;
                    break;
            }
        }
        if (left != right) {
            throw new ParserException (functionId + "mismatch of parenthesis: left = " + left + ", right = " + right);
        }
        if (valcnt != opcnt + 1) {
            throw new ParserException (functionId + "mismatch of values & operations: " + valcnt + " values, " + opcnt + "ops");
        }
        
        // save the number of operands
        Calculation.opCount = valcnt;
    }

    /**
     * classifies the type of entry contained in the argument String passed.
     * 
     * @param formula - the value to identify the type of
     * 
     * @return the entry type
     * 
     * @throws ParserException 
     */
    private EntryType classifyEntry (String formula) throws ParserException {
        String functionId = CLASS_NAME + ".classifyEntry: ";

        EntryType type;
        int strlen = formula.length();
        if (strlen < 1) {
            throw new ParserException (functionId + "Blank formula");
        }
        char firstch = formula.charAt(0);
        // in case 1st char is '-', see if 2nd char is digit to know if this is value or operation
        boolean bDigit = false;
        if (strlen > 1 && Character.isDigit(formula.charAt(1))) {
            bDigit = true;
        }
        switch (firstch) {
            case '(':
                type = EntryType.Lbracket;
                break;
            case ')':
                type = EntryType.Rbracket;
                break;
            case '+':
                type = EntryType.ADD;
                break;
            case '-':
                if (!bDigit)
                    type = EntryType.SUB;
                else
                    type = EntryType.Value;
                break;
            case '*':
                type = EntryType.MUL;
                break;
            case '/':
                type = EntryType.DIV;
                break;
            case '%':
                type = EntryType.MOD;
                break;
            case '$':
                type = EntryType.Param;
                break;
            default:
                if (!Character.isDigit(firstch)) {
                    throw new ParserException (functionId + "Invalid character entry for calculation: " + firstch);
                }
                type = EntryType.Value;
                break;
            }
            return type;
    }
    
    private String getNextEntry (String formula) {
        String entry = "";
        char nextch = formula.charAt(0);
        if (nextch == '$') {
            entry += nextch;
            // extracts the Parameter Reference name
            for (int ix = 1; ix < formula.length(); ix++) {
                nextch = formula.charAt(ix);
                if (Character.isLetterOrDigit(nextch) || nextch == '_') {
                    entry += formula.charAt(ix);
                } else {
                    break;
                }
            }
        }
        else if (formula.startsWith("0x")) {
            entry += "0x";
            // extracts the unsigned 32-bit value
            for (int ix = 2; ix < formula.length(); ix++) {
                nextch = formula.charAt(ix);
                if (Character.isDigit(nextch) || (nextch >= 'A' && nextch <= 'F') || (nextch >= 'a' && nextch <= 'f')) {
                    entry += formula.charAt(ix);
                } else {
                    break;
                }
            }
        }
        else {
            // extracts the 64-bit long Integer value
            for (int ix = 0; ix < formula.length(); ix++) {
                nextch = formula.charAt(ix);
                if (Character.isDigit(nextch) || nextch == '-') {
                    entry += formula.charAt(ix);
                } else {
                    break;
                }
            }
        }
        return entry;
    }
    
}
