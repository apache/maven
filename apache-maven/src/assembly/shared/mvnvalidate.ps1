# check mvn home
$MAVEN_HOME = (Get-Item $PSScriptRoot).Parent

# check if maven command exists
if (-not (Test-path "$MAVEN_HOME\bin\mvn.ps1")) {
    Write-Error -Message "Maven command ($MAVEN_HOME\bin\mvn.ps1) cannot be found" -ErrorAction Stop
}
