//
//	File:			Brain.java
//	Author:		Krzysztof Langner
//	Date:			1997/04/28
//
//    Modified by:	Paul Marlow

//    Modified by:      Edgar Acosta
//    Date:             March 4, 2008

package org.example;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.*;

import jason.architecture.AgArch;
import jason.asSemantics.Agent;
import jason.asSemantics.TransitionSystem;
import jason.asSyntax.Literal;
import jason.asSemantics.ActionExec;
import jason.bb.BeliefBase;
import jason.infra.local.RunLocalMAS;

class Brain extends AgArch implements Runnable, SensorInput {

    //===========================================================================
    // Private members
    private final SendCommand m_agent;          // robot which is controlled by this brain
    private final Memory m_memory;                // place where all information is stored
    private final String m_team;
    private final RoboCupAgent.PlayerType m_playerType;
    private final char m_side;
    private final int m_number;
    private volatile boolean m_timeOver;
    private final String m_playMode;

    private boolean actionPerformed;
    private Logger logger;

    public static String AGENT_FILE = "";
    public final static String LOGGING_FILE = "resources/logging.properties";
    //---------------------------------------------------------------------------
    // This constructor:
    // - stores connection to the agent
    // - starts thread for this object
    public Brain(SendCommand agent,
                 String team,
                 RoboCupAgent.PlayerType playerType,
                 char side,
                 int number,
                 String playMode) {
        m_timeOver = false;
        m_agent = agent;
        m_memory = new Memory();
        m_team = team;
        m_playerType = playerType;
        m_side = side;
        m_number = number;
        m_playMode = playMode;
        actionPerformed = false;

        new RunLocalMAS().setupLogger(LOGGING_FILE);

        logger = Logger.getLogger(m_team + m_number);

        if ( m_playerType.equals(RoboCupAgent.PlayerType.GOALIE) )
        {
            AGENT_FILE = "resources/goalie.asl";
        }
        else
        {
            AGENT_FILE = "resources/brain.asl";
        }

        // set up the Jason agent
        try {
            Agent ag = new Agent();
            new TransitionSystem(ag, null, null, this);
            ag.initAg();
            ag.loadInitialAS(AGENT_FILE);
            getAgName();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not setup the agent!", e);
        }

        getTS().getLogger().info("IM HERE");
    }


    //---------------------------------------------------------------------------
    // This is main brain function used to make decision
    // In each cycle we decide which command to issue based on
    // current situation. the rules are:
    //
    //	1. If you don't know where is ball then turn right and wait for new info
    //
    //	2. If ball is too far to kick it then
    //		2.1. If we are directed towards the ball then go to the ball
    //		2.2. else turn to the ball
    //
    //	3. If we don't know where is opponent goal then turn wait
    //				and wait for new info
    //
    //	4. Kick ball
    //
    //	To ensure that we don't send commands to often after each cycle
    //	we wait one simulator steps. (This of course should be done better)

    // ***************  Improvements ******************
    // Allways know where the goal is.
    // Move to a place on my side on a kick_off
    // ************************************************

