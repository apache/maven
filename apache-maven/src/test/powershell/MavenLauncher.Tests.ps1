<#
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
#>

param(
  [Parameter(Mandatory = $true)]
  [string] $MavenHome
)

$ErrorActionPreference = "Stop"
$MavenHome = (Get-Item -LiteralPath $MavenHome).FullName
$binDirectory = Join-Path $MavenHome "bin"
$temporaryRoot = Join-Path ([IO.Path]::GetTempPath()) ("maven-powershell-tests-" + [guid]::NewGuid().ToString("N"))

function Assert-Equal {
  param(
    $Expected,
    $Actual,
    [string] $Message
  )

  if ($Expected -ne $Actual) {
    throw "$Message Expected: <$Expected>; actual: <$Actual>."
  }
}

function Assert-Contains {
  param(
    [string] $Value,
    [string] $Expected,
    [string] $Message
  )

  if (-not $Value.Contains($Expected)) {
    throw "$Message Missing text: <$Expected>. Output:`n$Value"
  }
}

function Assert-NotContains {
  param(
    [string] $Value,
    [string] $Unexpected,
    [string] $Message
  )

  if ($Value.Contains($Unexpected)) {
    throw "$Message Unexpected text: <$Unexpected>. Output:`n$Value"
  }
}

function Invoke-Launcher {
  param(
    [string] $ScriptName,
    [string[]] $Arguments
  )

  $scriptPath = Join-Path $binDirectory $ScriptName
  $originalErrorWriter = [Console]::Error
  $capturedErrorWriter = New-Object IO.StringWriter
  [Console]::SetError($capturedErrorWriter)
  try {
    $output = @(& $scriptPath @Arguments 2>&1 | ForEach-Object { "$_" })
    $exitCode = $LASTEXITCODE
  }
  finally {
    [Console]::SetError($originalErrorWriter)
  }

  $capturedError = $capturedErrorWriter.ToString().TrimEnd()
  if ($capturedError) {
    $output += $capturedError
  }
  return [pscustomobject]@{
    ExitCode = $exitCode
    Output = $output -join [Environment]::NewLine
  }
}

function Test-ScriptSyntax {
  $failed = $false
  Get-ChildItem -LiteralPath $binDirectory -Filter "*.ps1" | ForEach-Object {
    $tokens = $null
    $parseErrors = $null
    [System.Management.Automation.Language.Parser]::ParseFile(
      $_.FullName,
      [ref] $tokens,
      [ref] $parseErrors
    ) > $null

    foreach ($parseError in $parseErrors) {
      $failed = $true
      Write-Error "$($_.Name):$($parseError.Extent.StartLineNumber):$($parseError.Extent.StartColumnNumber): $($parseError.Message)"
    }
  }

  if ($failed) {
    throw "One or more PowerShell launcher scripts have syntax errors."
  }
}

$environmentNames = @(
  "HOME",
  "MAVEN_ARGS",
  "MAVEN_DEBUG_ADDRESS",
  "MAVEN_DEBUG_OPTS",
  "MAVEN_DEBUG_SCRIPT",
  "MAVEN_OPTS",
  "MAVEN_SKIP_RC",
  "PROGRAMDATA",
  "USERPROFILE",
  "YJPLIB"
)
$savedEnvironment = @{}
foreach ($name in $environmentNames) {
  $savedEnvironment[$name] = [Environment]::GetEnvironmentVariable($name)
}

