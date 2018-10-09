ECHO OFF

CALL config.cmd

SET LOG=..\..\log\Selector_%DATE:~-4%-%DATE:~4,2%-%DATE:~7,2%_%TIME:~0,2%_%TIME:~3,2%.txt

java -Dddisplay.selector.inwindow=FALSE ^
     -Dddisplay.virtualdisplaynumbers=TRUE ^
     -Dddisplay.selector.public=FALSE ^
     -Xmx1024m ^
     gov.fnal.ppd.dd.MakeChannelSelector > %LOG% 2>&1

REM It would be nice to use the tee command (of Unix and Powershell) in this previous command.

