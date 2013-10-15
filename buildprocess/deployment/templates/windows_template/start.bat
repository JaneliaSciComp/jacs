@ECHO OFF
:: Go to the directory of the script itself, before launching commands, to ensure everything is where needed.
set DIR=%~dp0
cd %DIR%
START /B java -XX:+UseParallelGC -Xms512m -Xmx10000m -jar workstation.jar
::  No longer need to launch vaa3d from the batch script.
:: Best yet approach to a sleep 3 command equivalent.
::choice /N /C y /D y /T 3 > NUL
:: Run Vaa 3D in background
::START /B .\vaa3d -na
