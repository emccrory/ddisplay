<# 
    Note that one cannot run a PowerShell script without specifically allowing the system to do that.
    This is strongly discouraged by Microsoft unless you know exactly what you are doing.
    And it requires admin rights to change this restriction
#>

cd ..\..

echo Moving folders about

if ( Test-Path roc-dynamicdisplays-old5 ) {
    Remove-Item -Path roc-dynamicdisplays-old5 -recurse
}
if ( Test-Path roc-dynamicdisplays-old4 ) {
    Move-Item -Path roc-dynamicdisplays-old4 -Destination roc-dynamicdisplays-old5
}
if ( Test-Path roc-dynamicdisplays-old3 ) {
    Move-Item -Path roc-dynamicdisplays-old3 -Destination roc-dynamicdisplays-old4
}
if ( Test-Path roc-dynamicdisplays-old2 ) {
    Move-Item -Path roc-dynamicdisplays-old2 -Destination roc-dynamicdisplays-old3
}
if ( Test-Path roc-dynamicdisplays-old1 ) {
    Move-Item -Path roc-dynamicdisplays-old1 -Destination roc-dynamicdisplays-old2
}
if ( Test-Path roc-dynamicdisplays ) {
    Move-Item -Path roc-dynamicdisplays      -Destination roc-dynamicdisplays-old1
}

md roc-dynamicdisplays-new
cd roc-dynamicdisplays-new
md DynamicDisplays
cd DynamicDisplays

echo Getting the new software

$client = new-object System.Net.WebClient
$client.DownloadFile("http://dynamicdisplays.fnal.gov/dynamicdisplays.zip", "dynamicdisplays.zip")

unzip dynamicdisplays.zip

echo This is what we have for the new software
dir

cd ..\..
Move-Item -Path roc-dynamicdisplays-new -Destination roc-dynamicdisplays

echo All done.