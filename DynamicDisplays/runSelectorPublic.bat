set path="c:\Program Files (x86)\Java\jre7\bin";%path%

set classpath=bin;lib/mysql-connector-java-5.0.3-bin.jar;lib/slf4j-api-1.5.8.jar;lib/slf4j-log4j12-1.5.8.jar

java -Dddisplay.selector.inwindow=FALSE -Dddisplay.selector.public=TRUE -Xmx512m gov.fnal.ppd.ChannelSelector 