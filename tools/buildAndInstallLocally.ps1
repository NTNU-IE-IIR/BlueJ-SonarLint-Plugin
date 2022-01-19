mvn clean verify;

$InstallDir = "C:\Program Files\BlueJ";
$JarArtifact = Get-ChildItem -Path "$PSScriptRoot\..\target" | Where-Object {($_.FullName -like "*.jar") -and ($_.FullName -notlike "*-original.jar")};

Copy-Item -Path $JarArtifact -Destination "$InstallDir\lib\extensions2\";