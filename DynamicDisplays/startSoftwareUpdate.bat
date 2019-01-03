REM A test script to unpack a new version of the software on a Windows PC. Part 1 and 2
ECHO off

call config.cmd

REM The unpacking is done here in the java program.  That is part 1
(
  java -Xmx1024m gov.fnal.ppd.dd.util.DownloadNewSoftwareVersion && ( GOTO success ) || ( GOTO fail )
)
	
REM Now we need to move the folders around and re-run the launch program. (Part 2)
:success

COPY finishSoftwareUpdate.bat ..\..

CD ..\..

START /B finishSoftwareUpdate.bat

:fail
ECHO on