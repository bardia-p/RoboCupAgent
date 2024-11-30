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
    private final Logger logger;

    // Constants
    public static final int KICK_POWER = 100;
    public static final double DASH_COEFFICIENT = 10.0;
    public static final double PASS_COEFFICIENT = 25.0;
    public static final int SMALL_BROWSE_ANGLE = 40;
    public static final int LARGE_BROWSE_ANGLE = 90;
    public static final double KICKABLE_BALL_DISTANCE = 1.0;
    public static final double IN_GOAL_DISTANCE = 1.0;
    public static final double GOALIE_BALL_VERY_RIGHT_ANGLE = -20;
    public static final double GOALIE_BALL_VERY_LEFT_ANGLE = 20;
    public static final double GOALIE_BALL_CLOSE_TO_CENTRE_ANGLE = 20;
    public static final double GOALIE_BALL_CATCHABLE_ANGLE = 7.5;
    public static final double GOALIE_BALL_CATCHABLE_DISTANCE = 10;
    public static final double GOALIE_DISTANCE_VERY_EDGE_OF_GOAL = 36;
    public static final double GOALIE_DISTANCE_FROM_CENTRE = 51;


    public static final String ATTACKER_FILE = "resources/attacker.asl";
    public static final String DEFENDER_FILE = "resources/defender.asl";
    public static final String GOALIE_FILE = "resources/goalie.asl";
    public static final String LOGGING_FILE = "resources/logging.properties";
    public static final Double FIELD_LENGTH = 105.0;
    public static final Double FIELD_WIDTH = 68.0;
    public static final Double MIN_PASS_DISTANCE = 2.0;

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
                case GOALIE -> m_agent.move(-( FIELD_LENGTH - 5 ) / 2, 0);
                case DEFENDER -> m_agent.move(-Math.random() * FIELD_LENGTH / 2, FIELD_WIDTH/4 - Math.random() * FIELD_WIDTH / 2);
                case ATTACKER -> m_agent.move(-Math.random() * FIELD_LENGTH / 4, FIELD_WIDTH/2 - Math.random() * FIELD_WIDTH);
            }
        }

        try {
            while (isRunning()) {
                // calls the Jason engine to perform one reasoning cycle
                getTS().getLogger().info("Reasoning....");

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

                if ( canSleep() ) {
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
        List<Literal> l = new ArrayList<>();

        BallInfo ball = getBall();
        GoalInfo oppGoal = getOpponentGoal();
        GoalInfo ownGoal = getOwnGoal();
        PlayerInfo teammate = getFurthestTeammate();
        ObjectInfo centreOfMap = getFlag("c 0");
        int playerZone = getPlayerZone();

        ObjectInfo flagRightToGoal;
        ObjectInfo flagLeftToGoal;
        ObjectInfo criticalRightGoalFlag;
        ObjectInfo criticalLeftGoalFlag;

        switch(playerZone) {
            case 0 -> l.add(Literal.parseLiteral("in_home_zone"));
            case 1 -> l.add(Literal.parseLiteral("~in_home_zone"));
        }
        // getTS().getLogger().info("MY ZONE IS: " + playerZone);

        // Goalie: Retrieve specific flag perceptions
        if (m_side == 'r')
        {
            flagRightToGoal = getFlag("t r 50");
            flagLeftToGoal = getFlag("b r 50");
            criticalRightGoalFlag = getFlag("c t 0");
            criticalLeftGoalFlag = getFlag("c b 0");
        }
        else
        {
            flagRightToGoal = getFlag("b l 50");
            flagLeftToGoal = getFlag("t l 50");
            criticalRightGoalFlag = getFlag("c b 0");
            criticalLeftGoalFlag = getFlag("c t 0");
        }

        // Retrieve general ball perceptions
        if (ball != null) {
            l.add(Literal.parseLiteral("ball_in_view"));
            if (ball.m_direction == 0) {
                l.add(Literal.parseLiteral("aligned_with_ball"));
            }

            if (ball.m_distance <= KICKABLE_BALL_DISTANCE) {
                l.add(Literal.parseLiteral("ball_kickable"));
            }

            // Goalie: Conditions to catch the ball
            if ( Math.abs(ball.getDirection()) < GOALIE_BALL_CATCHABLE_ANGLE  )
            {
                l.add(Literal.parseLiteral("ball_angle_catchable"));
            }

            if ( ball.getDistance() <=  GOALIE_BALL_CATCHABLE_DISTANCE )
            {
                l.add(Literal.parseLiteral("ball_distance_catchable"));
            }

            // Goalie: check if ball is within an angle that requires position change
            // We only want to preceive one such critical angles
            boolean criticalFlagFound = false;

            if ( criticalRightGoalFlag != null )
            {
                if ( criticalRightGoalFlag.getDirection() < GOALIE_BALL_VERY_RIGHT_ANGLE )
                {
                    criticalFlagFound = true;
                    l.add(Literal.parseLiteral("ball_to_goalie_angle_very_right"));
                }
            }

            if ( criticalLeftGoalFlag != null && !criticalFlagFound )
            {
                if ( criticalLeftGoalFlag.getDirection() > GOALIE_BALL_VERY_LEFT_ANGLE )
                {
                    criticalFlagFound = true;
                    l.add(Literal.parseLiteral("ball_to_goalie_angle_very_left"));
                }
            }

            if ( centreOfMap != null && !criticalFlagFound )
            {
                if ( Math.abs(centreOfMap.getDirection()) < GOALIE_BALL_CLOSE_TO_CENTRE_ANGLE )
                {
                    l.add(Literal.parseLiteral("ball_to_goalie_angle_centre"));
                }
            }
        }

        // Goalie: Specific positioning of goalie based on certain flags
        if ( flagRightToGoal != null )
        {
            l.add(Literal.parseLiteral("flag_right_to_goal_in_view"));
            if ( flagRightToGoal.getDistance() < GOALIE_DISTANCE_VERY_EDGE_OF_GOAL )
            {
                l.add(Literal.parseLiteral("within_very_right_of_goal"));
            }
        }

        if ( flagLeftToGoal != null )
        {
            l.add(Literal.parseLiteral("flag_left_to_goal_in_view"));
            if ( flagLeftToGoal.getDistance() < GOALIE_DISTANCE_VERY_EDGE_OF_GOAL )
            {
                l.add(Literal.parseLiteral("within_very_left_of_goal"));
            }
        }

        if ( oppGoal != null ) {
            l.add(Literal.parseLiteral("opp_goal_in_view"));
        }

        if ( ownGoal != null ) {
            l.add(Literal.parseLiteral("own_goal_in_view"));

            if ( ownGoal.getDistance() < IN_GOAL_DISTANCE )
            {
                l.add(Literal.parseLiteral("inside_own_goal"));
            }
        }

        if ( centreOfMap != null )
        {
            l.add(Literal.parseLiteral("centre_visible"));
            if ( centreOfMap.getDistance() < GOALIE_DISTANCE_FROM_CENTRE )
            {
                l.add(Literal.parseLiteral("goalie_distance_from_centre"));
            }
        }

        if ( teammate != null ) {
            l.add(Literal.parseLiteral("teammate_in_view"));
        }

        // getTS().getLogger().info("Perceptions: " + l);

        return l;
    }

    // this method get the agent actions
    @Override
    public void act(ActionExec action) {
        getTS().getLogger().info("Agent " + getAgName() + " is doing: " + action.getActionTerm());

        String actionToDo = action.getActionTerm().toString();
        BallInfo ball = getBall();
        GoalInfo oppGoal = getOpponentGoal();
        GoalInfo ownGoal = getOwnGoal();
        PlayerInfo teammate = getFurthestTeammate();
        ObjectInfo centreOfMap = getFlag("c 0");

        ObjectInfo flagRightToGoal;
        ObjectInfo flagLeftToGoal;

        // Goalie: Retrieve specific flag perceptions
        if (m_side == 'r')
        {
            flagRightToGoal = getFlag("t r 50");
            flagLeftToGoal = getFlag("b r 50");
        }
        else
        {
            flagRightToGoal = getFlag("b l 50");
            flagLeftToGoal = getFlag("t l 50");
        }

        if (ball == null && (actionToDo.equals("align_ball_act") ||
                actionToDo.equals("run_to_ball_act") ||
                actionToDo.equals("kick_to_opp_goal_act") ||
                actionToDo.equals("pass_to_teammate_act") ||
                actionToDo.equals("catch_ball_act") )){
            getTS().getLogger().info("Could not perform ball related action! Missing ball!");
            return;
        }

        if (oppGoal == null && (actionToDo.equals("kick_to_opp_goal_act"))){
            getTS().getLogger().info("Could not perform opp goal related action! Missing opp goal!");
            return;
        }

        if (ownGoal == null && (actionToDo.equals("run_towards_own_goal_act") ||
                actionToDo.equals("align_own_goal_act"))) {
            getTS().getLogger().info("Could not perform own goal related action! Missing own goal!");
            return;
        }

        if (teammate == null && (actionToDo.equals("pass_to_teammate_act"))) {
            getTS().getLogger().info("Could not perform teammate related action! Missing teammate!");
            return;
        }

        if (flagRightToGoal == null && (actionToDo.equals("align_right_goal_act"))) {
            getTS().getLogger().info("Could not perform right goal flag related action! Missing right goal flag!");
            return;
        }

        if (flagLeftToGoal == null && (actionToDo.equals("align_left_goal_act"))) {
            getTS().getLogger().info("Could not perform left goal flag related action! Missing left goal flag!");
            return;
        }

        if (centreOfMap == null && (actionToDo.equals("align_centre_act"))) {
            getTS().getLogger().info("Could not perform left goal flag related action! Missing left goal flag!");
            return;
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
            case "find_own_goal_act":
                m_agent.turn(60);
                break;
            case "find_centre_act":
                m_agent.turn(80);
                break;
            case "align_ball_act":
                m_agent.turn(ball.getDirection());
                break;
            case "align_right_goal_act":
                m_agent.turn(flagRightToGoal.getDirection());
                break;
            case "align_left_goal_act":
                m_agent.turn(flagLeftToGoal.getDirection());
                break;
            case "align_own_goal_act":
                m_agent.turn(ownGoal.getDirection());
                break;
            case "align_centre_act":
                m_agent.turn(centreOfMap.getDirection());
                break;
            case "wait_act":
                m_agent.turn(0);
                break;
            case "run_to_ball_act":
                m_agent.dash(300);
                break;
            case "run_to_right_goal_act":
                m_agent.dash(100);
                break;
            case "run_to_left_goal_act":
                m_agent.dash(100);
                break;
            case "run_to_own_goal_act":
                m_agent.dash(100);
                break;
            case "run_to_centre_act":
                m_agent.dash(100);
                break;
            case "catch_ball_act":
                m_agent.catchBall(ball.getDirection());
                break;
            default:
                getTS().getLogger().warning("INVALID ACTION");
        }

        switch ( actionToDo ) {
            case "find_ball_act", "find_teammate_act" -> m_agent.turn(SMALL_BROWSE_ANGLE);
            case "find_own_goal_act" -> m_agent.turn(LARGE_BROWSE_ANGLE);
            case "align_ball_act" -> m_agent.turn(ball.m_direction);
            case "run_to_ball_act" -> m_agent.dash(300);
            case "run_towards_own_goal_act" -> m_agent.dash(100);
            case "kick_to_opp_goal_act" -> m_agent.kick(KICK_POWER, oppGoal.m_direction);
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

    private FlagInfo getFlag( String flagName ) {
        return (FlagInfo) m_memory.getObject("flag " + flagName);
    }


    /**
     * Returns the opponent's goal if present.
     * @return opponent's goal info, if not null
     */
    private GoalInfo getOpponentGoal() {
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
     * Returns the furthest teammate to the player if any.
     * NOTE: This function will not return the goalie!
     *
     * @return the nearest teammate to the player.
     */
    private PlayerInfo getFurthestTeammate() {
        Optional<PlayerInfo> player = m_memory.getAll("player").stream()
                .map(p -> (PlayerInfo) p)
                .filter(p -> p.m_teamName.equals(m_team) && !p.m_goalie && p.m_distance > MIN_PASS_DISTANCE)
                .max(Comparator.comparingDouble(p -> p.m_distance));

        return player.orElse(null);
    }

    /**
     * Returns the player's zone using the flags:
     * 0: in home zone
     * 1: in opposition zone
     * 2: unknown
     *
     * @return the zone for the player
     */
    private int getPlayerZone() {
        // First check the flags near the end
        FlagInfo top_home = getFlag(m_side + " t 0"); //TODO
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