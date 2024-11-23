//
//	File:			Reactive.java
//	Author:		    Krzysztof Langner
//	Date:			1997/04/28
//
//********************************************
//  Updated:        2008/03/01
//  By:             Edgar Acosta
//
//********************************************
//  Updated:        2024/09/22
//  By:             Bardia Parmoun
//                  Dylan Leveille
//                  Aron Arany-Takacs
//
//********************************************
package org.example;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

//***************************************************************************
//
//	This is main object class
//
//***************************************************************************
class RoboCupAgent implements SendCommand {
    // Some player types
    public enum PlayerType {ATTACKER, DEFENDER, GOALIE};

    //===========================================================================
    // Initialization member functions

    //---------------------------------------------------------------------------
    // The main appllication function.
    // Command line format:
    //
    // reactive [-parameter value]
    //
    // Parameters:
    //
    //	host (default "localhost")
    //		The host name can either be a machine name, such as "java.sun.com" 
    //		or a string representing its IP address, such as "206.26.48.100."
    //
    //	port (default 6000)
    //		Port number for communication with server
    //
    //	team (default Reactive)
    //		Team name. This name can not contain spaces.
    //
    //	
    public static void main(String[] a) throws IOException {
        String hostName = "";
        int port = 6000;
        String team = "Reactive";
        PlayerType playerType = PlayerType.GOALIE;

        try {
            // First look for parameters
            for (int c = 0; c < a.length; c += 2) {
                if (a[c].compareTo("-host") == 0) {
                    hostName = a[c + 1];
                } else if (a[c].compareTo("-port") == 0) {
                    port = Integer.parseInt(a[c + 1]);
                } else if (a[c].compareTo("-team") == 0) {
                    team = a[c + 1];
                } else if (a[c].compareTo("-playerType") == 0) {
                    playerType = PlayerType.valueOf(a[c + 1].toUpperCase().strip());
                } else {
                    throw new Exception();
                }
            }
        } catch (Exception e) {
            System.err.println();
            System.err.println("USAGE: reactive [-parameter value]");
            System.err.println();
            System.err.println("    Parameters  value        default");
            System.err.println("   ------------------------------------");
            System.err.println("    host        host_name    localhost");
            System.err.println("    port        port_number  6000");
            System.err.println("    team        team_name    Reactive");
            System.err.println();
            System.err.println("    Example:");
            System.err.println("      reactive -host www.host.com -port 6000 -team Poland");
            System.err.println("    or");
            System.err.println("      reactive -host 193.117.005.223");
            return;
        }

        RoboCupAgent player = new RoboCupAgent(InetAddress.getByName(hostName), port, team, playerType);

        // enter main loop
        player.mainLoop();
    }

    //---------------------------------------------------------------------------
    // This constructor opens socket for  connection with server
    public RoboCupAgent(InetAddress host, int port, String team, PlayerType playerType)
            throws SocketException {
        m_socket = new DatagramSocket();
        m_host = host;
        m_port = port;
        m_team = team;
        m_playerType = playerType;
        m_playing = true;
    }


    //===========================================================================
    // Protected member functions

    //---------------------------------------------------------------------------
    // This is main loop for player
    protected void mainLoop() throws IOException {
        byte[] buffer = new byte[MSG_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, MSG_SIZE);

        // first we need to initialize connection with server
        init();

        m_socket.receive(packet);
        parseInitCommand(new String(buffer));
        m_port = packet.getPort();

        // Now we should be connected to the server
        // we know side, player number and play mode
        while (m_playing)
            parseSensorInformation(receive());
        m_socket.close();
    }


    //===========================================================================
    // Implementation of SendCommand Interface

    //---------------------------------------------------------------------------
    // This function sends move command to the server
    public void move(double x, double y) {
        send("(move " + x + " " + y + ")");
    }

    //---------------------------------------------------------------------------
    // This function sends turn command to the server
    public void turn(double moment) {
        send("(turn " + moment + ")");
    }

    public void turn_neck(double moment) {
        send("(turn_neck " + moment + ")");
    }

    //---------------------------------------------------------------------------
    // This function sends dash command to the server
    public void dash(double power) {
        send("(dash " + power + ")");
    }

    //---------------------------------------------------------------------------
    // This function sends kick command to the server
    public void kick(double power, double direction) {
        send("(kick " + power + " " + direction + ")");
    }

