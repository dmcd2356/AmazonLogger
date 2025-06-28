/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

/**
 *
 * @author dan
 */
import static com.mycompany.amazonlogger.AmazonReader.props;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPServerMain {

    // port to use if not found in PropertiesFile
    private static final int SERVER_PORT = 6000;
    
    private int clientNumber = 1;

    public TCPServerMain(Integer port) throws IOException {

        if (port == null) {
            port = props.getPropertiesItem(PropertiesFile.Property.Port, SERVER_PORT);
        }
        System.out.println("Starting TCP server port " + port);
        
        // Create a server socket listening on selected port
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Port " + port + " is open");

        // Loop to continuously listen for new client connections
        while (true) {
            Socket socket = serverSocket.accept(); // Accept a new client connection
            
            // Create a new thread to handle the connected client
            TCPServerThread tcpServerThread = new TCPServerThread(socket, this);
            Thread thread = new Thread(tcpServerThread);
            thread.start();
        }
    }

    // Increment and return the unique client number
    public int getClientNumber() {
        return clientNumber++;
    }

}