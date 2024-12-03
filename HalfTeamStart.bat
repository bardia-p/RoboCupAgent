setlocal

set "path_1=target\classes;ASL;lib\*"
set "className_1=org.example.RoboCupAgent"
set "team_1=Home"


start java -cp "%path_1%" %className_1% -team %team_1% -playerType Goalie
ping localhost
start java -cp "%path_1%" %className_1% -team %team_1% -playerType Defender
ping localhost
start java -cp "%path_1%" %className_1% -team %team_1% -playerType Defender
ping localhost
start java -cp "%path_1%" %className_1% -team %team_1% -playerType Attacker
ping localhost
start java -cp "%path_1%" %className_1% -team %team_1% -playerType Attacker
ping localhost

endlocal