/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

/**
 *
 * @author dan
 */
import static com.mycompany.amazonlogger.UIFrame.STATUS_WARN;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

public class TCPServerThread implements Runnable {
    
    private final TCPServerMain tcpServerMain; // Reference to the main server
    private final Socket socket; // Client socket
    private static PrintWriter out_socket;
    private static boolean clientConnected = false;

    public TCPServerThread(Socket socket, TCPServerMain tcpServerMain) {
        this.socket = socket;
        this.tcpServerMain = tcpServerMain;
    }

    @Override
    public void run() {
        // wait for clients to connect
        int clientNumber = tcpServerMain.getClientNumber();
        System.out.println("Client " + clientNumber + " at " + socket.getInetAddress() + " has connected.");
            
        try {
            // Set up input and output streams for client communication
            BufferedReader in_socket = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out_socket = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            
            // Initial message to the client
            sendStatus("CONNECTED");
            clientConnected = true;
            
            // Read and print the client's message
            boolean bExit = false;
            while (! bExit) {
                String message = in_socket.readLine();
                bExit = executeCommand(message);
            }
            
            // Close the connection
            clientConnected = false;
            socket.close();
            System.out.println("Client " + clientNumber + " " + socket.getInetAddress() + " has disconnected.");
       
        } catch (IOException exMsg) {
            exMsg.printStackTrace();
        }
    }
    
    private static void sendMessage (String message) {
        if (out_socket != null) {
            out_socket.println(message);
        }
    }

    public static void sendStatus (String status) {
        if (out_socket != null && clientConnected) {
            sendMessage ("STATUS: " + status);
            
            String message = "";
            switch(status) {
                case "CONNECTED":
                    break;
                case "LOADED":
                    message = "Script loaded";
                    break;
                case "COMPILED":
                    message = "Script compiled";
                    break;
                case "EOF":
                    message = "Script completed";
                    break;
                case "PAUSED":
                    message = "Script paused";
                    break;
                default:
                    break;
            }
            
            if (! message.isEmpty()) {
                System.out.println(message);
            }
        }
    }
    
    public static void sendLogMessage (int counter, String message) {
        if (out_socket != null && clientConnected) {
            String countstr = "00000000" + Integer.toString(counter);
            int countlen = countstr.length();
            countstr = countstr.substring(countlen - 8);
            sendMessage ("LOGMSG: " + countstr + " " + message);
        }
    }
    
    public static void sendAllocations (ArrayList<String> varList) {
        if (out_socket != null && clientConnected) {
            for (int ix = 0; ix < varList.size(); ix++) {
                String entry = varList.get(ix);
                sendMessage ("ALLOC: " + entry);
            }
        }
    }
    
    public static void sendVarInfo (ArrayList<String> varInfo) {
        if (out_socket != null && clientConnected) {
            for (int ix = 0; ix < varInfo.size(); ix++) {
                String entry = varInfo.get(ix);
                sendMessage ("VARMSG: " + entry);
            }
        }
    }
    
    public static void sendLineInfo (int line) {
        if (out_socket != null && clientConnected) {
            String message = Integer.toString(line);
            sendMessage ("LINE: " + message);
        }
    }
    
    private static boolean executeCommand (String command) {
        if (command == null || command.isEmpty()) {
            System.out.println("CLIENT: received empty command");
            return true;
        }
        ArrayList<String> array = new ArrayList<>(Arrays.asList(command.split(" ")));
        if (array.isEmpty()) {
            System.out.println("CLIENT: received empty command");
            return true;
        }
        System.out.println("CLIENT: received command: " + command);
        try {
            switch (array.getFirst()) {
                case "LOAD":
                    if (array.size() < 2) {
                        System.out.println("LOAD command missing argument");
                    }
                    String fname = array.get(1);
                    PreCompile.init();
                    AmazonReader.selectScriptFile(fname);
                    sendStatus("LOADED");
                    break;
                case "COMPILE":
                    AmazonReader.compileScript();
                    sendStatus("COMPILED");
                    break;
                case "RUN":
                    AmazonReader.runScript();
                    sendStatus("EOF");
                    break;
                case "STOP":
                    AmazonReader.stopScript();
                    sendStatus("STOPPED");
                    break;
                case "PAUSE":
                    AmazonReader.pauseScript(true);
                    sendStatus("PAUSED");
                    break;
                case "RESUME":
                    AmazonReader.pauseScript(false);
                    sendStatus("RESUMED");
                    break;
                case "STEP":
                    AmazonReader.runScriptStep();
                    sendStatus("STEPPED");
                    break;
                case "EXIT":
                    System.out.println("User shut down");
                    System.exit(0);
                default:
                    System.out.println("invalid command: " + array.getFirst());
                    return true;
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
