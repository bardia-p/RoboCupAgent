setlocal

set "path_1=target\classes;ASL;lib\*"
set "className_1=org.example.RoboCupAgent"
set "team_1=Carleton"

set "path_2=target\classes;ASL;lib\*"             :: change this to the path of your agent's directory.
set "className_2=org.example.RoboCupAgent"        :: change this to your agent's main class.
set "team_2=University"

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

start java -cp "%path_2%" %className_2% -team %team_2% -playerType Goalie
ping localhost
start java -cp "%path_2%" %className_2% -team %team_2% -playerType Defender
ping localhost
start java -cp "%path_2%" %className_2% -team %team_2% -playerType Defender
ping localhost
start java -cp "%path_2%" %className_2% -team %team_2% -playerType Attacker
ping localhost
start java -cp "%path_2%" %className_2% -team %team_2% -playerType Attacker
ping localhost

endlocal