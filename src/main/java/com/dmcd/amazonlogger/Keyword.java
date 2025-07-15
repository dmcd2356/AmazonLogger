/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.dmcd.amazonlogger;

import com.dmcd.amazonlogger.GUILogPanel.MsgType;
import java.util.HashMap;

/**
 *
 * @author dan
 */
public class Keyword {
    
    private static final String CLASS_NAME = Keyword.class.getSimpleName();

    // these are the enums for the lines that are of interest to us in the clipboard contents
    public enum KeyTyp { NONE, HELLO_D, HELLO_C,
                         ORDER_PLACED, ORDER_DETAILS, TOTAL_COST, ORDER_NUMBER, 
                         RETURNED, REFUNDED, DELIVERED, ARRIVING, NOW_ARRIVING,
                         GROSS_COST, TAXES, SHIPPING_COST, ITEM_COST, SELLER, SUPPLIER,
                         DESCRIPTION, DESCRIPTION_2, QUANTITY, SHIP_TO, COMPLETE,
                         PACKAGE_LEFT, NOTICE, BUTTON, END_OF_RECORD };


    public class KeywordInfo {
        private KeyTyp  eKeyId;         // keyword id
        private int     keyLength;      // length of keyword
        
        public KeywordInfo (String keyword, KeyTyp id) {
            eKeyId = id;
            keyLength = keyword.length();
        }
        
        public KeywordInfo () {
            eKeyId = KeyTyp.NONE;
            keyLength = 0;
        }
        
        public KeyTyp getKeyType() {
            return eKeyId;
        }
        
        public int getKeyLength() {
            return keyLength;
        }
        
    }

    private static final HashMap <String, KeywordInfo> Keyword_Orders = new HashMap<>();

