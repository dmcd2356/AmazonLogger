/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import java.time.LocalDate;
import java.util.ArrayList;

/**
 *
 * @author dan
 */
public class AmazonOrder {

    private static final String CLASS_NAME = AmazonOrder.class.getSimpleName();
    
    private String      order_num;      // the Amazon order number
    private LocalDate   trans_date;     // the date of the transaction
    private Integer     total_cost;     // the amount of the transaction in cents (credits are -, debits are +)
    private Integer     gross_cost;     // total cost of all items in order less tax (in cents)
    private Integer     tax_cost;       // the estimated amount of tax (in cents)
    ArrayList<AmazonItem> item;         // one or more items in the order

    public AmazonOrder() {
        this.order_num = null;
        this.trans_date = null;
        this.total_cost = null;
        this.gross_cost = null;
        this.tax_cost = null;

        this.item = new ArrayList<>();
    }
        
    public void setOrderNumber(String order_num) {
        this.order_num = order_num;
    }
        
    public void setOrderDate(LocalDate trans_date) {
        this.trans_date = trans_date;
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
        
    public String getOrderNumber() {
        return this.order_num;
    }
        
    public LocalDate getOrderDate() {
        return this.trans_date;
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
        
    public int getItemCount() {
        return item.size();
    }
        
    public AmazonItem getItem(int index) {
        return item.get(index);
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
        return order_num != null && trans_date != null && !item.isEmpty();
    }
}
