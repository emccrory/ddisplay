ECHO OFF

REM Part 3 of the update process on Windows.
REM Unpack a new version of the software on a Windows PC - Part 3
REM The working directory should be ..\.. with respect to where the software usually runs.

ECHO %cd%

IF NOT EXIST roc-dynamicdisplays-new GOTO skipUpdate

ECHO A new version of the Dynamic Displays software has been downloaded
SET A=1
:nextpart
SET file=roc-dynamicdisplays-old00%A%
if %A% GTR 9 (
  SET file=roc-dynamicdisplays-old0%A%
)
if %A% GTR 99 (
  SET file=roc-dynamicdisplays-old%A%
)
if %A% GTR 400 (
  ECHO Too many folders here
  ECHO Manual intervantion is required
  GOTO foundit
)
if not exist %file% GOTO foundit
SET /a A=%A%+1
GOTO nextpart

:foundit
ECHO Downloaded the new version and now renaming roc-dynamicdisplays folder to %file%
ECHO ON
RENAME roc-dynamicdisplays %file%
ECHO OFF
ECHO Now renaming roc-dynamicdisplays-new folder to roc-dynamicdisplays
ECHO ON
RENAME roc-dynamicdisplays-new roc-dynamicdisplays
ECHO OFF
ECHO All done with the update
GOTO finish

:skipUpdate
ECHO There is no update available at this time

:finish  
REM Now we need to figure out what program to run to restart the system.  
REM At this time, all Windows nodes only run the channel selector, and nothing else.  
REM So lets go with that.

CD roc-dynamicdisplays\DynamicDisplays
ECHO on
START /b runSelector.bat
