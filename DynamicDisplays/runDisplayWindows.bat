REM This batch file has never been used.

call config.cmd

java -Xmx1024m gov.fnal.ppd.dd.display.client.selenium.DisplayAsConnectionThroughSelenium

REM Need to capture the output, and add more of the functionality from runDisplay.sh
