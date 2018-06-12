call config.cmd

:loop

java -Dddisplay.selector.inwindow=TRUE ^
     -Dddisplay.virtualdisplaynumbers=TRUE ^
     -Dddisplay.selector.public=FALSE ^
     -Xmx1024m gov.fnal.ppd.dd.MakeChannelSelector 

if %ERRORLEVEL% EQ -1 goto loop
