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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.*;

import jason.architecture.AgArch;
import jason.asSemantics.Agent;
import jason.asSemantics.TransitionSystem;
import jason.asSyntax.Literal;
import jason.asSemantics.ActionExec;
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
    private final Logger logger;

    // Constants
    public static final int KICK_POWER = 100;
    public static final double DASH_COEFFICIENT = 10.0;
    public static final double PASS_COEFFICIENT = 25.0;
    public static final int SMALL_BROWSE_ANGLE = 40;
    public static final int LARGE_BROWSE_ANGLE = 90;
    public static final double KICKABLE_BALL_DISTANCE = 1.0;
    public static final String ATTACKER_FILE = "resources/attacker.asl";
    public static final String DEFENDER_FILE = "resources/defender.asl";
    public static final String GOALIE_FILE = "resources/goalie.asl";
    public static final String LOGGING_FILE = "resources/logging.properties";
    public static final Double FIELD_LENGTH = 105.0;
    public static final Double FIELD_WIDTH = 68.0;

    // These determine what fraction of the half field is used to measure the home zone.
    public static final Double HOME_ZONE_FRACTION = 1.0 / 3.0;
    public static final Double OPP_ZONE_FRACTION = 7.0 / 16.0;


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

        // set up the Jason agent
        try {
            Agent ag = new Agent();
            new TransitionSystem(ag, null, null, this);
            ag.initAg();

            switch(m_playerType){
                case GOALIE -> ag.loadInitialAS(GOALIE_FILE);
                case DEFENDER -> ag.loadInitialAS(DEFENDER_FILE);
                case ATTACKER -> ag.loadInitialAS(ATTACKER_FILE);
            }

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
    // Always know where the goal is.
    // Move to a place on my side on a kick_off
    // ************************************************

    public void run() {
        // first put it somewhere on my side
        if (Pattern.matches("^before_kick_off.*", m_playMode)) {
            switch(m_playerType) {
                case GOALIE -> m_agent.move(-FIELD_LENGTH / 2, 0);
                case DEFENDER -> m_agent.move(-Math.random() * FIELD_LENGTH / 2, FIELD_WIDTH/4 - Math.random() * FIELD_WIDTH / 2);
                case ATTACKER -> m_agent.move(-Math.random() * FIELD_LENGTH / 4, FIELD_WIDTH/2 - Math.random() * FIELD_WIDTH);
            }
        }

        try {
            while (isRunning()) {
                // calls the Jason engine to perform one reasoning cycle
                getTS().getLogger().info("Reasoning....");
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
    // Here are supporting functions for implement logic
    // this method just add some perception for the agent
    @Override
    public List<Literal> perceive() {
        getTS().getLogger().info("Agent " + getAgName() + " is perceiving..." );
        List<Literal> l = new ArrayList<Literal>();
        BallInfo ball = getBall();
        GoalInfo opp_goal = getOpponentGoal();
        GoalInfo own_goal = getOwnGoal();
        PlayerInfo teammate = getNearestTeammate();
        int playerZone = getPlayerZone();

        switch(playerZone) {
            case 0 -> l.add(Literal.parseLiteral("in_home_zone"));
            case 1 -> l.add(Literal.parseLiteral("~in_home_zone"));
        }
        getTS().getLogger().info("MY ZONE IS: " + playerZone);


        if (ball != null) {
            l.add(Literal.parseLiteral("ball_in_view"));
            if (ball.m_direction == 0) {
                l.add(Literal.parseLiteral("aligned_with_ball"));
            }

            if (ball.m_distance <= KICKABLE_BALL_DISTANCE) {
                l.add(Literal.parseLiteral("ball_close"));
            }
        }

        if (opp_goal != null) {
            l.add(Literal.parseLiteral("opp_goal_in_view"));
        }

        if (own_goal != null) {
            l.add(Literal.parseLiteral("own_goal_in_view"));
        }

        if (teammate != null) {
            l.add(Literal.parseLiteral("teammate_in_view"));
        }

        getTS().getLogger().info("Perceptions: " + l);

        return l;
    }

    // this method get the agent actions
    @Override
    public void act(ActionExec action) {
        getTS().getLogger().info("Agent " + getAgName() + " is doing: " + action.getActionTerm());

        String actionToDo = action.getActionTerm().toString();
        BallInfo ball = getBall();
        GoalInfo opp_goal = getOpponentGoal();
        GoalInfo own_goal = getOwnGoal();
        PlayerInfo teammate = getNearestTeammate();

        if (ball == null && (actionToDo.equals("align_ball_act") ||
                actionToDo.equals("run_to_ball_act") ||
                actionToDo.equals("kick_to_opp_goal_act") ||
                actionToDo.equals("pass_to_teammate_act"))){
            getTS().getLogger().info("Could not perform ball related action! Missing ball!");
            return;
        }

        if (opp_goal == null && (actionToDo.equals("kick_to_opp_goal_act"))){
            getTS().getLogger().info("Could not perform opp goal related action! Missing opp goal!");
            return;
        }

        if (own_goal == null && (actionToDo.equals("run_towards_goal_act"))) {
            getTS().getLogger().info("Could not perform own goal related action! Missing own goal!");
            return;
        }

        if (teammate == null && (actionToDo.equals("pass_to_teammate_act"))){
            getTS().getLogger().info("Could not perform teammate related action! Missing teammate!");
            return;
        }

        switch ( actionToDo ) {
            case "find_ball_act", "find_teammate_act" -> m_agent.turn(SMALL_BROWSE_ANGLE);
            case "find_own_goal_act" -> m_agent.turn(LARGE_BROWSE_ANGLE);
            case "align_ball_act" -> m_agent.turn(ball.m_direction);
            case "run_to_ball_act" -> m_agent.dash(DASH_COEFFICIENT * ball.m_distance);
            case "run_towards_own_goal_act" -> m_agent.dash(DASH_COEFFICIENT * own_goal.m_distance);
            case "kick_to_opp_goal_act" -> m_agent.kick(KICK_POWER, opp_goal.m_direction);
            case "pass_to_teammate_act" -> m_agent.kick(PASS_COEFFICIENT * teammate.m_distance, teammate.m_direction);
            default -> getTS().getLogger().warning("INVALID ACTION");
        }

        getTS().getLogger().info("Agent " + getAgName() + " finished doing: " + action.getActionTerm());

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
        return m_team + "-" + m_number + "-" + m_playerType;
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

    /**
     * Returns the opponent's goal if present.
     * @return opponent's goal info, if not null
     */
    private GoalInfo getOpponentGoal(){
        char opponent_side = (m_side == 'l') ? 'r' : 'l';
        return (GoalInfo) m_memory.getObject("goal " + opponent_side);
    }

    /**
     * Returns our goal if present.
     * @return opponent's goal info, if not null
     */
    private GoalInfo getOwnGoal(){
        return (GoalInfo) m_memory.getObject("goal " + m_side);
    }

    /**
     * Returns the nearest teammate to the player if any.
     * NOTE*: This function will not return the goalie!
     * @return the nearest teammate to the player.
     */
    private PlayerInfo getNearestTeammate() {
        Optional<PlayerInfo> player = m_memory.getAll("player").stream()
                .map(p -> (PlayerInfo) p)
                .filter(p -> p.m_teamName.equals(m_team) && !p.m_goalie)
                .min(Comparator.comparingDouble(p -> p.m_distance));

        return player.isPresent() ? player.get() : null;
    }

    /**
     * Returns the player's zone using the flags:
     * 0: in home zone
     * 1: in opposition zone
     * 2: unknown
     *
     * @return true if the player is in home zone.
     */
    private int getPlayerZone() {
        // First check the flags near the end
        FlagInfo top_home = (FlagInfo) m_memory.getObject("f " + m_side + " t");
        GoalInfo goal_home = (GoalInfo) m_memory.getObject("goal " + m_side);
        FlagInfo bottom_home = (FlagInfo) m_memory.getObject("f " + m_side + " b");

        // Check the opposition goal
        char opp_side = (m_side == 'l') ? 'r' : 'l';
        FlagInfo top_opp = (FlagInfo) m_memory.getObject("f " + opp_side + " t");
        GoalInfo goal_opp = (GoalInfo) m_memory.getObject("goal " + opp_side);
        FlagInfo bottom_opp = (FlagInfo) m_memory.getObject("f " + opp_side + " b");

        // If you can see home, and you are close to it!
        if ((top_home != null && top_home.m_distance <= FIELD_LENGTH * HOME_ZONE_FRACTION) ||
                (goal_home != null && goal_home.m_distance <= FIELD_LENGTH * HOME_ZONE_FRACTION) ||
                (bottom_home != null && bottom_home.m_distance <= FIELD_LENGTH * HOME_ZONE_FRACTION)) {
            return 0;
        }

        // If you can see opposition, and you are close to it!
        if ((top_opp != null && top_opp.m_distance <= FIELD_LENGTH * OPP_ZONE_FRACTION) ||
                (goal_opp != null && goal_opp.m_distance <= FIELD_LENGTH * OPP_ZONE_FRACTION) ||
                (bottom_opp != null && bottom_opp.m_distance <= FIELD_LENGTH * OPP_ZONE_FRACTION)) {
            return 1;
        }

        return 2;
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