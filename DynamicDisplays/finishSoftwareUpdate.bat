ECHO OFF

REM Part 3 of the update process on Windows.
REM Unpack a new version of the software on a Windows PC - Part 3
REM The working directory should be ..\.. with respect to where the software usually runs.

REM Can download the newest Zip file like this:
REM bitsadmin /transfer myDownloadjob /download /priority high https://dynamicdisplays.fnal.gov/dynamicdisplays.zip z:\dynamicdisplays.zip
REM But there is not a good way to unzip a file using a batch file like this.  See https://stackoverflow.com/questions/1021557/how-to-unzip-a-file-using-the-command-line

ECHO %cd%

IF NOT EXIST unzip.vbs (
    IF NOT EXIST roc-dynamicdisplays/DynamicDisplays/unzip.vbs (
	ECHO Canont unzip the archive because the Visual Basic Script is not available.  Oops.
	EXIT /B
    )
    COPY roc-dynamicdisplays/DynamicDisplays/unzip.vbs unzip.vbs
)

ECHO ON
IF EXIST dynamicdisplays.zip GOTO unpackZipFile

IF NOT EXIST roc-dynamicdisplays-new GOTO skipUpdate

:tryAgain
ECHO OFF

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

:unpackZipFile

ECHO  Unpacking the ZIP archive
MKDIR roc-dynamicdisplays-new
CD    roc-dynamicdisplays-new
MKDIR DynamicDisplays
CD    DynamicDisplays

MOVE ..\..\dynamicdisplays.zip dynamicdisplays.zip
CSCRIPT //B ..\..\unzip.vbs dynamicdisplays.zip

ECHO Finished the unzip.  Here are the files
DIR

IF NOT EXIST runSelectorWindow.bat (
    ECHO There is a problem.  No files seem to have been unzipped.
    EXIT /B
)

ERASE dynamicdisplays.zip
CD ..\..

GOTO tryAgain

:skipUpdate
ECHO OFF
ECHO There is no update available at this time

:finish  
ECHO OFF
REM Now we need to figure out what program to run to restart the system.  
REM At this time, all Windows nodes only run the channel selector, and nothing else.  
REM So lets go with that.

ECHO ON
CD roc-dynamicdisplays\DynamicDisplays

START /b runSelector.bat
