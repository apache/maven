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

