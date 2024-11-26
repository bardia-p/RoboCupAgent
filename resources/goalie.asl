!wait.
in_centre_position.

+!wait:
    not(ball_in_view)
    <-
    find_ball_act; !wait.

+!wait:
    not(in_ball_direction) &
    ball_in_view &
    not(ball_close)
    <-
    turn_to_ball_act; !wait.

+!wait:
    not(ball_close) &
    in_ball_direction &
    ball_angle_too_right &
    not(in_right_position)
    <-
    !find_right_goal.

+!wait:
    not(ball_close) &
    in_ball_direction &
    ball_angle_too_left &
    not(in_left_position)
    <-
    !find_left_goal.

+!wait:
    not(ball_close) &
    in_ball_direction &
    ball_angle_centre &
    not(in_centre_position)
    <-
    !reset_centre.

+!wait:
    not(ball_close) &
    in_ball_direction &
    ( in_right_position |
    in_centre_position |
    in_left_position )
    <-
    wait_act; !wait.

+!wait:
    ball_close
    <-
    !offensive_mode.

+!offensive_mode:
    not(ball_close)
    <-
    !reset_centre;
    !wait.

+!offensive_mode:
    ball_kickable
    <-
    catch_ball_act;
    !offensive_mode.

+!offensive_mode:
    ball_close &
    ball_direction_good &
    not(ball_kickable)
    <-
    dash_to_ball_act;
    !offensive_mode.

+!offensive_mode:
    ball_close &
    not(ball_direction_good) &
    not(ball_kickable)
    <-
    turn_to_ball_act;
    !offensive_mode.

+!find_right_goal:
    not(right_goal_in_view)
    <-
    find_right_goal_act;
    !find_right_goal.

+!find_right_goal:
    right_goal_in_view
    <-
    turn_to_right_goal_act;
    !dash_to_right_goal.

+!dash_to_right_goal:
    not(within_right_goal)
    <-
    dash_to_right_goal_act;
    !dash_to_right_goal.

+!dash_to_right_goal:
    within_right_goal
    <-
    -in_centre_position;
    -in_left_position;
    +in_right_position;
    !wait.

+!find_left_goal:
    not(left_goal_in_view)
    <-
    find_left_goal_act;
    !find_left_goal.

+!find_left_goal:
    left_goal_in_view
    <-
    turn_to_left_goal_act;
    !dash_to_left_goal.

+!dash_to_left_goal:
    not(within_left_goal)
    <-
    dash_to_left_goal_act;
    !dash_to_left_goal.

+!dash_to_left_goal:
    within_left_goal
    <-
    -in_centre_position;
    -in_right_position;
    +in_left_position;
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
    turn_to_own_goal_act;
    !dash_to_own_goal.

+!dash_to_own_goal:
    not(own_goal_in_view)
    <-
    !find_own_goal.

+!dash_to_own_goal:
    not(within_own_goal) &
    own_goal_in_view
    <-
    dash_to_own_goal_act;
    !dash_to_own_goal.

+!dash_to_own_goal:
    within_own_goal
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
    turn_to_centre_act;
    !dash_to_centre.

+!dash_to_centre:
    not(within_centre)
    <-
    dash_to_centre_act;
    !dash_to_centre.

+!dash_to_centre:
    within_centre
    <-
    -in_right_position;
    -in_left_position;
    +in_centre_position;
    !wait.
