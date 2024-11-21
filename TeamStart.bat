setlocal

set "mode=%~1"
if "%mode%"=="" set "mode=deploy"

echo Mode: %mode%

if "%mode%"=="debug" (
    set "class_path=target\classes"
) else (
    set "class_path=classes"
)

set "path_1=%class_path%;resources;lib\*"
set "className_1=org.example.RoboCupAgent"
set "team_1=Carleton"

set "path_2=%class_path%;resources;lib\*"         :: change this to the path of your agent's directory.
set "className_2=org.example.RoboCupAgent"        :: change this to your agent's main class.
set "team_2=University"

set "num_players=5"

for /L %%i in (1,1,%num_players%) do (
    start java -cp "%path_1%" %className_1% -team %team_1%
    ping localhost
)

for /L %%i in (1,1,%num_players%-1) do (
    start java -cp "%path_2%" %className_2% -team %team_2%
    ping localhost
)

start java -cp "%path_2%" %className_2% -team %team_2%
ping localhost

endlocal