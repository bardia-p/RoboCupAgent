!wait.

+!wait:
    in_home_zone
    <-
    !run_to_opp_zone.

+!wait:
    not(in_home_zone) &
    goalie_has_ball
    <-
    !wait.

+!wait:
    not(in_home_zone) &
    not(goalie_has_ball) &
    not(ball_in_view)
    <-
    find_ball_act; !wait.

+!wait:
    not(in_home_zone) &
    not(goalie_has_ball) &
    ball_in_view &
    not(aligned_with_ball) &
    not(ball_kickable)
    <-
    align_ball_act; !wait.

+!wait:
    not(in_home_zone) &
    not(goalie_has_ball) &
    ball_in_view &
    aligned_with_ball &
    not(ball_kickable)
    <-
    run_to_ball_act; !wait.

+!wait:
    not(in_home_zone) &
    not(goalie_has_ball) &
    ball_in_view &
    ball_kickable &
    opp_goal_in_view
    <-
    kick_to_opp_goal_act; !wait.

+!wait:
    not(in_home_zone) &
    not(goalie_has_ball) &
    ball_in_view &
    ball_kickable &
    not(opp_goal_in_view)
    <-
    find_opp_goal_act; !wait.

+!run_to_opp_zone:
    not(~in_home_zone) &
    not(opp_goal_in_view)
    <-
    find_opp_goal_act; !run_to_opp_zone.

+!run_to_opp_zone:
    not(~in_home_zone) &
    opp_goal_in_view &
    not(aligned_with_designated_flag)
    <-
    align_designated_flag_act; !run_to_opp_zone.

+!run_to_opp_zone:
    not(~in_home_zone) &
    opp_goal_in_view &
    aligned_with_designated_flag
    <-
    run_towards_designated_flag_act; !run_to_opp_zone.

+!run_to_opp_zone:
    ~in_home_zone
    <-
    !wait.