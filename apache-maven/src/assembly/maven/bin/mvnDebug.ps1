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

<#-----------------------------------------------------------------------------
Apache Maven Debug Script

Environment Variable Prerequisites

JAVA_HOME           (Optional) Points to a Java installation.
MAVEN_OPTS          (Optional) Java runtime options used when Maven is executed.
MAVEN_SKIP_RC       (Optional) Flag to disable loading of mavenrc files.
MAVEN_DEBUG_ADDRESS (Optional) Set the debug address. Default value is localhost:8000
-----------------------------------------------------------------------------
#>

# set title
$Host.UI.RawUI.WindowTitle = $MyInvocation.MyCommand

if (-not $env:MAVEN_DEBUG_ADDRESS ) {
  $env:MAVEN_DEBUG_ADDRESS = "localhost:8000"
}

$env:MAVEN_DEBUG_OPTS = "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=$env:MAVEN_DEBUG_ADDRESS"

$mvnCmd = join-path -Path $PSScriptRoot -ChildPath "mvn.ps1"

&$mvnCmd $args
