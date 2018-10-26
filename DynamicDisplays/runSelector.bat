
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

REM It would be nice to use the tee command (of Unix and Powershell) in this previous command.

