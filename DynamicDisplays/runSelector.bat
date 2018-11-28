
SET LOG=..\..\log\Selector_%DATE:~-4%-%DATE:~4,2%-%DATE:~7,2%_%TIME:~0,2%_%TIME:~3,2%.txt
SET LOG=%LOG:0=_%

ECHO OFF

REM The first SET command, above: If the time is a one-digit number, the "Substitute" business with the TIME variable will insert a space.
REM The second SET command replaces that space (and any other spaces that may have worked their way in) with a zero.

CALL config.cmd

java -Dddisplay.selector.inwindow=FALSE ^
     -Dddisplay.virtualdisplaynumbers=TRUE ^
     -Dddisplay.selector.public=FALSE ^
     -Xmx1024m ^
     gov.fnal.ppd.dd.MakeChannelSelector > %LOG% 2>&1

REM I think -1, below, is the same thing as 255, maybe.

if %ERRORLEVEL% EQU -1 goto loop

if %ERRORLEVEL% EQU 99 {
	REM This is a special code that says we are trying to do a self-update
	cd ..\..
	
}
