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
Apache Maven Startup Script

Environment Variable Prerequisites

  JAVA_HOME       (Optional) Points to a Java installation.
  MAVEN_ARGS      (Optional) Arguments passed to Maven before CLI arguments.
  MAVEN_OPTS      (Optional) Java runtime options used when Maven is executed.
  MAVEN_SKIP_RC   (Optional) Flag to disable loading of mavenrc files.
  MAVEN_DEBUG_OPTS    (Optional) Specify the debug options used by --debug.
  MAVEN_DEBUG_ADDRESS (Optional) Debug address. Default is localhost:8000.
-----------------------------------------------------------------------------#>

$script:IsWindowsPlatform = $env:OS -eq "Windows_NT" -or $PSVersionTable.PSEdition -eq "Desktop"

function Write-MavenError {
  param([string] $Message)

  [Console]::Error.WriteLine($Message)
}

function Write-MavenDebug {
  param([string] $Message)

  if ($env:MAVEN_DEBUG_SCRIPT) {
    [Console]::Error.WriteLine("[DEBUG] $Message")
  }
}

function Import-MavenRc {
  if ($env:MAVEN_SKIP_RC) {
    return
  }

  [string[]] $rcFiles = @()
  if ($script:IsWindowsPlatform) {
    if ($env:PROGRAMDATA) {
      $rcFiles += Join-Path $env:PROGRAMDATA "mavenrc.ps1"
    }
    if ($env:USERPROFILE) {
      $rcFiles += Join-Path $env:USERPROFILE "mavenrc.ps1"
    }
  }
  else {
    $rcFiles += "/usr/local/etc/mavenrc.ps1"
    $rcFiles += "/etc/mavenrc.ps1"
    if ($env:HOME) {
      $rcFiles += Join-Path $env:HOME ".mavenrc.ps1"
    }
  }

  foreach ($rcFile in $rcFiles) {
    if (Test-Path -LiteralPath $rcFile -PathType Leaf) {
      Write-MavenDebug "Loading Maven RC file: $rcFile"
      . $rcFile
    }
  }
}

function ConvertFrom-MavenOptionString {
  param([AllowEmptyString()][string] $Value)

  $result = New-Object "System.Collections.Generic.List[string]"
  if ([string]::IsNullOrWhiteSpace($Value)) {
    return $result.ToArray()
  }

  $current = New-Object System.Text.StringBuilder
  $inDoubleQuotes = $false
  $inSingleQuotes = $false

  foreach ($character in $Value.ToCharArray()) {
    if ($character -eq '"' -and -not $inSingleQuotes) {
      $inDoubleQuotes = -not $inDoubleQuotes
    }
    elseif ($character -eq "'" -and -not $inDoubleQuotes) {
      $inSingleQuotes = -not $inSingleQuotes
    }
    elseif ([char]::IsWhiteSpace($character) -and -not $inDoubleQuotes -and -not $inSingleQuotes) {
      if ($current.Length -gt 0) {
        $result.Add($current.ToString())
        [void] $current.Clear()
      }
    }
    else {
      [void] $current.Append($character)
    }
  }

  if ($current.Length -gt 0) {
    $result.Add($current.ToString())
  }

  return $result.ToArray()
}

function Get-MavenJavaCommand {
  $javaExecutable = if ($script:IsWindowsPlatform) { "java.exe" } else { "java" }

  if ($env:JAVA_HOME) {
    $javaCommand = Join-Path (Join-Path $env:JAVA_HOME "bin") $javaExecutable
    if (-not (Test-Path -LiteralPath $javaCommand -PathType Leaf)) {
      Write-MavenError "The JAVA_HOME environment variable is not defined correctly, so Apache Maven cannot be started."
      Write-MavenError "JAVA_HOME is set to `"$env:JAVA_HOME`", but `"$javaCommand`" does not exist."
      throw "Java executable not found"
    }
    return (Get-Item -LiteralPath $javaCommand).FullName
  }

  $java = Get-Command $javaExecutable -CommandType Application -ErrorAction SilentlyContinue |
    Select-Object -First 1
  if (-not $java) {
    Write-MavenError "The java command does not exist in PATH nor is JAVA_HOME set, so Apache Maven cannot be started."
    throw "Java executable not found"
  }

  return $java.Source
}

