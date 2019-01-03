REM Part 3 of the update process on Windows

REM Unpack a new version of the software on a Windows PC - Part 3

REM The working directory should be ..\.. with respect to where the software usually runs.

echo %cd%

SET A=1
:nextpart
SET file=roc-dynamicdisplays-old00%A%
if %A% GTR 9 (
	SET file=roc-dynamicdisplays-old0%A%
)
if %A% GTR 99 (
	SET file=roc-dynamicdisplays-old%A%
)
if %A% GTR 999 (
	ECHO Too many folders here
	GOTO foundit
)
if not exist %file% GOTO foundit
SET /a A=%A%+1
GOTO nextpart

:foundit
ECHO Downloaded the new version and now renaming roc-dynamicdisplays folder to %file%

ECHO ON

RENAME roc-dynamicdisplays %file%

ECHO Now renaming roc-dynamicdisplays-new folder to roc-dynamicdisplays
RENAME roc-dynamicdisplays-new roc-dynamicdisplays

ECHO All done with the update

REM Now we need to figure out what program to run to restart the system.  At this time, all Windows
REM nodes only run the channel selector.  So lets go with that.

CD roc-dynamicdisplays\DynamicDisplays
ECHO on
START /b runSelector.bat
