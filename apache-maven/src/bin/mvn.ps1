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

-----------------------------------------------------------------------------
Apache Maven Startup Script

Environment Variable Prerequisites

  JAVA_HOME       (Optional) Points to a Java installation.
  MAVEN_ARGS      (Optional) Arguments passed to Maven before CLI arguments.
  MAVEN_OPTS      (Optional) Java runtime options used when Maven is executed.
  MAVEN_SKIP_RC   (Optional) Flag to disable loading of mavenrc files.
-----------------------------------------------------------------------------
#>

# set title
$Host.UI.RawUI.WindowTitle = (Get-Variable MyInvocation -Scope Script).Value.InvocationName

if (-not $env:MAVEN_SKIP_RC) {
  if (Test-Path -Path $env:PROGRAMDATA\mavenrc.ps1 -PathType Leaf) { &$env:PROGRAMDATA"\mavenrc.ps1" $args }
  if (Test-Path -Path $env:USERPROFILE\mavenrc.ps1 -PathType Leaf) { &$env:USERPROFILE"\mavenrc.ps1" $args }
}

if (-not (Test-path $env:JAVA_HOME)) {
  $JAVACMD = (get-command java).Source 
  if (-not $JAVACMD) {
    Write-Error -ErrorAction Stop -Message "The $env:JAVA_HOME has not been set, JAVA_HOME environment variable is not defined correctly, so Apache Maven cannot be started."
  }
}
else {
  $JAVACMD = $env:JAVA_HOME + "\bin\java.exe"
}

if (-not (Test-Path $JAVACMD)) {
  Write-Error -Message "The JAVA_HOME environment variable is not defined correctly, so Apache Maven cannot be started."
  $ERROR_MESSAGE = "JAVA_HOME is set to " + $env:JAVA_HOME + ", but " + $JAVACMD + " does not exist."
  Write-Error -ErrorAction Stop -Message $ERROR_MESSAGE
}

# check mvn home
if (-not $env:MAVEN_HOME) {
    $env:MAVEN_HOME = (Get-Item $PSScriptRoot"\..")
}

# check if maven command exists
if (-not (Test-path $env:MAVEN_HOME"\bin\mvn.ps1")) {
    Write-Error -Message "maven command (\bin\mvn.ps1) cannot be found" -ErrorAction Stop
}
# ==== END VALIDATION ====

$CLASSWORLDS_CONF = $env:MAVEN_HOME + "\bin\m2.conf"

# Find the project basedir, i.e., the directory that contains the directory ".mvn".
# Fallback to current working directory if not found.

$WDIR = Get-Location

# Look for the --file switch and start the search for the .mvn directory from the specified
# POM location, if supplied.

$i = 0
$file_flag_found = $false
foreach ($arg in $args) {
  if (($arg -ceq "-f") || ($arg -ceq "--file")) {
    $file_flag_found = $true
    break
  }
  $i += 1
}

function IsNotRoot {
  param (
    [String] $path
  )

  return -not $path.endsWith(":\")
}

function RetrieveContentsJvmConfig {
  param (
    [String] $path
  )

  $jvm_location = $path + "\.mvn\jvm.config"

  if (Test-Path $jvm_location) {
    return $env:MAVEN_OPTS + (Get-Content $jvm_location).Replace("`n", "").Replace("`r", "");
  }
  return $env:MAVEN_OPTS;
}

$basedir = ""

if ($file_flag_found) {
  # we need to assess if the file exists
  # and then search for the maven project base dir. 
  $pom_file_reference = $args[$i + 1]

  if (Test-Path $pom_file_reference) {
    $basedir = (Get-Item $pom_file_reference).DirectoryName
  }
  else {
    $pom_file_error = "POM file " + $pom_file_reference + " specified the -f/--file command-line argument does not exist"
    Write-Error -Message $pom_file_error -ErrorAction Stop
  }
}
else {
  # if file flag is not found, then the pom.xml is relative to the working dir 
  # and the jvm.config can be found in the maven project base dir. 

  $basedir = $WDIR

  while (IsNotRoot($WDIR.Path)) {
    if (Test-Path $WDIR"\.mvn") {
      $basedir = $WDIR
      break
    }

    if ($WDIR) {
      $WDIR = Split-Path $WDIR      
    }
    else {
      break
    }  
  }
}

$MAVEN_OPTS = (RetrieveContentsJvmConfig $basedir)

$LAUNCHER_JAR = Get-Item $env:MAVEN_HOME"\boot\plexus-classworlds*.jar"
$LAUNCHER_CLASS = "org.codehaus.plexus.classworlds.launcher.Launcher"

& $JAVACMD `
  $MAVEN_OPTS `
  $MAVEN_DEBUG_OPTS `
  -classpath $LAUNCHER_JAR `
  "-Dclassworlds.conf=$CLASSWORLDS_CONF" `
  "-Dmaven.home=$env:MAVEN_HOME" `
  "-Dlibrary.jansi.path=$env:MAVEN_HOME\lib\jansi-native" `
  "-Dmaven.multiModuleProjectDirectory=$basedir" `
  $LAUNCHER_CLASS `
  $MAVEN_ARGS `
  $args