try {
  New-Item -ItemType Directory -Path $temporaryRoot > $null
  $projectDirectory = Join-Path $temporaryRoot "project with spaces"
  $nestedDirectory = Join-Path $projectDirectory "nested"
  $mavenDirectory = Join-Path $projectDirectory ".mvn"
  New-Item -ItemType Directory -Path $nestedDirectory > $null
  New-Item -ItemType Directory -Path $mavenDirectory > $null

  [IO.File]::WriteAllText(
    (Join-Path $projectDirectory "pom.xml"),
    "<project xmlns=`"http://maven.apache.org/POM/4.0.0`"><modelVersion>4.0.0</modelVersion><groupId>test</groupId><artifactId>launcher</artifactId><version>1</version></project>"
  )
  [IO.File]::WriteAllLines(
    (Join-Path $mavenDirectory "jvm.config"),
    @(
      "-Dps.jvm=`"jvm value`"",
      "-Dps.pipe=`"foo|bar`"",
      "-Dps.at=@literal"
    )
  )

  foreach ($name in $environmentNames) {
    [Environment]::SetEnvironmentVariable($name, $null)
  }

  Test-ScriptSyntax
  Write-Output "[PASS] launcher scripts parse"

  $version = Invoke-Launcher -ScriptName "mvn.ps1" -Arguments @("--version")
  Assert-Equal 0 $version.ExitCode "mvn.ps1 --version should succeed."
  Assert-Contains $version.Output "Apache Maven" "Version output should identify Maven."
  Write-Output "[PASS] core launcher executes Maven"

  $invalid = Invoke-Launcher -ScriptName "mvn.ps1" -Arguments @("--not-a-real-option")
  if ($invalid.ExitCode -eq 0) {
    throw "Invalid Maven arguments should return a nonzero exit code."
  }
  Write-Output "[PASS] launcher propagates Maven failures"

  $env:MAVEN_DEBUG_SCRIPT = "1"
  $env:MAVEN_OPTS = "-Dps.maven=`"maven value`" -Dps.ampersand=`"foo&bar`""
  $env:MAVEN_ARGS = "-Dps.maven.args=`"argument value`""
  Push-Location $nestedDirectory
  try {
    $quoted = Invoke-Launcher -ScriptName "mvn.ps1" -Arguments @("--version")
  }
  finally {
    Pop-Location
  }
  Assert-Equal 0 $quoted.ExitCode "Launcher with quoted options should succeed."
  Assert-Contains $quoted.Output '"-Dps.maven=maven value"' "MAVEN_OPTS should preserve spaces."
  Assert-Contains $quoted.Output '"-Dps.jvm=jvm value"' "jvm.config should preserve spaces."
  Assert-Contains $quoted.Output "-Dps.pipe=foo|bar" "jvm.config should preserve pipe characters."
  Assert-Contains $quoted.Output "-Dps.at=@literal" "jvm.config should preserve at signs."
  Assert-Contains $quoted.Output '"-Dps.maven.args=argument value"' "MAVEN_ARGS should preserve spaces."
  Assert-Contains $quoted.Output "-Dmaven.multiModuleProjectDirectory=$projectDirectory" "The nearest .mvn directory should be used."
  Write-Output "[PASS] options, jvm.config, and project discovery preserve arguments"

  $fileResult = Invoke-Launcher -ScriptName "mvn.ps1" -Arguments @(
    "-f",
    (Join-Path $projectDirectory "pom.xml"),
    "--version"
  )
  Assert-Equal 0 $fileResult.ExitCode "-f with an existing POM should succeed."
  Assert-Contains $fileResult.Output "-Dmaven.multiModuleProjectDirectory=$projectDirectory" "-f should select the POM directory."

  $missingResult = Invoke-Launcher -ScriptName "mvn.ps1" -Arguments @(
    "-f",
    (Join-Path $temporaryRoot "missing.xml"),
    "--version"
  )
  Assert-Equal 1 $missingResult.ExitCode "-f with a missing POM should fail."
  Write-Output "[PASS] -f file selection and validation work"

  $env:MAVEN_DEBUG_OPTS = "-Dps.debug=true"
  $debugResult = Invoke-Launcher -ScriptName "mvnDebug.ps1" -Arguments @("--version")
  Assert-Equal 0 $debugResult.ExitCode "mvnDebug.ps1 should delegate successfully."
  Assert-Contains $debugResult.Output "deprecated for removal" "The debug wrapper should report deprecation."
  Write-Output "[PASS] debug wrapper delegates and preserves custom debug options"

  $env:MAVEN_ARGS = "-Dshould.not.reach.specialized.launchers=true"
  $modeTests = @(
    @("mvnenc.ps1", "org.apache.maven.cling.MavenEncCling"),
    @("mvnsh.ps1", "org.apache.maven.cling.MavenShellCling"),
    @("mvnup.ps1", "org.apache.maven.cling.MavenUpCling")
  )
  foreach ($modeTest in $modeTests) {
    $modeResult = Invoke-Launcher -ScriptName $modeTest[0] -Arguments @("--help")
    Assert-Equal 0 $modeResult.ExitCode "$($modeTest[0]) --help should succeed."
    Assert-Contains $modeResult.Output "-Dmaven.mainClass=$($modeTest[1])" "$($modeTest[0]) should select its MavenCling class."
    Assert-NotContains $modeResult.Output "-Dshould.not.reach.specialized.launchers=true" "MAVEN_ARGS should not reach specialized launchers."
  }
  Write-Output "[PASS] specialized wrappers select the correct MavenCling classes"

  $env:YJPLIB = $null
  $yjpResult = Invoke-Launcher -ScriptName "mvnyjp.ps1" -Arguments @("--version")
  Assert-Equal 1 $yjpResult.ExitCode "mvnyjp.ps1 should fail when YJPLIB is unavailable."
  Assert-Contains $yjpResult.Output "Please set YJPLIB" "YourKit failure should explain the required variable."
  Write-Output "[PASS] YourKit validation fails clearly"

  $testHome = Join-Path $temporaryRoot "test home"
  New-Item -ItemType Directory -Path $testHome > $null
  if ($env:OS -eq "Windows_NT" -or $PSVersionTable.PSEdition -eq "Desktop") {
    $env:PROGRAMDATA = Join-Path $temporaryRoot "program data"
    $env:USERPROFILE = $testHome
    New-Item -ItemType Directory -Path $env:PROGRAMDATA > $null
    $rcFile = Join-Path $testHome "mavenrc.ps1"
  }
  else {
    $env:HOME = $testHome
    $rcFile = Join-Path $testHome ".mavenrc.ps1"
  }
  [IO.File]::WriteAllText($rcFile, '$env:MAVEN_OPTS = "-Dps.rc=loaded"')
  $env:MAVEN_OPTS = $null
  $env:MAVEN_SKIP_RC = $null
  $rcResult = Invoke-Launcher -ScriptName "mvn.ps1" -Arguments @("--version")
  Assert-Equal 0 $rcResult.ExitCode "A user Maven RC script should load successfully."
  Assert-Contains $rcResult.Output "-Dps.rc=loaded" "The user Maven RC script should affect launcher options."

  $env:MAVEN_OPTS = $null
  $env:MAVEN_SKIP_RC = "1"
  $skipRcResult = Invoke-Launcher -ScriptName "mvn.ps1" -Arguments @("--version")
  Assert-Equal 0 $skipRcResult.ExitCode "MAVEN_SKIP_RC should not prevent Maven startup."
  Assert-NotContains $skipRcResult.Output "-Dps.rc=loaded" "MAVEN_SKIP_RC should suppress RC scripts."
  Write-Output "[PASS] user RC loading and MAVEN_SKIP_RC work"
}
finally {
  foreach ($name in $environmentNames) {
    [Environment]::SetEnvironmentVariable($name, $savedEnvironment[$name])
  }
  if (Test-Path -LiteralPath $temporaryRoot) {
    Remove-Item -LiteralPath $temporaryRoot -Recurse -Force
  }
}
