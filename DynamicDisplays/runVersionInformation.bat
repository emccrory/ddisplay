call config.cmd

java gov.fnal.ppd.dd.util.version.VersionInformationComparison
IF ERRORLEVEL  0 GOTO NEW
IF ERRORLEVEL -1 GOTO OLD

goto END

:OLD
	ECHO The local version of the code needs to be updated
	Msg * "The local version of the Dynamic Display code needs to be updated.  Continuing for now."
	GOTO END

:NEW
	ECHO The local version of the code is up to date
	GOTO END

:END


