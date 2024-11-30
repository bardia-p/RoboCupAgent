!find_ball(X).

+!find_ball(X): not(ball_in_view(X)) <- look_for_ball; !find_ball(X).
+!find_ball(X): ball_in_view(X) <- !turn_to_ball(X).

+!turn_to_ball(X): not(ball_in_view(X)) <- !find_ball(X).
+!turn_to_ball(X): not(in_ball_direction(X)) <- face_ball; !turn_to_ball(X).
+!turn_to_ball(X): in_ball_direction(X) <- !dash_to_ball(X).

+!dash_to_ball(X): not(in_ball_direction(X)) <- !turn_to_ball(X).
+!dash_to_ball(X): in_ball_direction(X) <- run_to_ball; !dash_to_ball(X).