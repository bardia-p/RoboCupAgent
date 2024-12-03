!wait.
in_centre_position.

+!wait:
    not(ball_in_view)
    <-
    find_ball_act; !wait.

+!wait:
    not(aligned_with_ball) &
    ball_in_view &
    not(ball_distance_catchable)
    <-
    align_ball_act; !wait.

+!wait:
    not(ball_distance_catchable) &
    aligned_with_ball &
    ball_to_goalie_angle_right &
    not(in_right_position)
    <-
    !in_right_goal.

+!wait:
    not(ball_distance_catchable) &
    aligned_with_ball &
    ball_to_goalie_angle_very_right &
    not(in_very_right_position)
    <-
    !in_very_right_goal.

+!wait:
    not(ball_distance_catchable) &
    aligned_with_ball &
    ball_to_goalie_angle_left &
    not(in_left_position)
    <-
    !in_left_goal.

+!wait:
    not(ball_distance_catchable) &
    aligned_with_ball &
    ball_to_goalie_angle_very_left &
    not(in_very_left_position)
    <-
    !in_very_left_goal.

+!wait:
    not(ball_distance_catchable) &
    aligned_with_ball &
    ball_to_goalie_angle_centre &
    not(in_centre_position)
    <-
    !reset_centre.

+!wait:
    not(ball_distance_catchable) &
    aligned_with_ball &
    ( in_very_right_position |
    in_right_position |
    in_centre_position |
    in_left_position |
    in_very_left_position )
    <-
    wait_act; !wait.

+!wait:
    ball_distance_catchable
    <-
    !offensive_mode.

+!offensive_mode:
    not(ball_distance_catchable) |
    not(ball_in_view)
    <-
    !reset_centre.

+!offensive_mode:
    caught_ball
    <-
    !kick_to_teammate_setup.

+!offensive_mode:
    ball_kickable &
    not(caught_ball)
    <-
    catch_ball_act;
    !offensive_mode.

+!offensive_mode:
    ball_distance_catchable &
    ball_angle_catchable &
    not(ball_kickable) &
    not(caught_ball)
    <-
    run_to_ball_goalie_act;
    !offensive_mode.

+!offensive_mode:
    ball_distance_catchable &
    not(ball_angle_catchable) &
    not(ball_kickable) &
    not(caught_ball)
    <-
    align_ball_act;
    !offensive_mode.

+!in_right_goal:
    not(flag_right_to_goal_in_view)
    <-
    find_right_goal_act;
    !in_right_goal.

+!in_right_goal:
    flag_right_to_goal_in_view
    <-
    align_right_goal_act;
    !run_to_right_goal.

+!run_to_right_goal:
    not(within_right_of_goal)
    <-
    run_to_right_goal_goalie_act;
    !run_to_right_goal.

+!run_to_right_goal:
    within_very_right_of_goal
    <-
    run_backwards_right_goal_goalie_act;
    !run_to_right_goal.

+!run_to_right_goal:
    within_right_of_goal
    <-
    -in_centre_position;
    -in_left_position;
    -in_very_left_position;
    -in_very_right_position;
    +in_right_position;
    !wait.

+!in_very_right_goal:
    not(flag_right_to_goal_in_view)
    <-
    find_right_goal_act;
    !in_very_right_goal.

+!in_very_right_goal:
    flag_right_to_goal_in_view
    <-
    align_right_goal_act;
    !run_to_very_right_goal.

+!run_to_very_right_goal:
    not(within_very_right_of_goal)
    <-
    run_to_right_goal_goalie_act;
    !run_to_very_right_goal.

+!run_to_very_right_goal:
    within_very_right_of_goal
    <-
    -in_centre_position;
    -in_right_position;
    -in_left_position;
    -in_very_left_position;
    +in_very_right_position;
    !wait.

+!in_left_goal:
    not(flag_left_to_goal_in_view)
    <-
    find_left_goal_act;
    !in_left_goal.

