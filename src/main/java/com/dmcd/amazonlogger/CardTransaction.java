/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.dmcd.amazonlogger;

import java.util.ArrayList;

/**
 *
 * @author dan
 */

// this class is the information that is extracted from the charge card PDF file for
// balancing the amounts charged to the account with the Amazon purchases.
public class CardTransaction {
    
    private static final String CLASS_NAME = CardTransaction.class.getSimpleName();
    
    // these are extracted from the PDF file
    private final String  trans_date;   // the date of the transaction
    private       String  order_num;    // the Amazon order number
    private       Integer amount;       // the amount of the transaction in cents (credits are -, debits are +)
    private final String  vendor;       // the vendor name
    // these are extracted from the spreadsheet
    private String  tab_name;           // name of the spreadsheet tab
    private Integer row;                // the spreadsheet row containing the entry
    private Integer items;              // number of items in the order
    private String  payment;            // the amount paid for the item
    private String  pending;            // amount pending (payment or refund)
    private String  total;              // the total for the order
    private String  refund;             // the amount refunded
    // status values
    private       boolean completed;    // true when the item has been found in the spreadsheet
    private final boolean amazonEntry;  // true if entry is an Amazon type
        
    CardTransaction (String strDate, String strVendor, Integer amount) {
        this.completed  = false;
        this.trans_date = strDate;
        this.vendor     = strVendor;
        this.amount     = amount;

        // now let's check for Amazon receipts only
        if (this.vendor.contentEquals("AMAZON MKT") ||
            this.vendor.contentEquals("AMZN Mktp ") ||
            this.vendor.contentEquals("Amazon.com") ) {
            this.amazonEntry = true;
        } else {
            this.amazonEntry = false;
        }
    }
        
    public void setOrderNumber (String orderNum) {
        this.order_num = orderNum;
    }

    // this is called when the listed entry has been balanced in the spreadsheet
    public void setCompleted() {
        this.completed = true;
    }
        
    public void modifyAmount (Integer amount) {
        this.amount += amount;
    }
    
    public void setSpreadsheetEntries(String tabName, int row, ArrayList<String> rowInfo, ArrayList<Spreadsheet.Column> col) throws ParserException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";

        if (rowInfo.size() != col.size()) {
            throw new ParserException(functionId + "row " + row + " data has incorrect number of entries: " + rowInfo.size() + ", col size = " + col.size());
        }
        this.tab_name = tabName;
        this.row = row;
        for (int ix = 0; ix < col.size(); ix++) {
            Spreadsheet.Column colName = col.get(ix);
            String rowValue = rowInfo.get(ix);
            switch (colName) {
                case ItemIndex:
                    Integer count = 0;
                    if (rowValue == null || rowValue.isEmpty()) {
                        count = 1;
                    } else if (rowValue.startsWith("1 of ")) {
                        rowValue = rowValue.substring(5);
                        count  = Integer.valueOf(rowValue);
                    }
                    this.items = count;
                    break;
                case Total:
                    this.total = rowValue;
                    break;
                case Payment:
                    this.payment = rowValue;
                    break;
                case Pending:
                    this.pending = rowValue;
                    break;
                case Refund:
                    this.refund = rowValue;
                    break;
                default:
                    throw new ParserException(functionId + "row " + row + " has invalid column selection: " + colName);
            }
        }
    }

    public String getTabName() {
        return this.tab_name;
    }
        
    public String getOrderNumber() {
        return this.order_num;
    }
        
    public String getTransDate() {
        return this.trans_date;
    }
        
    public String getVendor() {
        return this.vendor;
    }
        
    public Integer getAmount() {
        return this.amount;
    }

    public Integer getItemCount() {
        return this.items;
    }

    public String getPayment () {
        return this.payment;
    }
        
    public String getPending () {
        return this.pending;
    }
        
    public String getTotal () {
        return this.total;
    }
        
    public String getRefund () {
        return this.refund;
    }
    
    public Integer getIntPayment () throws ParserException {
        String value = this.payment;
        return (value == null || value.isEmpty()) ? 0 : Utils.getAmountValue(value);
    }
        
    public Integer getIntPending () throws ParserException {
        String value = this.pending;
        return (value == null || value.isEmpty()) ? 0 : Utils.getAmountValue(value);
    }
        
    public Integer getIntTotal () throws ParserException {
        String value = this.total;
        return (value == null || value.isEmpty()) ? 0 : Utils.getAmountValue(value);
    }
        
    public Integer getIntRefund () throws ParserException {
        String value = this.refund;
        return (value == null || value.isEmpty()) ? 0 : Utils.getAmountValue(value);
    }
    
    public Integer getRow() {
        return this.row;
    }
    
    public boolean isAmazonEntry() {
        return this.amazonEntry;
    }
        
    public boolean isCompleted() {
        return this.completed;
    }
    
}
