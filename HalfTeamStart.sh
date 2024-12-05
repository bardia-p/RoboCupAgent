#!/bin/bash

path_1="target/classes:ASL:lib/*"
className_1="org.example.RoboCupAgent"
team_1="Champions"

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

# Load team one
java -cp "$path_1" $className_1 -team $team_1 -playerType Goalie &
ping localhost &
sleep .2
java -cp "$path_1" $className_1 -team $team_1 -playerType Defender &
ping localhost &
sleep .2
java -cp "$path_1" $className_1 -team $team_1 -playerType Defender &
ping localhost &
sleep .2
java -cp "$path_1" $className_1 -team $team_1 -playerType Attacker &
ping localhost &
sleep .2
java -cp "$path_1" $className_1 -team $team_1 -playerType Attacker &
ping localhost