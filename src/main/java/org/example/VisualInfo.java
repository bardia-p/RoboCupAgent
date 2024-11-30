//
//	File:			VisualInfo.java
//	Author:		    Krzysztof Langner
//	Date:			1997/04/28

//  Modified by:    Paul Marlow, Amir Ghavam, Yoga Selvaraj
//  Course:         Software Agents
//  Date Due:       November 30, 2000

//  Modified by:    Tarek Hassan
//  Date:           June 15, 2001

//  Modified by:    Paul Marlow
//  Date:		    February 22, 2004

//  Modified by:    Edgar Acosta
//  Date:		    March 5, 2008
package org.example;

import java.io.*;
import java.util.*;
import java.util.regex.*;

class VisualInfo {
    private int m_time;
    public Vector<ObjectInfo> m_objects;
    public String m_message;

    // Split objects into specific lists
    private final Vector<?> m_ball_list;
    private final Vector<?> m_player_list;
    private final Vector<?> m_flag_list;
    private final Vector<?> m_goal_list;
    private final Vector<?> m_line_list;

    // Constructor for 'see' information
    public VisualInfo(String info) {
        m_message = info.trim();
        m_player_list = new Vector<>(22);
        m_ball_list = new Vector<>(1);
        m_goal_list = new Vector<>(10);
        m_line_list = new Vector<>(20);
        m_flag_list = new Vector<>(60);
        m_objects = new Vector<>(113);
    }

    public Vector<?> getBallList() {
        return m_ball_list;
    }

    public Vector<?> getPlayerList() {
        return m_player_list;
    }

    public Vector<?> getGoalList() {
        return m_goal_list;
    }

    public Vector<?> getLineList() {
        return m_line_list;
    }

    public Vector<?> getFlagList() {
        return m_flag_list;
    }

    public int getTime() {
        return m_time;
    }

    //---------------------------------------------------------------------------
    // This function parses visual information from the server
    public void parse()
            throws IOException {
        String m_type;
        String m_objectsString;
        ObjectInfo objInfo;

        m_player_list.clear();
        m_ball_list.clear();
        m_goal_list.clear();
        m_line_list.clear();
        m_flag_list.clear();
        m_objects.clear();
        //Parse all the message, and obtain the three main parts
        //(message type, time, and Object Info)
        Pattern pattern = Pattern.compile("^\\((\\w+?)\\s(\\d+?)\\s(.*)\\).*");
        Matcher matcher = pattern.matcher(m_message);
        if (!matcher.matches()) {
            throw new IOException(m_message);
        }
        m_type = matcher.group(1);
        m_time = Integer.parseInt(matcher.group(2));
        m_objectsString = matcher.group(3);
        //Don't parse information if it's not 'see' information
        if (m_type.compareTo("see") != 0)
            return;
        // Now parse the Object Info to obtain the Object Name (to be
        // parsed with createNewObject, and other info about the object).
        Pattern Objects_p = Pattern.compile("\\(\\((.*?)\\)\\s(.*?)\\)");
        Matcher Objects_m = Objects_p.matcher(m_objectsString);
        // For each match, create the object, and append info
        while (Objects_m.find()) {
            objInfo = createNewObject(Objects_m.group(1));
            //if(objInfo.valid())
            m_objects.addElement(objInfo);
            // this splits the string containing the other info about
            // the object (distance, direction, etc.)
            String[] relPos = m_info_p.split(Objects_m.group(2));
            // append the info depending on the number of additional attributes.
            int len = relPos.length;
            switch (len) {
                case 6:
                    ((PlayerInfo) (objInfo)).m_headDir = Float.parseFloat(relPos[5]);
                case 5:
                    ((PlayerInfo) (objInfo)).m_bodyDir = Float.parseFloat(relPos[4]);
                case 4:
                    objInfo.m_dirChange = Float.parseFloat(relPos[3]);
                case 3:
                    objInfo.m_distChange = Float.parseFloat(relPos[2]);
                case 2:
                    objInfo.m_distance = Float.parseFloat(relPos[0]);
                    objInfo.m_direction = Float.parseFloat(relPos[1]);
                    break;
                default:
                    objInfo.m_direction = Float.parseFloat(relPos[0]);
                    break;
            }
        }
    }

    //===========================================================================
    // Private implementations

