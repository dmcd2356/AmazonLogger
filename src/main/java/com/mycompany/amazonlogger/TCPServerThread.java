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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

public class TCPServerThread implements Runnable {
    
    private final  TCPServerMain tcpServerMain; // Reference to the main server
    private final  Socket        socket; // Client socket
    private static PrintWriter   out_socket = null;
    private static boolean       clientConnected = false;

    public  static final BoundedBuffer buffer = new BoundedBuffer();
    private final ServerSocket    ss = null;
    private final DataInputStream in = null;
    
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
            System.out.println("SENT: CONNECTED");
            clientConnected = true;

            // create the thread that will do the command execution
            Thread scriptThread = new Thread(new ScriptThread());
            scriptThread.start();
            
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

        } catch (IOException | InterruptedException exMsg) {
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
            System.out.println("SENT: STATUS: " + status);
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
            System.out.println("SENT: ALLOC: " + varList.size() + " msgs");
        }
    }
    
    public static void sendVarInfo (String varInfo) {
        if (out_socket != null && clientConnected) {
            sendMessage ("VARMSG: " + varInfo);
            System.out.println("SENT: VARMSG: " + varInfo);
        }
    }

    public static void sendUserOutputInfo (String varInfo) {
        if (out_socket != null && clientConnected) {
            sendMessage ("OUTPUT: " + varInfo);
        }
    }

    public static void sendSubInfo (String varInfo) {
        if (out_socket != null && clientConnected) {
            sendMessage ("SUBSTACK: " + varInfo);
            System.out.println("SENT: SUBSTACK: " + varInfo);
        }
    }

    public static void sendLineInfo (int line) {
        if (out_socket != null && clientConnected) {
            String message = Integer.toString(line);
            sendMessage ("LINE: " + message);
            System.out.println("SENT: LINE: " + message);
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
        boolean bRunThread = false;
        try {
            String key = array.getFirst();
            switch (key) {
                case "LOAD":
                    if (array.size() < 2) {
                        System.out.println("LOAD command missing argument");
                    }
                    String fname = array.get(1);
                    PreCompile.init();
                    AmazonReader.selectScriptFile(fname);
                    break;
                case "COMPILE":
                    ScriptThread.enableRun();
                    AmazonReader.compileScript();
                    command = "RESET"; // follow compile with a RESET
                    bRunThread = true;
                    break;
                case "STOP":
                    ScriptThread.stopScript();
                    bRunThread = true;
                    break;
                case "PAUSE":
                    ScriptThread.pauseScript();
                    bRunThread = true;
                    break;
                case "RUN":
                    ScriptThread.enableRun();
                    bRunThread = true;
                    break;
                case "RESUME":
                    ScriptThread.enableRun();
                    bRunThread = true;
                    break;
                case "STEP":
                    bRunThread = true;
                    break;
                case "RESET":
                    bRunThread = true;
                    break;
                case "BREAKPT":
                    bRunThread = true;
                    break;
                case "DISCONNECT":
                    // this will exit this ServerThread
                    return true;
                case "EXIT":
                    // this will close the program entirely
                    System.out.println("User shut down");
                    clientConnected = false;
                    socket.close();
                    System.exit(0);
                default:
                    System.out.println("invalid command: " + key);
                    sendStatus("UNKNOWN COMMAND: " + command);
                    return false;
            }
            if (bRunThread) {
                // run these in seperate ScriptThread
                buffer.produce(command);
            }
        } catch (ParserException exMsg) {
            AmazonReader.frame.outputInfoMsg(STATUS_WARN, exMsg.getMessage());
            // ignore for now, but may want to send error indication back to client
            return false;
        } catch (IOException | SAXException | TikaException exMsg) {
            exMsg.printStackTrace();
            clientConnected = false;
            try {
                socket.close();
            } catch (IOException ex) {
                // ignore
            }
            System.exit(0);
            //return true;
        }
        return false;
    }
    
}
