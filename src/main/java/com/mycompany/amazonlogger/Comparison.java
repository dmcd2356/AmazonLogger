/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.UIFrame.STATUS_PROGRAM;

/**
 *
 * @author dan
 */
public class Comparison {

    private static final String CLASS_NAME = Comparison.class.getSimpleName();

    private static boolean bStatus = false;

    Comparison (ParameterStruct value1, ParameterStruct value2, String compSign) throws ParserException {

        // check if we are doing a String comparison
        if (value1.getParamType() == ParameterStruct.ParamType.String) {
            String strval1 = value1.getStringValue();
            String strval2 = value2.getStringValue();
            bStatus = Utils.compareParameterValues (strval1, strval2, compSign);
            frame.outputInfoMsg(STATUS_PROGRAM, "    Compare: " + strval1 + " " + compSign + " " + strval2 + " = " + bStatus);
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
        frame.outputInfoMsg(STATUS_PROGRAM, "    Compare: " + calc1 + " " + compSign + " " + calc2 + " = " + bStatus);
    }
    
    public boolean getStatus() {
        return bStatus;
    }
    
}
