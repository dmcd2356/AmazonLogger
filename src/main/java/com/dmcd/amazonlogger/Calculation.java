/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.dmcd.amazonlogger;

import com.dmcd.amazonlogger.GUILogPanel.MsgType;
import java.util.ArrayList;

/**
 *
 * @author dan
 */
public class Calculation {

    private static final String CLASS_NAME = Calculation.class.getSimpleName();
    
    // this holds the array of operands (values/params), operations, and parenthesis
    private static ArrayList<CalcEntry> calcList;
    private static int opCount; // number of operands in calcList
    
    
    public enum EntryType {
        ADD,            // addition
        SUB,            // subtraction
        MUL,            // multiplication
        DIV,            // division
        MOD,            // modulus
        
        NOT,            // bitwise NOT (inversion)
        AND,            // bitwise AND
        OR,             // bitwise OR
        XOR,            // bitwise XOR
        ROR,            // bitwise rotate right (32 bits)
        ROL,            // bitwise rotate left  (32 bits)
        
        Lbracket,       // left  (opening) parenthesis
        Rbracket,       // right (closing) parenthesis
        
        Param,          // parameter reference value
        Value,          // numeric value
    };
    
    /**
     * Extracts the pertinent parts of a formula into an array of CalcEntry objects.
     * 
     * This is only used in the Compilation phase, so we are creating the parameter
     *   entry and verifying the type is valid, but if it is a Variable reference,
     *   don't replace the Variable with its value.
     *   That can only be done during execution phase.
     * 
     * @param formula    - the string containing the calculation formula to extract
     * @param resultType - the data type of the result
     * 
     * @throws ParserException 
     */
    Calculation (String formula, ParameterStruct.ParamType resultType) throws ParserException {
        String functionId = CLASS_NAME + " (new): ";

        if (null == formula || null == resultType) {
            throw new ParserException (functionId + "Calculation type is null");
        } else if (resultType != ParameterStruct.ParamType.Integer  &&
                   resultType != ParameterStruct.ParamType.Unsigned &&
                   resultType != ParameterStruct.ParamType.IntArray) {
            throw new ParserException (functionId + "Calculation not permitted for type: " + resultType.toString());
        }
        
        calcList = new ArrayList<>();
        boolean bNot = false;
        while (!formula.isBlank()) {
            // strip off any leading whitespace
            formula = formula.stripLeading();
            
            // seperate the formula into its components of either operation,
            //  parenthesis, or value
            EntryType type = classifyEntry (formula, resultType);
            switch (type) {
                case EntryType.NOT:
                    bNot = true;
                    formula = formula.substring(1);
                    break;
                case EntryType.Value:
                case EntryType.Param:
                    // if it was a numeric or parameter reference, add it
                    String entry = getNextEntry(formula);
                    calcList.add(new CalcEntry (type, entry, bNot));
                    formula = formula.substring(entry.length());
                    bNot = false;
                    break;
                case EntryType.Lbracket:
                    calcList.add(new CalcEntry (type, null, bNot));
                    formula = formula.substring(1);
                    bNot = false;
                    break;
                default:
                    // else, classify and add the paramthesis or operation
                    if (bNot) {
                        throw new ParserException (functionId + "! character must be followed by either a value, a parameter, or an opening parenthesis: " + formula);
                    }
                    calcList.add(new CalcEntry (type, null, false));
                    int size = 1;
                    switch (type) {
                        case EntryType.AND -> size = 3;
                        case EntryType.OR  -> size = 2;
                        case EntryType.XOR -> size = 3;
                        case EntryType.ROR -> size = 3;
                        case EntryType.ROL -> size = 3;
                        default -> {
                        }
                    }
                    formula = formula.substring(size);
                    break;

            }
        }
        verify();
    }

    /**
     * copies a saved calculation array to this class to be executed.
     * 
     * This is used when executing a saved calculation without changing
     *  the saved value, since running the compute() method will modify
     *  the calculation contents.
     * 
     * @param savedValue - the saved calculation value to process
     */
    Calculation (ArrayList<CalcEntry> savedValue) throws ParserException {
        calcList = new ArrayList<>();
        for (int ix = 0; ix < savedValue.size(); ix++) {
            calcList.add(savedValue.get(ix));
        }
        verify();
    }

    /**
     * returns the number of calculation operand values.
     * 
     * @return the number of calculation operands
     */
    public int getCalcCount() {
        return opCount;
    }
    
    /**
     * returns the calculation operand value if there is only 1 operand.
     * 
     * @return the calculation operand value if no calculation to perform, else returns null
     * 
     * @throws ParserException
     */
    public Long getCalcValue() throws ParserException {
        if (opCount == 1 && calcList.getFirst().getType() == EntryType.Value) {
            return calcList.getFirst().getValue();
        }
        return null;
    }
    
