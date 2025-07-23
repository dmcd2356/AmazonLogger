/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.dmcd.amazonlogger;

/**
 *
 * @author dan
 */

// this class is the information that is extracted from the charge card PDF file for
// balancing the amounts charged to the account with the Amazon purchases.
public class CardTransaction {
    
    private static final String CLASS_NAME = CardTransaction.class.getSimpleName();
    
    private String  trans_date;     // the date of the transaction
    private String  order_num;      // the Amazon order number
    private Integer amount;         // the amount of the transaction in cents (credits are -, debits are +)
    private String  vendor;         // the vendor name
    // from the spreadsheet
    private String  paid;           // the amount paid for the item
    private String  pending;        // amount pending (payment or refund)
    private String  total;          // the total for the order
    private String  refund;         // the amount refunded
    // statuc 
    private boolean completed;      // true when the item has been found in the spreadsheet
    private boolean amazonEntry;    // true if entry is an Amazon type
        
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
        
    public void modifyAmount (Integer amount) {
        this.amount += amount;
    }
        
    public void setPaid (String amount) {
        this.paid = amount;
    }
        
    public void setPending (String amount) {
        this.pending = amount;
    }
        
    public void setTotal (String amount) {
        this.total = amount;
    }
        
    public void setRefund (String amount) {
        this.refund = amount;
    }
        
    public void setCompleted() {
        this.completed = true;
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
        
    public String getPaid () {
        return this.paid;
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
        
    public boolean isAmazonEntry() {
        return this.amazonEntry;
    }
        
    public boolean isCompleted() {
        return this.completed;
    }
    
}
