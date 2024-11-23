!find_ball(X).

+!find_ball(X): not(ball_in_view(X)) <- look_for_ball; !find_ball(X).