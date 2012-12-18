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

::  Get last line of file.  If file contains a path to new installation, it will be that one.
::
set DOWNLOAD=
FOR /F "delims=" %%i IN (autoupdate.log) DO set DOWNLOAD=%%i

echo The putative download line is %DOWNLOAD%
::  Search the auto updater log for the string indicating no update needed.
::
FINDSTR /m /c:"Already at latest version" autoupdate.log >NUL
IF "%ErrorLevel%"=="0" goto Latest
    echo Updater downloaded a new version to %DOWNLOAD%.

    ::  Now need to create a temporary batch file to run the update, which replaces the running batch script as well.
    ::
    set UPDATEBAT="%TEMP%"\update__workstation.bat
	del "%UPDATEBAT%"
    echo @echo off                            > %UPDATEBAT%
    echo echo Updating...                    >> %UPDATEBAT%
    echo echo rmdir /S /Q %INSTALL%               >> %UPDATEBAT%
    echo xcopy /S %DOWNLOAD% %INSTALL%\      >> %UPDATEBAT%
	::  Parens echoed here are confused with ending of IF statement enclosing this block of code.
    echo IF NOT EXIST %INSTALL%\workstation.bat echo failed! >> %UPDATEBAT%
    echo IF NOT EXIST %INSTALL%\workstation.bat exit 0 >> %UPDATEBAT%
    echo echo done.  Update complete.        >> %UPDATEBAT%
    echo COMMAND /S %INSTALL%\start.bat      >> %UPDATEBAT%

    :: Now run the file created above.  Exit afterwards.
    ::
    echo Executing update...
    call %UPDATEBAT%

	goto Finish
	
:Latest
    echo Already at latest version.
	cd %INSTALL%
    call start.bat
	goto Finish

:Finish
	::exit 1
