!wait.

+!wait:
    ~in_home_zone
    <-
    !run_to_home_zone.

+!wait:
    not(~in_home_zone) &
    goalie_has_ball &
    close_to_goalie
    <-
    !give_goalie_space.

+!wait:
    not(~in_home_zone) &
    goalie_has_ball &
    not(close_to_goalie)
    <-
    !wait.

+!wait:
    not(~in_home_zone) &
    not(goalie_has_ball) &
    not(ball_in_view)
    <-
    find_ball_act; !wait.

+!wait:
    not(~in_home_zone) &
    not(goalie_has_ball) &
    ball_in_view &
    not(aligned_with_ball) &
    not(ball_kickable)
    <-
    align_ball_act; !wait.

+!wait:
    not(~in_home_zone) &
    not(goalie_has_ball) &
    ball_in_view &
    aligned_with_ball &
    not(ball_kickable)
    <-
    run_to_ball_act; !wait.

+!wait:
    not(~in_home_zone) &
    not(goalie_has_ball) &
    ball_in_view &
    ball_kickable &
    opp_goal_in_view
    <-
    kick_to_opp_goal_act; !wait.

+!wait:
    not(~in_home_zone) &
    not(goalie_has_ball) &
    ball_in_view &
    ball_kickable &
    not(opp_goal_in_view) &
    attacker_teammate_in_view
    <-
    pass_to_attacker_teammate_act; !wait.

+!wait:
    not(~in_home_zone) &
    not(goalie_has_ball) &
    ball_in_view &
    ball_kickable &
    not(opp_goal_in_view) &
    not(attacker_teammate_in_view)
    <-
    find_attacker_teammate_act; !wait.

+!run_to_home_zone:
    not(in_home_zone) &
    not(own_goal_in_view)
    <-
    find_own_goal_act; !run_to_home_zone.

+!run_to_home_zone:
    not(in_home_zone) &
    own_goal_in_view &
    not(aligned_with_designated_flag)
    <-
    align_designated_flag_act; !run_to_home_zone.

+!run_to_home_zone:
    not(in_home_zone) &
    own_goal_in_view &
    aligned_with_designated_flag
    <-
    run_towards_designated_flag_act; !run_to_home_zone.

+!run_to_home_zone:
    in_home_zone
    <-
    !wait.

+!give_goalie_space:
    not(defender_close_to_centre) &
    not(centre_visible)
    <-
    find_centre_act; !give_goalie_space.

+!give_goalie_space:
    not(defender_close_to_centre) &
    centre_visible &
    not(aligned_with_centre_defender)
    <-
    align_centre_defender_act; !give_goalie_space.

+!give_goalie_space:
    not(defender_close_to_centre) &
    centre_visible &
    aligned_with_centre_defender
    <-
    run_to_centre_act; !give_goalie_space.

+!give_goalie_space:
    defender_close_to_centre
    <-
    !wait.