/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.UIFrame.STATUS_DEBUG;
import static com.mycompany.amazonlogger.UIFrame.STATUS_PROGRAM;
import java.util.Objects;

/**
 *
 * @author dan
 */
public class Comparison {

    private static final String CLASS_NAME = Comparison.class.getSimpleName();

    private static boolean bStatus = false;

    
    private static final int INTEGER = 0x01;
    private static final int BOOLEAN = 0x02;
    private static final int STRING  = 0x04;
    private static final int IARRAY  = 0x08;
    private static final int SARRAY  = 0x10;

    Comparison (ParameterStruct value1, ParameterStruct value2, String compSign) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + " (S): ";

        // check what value types we have to compare to determine if it is valid and what to compare.
        int types1 = getValidTypes(value1);
        int types2 = getValidTypes(value2);
        frame.outputInfoMsg(STATUS_DEBUG, "    Compare types: param1 = " + types1 + ", param2 = " + types2);
        if (types1 == 0) {
            throw new ParserException(functionId + "No valid values in argument 1 of Comparison");
        }
        if (types2 == 0) {
            throw new ParserException(functionId + "No valid values in argument 2 of Comparison");
        }
        
        // first check for array types, since they should not be compared to non-array type
        int matchType = types1 & types2;
        if ((matchType & (IARRAY | SARRAY)) != 0) {
            int size1, size2;
            if ((types1 & IARRAY) != 0)
                size1 = value1.getIntArraySize();
            else
                size1 = value1.getStrArraySize();
            if ((types2 & IARRAY) != 0)
                size2 = value2.getIntArraySize();
            else
                size2 = value2.getStrArraySize();
            bStatus = Utils.compareParameterValues (size1, size2, compSign);
            frame.outputInfoMsg(STATUS_PROGRAM, "    Array Size Compare: " + size1 + " " + compSign + " " + size2 + " => " + bStatus);
            return;
        }
        
        // next check if no exact match in data types and if we can do conversion
        if (matchType == 0) {
            // if neither type is String, we can't convert
            if ((types1 & STRING) == 0 && (types2 & STRING) == 0) {
                throw new ParserException(functionId + "Unable to compare data types " + value1.getParamType() + " to " + value2.getParamType());
            }
            // we simply convert one value to a string and compare strings
            String strval1 = value1.getStringValue();
            String strval2 = value2.getStringValue();
            if ((types1 & STRING) != 0) {
                if ((types2 & INTEGER) != 0)
                    strval2 = value2.getIntegerValue().toString();
                else if ((types2 & BOOLEAN) != 0)
                    strval2 = value2.getBooleanValue().toString();
                else
                    throw new ParserException(functionId + "Invalid type for Compare param 2: " + types2);
            } else {
                if ((types1 & INTEGER) != 0)
                    strval1 = value1.getIntegerValue().toString();
                else if ((types2 & BOOLEAN) != 0)
                    strval1 = value1.getBooleanValue().toString();
                else
                    throw new ParserException(functionId + "Invalid type for Compare param 1: " + types2);
            }

            bStatus = Utils.compareParameterValues (strval1, strval2, compSign);
            frame.outputInfoMsg(STATUS_PROGRAM, "    String Compare: " + strval1 + " " + compSign + " " + strval2 + " => " + bStatus);
            return;
        }
        
        if ((matchType & BOOLEAN) != 0) {
            if (! compSign.contentEquals("==") && ! compSign.contentEquals("!=")) {
                throw new ParserException(functionId + "Attempting to compare Booleans with > or <");
            }
            boolean boo1 = value1.getBooleanValue();
            boolean boo2 = value1.getBooleanValue();
            boolean bComp = Objects.equals(boo1, boo2);
            bStatus = compSign.contentEquals("==") ? bComp : ! bComp;
            frame.outputInfoMsg(STATUS_PROGRAM, "    Boolean Compare: " + boo1 + " " + compSign + " " + boo2 + " => " + bStatus);
            return;
        }
        
        // check if we are doing a String comparison
        if ((matchType & STRING) != 0) {
            String strval1 = value1.getStringValue();
            String strval2 = value2.getStringValue();
            bStatus = Utils.compareParameterValues (strval1, strval2, compSign);
            frame.outputInfoMsg(STATUS_PROGRAM, "    String Compare: " + strval1 + " " + compSign + " " + strval2 + " => " + bStatus);
            return;
        }
        
        // otherwise, it is a numeric comparison
        Long calc1, calc2;
        if (value1.isCalculation()) {
            calc1 = value1.getCalculationValue(ParameterStruct.ParamType.Integer);
        } else {
            calc1 = value1.getIntegerValue();
        }
        if (value2.isCalculation()) {
            calc2 = value2.getCalculationValue(ParameterStruct.ParamType.Integer);
        } else {
            calc2 = value2.getIntegerValue();
        }
        bStatus = Utils.compareParameterValues (calc1, calc2, compSign);
        frame.outputInfoMsg(STATUS_PROGRAM, "    Numeric Compare: " + calc1 + " " + compSign + " " + calc2 + " => " + bStatus);
    }
    
    public boolean getStatus() {
        return bStatus;
    }

    private static int getValidTypes (ParameterStruct value) {
        int types = 0;
        if (value.getIntegerValue() != null)    types |= INTEGER;
        if (value.getBooleanValue() != null)    types |= BOOLEAN;
        if (value.getStringValue()  != null)    types |= STRING;
        if (value.getIntArray()     != null)    types |= IARRAY;
        if (value.getStrArray()     != null)    types |= SARRAY;
        return types;
    }
    
}
