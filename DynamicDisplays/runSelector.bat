call config.cmd

:loop

java -Dddisplay.selector.inwindow=FALSE -Dddisplay.virtualdisplaynumbers=TRUE -Dddisplay.selector.public=FALSE -Xmx1024m gov.fnal.ppd.dd.MakeChannelSelector 

if %ERRORLEVEL% == -1 goto loop

