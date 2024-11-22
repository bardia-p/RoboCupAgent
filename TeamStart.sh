#!/bin/bash

mode=${1:-"deploy"}

echo "Mode: $mode"

if [ "$mode" = "debug" ]; then
  class_path="target/classes"
else
  class_path="classes"
fi

path_1="$class_path:ASL:lib/*"
className_1="org.example.RoboCupAgent"
team_1="Carleton"

path_2="$class_path:ASL:lib/*"             # change this to the path of your agent's directory.
className_2="org.example.RoboCupAgent"     # change this to your agent's main class.
team_2="University"

num_players=5

kill_all_tasks() {
  echo "Killing all Java processes..."

  pids=$(ps aux | grep '[j]ava.*' | awk '{print $2}')

  if [ -n "$pids" ]; then
    echo "Killing processes: $pids"
    kill -9 $pids 2>/dev/null
  else
    echo "No processes matching the pattern found."
  fi
}

trap kill_all_tasks SIGINT

for (( i=0; i < num_players; i++ ))
do
	java -cp "$path_1" $className_1 -team $team_1 &
	ping localhost &
done

for (( i=0; i < num_players - 1; i++ ))
do
	java -cp "$path_2" $className_2 -team $team_2 &
	ping localhost &
done

java -cp "$path_2" $className_2 -team $team_2 &
ping localhost