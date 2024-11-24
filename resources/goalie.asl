!wait(X).

+!wait(X):
    ~ball_in_view(X)
    <-
    find_ball_act; !wait(X).

+!wait(X):
    ~in_ball_direction(X) &
    ball_in_view(X)
    <-
    turn_to_ball_act; !wait(X).

+!wait(X):
    ~ball_close(X) &
    in_ball_direction(X)
    <-
    wait_act; !wait(X).

+!wait(X):
    ball_close(X)
    <-
    !offensive_mode(X).

+!offensive_mode(X):
    ~ball_close(X)
    <-
    !wait(X).

+!offensive_mode(X):
    ball_close(X) &
    ~ball_kickable(X)
    <-
    dash_to_ball_act;
    !offensive_mode(X).

+!offensive_mode(X):
    ball_close(X) &
    ball_kickable(X)
    <-
    kick_random_act;
    !offensive_mode(X).

