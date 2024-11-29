!wait.
in_home_zone.

+!wait:
    ~in_home_zone
    <-
    !run_to_home_zone.

+!wait:
    not ~in_home_zone &
    not ball_in_view
    <-
    find_ball_act; !wait.

+!wait:
    not ~in_home_zone &
    ball_in_view &
    not aligned_with_ball &
    not ball_close
    <-
    align_ball_act; !wait.

+!wait:
    not ~in_home_zone &
    ball_in_view &
    aligned_with_ball &
    not ball_close
    <-
    run_to_ball_act; !wait.

+!wait:
    not ~in_home_zone &
    ball_in_view &
    ball_close &
    opp_goal_in_view
    <-
    kick_to_opp_goal_act; !wait.

+!wait:
    not ~in_home_zone &
    ball_in_view &
    ball_close &
    not opp_goal_in_view &
    teammate_in_view
    <-
    pass_to_teammate_act; !wait.

+!wait:
    not ~in_home_zone &
    ball_in_view &
    ball_close &
    not opp_goal_in_view &
    not teammate_in_view
    <-
    find_teammate_act; !wait.

+!run_to_home_zone:
    ~in_home_zone &
    own_goal_in_view
    <-
    run_towards_own_goal_act; !run_to_home_zone.

+!run_to_home_zone:
    ~in_home_zone
    <-
    find_own_goal_act; !run_to_home_zone.

+!run_to_home_zone:
    in_home_zone
    <-
    !wait.