# check mvn home
if (-not $env:MAVEN_HOME) {
    $env:MAVEN_HOME = (Get-Item $PSScriptRoot"\..")
}

# check if maven command exists
if (-not (Test-path $env:MAVEN_HOME"\bin\mvn.ps1")) {
    Write-Error -Message "maven command (\bin\mvn.ps1) cannot be found" -ErrorAction Stop
}