+!in_left_goal:
    flag_left_to_goal_in_view
    <-
    align_left_goal_act;
    !run_to_left_goal.

+!run_to_left_goal:
    not(within_left_of_goal)
    <-
    run_to_left_goal_goalie_act;
    !run_to_left_goal.

+!run_to_left_goal:
    within_very_left_of_goal
    <-
    run_backwards_left_goal_goalie_act;
    !run_to_left_goal.

+!run_to_left_goal:
    within_left_of_goal
    <-
    -in_centre_position;
    -in_very_left_position;
    -in_very_right_position;
    -in_right_position;
    +in_left_position;
    !wait.

+!in_very_left_goal:
    not(flag_left_to_goal_in_view)
    <-
    find_left_goal_act;
    !in_very_left_goal.

+!in_very_left_goal:
    flag_left_to_goal_in_view
    <-
    align_left_goal_act;
    !run_to_very_left_goal.

+!run_to_very_left_goal:
    not(within_very_left_of_goal)
    <-
    run_to_left_goal_goalie_act;
    !run_to_very_left_goal.

+!run_to_very_left_goal:
    within_very_left_of_goal
    <-
    -in_centre_position;
    -in_left_position;
    -in_right_position;
    -in_very_right_position;
    +in_very_left_position;
    !wait.

+!reset_centre:
    true
    <-
    !find_own_goal.

+!find_own_goal:
    not(own_goal_in_view)
    <-
    find_own_goal_act;
    !find_own_goal.

+!find_own_goal:
    own_goal_in_view
    <-
    align_own_goal_act;
    !run_to_own_goal.

+!run_to_own_goal:
    not(own_goal_in_view)
    <-
    !find_own_goal.

+!run_to_own_goal:
    not(inside_own_goal)
    <-
    run_to_own_goal_goalie_act;
    !run_to_own_goal.

+!run_to_own_goal:
    inside_own_goal
    <-
    !align_with_centre.

+!align_with_centre:
    not(centre_visible)
    <-
    find_centre_act;
    !align_with_centre.

+!align_with_centre:
    centre_visible
    <-
    align_centre_act;
    !run_to_centre.

+!run_to_centre:
    not(goalie_distance_from_centre)
    <-
    run_to_centre_goalie_act;
    !run_to_centre.

+!run_to_centre:
    goalie_distance_from_centre
    <-
    -in_left_position;
    -in_right_position;
    -in_very_right_position;
    -in_very_left_position;
    +in_centre_position;
    !wait.

+!kick_to_teammate_setup:
    not(centre_visible)
    <-
    find_centre_act;
    !kick_to_teammate_setup.

+!kick_to_teammate_setup:
    centre_visible
    <-
    align_centre_act;
    !kick_to_teammate.

+!kick_to_teammate:
    attacker_teammate_in_view &
    not(opponent_in_the_way_of_attacker_teammate)
    <-
    !kick_to_teammate_attacker.

+!kick_to_teammate:
    defender_teammate_in_view &
    not(opponent_in_the_way_of_defender_teammate)
    <-
    !kick_to_teammate_defender.

+!kick_to_teammate:
    not ( attacker_teammate_in_view &
    not(opponent_in_the_way_of_attacker_teammate) ) &
    not ( defender_teammate_in_view &
    not(opponent_in_the_way_of_defender_teammate) )
    <-
    !kick_to_corner_flag.

+!kick_to_teammate_defender:
    not(defender_teammate_in_view)
    <-
    !kick_to_teammate_setup.

+!kick_to_teammate_defender:
    defender_teammate_in_view
    <-
    pass_to_defender_teammate_act;
    !offensive_mode.

+!kick_to_teammate_attacker:
    not(attacker_teammate_in_view)
    <-
    !kick_to_teammate_setup.

+!kick_to_teammate_attacker:
    attacker_teammate_in_view
    <-
    pass_to_attacker_teammate_act;
    !offensive_mode.

+!kick_to_corner_flag
    <-
    pass_random_act;
    !offensive_mode.