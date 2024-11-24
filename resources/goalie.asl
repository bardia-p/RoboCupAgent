!wait.
in_centre_position.

+!wait:
    ~ball_in_view
    <-
    find_ball_act; !wait.

+!wait:
    ~in_ball_direction &
    ball_in_view
    <-
    turn_to_ball_act; !wait.

+!wait:
    ~ball_close &
    in_ball_direction &
    ball_angle_too_right &
    not(in_right_position)
    <-
    !find_right_goal.

+!wait:
    ~ball_close &
    in_ball_direction &
    ball_angle_too_left &
    not(in_left_position)
    <-
    !find_left_goal.

+!wait:
    ~ball_close &
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
    ~ball_close
    <-
    !wait.

+!offensive_mode:
    ball_close &
    ~ball_kickable
    <-
    dash_to_ball_act;
    !offensive_mode.

+!offensive_mode:
    ball_close &
    ball_kickable
    <-
    kick_random_act;
    !offensive_mode.

+!find_right_goal:
    ~right_goal_in_view
    <-
    find_right_goal_act;
    !find_right_goal.

+!find_right_goal:
    right_goal_in_view &
    ~within_right_goal
    <-
    turn_to_right_goal_act;
    dash_to_right_goal_act;
    !find_right_goal.

+!find_right_goal:
    right_goal_in_view &
    within_right_goal
    <-
    -in_centre_position;
    -in_left_position;
    +in_right_position;
    !wait.

+!find_left_goal:
    ~left_goal_in_view
    <-
    find_left_goal_act;
    !find_left_goal.

+!find_left_goal:
    left_goal_in_view &
    ~within_left_goal
    <-
    turn_to_left_goal_act;
    dash_to_left_goal_act;
    !find_left_goal.

+!find_left_goal:
    left_goal_in_view &
    within_left_goal
    <-
    -in_centre_position;
    -in_right_position;
    +in_left_position;
    !wait.