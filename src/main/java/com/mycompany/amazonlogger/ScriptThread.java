/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.UIFrame.STATUS_WARN;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

/**
 *
 * @author dan
 */
public class ScriptThread implements Runnable {
    
    public void run() {
        System.out.println("ScriptThread started...");
        while (true) {
            try {
                String msg = TCPServerThread.buffer.consume();
                System.out.println("ScriptThread received message: " + msg);
                executeCommand (msg);
            } catch (InterruptedException ex) {
                //
            }
        }
    }
    
    private boolean executeCommand (String command) throws InterruptedException {
        if (command == null || command.isEmpty()) {
            //System.out.println("CLIENT: received empty command");
            return false;
        }
        ArrayList<String> array = new ArrayList<>(Arrays.asList(command.split(" ")));
        if (array.isEmpty()) {
            System.out.println("CLIENT: received empty command");
            return false;
        }
        System.out.println("CLIENT: received command: " + command);
        try {
            switch (array.getFirst()) {
//                case "LOAD":
//                    if (array.size() < 2) {
//                        System.out.println("LOAD command missing argument");
//                    }
//                    String fname = array.get(1);
//                    PreCompile.init();
//                    AmazonReader.selectScriptFile(fname);
//                    sendStatus("LOADED");
//                    break;
//                case "COMPILE":
//                    AmazonReader.compileScript();
//                    sendStatus("COMPILED");
//                    break;
                case "RUN":
                    AmazonReader.runScriptNetwork();
                    break;
                case "STOP":
                    AmazonReader.stopScript();
                    break;
                case "PAUSE":
                    AmazonReader.pauseScript(true);
                    break;
                case "RESUME":
                    AmazonReader.pauseScript(false);
                    break;
                case "STEP":
                    AmazonReader.runScriptStep();
                    break;
                case "RESET":
                    AmazonReader.resetScript();
                    break;
                case "BREAKPT":
                    AmazonReader.setBreakpoint(array.get(1));
                    break;
//                case "DISCONNECT":
//                    return true;
//                case "EXIT":
//                    System.out.println("User shut down");
//                    clientConnected = false;
//                    socket.close();
//                    System.exit(0);
                default:
//                    System.out.println("invalid command: " + array.getFirst());
//                    sendStatus("UNKNOWN COMMAND: " + command);
                    return false;
            }
        } catch (ParserException exMsg) {
            AmazonReader.frame.outputInfoMsg(STATUS_WARN, exMsg.getMessage());
            // ignore for now, but may want to send error indication back to client
            return false;
        } catch (IOException | SAXException | TikaException exMsg) {
            exMsg.printStackTrace();
            return true;
        }
        return false;
    }
}
