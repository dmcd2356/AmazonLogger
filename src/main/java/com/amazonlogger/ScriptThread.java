/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.amazonlogger;

import com.amazonlogger.GUILogPanel.MsgType;
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
    
    /*
    *  THESE COMMANDS ARE ONLY EXECUTED WHEN RUNNING FROM THE NETWORK!
    *  MOST OF THESE ARE RUN FROM THIS SCRIPT THREAD, BUT SOME ARE CALLED BY
    *  THE MAIN THREAD, AmazonReader, TO SET FLAGS IN THIS THREAD.
    */
    
    private static final String CLASS_NAME = AmazonReader.class.getSimpleName();
    
    // command index value that represents end of file reached
    public static final int CMD_INDEX_EOF = -1;

    private static int     netCmdIndex = 0; // commandIndex for network runs
    private static ScriptExecute exec = null;
    
    private static boolean pause = false;
    private static boolean stop  = false;
    private static int     breakIndex = CMD_INDEX_EOF;

    //==============================================================
    // THE FOLLOWING FUNCTIONS ARE ONLY EXECUTED FROM AmazonReader thread
    //==============================================================
    
    /**
     * Starts the new thread to run some of the commands that may take awhile to run.
     * This allows the main thread to be responsive to commands from the client.
     * This will loop continuously waiting for commands from AmazonReader until
     *  it receives a command that causes the thread to exit.
     */
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

    /**
     * starts the class that will handle execution of the compiled commands.
     * This is called by AmazonReader thread when the compiler has completed.
     */
    public static void compilerComplete () {
        exec = new ScriptExecute(ScriptCompile.getFilename(), ScriptCompile.getMaxLines());
    }
    
    /**
     * set flag to allow runScriptNetwork() to run.
     * This is called from AmazonReader thread prior to sending request to this thread
     *  when handling RUN and RESUME commands.
     */
    public static void enableRun() {
        pause = false;
    }
    
    /**
     * pause the script.
     * This is called from AmazonReader thread prior to sending request to this thread
     *  when handling PAUSE command.
     */
    public static void pauseScript () {
        pause = true;
        GUILogPanel.outputInfoMsg(MsgType.PROGRAM, "Script begining PAUSE");
    }

    /**
     * stop the script from running.
     * This is called from AmazonReader thread prior to sending request to this thread
     *  when handling STOP command.
     */
    public static void stopScript () {
        stop = true;
        GUILogPanel.outputInfoMsg(MsgType.PROGRAM, "Script begining STOP");
    }

    /**
     * initializes the breakpoint to disabled.
     * This is called from AmazonReader thread when a file is compiled.
     */
    public static void initBreakpoint() {
        breakIndex = ScriptThread.CMD_INDEX_EOF;
    }

    //==============================================================
    // THE FOLLOWING FUNCTIONS ARE ONLY EXECUTED FROM ScriptThread
    //==============================================================
    
    /**
     * This handles the execution of commands that were deferred from AmazonReader.
     * 
     * @param command - the command to execute
     * 
     * @return true to exit the thread.
     * 
     * @throws InterruptedException 
     */
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
                case "STOP":
                    stopComplete();
                    break;
                case "PAUSE":
                    pauseComplete();
                    break;
                case "RUN":
                    runScriptNetwork();
                    break;
                case "RESUME":
                    resumeScript();
                    break;
                case "STEP":
                    runScriptStep();
                    break;
                case "RESET":
                    resetScript();
                    break;
                case "BREAKPT":
                    setBreakpoint(array.get(1));
                    break;
                default:
                    return false;
            }
        } catch (ParserException exMsg) {
            GUILogPanel.outputInfoMsg(MsgType.WARN, exMsg.getMessage());
            TCPServerThread.sendStatus("ERROR: " + array.getFirst() + " " + exMsg.getMessage());
            return false;
        } catch (IOException | SAXException | TikaException exMsg) {
            exMsg.printStackTrace();
            return true;
        }
        return false;
    }

    /**
     * determine if the script has completed.
     * 
     * @return true if it completed
     */
    private static boolean isScriptCompleted() {
        int cmdSize = ScriptCompile.getCompiledSize();
        return (netCmdIndex >= cmdSize || netCmdIndex < 0);
    }
    
    private static void scriptInit () {
        GUILogPanel.reset();        // reset the GUI settings
        FileIO.init();              // reset the File settings
        PreCompile.variables.resetVariables();  // reset all variable values back to default
        LoopStruct.resetStack();    // reset the loop stack
        Subroutine.resetStack();    // reset subroutine stack
        Spreadsheet.init();         // reset spreadsheet params
        OpenDoc.init();             // reset the OpenDoc params
        GUIMain.elapsedTimerDisable();  // stop the timer for the timestamp
        GUILogPanel.outputInfoMsg(MsgType.PROGRAM, "Resetting program index to begining");
        netCmdIndex = 0;            // reset the command pointer to the begining
    }
    
    /**
     * resets the script program counter to 0.
     */
    private static void resetScript() {
        scriptInit();

        // send back the line info of initial execution instruction
        int lineNumber = ScriptCompile.getLineNumber(netCmdIndex);
        TCPServerThread.sendLineInfo (lineNumber);
    }

    /**
     * indicate pause has completed.
     * This is called from the ScriptThread when runScriptNetwork has completed
     */
    private static void pauseComplete () {
        TCPServerThread.sendStatus("PAUSED");
    }

    /**
     * indicate pause has completed.
     * This is called from the ScriptThread when runScriptNetwork has stopped
     */
    private static void stopComplete () {
        TCPServerThread.sendStatus("STOPPED");
    }

    /**
     * sets or disables the breakpoint for executing from the network.
     * 
     * @param value - OFF or the script line number of the breakpoint
     */
    private static void setBreakpoint (String value) {
        if (value.contentEquals("OFF")) {
            breakIndex = CMD_INDEX_EOF;
            GUILogPanel.outputInfoMsg(MsgType.PROGRAM, "Script breakpoint disabled");
        } else {
            int line;
            try {
                line = Integer.parseInt(value);
            } catch (NumberFormatException exMsg) {
                GUILogPanel.outputInfoMsg(MsgType.WARN, "Invalid Script breakpoint value: " + value);
                TCPServerThread.sendStatus("BREAKPT INVALID");
                return;
            }

            // get the corresponding command index value for the line
            breakIndex = ScriptCompile.getCommandIndex(line);
            if (breakIndex == CMD_INDEX_EOF) {
                TCPServerThread.sendStatus("BREAKPT INVALID");
                return;
            }
            GUILogPanel.outputInfoMsg(MsgType.PROGRAM, "Script breakpoint set to line: " + value);
            TCPServerThread.sendStatus("BREAKPT SET");
        }
    }
    
    /**
     * resume the script.
     * 
     * @throws ParserException
     * @throws IOException
     * @throws SAXException
     * @throws TikaException
     */
    private static void resumeScript () throws ParserException, IOException, SAXException, TikaException {
        GUILogPanel.outputInfoMsg(MsgType.PROGRAM, "Script begining RESUME");
        TCPServerThread.sendStatus("RESUMED");

        // reset the variable change flags
        PreCompile.variables.resetUpdate();
        
        runScriptNetwork();
    }

    /**
     * runs the currently compiled script.
     * 
     * @throws ParserException
     * @throws IOException
     * @throws SAXException
     * @throws TikaException 
     */
    private static void runScriptNetwork () throws ParserException, IOException, SAXException, TikaException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        int compileSize = ScriptCompile.getCompiledSize();
        if (compileSize <= 0 || exec == null || netCmdIndex < 0 || netCmdIndex > compileSize) {
            throw new ParserException(functionId + "No script file has been compiled: size = " + compileSize + ", cmdIx = " + netCmdIndex);
        }

        // execute the program by running each 'netCompileList' entry
        if (netCmdIndex == 0) {
            GUILogPanel.outputInfoMsg(MsgType.PROGRAM, "===== BEGINING PROGRAM EXECUTION =====");
            Subroutine.sendSubStackList();
        } else {
            GUILogPanel.outputInfoMsg(MsgType.PROGRAM, "===== RESUMING PROGRAM EXECUTION =====");
        }

        // enable timestamp on log messages
        GUIMain.elapsedTimerEnable();

        try {
            while (netCmdIndex >= 0 && netCmdIndex < compileSize) {
                // execute next command
                CommandStruct command = ScriptCompile.getExecCommand(netCmdIndex);
                netCmdIndex = exec.executeProgramCommand (netCmdIndex, command);
                
                // check for termination causes
                if (netCmdIndex == breakIndex) {
                    TCPServerThread.sendStatus("BREAK");
                    break;
                }
                if (pause) {
                    break;
                }
                if (stop) {
                    stop = false;
                    netCmdIndex = CMD_INDEX_EOF;
                    GUILogPanel.outputInfoMsg(MsgType.PROGRAM, "Script STOPPED");
                    break;
                }
            }
        } catch (ParserException exMsg) {
            Utils.throwAddendum (exMsg.getMessage(), functionId);
        }

        // pause the timer
        GUIMain.elapsedTimerPause();

        // send back the line info of where we stopped
        int lineNumber = ScriptCompile.getLineNumber(netCmdIndex);
        TCPServerThread.sendLineInfo (lineNumber);

        // send back info on any variable changes
        PreCompile.variables.sendVarChange();
        
        // we have completed - if running from network, inform the client and stop the timer
        if (isScriptCompleted()) {
            TCPServerThread.sendStatus("EOF");
            GUIMain.elapsedTimerDisable();
        }
    }
    
    /**
     * runs the currently compiled script.
     * 
     * @throws ParserException
     * @throws IOException
     * @throws SAXException
     * @throws TikaException 
     */
    private static void runScriptStep () throws ParserException, IOException, SAXException, TikaException {
        String functionId = CLASS_NAME + "." + Utils.getCurrentMethodName() + ": ";
        
        int compileSize = ScriptCompile.getCompiledSize();
        if (compileSize <= 0 || exec == null || netCmdIndex < 0 || netCmdIndex > compileSize) {
            throw new ParserException(functionId + "No script file has been compiled");
        }

        if (netCmdIndex == 0) {
            GUILogPanel.outputInfoMsg(MsgType.PROGRAM, "===== BEGINING PROGRAM EXECUTION =====");
            Subroutine.sendSubStackList();
        }

        // reset the variable change flags
        PreCompile.variables.resetUpdate();
        
        // enable timestamp on log messages
        GUIMain.elapsedTimerEnable();
        
        // run command instruction
        try {
            // execute next command
            CommandStruct command = ScriptCompile.getExecCommand(netCmdIndex);
            netCmdIndex = exec.executeProgramCommand (netCmdIndex, command);
        } catch (ParserException exMsg) {
            Utils.throwAddendum (exMsg.getMessage(), functionId);
        }

        // pause the timer
        GUIMain.elapsedTimerPause();

        // send the next line number to the cliend
        int lineNumber = ScriptCompile.getLineNumber(netCmdIndex);
        TCPServerThread.sendLineInfo (lineNumber);

        // send back info on any variable changes
        PreCompile.variables.sendVarChange();
        
        // reset ptr to begining if we reached the end of the script
        if (isScriptCompleted()) {
            TCPServerThread.sendStatus("EOF");
        } else {
            TCPServerThread.sendStatus("STEPPED");
        }
    }

}
