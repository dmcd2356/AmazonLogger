/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.dmcd.amazonlogger;

import java.util.LinkedList;
import java.util.Queue;

/**
 *
 * @author dan
 */
public class BoundedBuffer {
    
    private final Queue<String> buffer = new LinkedList<>();
    private final int BUFFER_SIZE = 2;
    
    public BoundedBuffer () {
    }
    
    // Produce a value
    public synchronized void produce (String msg) throws InterruptedException {
        while (buffer.size() == BUFFER_SIZE) {
            wait();  // Wait if buffer is full
        }
        
        buffer.add(msg);
        System.out.println("Produced: " + msg);
        notifyAll();  // Notify consumers that a new item is available
    }
    
    // Consume a value
    public synchronized String consume() throws InterruptedException {
        while (buffer.isEmpty()) {
            wait();  // Wait if buffer is empty
        }
        String msg = (String) buffer.poll();
        System.out.println("Consumed: " + msg);
        notifyAll();  // Notify producers that a slot is available
        return msg;
    }
    
}