    //---------------------------------------------------------------------------
    // This function creates new object based on the see message sent from the
    // server
    private ObjectInfo createNewObject(String m_nameString) {
        ObjectInfo objInfo = null;

        //this splits the elements of the object name
        String[] objectName = m_info_p.split(m_nameString);

        int len = objectName.length;
        String n = objectName[0];

        //Player
        if (p_player.matcher(n).matches()) {
            int uniformNumber = 0;
            boolean goalie = false;
            switch (len) {
                case 4:
                    goalie = (objectName[3].compareTo("goalie") == 0); //if it is a goalie
                case 3:
                    uniformNumber = Integer.parseInt(objectName[2]); //if the player number is available
                case 2:
                    String team = p_quote.matcher(objectName[1]).replaceAll(""); //Team Name (remove quotation marks)
                    objInfo = new PlayerInfo(team, uniformNumber, goalie);
                    break;
                default:
                    objInfo = new PlayerInfo();
                    break;
            }
        } //Ball
        else if (p_ball.matcher(n).matches())
            objInfo = new BallInfo();
            //Goal
        else if (p_goal.matcher(n).matches()) {
            if (len == 2)
                objInfo = new GoalInfo(objectName[1].charAt(0)); //if there is side info
            else
                objInfo = new GoalInfo();
        } //Line
        else if (p_line.matcher(n).matches()) {
            if (len == 2)
                objInfo = new LineInfo(objectName[1].charAt(0)); //if we know which line it is
            else
                objInfo = new LineInfo();
        } //Flag
        else if (p_flag.matcher(n).matches()) {
            char type = ' '; // p|g
            char pos1 = ' '; // l|r|t|b|c
            char pos2 = ' '; // t|b|l|r|c
            int num = 0;     // 0|10|20|30|40|50
            boolean out = true;
            if (len == 1)
                objInfo = new FlagInfo();
            else {
                if (p_type.matcher(objectName[1]).matches()) {
                    type = objectName[1].charAt(0);
                    out = false;
                    switch (len) {
                        case 4 -> {
                            pos2 = objectName[3].charAt(0);
                            pos1 = objectName[2].charAt(0);
                        }
                        case 3 -> { //Is this possible?
                            if (p_lr.matcher(objectName[2]).matches())
                                pos1 = objectName[2].charAt(0);
                            else
                                pos2 = objectName[2].charAt(0);
                        }
                    }
                } else if (objectName[len - 1].compareTo("0") == 0) {
                    if (len == 3) //Is another option possible?
                        pos1 = objectName[1].charAt(0);
                } else if (p_number.matcher(objectName[len - 1]).matches()) {
                    num = Integer.parseInt(objectName[len - 1]);
                    switch (len) {
                        case 4 -> {
                            pos2 = objectName[2].charAt(0);
                            pos1 = objectName[1].charAt(0);
                        }
                        case 3 -> { //Is this possible?
                            if (p_lr.matcher(objectName[1]).matches())
                                pos1 = objectName[1].charAt(0);
                            else
                                pos2 = objectName[1].charAt(0);
                        }
                    }
                } else {
                    out = false;
                    switch (len) {
                        case 3 -> {
                            pos2 = objectName[2].charAt(0);
                            pos1 = objectName[1].charAt(0);
                        }
                        case 2 -> { // I don't think t|b occurs, but better safe than sorry
                            if (p_lrc.matcher(objectName[1]).matches())
                                pos1 = objectName[1].charAt(0);
                            else
                                pos2 = objectName[1].charAt(0);
                        }
                    }
                }
                String flagType = "flag";
//                System.out.println("type " + type);
//                System.out.println("pos1 " + pos1);
//                System.out.println("pos2 " + pos2);
//                System.out.println("num " + num);
                if (type != ' ') flagType = flagType + " " + type;
                if (pos1 != ' ') flagType = flagType + " " + pos1;
                if (pos2 != ' ') flagType = flagType + " " + pos2;
                flagType += " " + num;
                // Implementing flags like this, allows one to specifically find a
                // particular flag (i.e. "flag c", or "flag p l t")
                objInfo = new FlagInfo(flagType, type, pos1, pos2, num, out);
            }
        }
        return objInfo;
    }

    //===========================================================================
    // Private members
    private static final Pattern m_info_p = Pattern.compile("\\s");
    private static final int p_flags = Pattern.CASE_INSENSITIVE;
    private static final Pattern p_player = Pattern.compile("^(player|p)$", p_flags);
    private static final Pattern p_ball = Pattern.compile("^(ball|b)$", p_flags);
    private static final Pattern p_goal = Pattern.compile("^(goal|g)$", p_flags);
    private static final Pattern p_flag = Pattern.compile("^(flag|f)$", p_flags);
    private static final Pattern p_line = Pattern.compile("^(line|l)$", p_flags);
    private static final Pattern p_quote = Pattern.compile("\"");
    private static final Pattern p_type = Pattern.compile("^(pg)$");
    private static final Pattern p_number = Pattern.compile("^\\d{2}$");
    private static final Pattern p_lr = Pattern.compile("^([lr])$");
    private static final Pattern p_lrc = Pattern.compile("^([lrc])$");
}

