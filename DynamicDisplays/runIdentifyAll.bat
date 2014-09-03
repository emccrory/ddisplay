set path="c:\Program Files (x86)\Java\jre7\bin";%path%

set classpath=bin;lib/mysql-connector-java-5.0.3-bin.jar;lib/slf4j-api-1.5.8.jar;lib/slf4j-log4j12-1.5.8.jar

java -Dsignage.selector.location=0 -Xmx512m gov.fnal.ppd.IdentifyAll