    public void run() {
        ObjectInfo object;

        // first put it somewhere on my side

        if (Pattern.matches("^before_kick_off.*", m_playMode)) {
            if ( m_playerType.equals(RoboCupAgent.PlayerType.GOALIE) ) {
                m_agent.move(-50, 0);
            }
            else
            {
                m_agent.move(-Math.random() * 52.5, 34 - Math.random() * 68.0);
            }
        }

        try {
            while (isRunning()) {
                // calls the Jason engine to perform one reasoning cycle
                logger.fine("Reasoning....");

//                try {
//                    Thread.sleep(2000);
//                } catch (Exception e) {
//                    System.out.println(e);
//                }

//                BeliefBase bb = getTS().getAg().getBB();
//
//                System.out.println("Agent Beliefs:");
//                // Iterate through all beliefs and print them
//                for (Literal belief : bb) {
//                    System.out.println(belief.toString());
//                }

                getTS().reasoningCycle();

                if (canSleep()) {
                    getTS().getLogger().info("Agent sleep" );
                    actionPerformed = false;
                    sleep();
                }
                else {
                    getTS().getLogger().info("Agent cannot sleep" );
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Run error", e);
        }

        m_agent.bye();
    }


    //===========================================================================
    // Here are suporting functions for implement logic
    // this method just add some perception for the agent
    @Override
    public List<Literal> perceive() {
        getTS().getLogger().info("Agent " + getAgName() + " is perceiving..." );
        List<Literal> l = new ArrayList<Literal>();
        l.add(Literal.parseLiteral("~ball_in_view"));
        l.add(Literal.parseLiteral("~in_ball_direction"));
        l.add(Literal.parseLiteral("~ball_close"));
        l.add(Literal.parseLiteral("~ball_kickable"));
        l.add(Literal.parseLiteral("~ball_angle_too_right"));
        l.add(Literal.parseLiteral("~ball_angle_too_left"));

        l.add(Literal.parseLiteral("~right_goal_in_view"));
        l.add(Literal.parseLiteral("~left_goal_in_view"));

        l.add(Literal.parseLiteral("~within_right_goal"));
        l.add(Literal.parseLiteral("~within_left_goal"));


        BallInfo ball = getBall();
        ObjectInfo rightFlag;
        ObjectInfo leftFlag;
        ObjectInfo criticalRightFlag;
        ObjectInfo criticalLeftFlag;

        if (m_side == 'r')
        {
            rightFlag = getFlag("t r 50");
            leftFlag = getFlag("b r 50");
            criticalRightFlag = getFlag("c t 0");
            criticalLeftFlag = getFlag("c b 0");
        }
        else
        {
            rightFlag = getFlag("b l 50");
            leftFlag = getFlag("t l 50");
            criticalRightFlag = getFlag("c b 0");
            criticalLeftFlag = getFlag("c t 0");
        }

        if ( null != ball )
        {
            l.remove(Literal.parseLiteral("~ball_in_view"));
            l.add(Literal.parseLiteral("ball_in_view"));
            if ( ball.m_direction == 0 )
            {
                getTS().getLogger().info("Agent in ball direction" );
                l.remove(Literal.parseLiteral("~in_ball_direction"));
                l.add(Literal.parseLiteral("in_ball_direction"));
            }

            if ( ball.getDistance() <=  10 )
            {
                l.remove(Literal.parseLiteral("~ball_close"));
                l.add(Literal.parseLiteral("ball_close"));
            }

            if ( ball.getDistance() <=  1.0)
            {
                l.remove(Literal.parseLiteral("~ball_kickable"));
                l.add(Literal.parseLiteral("ball_kickable"));
            }

            if (criticalRightFlag != null )
            {
                if ((criticalRightFlag.getDirection() - 20) < ball.getDirection())
                {
                    getTS().getLogger().info("ball angle bad" );
                    l.remove(Literal.parseLiteral("~ball_angle_too_right"));
                    l.add(Literal.parseLiteral("ball_angle_too_right"));
                }
            }

            if (criticalLeftFlag != null )
            {
                if ( (criticalLeftFlag.getDirection()+20) > ball.getDirection())
                {
                    getTS().getLogger().info("ball angle bad" );
                    l.remove(Literal.parseLiteral("~ball_angle_too_left"));
                    l.add(Literal.parseLiteral("ball_angle_too_left"));
                }
            }

        }

        if ( rightFlag != null )
        {
            l.remove(Literal.parseLiteral("~right_goal_in_view"));
            l.add(Literal.parseLiteral("right_goal_in_view"));
            getTS().getLogger().info("right flag in view" );
            if ( rightFlag.getDistance() < 35 )
            {
                getTS().getLogger().info("within right flag" );
                l.remove(Literal.parseLiteral("~within_right_goal"));
                l.add(Literal.parseLiteral("within_right_goal"));
            }
        }

        if ( leftFlag != null )
        {
            l.remove(Literal.parseLiteral("~left_goal_in_view"));
            l.add(Literal.parseLiteral("left_goal_in_view"));
            getTS().getLogger().info("left flag in view" );
            if ( leftFlag.getDistance() < 35 )
            {
                getTS().getLogger().info("within left flag" );
                l.remove(Literal.parseLiteral("~within_left_goal"));
                l.add(Literal.parseLiteral("within_left_goal"));
            }
        }


        return l;
    }

    // this method get the agent actions
    @Override
    public void act(ActionExec action) {
        getTS().getLogger().info("Agent " + getAgName() + " is doing: " + action.getActionTerm());

        String actionToDo = action.getActionTerm().toString();
        BallInfo ball = getBall();
        ObjectInfo rightFlag;
        ObjectInfo leftFlag;
        ObjectInfo criticalRightFlag;
//        ObjectInfo criticalLeftFlag;

        if (m_side == 'r')
        {
            rightFlag = getFlag("t r 50");
            leftFlag = getFlag("b r 50");
        }
        else
        {
            rightFlag = getFlag("b l 50");
            leftFlag = getFlag("t l 50");
        }


        switch ( actionToDo )
        {
            case "find_ball_act":
                m_agent.turn(40);
                break;
            case "find_right_goal_act":
                m_agent.turn(40);
                break;
            case "find_left_goal_act":
                m_agent.turn(-40);
                break;
            case "turn_to_ball_act":
                m_agent.turn(ball.getDirection());
                break;
            case "turn_to_right_goal_act":
                m_agent.turn(rightFlag.getDirection());
                break;
            case "turn_to_left_goal_act":
                m_agent.turn(leftFlag.getDirection());
                break;
            case "wait_act":
                m_agent.turn(0);
                break;
            case "dash_to_ball_act":
                m_agent.dash(10*ball.getDistance());
                break;
            case "dash_to_right_goal_act":
                m_agent.dash(5*rightFlag.getDistance());
                break;
            case "dash_to_left_goal_act":
                m_agent.dash(5*leftFlag.getDistance());
                break;
            case "kick_random_act":
                m_agent.kick(100, 90);
                break;
            default:
        }

        // set that the execution was ok
        actionPerformed = true;
        action.setResult(true);
        actionExecuted(action);
    }

    @Override
    public boolean canSleep() {
        return actionPerformed;
    }

    @Override
    public boolean isRunning() {
        return !m_timeOver;
    }

    @Override
    public String getAgName() {
        return m_team + m_number; //Jason parser hates # character
    }

    // a very simple implementation of sleep
    public void sleep() {
        // sleep one step to ensure that we will not send
        // two commands in one cycle.
        try {
            Thread.sleep(2 * SoccerParams.simulator_step);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Run error", e);
            throw new RuntimeException(e);
        }
    }

    //===========================================================================
    // Implementation of SensorInput Interface
    /**
     * Returns the ball if present.
     * @return ball info, if not null
     */
    private BallInfo getBall(){
        return (BallInfo) m_memory.getObject("ball");
    }

    private ObjectInfo getFlag(String flagName)     {

//        ArrayList<ObjectInfo> flags = m_memory.getAll("flag " + flagName);
//        for (ObjectInfo flag : flags)
//        {
//            FlagInfo casted = (FlagInfo) flag;
//            getTS().getLogger().info("Flag " + casted.m_type + casted.m_pos1 + casted.m_pos2);
//            if ( casted.m_type == flagName.charAt(0) &&
//                 casted.m_pos1 == flagName.charAt(1) &&
//                 casted.m_pos2 == flagName.charAt(2)  )
//            {
//                return casted;
//            }
//        }
//        return null;
        return m_memory.getObject("flag " + flagName);
    }


    /**
     * Returns the opponent's goal if present.
     * @return opponent's goal info, if not null
     */
    private GoalInfo getOpponentGoal(){
        char opponent_side = (m_side == 'l') ? 'r' : 'l';
        return (GoalInfo) m_memory.getObject("goal " + opponent_side);
    }

    //---------------------------------------------------------------------------
    // This function sends see information
    public void see(VisualInfo info) {
        m_memory.store(info);
    }


    //---------------------------------------------------------------------------
    // This function receives hear information from player
    public void hear(int time, int direction, String message) {
    }

    //---------------------------------------------------------------------------
    // This function receives hear information from referee
    public void hear(int time, String message) {
        if (message.compareTo("time_over") == 0)
            m_timeOver = true;
    }
}
