# Simple toolscript for updating BlueJ dependencies

[CmdletBinding()]
param (
  [Parameter(
    Mandatory = $true, 
    HelpMessage = "The version the BlueJ extensions2 library. Should follow the format: n.n.n (e.g: 5.0.2)"
  )]
  [String] $Version
);

# list of JAR files to update
$JarDependencies = @("bluej");
$InstallDir = "C:\Program Files\BlueJ";

foreach ($JarName in $JarDependencies) {
  $ExtensionsJar = "$InstallDir\lib\$JarName.jar";
  
  $fileExists = Test-Path -Path $ExtensionsJar;
  
  if ($fileExists) {
    mvn install:install-file `
      -Dfile="$InstallDir\lib\$JarName.jar" `
      -DgroupId="bluej" `
      -DartifactId="$JarName" `
      -Dversion="$Version" `
      -Dpackaging="jar" `
      -DgeneratePom="true" `
      -DlocalRepositoryPath="$PSScriptRoot\..\lib\"
  }
  else {
    return "$ExtensionJar does not exist.";
  }
}