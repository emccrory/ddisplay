set path="c:\Program Files (x86)\Java\jre7\bin";%path%

set classpath=bin;lib/mysql-connector-java-5.0.3-bin.jar;lib/slf4j-api-1.5.8.jar;lib/slf4j-log4j12-1.5.8.jar
set "public=false"
set "loc=0"
set "databaseServer=mccrory.fnal.gov"
set "messagingServer=dd-124709.fnal.gov"

java -Dddisplay.selector.inwindow=TRUE^
     -Dddisplay.selector.public=%public% ^
     -Dddisplay.selector.location=%loc% ^
     -Dddisplay.messagingserver=%messagingServer% ^
     -Dddisplay.dbserver=%databaseServer% ^
     -Xmx512m gov.fnal.ppd.ChannelSelector 

