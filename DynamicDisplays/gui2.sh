#!/bin/bash

export CLASSPATH=bin:lib/engine-gecko15.jar:lib/engine-gecko.jar:lib/jniwrap-3.8.4.jar:lib/jxbrowser-3.4.jar:lib/license.jar:lib/log4j-1.2.15.jar:lib/mysql-connector-java-5.0.3-bin.jar:lib/runtime.jar:lib/slf4j-api-1.5.8.jar:lib/slf4j-log4j12-1.5.8.jar:lib/xulrunner-linux64.jar

export PATH=$PATH:~/ejre1.7.0_45/bin

java gov.fnal.ppd.signage.display.testing.BrowserServer