    //---------------------------------------------------------------------------
    // This function sends say command to the server
    public void say(String message) {
        send("(say " + message + ")");
    }

    //---------------------------------------------------------------------------
    // This function sends chage_view command to the server
    public void changeView(String angle, String quality) {
        send("(change_view " + angle + " " + quality + ")");
    }

    //---------------------------------------------------------------------------
    // This function sends bye command to the server
    public void bye() {
        m_playing = false;
        send("(bye)");
    }

    //---------------------------------------------------------------------------
    // This function parses initial message from the server
    protected void parseInitCommand(String message)
            throws IOException {
        Matcher m = Pattern.compile("^\\(init\\s(\\w)\\s(\\d{1,2})\\s(\\w+?)\\).*$").matcher(message);
        if (!m.matches()) {
            throw new IOException(message);
        }

        // initialize player's brain
        m_brain = new Brain(this,
                m_team,
                m_playerType,
                m.group(1).charAt(0),
                Integer.parseInt(m.group(2)),
                m.group(3));

        Thread brainThread = new Thread((Runnable) m_brain);
        brainThread.start();
    }


    //===========================================================================
    // Here comes collection of communication function
    //---------------------------------------------------------------------------
    // This function sends initialization command to the server
    private void init() {
        String goaliePlayer = m_playerType.equals(PlayerType.GOALIE) ? " (goalie)" : "";
        send("(init " + m_team + goaliePlayer + " (version 9))");
    }

    //---------------------------------------------------------------------------
    // This function parses sensor information
    private void parseSensorInformation(String message)
            throws IOException {
        // First check kind of information
        Matcher m = message_pattern.matcher(message);
        if (!m.matches()) {
            throw new IOException(message);
        }
        if (m.group(1).compareTo("see") == 0) {
            VisualInfo info = new VisualInfo(message);
            info.parse();
            m_brain.see(info);
        } else if (m.group(1).compareTo("hear") == 0)
            parseHear(message);
    }


    //---------------------------------------------------------------------------
    // This function parses hear information
    private void parseHear(String message)
            throws IOException {
        // get hear information
        Matcher m = hear_pattern.matcher(message);
        int time;
        String sender;
        String uttered;
        if (!m.matches()) {
            throw new IOException(message);
        }
        time = Integer.parseInt(m.group(1));
        sender = m.group(2);
        uttered = m.group(3);
        if (sender.compareTo("referee") == 0)
            m_brain.hear(time, uttered);
            //else if( coach_pattern.matcher(sender).find())
            //    m_brain.hear(time,sender,uttered);
        else if (sender.compareTo("self") != 0)
            m_brain.hear(time, Integer.parseInt(sender), uttered);
    }


    //---------------------------------------------------------------------------
    // This function sends via socket message to the server
    private void send(String message) {
        byte[] buffer = Arrays.copyOf(message.getBytes(), MSG_SIZE);
        try {
            DatagramPacket packet = new DatagramPacket(buffer, MSG_SIZE, m_host, m_port);
            m_socket.send(packet);
        } catch (IOException e) {
            System.err.println("socket sending error " + e);
        }

    }

    //---------------------------------------------------------------------------

    // This function waits for new message from server
    private String receive() {
        byte[] buffer = new byte[MSG_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, MSG_SIZE);
        try {
            m_socket.receive(packet);
        } catch (SocketException e) {
            System.out.println("shutting down...");
        } catch (IOException e) {
            System.err.println("socket receiving error " + e);
        }
        return new String(buffer);
    }


    //===========================================================================
    // Private members
    // class members
    private final DatagramSocket m_socket;        // Socket to communicate with server
    private final InetAddress m_host;            // Server address
    private int m_port;            // server port
    private final String m_team;            // team name
    private final PlayerType m_playerType;            // team name
    private SensorInput m_brain;        // input for sensor information
    private boolean m_playing;              // controls the MainLoop
    private final Pattern message_pattern = Pattern.compile("^\\((\\w+?)\\s.*");
    private final Pattern hear_pattern = Pattern.compile("^\\(hear\\s(\\w+?)\\s(\\w+?)\\s(.*)\\).*");
    //private Pattern coach_pattern = Pattern.compile("coach");
    // constants
    private static final int MSG_SIZE = 4096;    // Size of socket buffer

}