    /**
     * returns the calculation operand value if there is only 1 operand.
     * 
     * @return the calculation operand parameter name if no calculation to perform, else returns null
     * 
     * @throws ParserException
     */
    public String getCalcParam() throws ParserException {
        if (opCount == 1 && calcList.getFirst().getType() == EntryType.Param) {
            return calcList.getFirst().getParam();
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
     * @param type - the data type being calculated
     * 
     * @return the computed value of the full calculation
     * 
     * @throws ParserException
     */    
    public Long compute (ParameterStruct.ParamType type) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        // should repeat this process until only 1 entry remains, and it
        //  should be a Value, which is the final calculation result.
        boolean bBrackets = true;
        while (bBrackets) {
            // find the starting locations of each lowest level of parenthesis blocks.
            // This way the highest priority of operations is the Parenthesis.
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
                    // if the bracketed section was NOTted, invert the result now.
                    if (entry.isInverted()) {
                        Long newValue = ~value & 0xFFFFFFFF;
                        GUILogPanel.outputInfoMsg(MsgType.PROGRAM, "      Calc: ! " + value + " = " + newValue);
                        value = newValue;
                    }
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
        
        // may still have operations to perform, but should not have anymore parenthesis,
        // so perform any remaining operations from left-to-right.
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
        
        Long result = entry.getValue();
        if (type == ParameterStruct.ParamType.Unsigned) {
            result &= 0xFFFFFFFF; // truncate result to 32 bits if unsigned
        }
        GUILogPanel.outputInfoMsg(MsgType.PROGRAM, "      Calc Result: " + result);
        return result;
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
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

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
                    // (if the value was negated, do it here
                    Long opValue = entry.getValue();
                    if (entry.isInverted()) {
                        Long newValue = ~opValue & 0xFFFFFFFF;
                        GUILogPanel.outputInfoMsg(MsgType.DEBUG, "      Calc: ! " + opValue + " = " + newValue);
                        opValue = newValue;
                    }
                    bracket.add(opValue);
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
        // followed by lower and lower priorities.
        ArrayList<EntryType> filter = new ArrayList<>();
        filter.add(EntryType.MUL);
        filter.add(EntryType.DIV);
        filter.add(EntryType.MOD);
        computeOperationType (bracket, ops, filter);
        filter.clear();
        filter.add(EntryType.ADD);
        filter.add(EntryType.SUB);
        computeOperationType (bracket, ops, filter);
        filter.clear();
        filter.add(EntryType.AND);
        filter.add(EntryType.OR);
        filter.add(EntryType.XOR);
        filter.add(EntryType.ROR);
        filter.add(EntryType.ROL);
        computeOperationType (bracket, ops, filter);

        // we should only have a single entry in the bracket array and the ops array
        // should be empty. verify this and if no problems, the result is the last
        // entry in bracket.
        if (!ops.isEmpty() || bracket.size() != 1) {
            throw new ParserException (functionId + "calc did not complete correctly. " + ops.size()
                    + " operations still found and " + bracket.size() + " operands");
        }
        
        GUILogPanel.outputInfoMsg(MsgType.PROGRAM, "    Calc block (" + strDebug + ") converted to: " + bracket.getFirst());
        return bracket.getFirst();
    }

    /**
     * computes a selected list of operations on the bracket in calcLists.
     * The bracket and ops lists are modified to remove the operations performed
     * and get replaced by the resultant value of the operations.
     * 
     * @param bracket - list of operands to perform action on
     * @param ops     - list of operations to perform
     * @param filter  - list of operations that are allowed on this iteration
     * 
     * @throws ParserException
     * 
     */    
    private void computeOperationType (ArrayList<Long> bracket, ArrayList<EntryType> ops, ArrayList<EntryType> filter) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        // first determine if any of the allowed operations is in the formula
        int count = 0;
        for (int ix = 0; ix < filter.size(); ix++) {
            for (int iy = 0; iy < ops.size(); iy++) {
                if (filter.get(ix) == ops.get(iy)) {
                    count++;
                }
            }
        }
        GUILogPanel.outputInfoMsg(MsgType.DEBUG, "    Calc ops for " + filter.toString() + ": " + count);
        if (count == 0) {
            return;
        }
        
        // now perform the allowed operations one at a time in a left-to-right direction,
        //  replacing the 2 operands with the single result & eliminating the operation performed.
        boolean bRanCalc = true;
        while (bRanCalc) {
            bRanCalc = false;
            for (int ix = 0; ix < ops.size(); ix++) {
                EntryType curOp = ops.get(ix);
                Long value = null;
                Long op1 = bracket.get(ix);
                Long op2 = bracket.get(ix+1);
                Integer intVal;
                String strOp = "?";
                switch (curOp) {
                    case EntryType.MUL:
                        strOp = "*";
                        if (filter.contains(curOp)) {
                            value = op1 * op2;
                        }
                        break;
                    case EntryType.DIV:
                        if (op2 == 0) {
                            throw new ParserException (functionId + "Division by 0 in calc: " + op1 + " / " + op2);
                        }
                        strOp = "/";
                        if (filter.contains(curOp)) {
                            value = op1 / op2;
                        }
                        break;
                    case EntryType.MOD:
                        strOp = "%";
                        if (filter.contains(curOp)) {
                            value = op1 % op2;
                        }
                        break;
                    case EntryType.ADD:
                        strOp = "+";
                        if (filter.contains(curOp)) {
                            value = op1 + op2;
                        }
                        break;
                    case EntryType.SUB:
                        strOp = "-";
                        if (filter.contains(curOp)) {
                            value = op1 - op2;
                        }
                        break;
                    case EntryType.AND:
                        strOp = "AND";
                        if (filter.contains(curOp)) {
                            intVal = op1.intValue() & op2.intValue();
                            value = intVal.longValue();
                        }
                        break;
                    case EntryType.OR:
                        strOp = "OR";
                        if (filter.contains(curOp)) {
                            intVal = op1.intValue() | op2.intValue();
                            value = intVal.longValue();
                        }
                        break;
                    case EntryType.XOR:
                        strOp = "XOR";
                        if (filter.contains(curOp)) {
                            intVal = op1.intValue() ^ op2.intValue();
                            value = intVal.longValue();
                        }
                        break;
                    case EntryType.ROR:
                        strOp = "ROR";
                        if (filter.contains(curOp)) {
                            intVal = Integer.rotateRight(op1.intValue(), op2.intValue());
                            value = intVal.longValue();
                        }
                        break;
                    case EntryType.ROL:
                        strOp = "ROL";
                        if (filter.contains(curOp)) {
                            intVal = Integer.rotateLeft(op1.intValue(), op2.intValue());
                            value = intVal.longValue();
                        }
                        break;
                    default:
                        break;
                }

                // if an entry was found, replace the 1st operand with the result
                // and remove the second operand and the operator.
                if (value != null) {
                    GUILogPanel.outputInfoMsg(MsgType.DEBUG, "      Calc: " + op1 + " " + strOp + " " + op2 + " = " + value);
                    bracket.set(ix, value);
                    bracket.remove(ix+1);
                    ops.remove(ix);
                    bRanCalc = true;
                    break;
                }
            }
        }
    }
    
    /**
     * Verifies the format of the Calculation entries.
     * 
     * @throws ParserException 
     */    
    private void verify () throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

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
     * This is only called in the Compilation phase, so we don't have assigned
     * values for variables yet.
     * 
     * @param formula - the value to identify the type of
     * @param ptype   - data type of the calculation result
     * 
     * @return the entry type
     * 
     * @throws ParserException 
     */
    private EntryType classifyEntry (String formula, ParameterStruct.ParamType ptype) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

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
        // we allow bitwise operations as well for unsigned values
        boolean bBitwise = true;
        if      (formula.startsWith("!"))    type = EntryType.NOT;
        else if (formula.startsWith("AND"))  type = EntryType.AND;
        else if (formula.startsWith("OR"))   type = EntryType.OR;
        else if (formula.startsWith("XOR"))  type = EntryType.XOR;
        else if (formula.startsWith("ROR"))  type = EntryType.ROR;
        else if (formula.startsWith("ROL"))  type = EntryType.ROL;
        else {
            bBitwise = false;
            switch (firstch) {
                case '(' -> type = EntryType.Lbracket;
                case ')' -> type = EntryType.Rbracket;
                case '+' -> type = EntryType.ADD;
                case '-' -> type = EntryType.SUB;
                case '*' -> type = EntryType.MUL;
                case '/' -> type = EntryType.DIV;
                case '%' -> type = EntryType.MOD;
                case '$' -> type = EntryType.Param;
                default -> {
                    if (!Character.isDigit(firstch)) {
                        throw new ParserException (functionId + "Invalid character entry for calculation: " + firstch);
                    }
                    type = EntryType.Value;
                }
            }
        }

        // change 
        if (bDigit && type == EntryType.SUB) {
            type = EntryType.Value;
        }

        // now make sure the operation is allowed for the data type
        if (ptype == ParameterStruct.ParamType.Integer && bBitwise) {
            throw new ParserException (functionId + "Invalid operator entry for Integer calculation: " + type.toString());
        }
        
        return type;
    }
    
    private String getNextEntry (String formula) {
        String entry = "";
        char nextch = formula.charAt(0);
        if (nextch == '$') {
            entry += nextch;
            char mode = 'A';
            boolean bExit = false;
            // extracts the Parameter Reference name
            // (this will include any Trait or Bracketed index value attached to it)
            for (int ix = 1; !bExit && ix < formula.length(); ix++) {
                nextch = formula.charAt(ix);
                switch (mode) {
                    case 'A':
                        if (Character.isLetterOrDigit(nextch) || nextch == '_') {
                            entry += formula.charAt(ix);
                        } else if (nextch == '.' || nextch == '[') {
                            entry += formula.charAt(ix);
                            mode = nextch;
                        } else {
                            bExit = true;
                        }
                        break;
                    case '.':
                        if (Character.isUpperCase(nextch)) {
                            entry += formula.charAt(ix);
                        } else {
                            bExit = true;
                        }
                        break;
                    case '[':
                        if (Character.isLetterOrDigit(nextch) ||
                            nextch == '_' || nextch == '$' || nextch == ']') {
                            entry += formula.charAt(ix);
                        } else {
                            bExit = true;
                        }
                        break;
                    default:
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
