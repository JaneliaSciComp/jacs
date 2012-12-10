@ECHO OFF
::
:: Check for updates before starting the FlySuite.  NOTE: this script should never run on Mac, so no
:: extra-pathing info will be checked.
::

set INSTALL=%~dp0
cd %INSTALL%

:: Reading the last line of the file produced by running the workstation AutoUpdater application.
::  The last line should be the full path to a downloaded update.
::
java -cp workstation.jar -Xms512m -Xmx1024m org.janelia.it.FlyWorkstation.gui.application.AutoUpdater > autoupdate.log
set DOWNLOAD=
FOR /F "usebackq delims=" %%i IN (autoupdate.log) DO set DOWNLOAD=%%i
if NOT "%DOWNLOAD%"=="" (
    echo Updater downloaded a new version to %DOWNLOAD%.

    ::  Now need to create a temporary batch file to run the update, which replaces the running batch script as well.
    ::
    set UPDATEBAT=%TEMP%\update__workstation.bat
    echo @echo off                            > %UPDATEBAT%
    echo Updating...                         >> %UPDATEBAT%
    echo rmdir /S /Q %INSTALL%               >> %UPDATEBAT%
    echo xcopy /S %DOWNLOAD% %INSTALL%\      >> %UPDATEBAT%
    echo IF NOT EXISTS %INSTALL%\workstation.bat ( >> %UPDATEBAT%
    echo   echo failed!                      >> %UPDATEBAT%
    echo   exit 0                            >> %UPDATEBAT%
    echo )                                   >> %UPDATEBAT%
    echo echo done.  Update complete.        >> %UPDATEBAT%
    echo COMMAND /S %INSTALL%\start.bat      >> %UPDATEBAT%

    :: Now run the file created above.  Exit afterwards.
    ::
    echo Executing update...
    call %UPDATEBAT%
    exit 1
) ELSE (
    echo Already at latest version.
    call %INSTALL%\start.bat
    exit 1
)
