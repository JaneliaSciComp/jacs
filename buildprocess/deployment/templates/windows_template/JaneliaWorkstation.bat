REM This script assumes
REM   1) \\dm11\jacsData is mounted and assigned drive letter Q:
REM   2) java.exe is on your PATH
REM   3) vaa3d.exe is on your PATH
REM Run this script from jacsData windows area
REM start Janelia Workstation in background
start /b java -XX:+UseParallelGC -jar workstation.jar -Xms512m -Xmx10000m &
REM wait 3 seconds
CHOICE /N /C y /D y /T 3 > NUL
REM Start Vaa3D in NeuronAnnotator mode
REM vaa3d /na