function Test-MavenJavaVersion {
  param([string] $JavaCommand)

  $originalErrorActionPreference = $ErrorActionPreference
  try {
    # Windows PowerShell 5.1 reports native stderr as an error record when it is redirected.
    # java -version writes its normal output to stderr, so do not let a caller's Stop preference abort the probe.
    $ErrorActionPreference = "Continue"
    & $JavaCommand --enable-native-access=ALL-UNNAMED -version *> $null
    $javaVersionExitCode = $LASTEXITCODE
    if ($javaVersionExitCode -ne 0) {
      Write-MavenError "Error: Apache Maven 4.x requires Java 17 or newer to run."
      & $JavaCommand -version
      Write-MavenError "Please upgrade your Java installation or set JAVA_HOME to point to a compatible JDK."
      throw "Unsupported Java version"
    }
  }
  finally {
    $ErrorActionPreference = $originalErrorActionPreference
  }
}

function Get-MavenHome {
  return (Get-Item -LiteralPath (Join-Path $PSScriptRoot "..")).FullName
}

function Get-FileArgumentBaseDirectory {
  param([string[]] $Arguments)

  $baseDirectory = (Get-Location).ProviderPath
  for ($index = 0; $index -lt $Arguments.Count; $index++) {
    if ($Arguments[$index] -ne "-f" -and $Arguments[$index] -ne "--file") {
      continue
    }

    if ($index + 1 -ge $Arguments.Count) {
      return $baseDirectory
    }

    $fileArgument = $Arguments[$index + 1]
    if (-not (Test-Path -LiteralPath $fileArgument)) {
      Write-MavenError "POM file $fileArgument specified with the -f/--file command line argument does not exist"
      throw "POM file not found"
    }

    $item = Get-Item -LiteralPath $fileArgument
    if ($item.PSIsContainer) {
      return $item.FullName
    }

    if (-not $item.Directory) {
      Write-MavenError "Directory extracted from the -f/--file command-line argument $fileArgument does not exist"
      throw "POM directory not found"
    }
    return $item.Directory.FullName
  }

  return $baseDirectory
}

function Get-MavenProjectBaseDirectory {
  param([string[]] $Arguments)

  $baseDirectory = Get-FileArgumentBaseDirectory -Arguments $Arguments
  $searchDirectory = Get-Item -LiteralPath $baseDirectory
  while ($searchDirectory) {
    if (Test-Path -LiteralPath (Join-Path $searchDirectory.FullName ".mvn") -PathType Container) {
      return $searchDirectory.FullName
    }

    $parent = $searchDirectory.Parent
    if (-not $parent -or $parent.FullName -eq $searchDirectory.FullName) {
      break
    }
    $searchDirectory = $parent
  }

  return (Get-Item -LiteralPath $baseDirectory).FullName
}

