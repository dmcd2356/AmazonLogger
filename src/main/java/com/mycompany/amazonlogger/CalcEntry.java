/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

/**
 *
 * @author dan
 */
public class CalcEntry {
        private Calculation.EntryType type;
        private Long    value;
        private String  paramName;

        CalcEntry (Calculation.EntryType type, String entry) throws ParserException {
            this.type = type;
            if (null == type) {
                throw new ParserException ("CalcEntry (new): null component");
            }
            switch (type) {
                case Value:
                    this.value = ParameterStruct.getLongOrUnsignedValue(entry);
                    this.paramName = null;
                    break;
                case Param:
                    this.value = null;
                    this.paramName = entry;
                    break;
                default:
                    this.value = null;
                    this.paramName = null;
                    break;
            }
        }
        
        CalcEntry (Long value) throws ParserException {
            this.type = Calculation.EntryType.Value;
            this.value = value;
            this.paramName = null;
        }
        
        public Calculation.EntryType getType() {
            return this.type;
        }

        public Long getValue() throws ParserException {
            // if entry is parameter, do the parameter conversion to Integer value first
            if (this.type == Calculation.EntryType.Param) {
                if (this.paramName == null) {
                    throw new ParserException ("CalcEntry.getValue: Parameter name not found (null)");
                }
                ParameterStruct param = new ParameterStruct(this.paramName, 'I');
                param.updateFromReference();
                this.value = param.getIntegerValue();
            }
            if (this.value == null) {
                throw new ParserException ("CalcEntry.getValue: Integer value not found (null)");
            }
            return this.value;
        }
    
}
