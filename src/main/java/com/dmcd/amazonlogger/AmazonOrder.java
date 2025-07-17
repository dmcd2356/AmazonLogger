/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.dmcd.amazonlogger;

import java.time.LocalDate;
import java.util.ArrayList;

/**
 *
 * @author dan
 */
public class AmazonOrder {

    private static final String CLASS_NAME = AmazonOrder.class.getSimpleName();
    
    private boolean     bOldDate;       // the date is from last year or is already included in spreadsheet
    private String      order_num;      // the Amazon order number
    private LocalDate   trans_date;     // the date of the transaction
    private Integer     total_cost;     // the amount of the transaction in cents (credits are -, debits are +)
    private Integer     gross_cost;     // total cost of all items in order less tax (in cents)
    private Integer     tax_cost;       // the estimated amount of tax (in cents)
    private LocalDate   last_delivery;  // the delivery date of the last item in the order
    private ArrayList<AmazonItem> item;         // one or more items in the order

    public AmazonOrder() {
        this.bOldDate = false;
        this.order_num = null;
        this.trans_date = null;
        this.total_cost = null;
        this.gross_cost = null;
        this.tax_cost = null;
        this.last_delivery = null;

        // create the array of items and instantiate the first one
        this.item = new ArrayList<>();
        AmazonItem newItem = new AmazonItem();
        item.add(newItem);
    }
    
    public void setInvalidDate() {
        this.bOldDate = true;
    }
    
    public void setOrderNumber(String order_num) {
        this.order_num = order_num;
    }
        
    public void setOrderDate(LocalDate trans_date) {
        this.trans_date = trans_date;
    }
        
    public void setDeliveryDate(LocalDate trans_date) {
        this.last_delivery = trans_date;
    }
        
    public void setTotalCost(int total_cost) {
        this.total_cost = total_cost;
    }
        
    public void setGrossCost(int gross_cost) {
        this.gross_cost = gross_cost;
    }
        
    public void setTaxCost(int tax_cost) {
        this.tax_cost = tax_cost;
    }
        
    public AmazonItem addNewItem() {
        AmazonItem newItem = new AmazonItem();
        item.add(newItem);
        return newItem;
    }
        
    public boolean isInvalidDate() {
        return this.bOldDate;
    }
    
    public String getOrderNumber() {
        return this.order_num;
    }
        
    public LocalDate getOrderDate() {
        return this.trans_date;
    }
        
    public LocalDate getDeliveryDate() {
        return this.last_delivery;
    }
        
    public Integer getTotalCost() {
        return this.total_cost;
    }
        
    public Integer getGrossCost() {
        return this.gross_cost;
    }
        
    public Integer getTaxCost() {
        return this.tax_cost;
    }
        
    public boolean isItemEmpty() {
        return item == null || item.isEmpty();
    }
        
    public int getItemCount() {
        return item.size();
    }
        
    public AmazonItem getItem(int index) {
        return item.get(index);
    }
        
    public AmazonItem getLastItem() {
        return item.getLast();
    }
        
    public AmazonItem getItem(String description) {
        for (int ix = 0; ix < item.size(); ix++) {
            AmazonItem amazonItem = item.get(ix);
            if (description.contentEquals(amazonItem.getDescription())) {
                return amazonItem;
            }
        }
        return null;
    }
        
    public boolean isOrderDefined() {
        return (order_num != null && !this.order_num.isEmpty());
    }
        
    public boolean isOrderComplete() {
        if (order_num == null || trans_date == null || item.isEmpty()) {
            return false;
        }
        for (int ix = 0; ix < item.size(); ix++) {
            AmazonItem entry = item.get(ix);
            if (entry.getDescription() == null || entry.getQuantity() == null) {
                return false;
            }
        }
        return true;
    }
}