function Get-MavenLauncherJar {
  param([string] $MavenHome)

  $launcherJars = @(Get-ChildItem -LiteralPath (Join-Path $MavenHome "boot") -Filter "plexus-classworlds-*.jar")
  if ($launcherJars.Count -ne 1) {
    Write-MavenError "Expected one plexus-classworlds launcher JAR in `"$MavenHome`", found $($launcherJars.Count)."
    throw "Maven launcher JAR not found"
  }

  return $launcherJars[0].FullName
}

function Get-MavenJvmConfigArguments {
  param(
    [string] $JavaCommand,
    [string] $MavenHome,
    [string] $ProjectBaseDirectory
  )

  $jvmConfig = Join-Path (Join-Path $ProjectBaseDirectory ".mvn") "jvm.config"
  if (-not (Test-Path -LiteralPath $jvmConfig -PathType Leaf)) {
    return @()
  }

  $parser = Join-Path (Join-Path $MavenHome "bin") "JvmConfigParser.java"
  Write-MavenDebug "Found jvm.config file at: $jvmConfig"
  Write-MavenDebug "Running JvmConfigParser with Java: $JavaCommand"

  $parserOutput = @(& $JavaCommand $parser $jvmConfig $ProjectBaseDirectory 2>&1)
  $parserExitCode = $LASTEXITCODE
  Write-MavenDebug "JvmConfigParser exit code: $parserExitCode"
  if ($parserExitCode -ne 0) {
    Write-MavenError "ERROR: JvmConfigParser failed with exit code $parserExitCode"
    Write-MavenError "  jvm.config path: $jvmConfig"
    Write-MavenError "  Maven basedir: $ProjectBaseDirectory"
    Write-MavenError "  Java command: $JavaCommand"
    foreach ($line in $parserOutput) {
      Write-MavenError "    $line"
    }
    throw "JVM configuration parsing failed"
  }

  $joinedOutput = ($parserOutput | ForEach-Object { "$_" }) -join [Environment]::NewLine
  return @(ConvertFrom-MavenOptionString -Value $joinedOutput)
}

function Format-MavenDebugArgument {
  param([string] $Argument)

  if ($Argument -match '[\s"]') {
    return '"' + $Argument.Replace('"', '\"') + '"'
  }
  return $Argument
}

function Invoke-MavenLauncher {
  param([string[]] $Arguments)

  Import-MavenRc

  $mavenHome = Get-MavenHome
  $javaCommand = Get-MavenJavaCommand
  Test-MavenJavaVersion -JavaCommand $javaCommand

  $projectBaseDirectory = Get-MavenProjectBaseDirectory -Arguments $Arguments
  $launcherJar = Get-MavenLauncherJar -MavenHome $mavenHome
  $classworldsConfig = Join-Path (Join-Path $mavenHome "bin") "m2.conf"
  $jlineNativeDirectory = Join-Path (Join-Path $mavenHome "lib") "jline-native"

  [string[]] $internalMavenOptions = @()
  [string[]] $mavenOptions = @(ConvertFrom-MavenOptionString -Value $env:MAVEN_OPTS)
  [string[]] $jvmConfigOptions = @(Get-MavenJvmConfigArguments `
      -JavaCommand $javaCommand `
      -MavenHome $mavenHome `
      -ProjectBaseDirectory $projectBaseDirectory)
  [string[]] $debugOptions = @(ConvertFrom-MavenOptionString -Value $env:MAVEN_DEBUG_OPTS)
  $mainClass = "org.apache.maven.cling.MavenCling"

  foreach ($argument in $Arguments) {
    switch ($argument) {
      "--debug" {
        if ($debugOptions.Count -eq 0) {
          $debugAddress = if ($env:MAVEN_DEBUG_ADDRESS) { $env:MAVEN_DEBUG_ADDRESS } else { "localhost:8000" }
          $debugOptions = @("-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=$debugAddress")
        }
        else {
          Write-Output "Ignoring --debug option as MAVEN_DEBUG_OPTS is already set"
        }
      }
      "--yjp" {
        if (-not $env:YJPLIB -or -not (Test-Path -LiteralPath $env:YJPLIB -PathType Leaf)) {
          Write-MavenError "Error: Unable to autodetect the YJP library location. Please set YJPLIB variable"
          throw "YourKit library not found"
        }
        $internalMavenOptions += "-agentpath:$env:YJPLIB=onexit=snapshot,onexit=memory,tracing,onlylocal"
      }
      "--enc" {
        $mainClass = "org.apache.maven.cling.MavenEncCling"
      }
      "--shell" {
        $mainClass = "org.apache.maven.cling.MavenShellCling"
      }
      "--up" {
        $mainClass = "org.apache.maven.cling.MavenUpCling"
      }
    }
  }

  [string[]] $javaArguments = @()
  $javaArguments += $internalMavenOptions
  $javaArguments += $mavenOptions
  $javaArguments += $jvmConfigOptions
  $javaArguments += $debugOptions
  $javaArguments += @(
    "--enable-native-access=ALL-UNNAMED"
    "-classpath"
    $launcherJar
    "-Dclassworlds.conf=$classworldsConfig"
    "-Dmaven.home=$mavenHome"
    "-Dmaven.mainClass=$mainClass"
    "-Dlibrary.jline.path=$jlineNativeDirectory"
    "-Dmaven.multiModuleProjectDirectory=$projectBaseDirectory"
    "org.codehaus.plexus.classworlds.launcher.Launcher"
  )
  if ($mainClass -eq "org.apache.maven.cling.MavenCling") {
    $javaArguments += @(ConvertFrom-MavenOptionString -Value $env:MAVEN_ARGS)
  }
  $javaArguments += $Arguments

  if ($env:MAVEN_DEBUG_SCRIPT) {
    $debugCommand = (@($javaCommand) + $javaArguments |
        ForEach-Object { Format-MavenDebugArgument -Argument $_ }) -join " "
    Write-MavenDebug "Launching JVM with command:"
    Write-MavenDebug "  $debugCommand"
  }

  # Native stderr does not indicate failure. In Windows PowerShell 5.1 it can become an error record
  # when the caller redirects output, so let Java finish and use its exit code as the result.
  $ErrorActionPreference = "Continue"
  & $javaCommand @javaArguments
  $script:MavenProcessExitCode = $LASTEXITCODE
}

try {
  Invoke-MavenLauncher -Arguments $args
  exit $script:MavenProcessExitCode
}
catch {
  if ($_.Exception.Message -and
      $_.Exception.Message -notmatch "^(Java executable|Unsupported Java|POM|Maven launcher|JVM configuration|YourKit)") {
    Write-MavenError $_.Exception.Message
  }
  exit 1
}