    Keyword () {
        // these are the items we look for in the Orders and Invoice pages
        putKeywordInfo ("Hello, Dan"                , KeyTyp.HELLO_D);
        putKeywordInfo ("Hello, Connie"             , KeyTyp.HELLO_D);
        putKeywordInfo ("Order Details"             , KeyTyp.ORDER_DETAILS);
        putKeywordInfo ("Order placed"              , KeyTyp.ORDER_PLACED);     // date placed
        putKeywordInfo ("Total"                     , KeyTyp.TOTAL_COST);       // cost of order (with taxes & shipping)
        putKeywordInfo ("Grand Total:"              , KeyTyp.TOTAL_COST);       //  "       " (for INVOICE)
        putKeywordInfo ("Order #"                   , KeyTyp.ORDER_NUMBER);     // order number
        putKeywordInfo ("Returned"                  , KeyTyp.RETURNED);
        putKeywordInfo ("Return started"            , KeyTyp.RETURNED);
        putKeywordInfo ("Return complete"           , KeyTyp.RETURNED);
        putKeywordInfo ("Refunded"                  , KeyTyp.REFUNDED);
        putKeywordInfo ("Item refunded"             , KeyTyp.REFUNDED);
        putKeywordInfo ("Your package was left"     , KeyTyp.PACKAGE_LEFT);
        putKeywordInfo ("Package was left"          , KeyTyp.PACKAGE_LEFT);
        putKeywordInfo ("Delivered"                 , KeyTyp.DELIVERED);        // date delivered
        putKeywordInfo ("Arriving"                  , KeyTyp.ARRIVING);         // date arriving
        putKeywordInfo ("Now arriving"              , KeyTyp.NOW_ARRIVING);     // date arriving
        putKeywordInfo ("Ship to"                   , KeyTyp.SHIP_TO);          // person
        putKeywordInfo ("Grand Total:"              , KeyTyp.TOTAL_COST);       // cost of order (with taxes & shipping)
        putKeywordInfo ("Sold by:"                  , KeyTyp.SELLER);           // seller
        putKeywordInfo ("Supplied by:"              , KeyTyp.SUPPLIER);         // supplier
//        putKeywordInfo ("$"                         , KeyTyp.ITEM_COST);        // cost of item
        putKeywordInfo ("Total before tax:"         , KeyTyp.GROSS_COST);       // cost of order (before taxes & shipping)
        putKeywordInfo ("Estimated tax to be collected:", KeyTyp.TAXES);        // cost of tax
        putKeywordInfo ("Shipping & Handling:"      , KeyTyp.SHIPPING_COST);    // cost of shipping

        // buttons (these are ignored, but helps us to isolate the description
        putKeywordInfo ("Buy it again"              , KeyTyp.BUTTON);
        putKeywordInfo ("View your item"            , KeyTyp.BUTTON);
        putKeywordInfo ("Problem with order"        , KeyTyp.BUTTON);
        putKeywordInfo ("Get product support"       , KeyTyp.BUTTON);
        putKeywordInfo ("Ask Product Question"      , KeyTyp.BUTTON);
        putKeywordInfo ("Leave seller feedback"     , KeyTyp.BUTTON);
        putKeywordInfo ("Write a product review"    , KeyTyp.BUTTON);
        putKeywordInfo ("View order details"        , KeyTyp.BUTTON);
        putKeywordInfo ("View button"               , KeyTyp.BUTTON);
        putKeywordInfo ("Track package"             , KeyTyp.BUTTON);
        
        // these are notices that are also ignored
        putKeywordInfo ("Return window closed"      , KeyTyp.NOTICE);
        putKeywordInfo ("Return or replace item"    , KeyTyp.NOTICE);
        putKeywordInfo ("Return item"               , KeyTyp.NOTICE);
        
        // these are items that will indicate there are no more orders in the page
        putKeywordInfo ("←Previous"                 , KeyTyp.END_OF_RECORD);
        putKeywordInfo ("Add to cart"               , KeyTyp.END_OF_RECORD);
        putKeywordInfo ("Bargain recommendations"   , KeyTyp.END_OF_RECORD);
        putKeywordInfo ("Consider these items"      , KeyTyp.END_OF_RECORD);
        putKeywordInfo ("Next set of slides"        , KeyTyp.END_OF_RECORD);
        putKeywordInfo ("Recommended based on"      , KeyTyp.END_OF_RECORD);
        putKeywordInfo ("Recommended for you"       , KeyTyp.END_OF_RECORD);
        putKeywordInfo ("Sponsored"                 , KeyTyp.END_OF_RECORD);
        putKeywordInfo ("Related to items you've viewedSee more", KeyTyp.END_OF_RECORD);
    }

    private void putKeywordInfo (String keyString, KeyTyp key) {
        Keyword_Orders.put(keyString, new KeywordInfo (keyString, key));
    }
    

    /**
     * finds keyword match to current line.
     * 
     * @param line     - the line read from the web page
     * 
     * @return the structure indicating the type of keyword found, null if not found
     */
    public static KeywordInfo getKeyword (String line) {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        for (HashMap.Entry<String, KeywordInfo> mapEntry : Keyword_Orders.entrySet()) {
            String keyStr = mapEntry.getKey();
            KeywordInfo keyInfo = mapEntry.getValue();
            if (line.startsWith(keyStr)) {
                    GUILogPanel.outputInfoMsg (MsgType.DEBUG, "PARTIAL : KeyTyp." + keyInfo.eKeyId +
                                                    " : Length." + keyInfo.keyLength + " : " + line);
                    return keyInfo;
            }
        }
        String shortLine = line.substring(0, (line.length() > 125 ? 125 : line.length()));
        GUILogPanel.outputInfoMsg (MsgType.DEBUG, functionId + "NO MATCH: "  + shortLine);
        return null;
    }

    /**
     * finds the Keyword entry in the ORDERS table that has the specified Key type
     * 
     * @param keyId - the Key type to find
     * 
     * @return the corresponding Keyword entry, null if not found
     */
    public static KeywordInfo findOrdersKey (KeyTyp keyId) {
        for (HashMap.Entry<String, KeywordInfo> mapEntry : Keyword_Orders.entrySet()) {
            KeywordInfo keyInfo = mapEntry.getValue();
            if (keyInfo.eKeyId == keyId) {
                return keyInfo;
            }
        }
        return null;
    }

}
