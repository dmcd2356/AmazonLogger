/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.dmcd.amazonlogger;

import java.time.LocalDate;

/**
 *
 * @author dan
 */
public class AmazonItem {
    
    private static final String CLASS_NAME = AmazonItem.class.getSimpleName();

    // this class is the information extracted from the Amazon web page for filling in
    // the spreadsheet columns 0 - 5 that relate to the ordering of items.
    private String      description;    // the item description for the purchase
    private Integer     quantity;       // the quantity of the item ordered
    private LocalDate   delivery_date;  // the date the item is expectedor was delivered
    private Integer     item_cost;      // the cost of the individual item
    private String      seller;         // name of the seller of the item
    private boolean     returned;       // true if the item was returned (or is in process)
    
    public AmazonItem() {
        description = null;
        quantity = null;
        delivery_date = null;
        item_cost = null;
        seller = null;
        returned = false;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
        
    public void setDeliveryDate(LocalDate delivery_date) {
        this.delivery_date = delivery_date;
    }
        
    public void setDescription(String description) {
        this.description = description;
    }
        
    public void setItemCost(int item_cost) {
        this.item_cost = item_cost;
    }
        
    public void setSeller(String seller) {
        this.seller = seller;
    }
        
    public void setReturned() {
        this.returned = true;
    }

    public Integer getQuantity() {
        return this.quantity;
    }
        
    public LocalDate getDeliveryDate() {
        return this.delivery_date;
    }
        
    public String getDescription() {
        return this.description;
    }
        
    public Integer getItemCost() {
        return this.item_cost;
    }
        
    public String getSeller() {
        return this.seller;
    }
        
    public boolean getReturned() {
        return this.returned;
    }
        
    public boolean isItemDefined() {
        return (this.description != null && !this.description.isEmpty());
    }

}
