/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.amazonlogger;

import static com.mycompany.amazonlogger.AmazonReader.frame;
import static com.mycompany.amazonlogger.UIFrame.STATUS_DEBUG;
import static com.mycompany.amazonlogger.UIFrame.STATUS_PROGRAM;
import java.util.ArrayList;

/**
 *
 * @author dan
 */
public class CommandStruct {
    
    private static final String CLASS_NAME = CommandStruct.class.getSimpleName();
    
    // defines the structure for file commands
    public int          line;       // source code line number for the command
    public CommandTable command;    // the command to execute
    public String       option;     // the command option for command line usage
    public ArrayList<ParameterStruct> params;   // the arguments associated with the command
        
    public static enum CommandTable {
        EXIT,       // this command is added automatically by the compiler
        STARTUP,    // begining of startup commands that run during pre-compile
        ENDSTARTUP, // end of startup commands
        TESTPATH,   // sets the base path to use for basing relative paths from
        LOGFILE,    // specifies the log file location and characteristics
        RUN,        // this command is for running the command-line commands
        PRINT,      // outputs text to console
        DIRECTORY,  // text file access functions
        FEXISTS,    //  "       "       "
        CD,         //  "       "       "
        MKDIR,      //  "       "       "
        RMDIR,      //  "       "       "
        FDELETE,    //  "       "       "
        FCREATE,    //  "       "       "
        FOPEN,      //  "       "       "
        FCLOSE,     //  "       "       "
        FREAD,      //  "       "       "
        FWRITE,     //  "       "       "
        FGETSIZE,   //  "       "       "
        FGETLINES,  //  "       "       "
        OCRSCAN,    // this does an OCR scan of the specified PDF file
        ALLOCATE,   // this allocates the parameters used by the program
        SET,        // this sets the value of the parameters
        IF,         // these handles the conditional IF-ELSEIF-ELSE-ENDIF
        ELSE,       //  "       "       "
        ELSEIF,     //  "       "       "
        ENDIF,      //  "       "       "
        FOR,        // these handle the FOR loop
        BREAK,      //  "       "       "
        BREAKIF,    //  "       "       "
        SKIP,       //  "       "       "
        SKIPIF,     //  "       "       "
        NEXT,       //  "       "       "
        ENDFOR,     // (not a user command, but inserted by compiler)
        ENDMAIN,    // marks end of MAIN program so subroutines can be defined
        SUB,        // defines the start of a subroutine
        ENDSUB,     // defines the end   of a subroutine
        GOSUB,      // calls a subroutine
        RETURN,     // returns from a subroutine
        INSERT,     // these are Array commands only
        APPEND,     //  "       "       "
        MODIFY,     //  "       "       "
        REMOVE,     //  "       "       "
        TRUNCATE,   //  "       "       "
        POP,        //  "       "       "
        CLEAR,      //  "       "       "
        FILTER,     //  "       "       "
        RESET,      // (not a command, but used as argument for FILTER)
    };

    /**
     * this is used for initializing a Program Command entry.
     * The parameters are added later
     * 
     * @param cmd     - the program command enum value to add
     * @param linenum - the script file line number associated with the command
     */
    CommandStruct(CommandTable cmd, int linenum) {
        line    = linenum;
        command = cmd;
        option  = "";
        params  = new ArrayList<>();
    }
        
    /**
     * this is used for initializing a Command Option entry.
     * The parameters are added later
     * 
     * @param cmd     - the option command to add (must start with a '-' char)
     * @param linenum - the script file line number associated with the command
     * 
     * @throws ParserException
     */
    CommandStruct(String cmd, int linenum) throws ParserException {
        String functionId = CLASS_NAME + " (new): ";

        if (!cmd.startsWith("-")) {
            throw new ParserException(functionId + "Invalid command option " + cmd + " on line " + linenum);
        }
        line    = linenum;
        command = CommandTable.RUN;
        option  = cmd;
        params  = new ArrayList<>();
    }
        
    void showCommand (String preface) {
        String strCommand = command.toString();
        if (option != null && !option.isEmpty()) {
            strCommand += " option " + option + ": ";
        }
        frame.outputInfoMsg(STATUS_PROGRAM, preface + strCommand);

        for (int ix = 0; ix < params.size(); ix++) {
            ParameterStruct parStc = params.get(ix);
            frame.outputInfoMsg(STATUS_DEBUG, "        " + parStc.showParam(ix));
        }
    }

    /**
     * checks if a string is one of the reserved command values
     * 
     * @param strValue - the string to check
     * 
     * @return corresponding enum value
     */
    public static CommandTable isValidCommand (String strValue) {
        for(CommandTable entry : CommandTable.values()){
            if( entry.toString().equals(strValue)){
                return entry;
            }
        }
        return null;
    }
    
}
