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
$temporaryRoot = Join-Path ([IO.Path]::GetTempPath()) ("maven-native-tests-" + [guid]::NewGuid().ToString("N"))
$fixture = Join-Path $temporaryRoot "Maven home with spaces"
$bin = Join-Path $fixture "bin"
$outputFile = Join-Path $temporaryRoot "java output.txt"
$environmentNames = @("MAVEN_OPTS", "MAVEN_ARGS", "MAVEN_DEBUG_OPTS", "MAVEN_DEBUG_SCRIPT", "MAVEN_SKIP_RC", "MAVEN_POWERSHELL_EXECUTABLE", "JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS")
$savedEnvironment = @{}
foreach ($name in $environmentNames) {
  $savedEnvironment[$name] = [Environment]::GetEnvironmentVariable($name)
  [Environment]::SetEnvironmentVariable($name, $null)
}
$savedPassing = Get-Variable PSNativeCommandArgumentPassing -ValueOnly -ErrorAction SilentlyContinue

function Encode-Option {
  param([string] $Value)
  return "'" + $Value.Replace("'", "'" + '"' + "'" + '"' + "'") + "'"
}

function Encode-Value {
  param([AllowEmptyString()][string] $Value)
  return [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($Value))
}

function Assert-Line {
  param([string[]] $Lines, [string] $Expected)
  if ($Lines -cnotcontains $Expected) {
    throw "Java did not receive <$Expected>. Actual: $($Lines -join '; ')"
  }
}

function Assert-Arguments {
  param([string[]] $Lines, [AllowEmptyString()][string[]] $Values)
  $actual = @($Lines | Where-Object { $_.StartsWith("arg=") })
  $expected = @($Values | ForEach-Object { "arg=" + (Encode-Value $_) })
  if (($actual -join "`n") -cne ($expected -join "`n")) {
    throw "Java received different arguments: $($actual -join '; '), expected: $($expected -join '; ')"
  }
}

function Invoke-Host {
  param([string[]] $Arguments, [string] $InputText = "", [int] $ExpectedExit = 7)

  $hostName = if ($PSVersionTable.PSEdition -eq "Desktop") { "powershell.exe" } elseif ($env:OS -eq "Windows_NT") { "pwsh.exe" } else { "pwsh" }
  $start = New-Object Diagnostics.ProcessStartInfo
  $start.FileName = Join-Path $PSHOME $hostName
  $start.UseShellExecute = $false
  $start.RedirectStandardInput = $true
  $start.RedirectStandardOutput = $true
  $start.RedirectStandardError = $true
  $start.WorkingDirectory = $temporaryRoot
  # ProcessStartInfo.ArgumentList is unavailable on Windows PowerShell 5.1.
  $quoted = @($Arguments | ForEach-Object {
      '"' + [regex]::Replace([regex]::Replace($_, '(\\*)"', '$1$1\"'), '(\\+)$', '$1$1') + '"'
    })
  $start.Arguments = $quoted -join " "
  $process = New-Object Diagnostics.Process
  $process.StartInfo = $start
  try {
    if (Test-Path -LiteralPath $outputFile) { Remove-Item -LiteralPath $outputFile }
    [void] $process.Start()
    $processId = $process.Id
    $stdout = $process.StandardOutput.ReadToEndAsync()
    $stderr = $process.StandardError.ReadToEndAsync()
    $process.StandardInput.Write($InputText)
    $process.StandardInput.Close()
    if (-not $process.WaitForExit(20000)) {
      $process.Kill()
      throw "PowerShell child did not finish"
    }
    $result = [pscustomobject]@{
      ProcessId = $processId
      ExitCode = $process.ExitCode
      Output = $stdout.GetAwaiter().GetResult()
      Error = $stderr.GetAwaiter().GetResult()
    }
    if ($result.ExitCode -ne $ExpectedExit) { throw "Expected child exit ${ExpectedExit}: $($result | Out-String)" }
    return $result
  }
  finally {
    $process.Dispose()
  }
}

function Assert-Process {
  param($Result, [bool] $Replaced)
  $lines = [IO.File]::ReadAllLines($outputFile)
  $javaPid = ($lines | Where-Object { $_.StartsWith("pid=") }) -replace '^pid=', ''
  if (($javaPid -eq "$($Result.ProcessId)") -ne $Replaced) {
    throw "Unexpected process replacement: PowerShell=$($Result.ProcessId), Java=$javaPid, expected=$Replaced"
  }
  if (-not $Result.Output.Contains("fixture-stdout") -or -not $Result.Error.Contains("fixture-stderr")) {
    throw "Child streams were not preserved: $($Result | Out-String)"
  }
}

try {
  New-Item -ItemType Directory -Path $bin -Force > $null
  New-Item -ItemType Directory -Path (Join-Path $fixture "boot") > $null
  Copy-Item -Path (Join-Path $MavenHome "bin/*.ps1") -Destination $bin
  Copy-Item -LiteralPath (Join-Path $MavenHome "bin/JvmConfigParser.java") -Destination $bin
  $source = Join-Path $temporaryRoot "Launcher.java"
  [IO.File]::WriteAllText($source, @'
package org.codehaus.plexus.classworlds.launcher;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
public class Launcher {
    static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
    public static void main(String[] args) throws Exception {
        List<String> lines = new ArrayList<>();
        for (String arg : args) lines.add("arg=" + encode(arg));
        lines.add("property=" + encode(System.getProperty("probe.value", "")));
        lines.add("cwd=" + encode(Paths.get("").toAbsolutePath().toString()));
        lines.add("pid=" + ProcessHandle.current().pid());
        lines.add("host=" + encode(System.getenv("MAVEN_POWERSHELL_EXECUTABLE")));
        if (Arrays.asList(args).contains("--read-stdin")) {
            lines.add("stdin=" + encode(new java.io.BufferedReader(new java.io.InputStreamReader(
                    System.in, StandardCharsets.UTF_8)).readLine()));
        }
        Files.write(Paths.get(System.getProperty("probe.output")), lines, StandardCharsets.UTF_8);
        System.out.println("fixture-stdout");
        System.err.println("fixture-stderr");
        if (Arrays.asList(args).contains("--fail")) System.exit(7);
    }
}
'@)
  $javaBin = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin" } else { Split-Path (Get-Command java -CommandType Application).Source }
  & (Join-Path $javaBin "javac") -d $temporaryRoot $source
  if ($LASTEXITCODE -ne 0) { throw "Could not compile launcher argument fixture" }
  & (Join-Path $javaBin "jar") cf (Join-Path $fixture "boot/plexus-classworlds-fixture.jar") -C $temporaryRoot org
  if ($LASTEXITCODE -ne 0) { throw "Could not package launcher argument fixture" }

  $env:MAVEN_SKIP_RC = "true"
  $property = 'space "quote" and apostrophe '' with C:\path\ $HOME & | @literal'
  $env:MAVEN_OPTS = (Encode-Option "-Dprobe.output=$outputFile") + " " + (Encode-Option "-Dprobe.value=$property")
  $values = @("a b", 'a"b', "a'b", 'a\"b', 'a\\"b', "", 'C:\folder with spaces\', '$HOME & | ; @arg', ([string][char]0x17e + "lutoucky"))
  $modes = @("Legacy")
  if ($null -ne $savedPassing) { $modes += "Standard" }
  foreach ($mode in $modes) {
    $PSNativeCommandArgumentPassing = $mode
    & (Join-Path $bin "mvn.ps1") @values
    if ($LASTEXITCODE -ne 0) { throw "Launcher failed in $mode argument mode" }
    $lines = [IO.File]::ReadAllLines($outputFile)
    Assert-Arguments $lines $values
    Assert-Line $lines ("property=" + (Encode-Value $property))
    Write-Output "[PASS] Java receives exact arguments and JVM properties in $mode mode"
  }

  # Empty quoted MAVEN_ARGS must survive the option parser as an argument.
  $env:MAVEN_ARGS = "''"
  & (Join-Path $bin "mvn.ps1")
  if ($LASTEXITCODE -ne 0) { throw "Empty quoted argument launch failed" }
  Assert-Line ([IO.File]::ReadAllLines($outputFile)) "arg="
  $env:MAVEN_ARGS = $null
  Write-Output "[PASS] option parsing preserves an empty quoted argument"

  $canReplace = $env:OS -ne "Windows_NT" -and $PSVersionTable.PSEdition -ne "Desktop" -and
    $PSVersionTable.PSVersion -ge [version] "7.3" -and
    [bool](Get-Command "Microsoft.PowerShell.Core\Switch-Process" -CommandType Cmdlet -ErrorAction SilentlyContinue)
  $env:MAVEN_DEBUG_OPTS = "-Dprobe.debug=true"
  foreach ($name in @("mvn.ps1", "mvnDebug.ps1", "mvnenc.ps1", "mvnsh.ps1", "mvnup.ps1")) {
    $result = Invoke-Host -Arguments (@("-NoProfile", "-File", (Join-Path $bin $name)) + $values + @("--fail", "-NoExit"))
    Assert-Process $result $canReplace
    $lines = [IO.File]::ReadAllLines($outputFile)
    $mode = @{ "mvnDebug.ps1" = "--debug"; "mvnenc.ps1" = "--enc"; "mvnsh.ps1" = "--shell"; "mvnup.ps1" = "--up" }
    $expectedArguments = @()
    if ($mode.ContainsKey($name)) { $expectedArguments += $mode[$name] }
    Assert-Arguments $lines ($expectedArguments + $values + @("--fail", "-NoExit"))
    Assert-Line $lines ("property=" + (Encode-Value $property))
    Assert-Line $lines ("cwd=" + (Encode-Value $temporaryRoot))
  }
  Write-Output "[PASS] dedicated launcher and wrapper processes preserve PID policy, arguments, streams and exit status"

  $result = Invoke-Host -Arguments @("-NoProfile", "-File", (Join-Path $bin "mvn.ps1"), "--read-stdin", "--fail") -InputText "input-marker`n"
  Assert-Process $result $canReplace
  Assert-Line ([IO.File]::ReadAllLines($outputFile)) ("stdin=" + (Encode-Value "input-marker"))
  Write-Output "[PASS] Java receives redirected stdin"

  foreach ($prefix in @(@("-NoProfile", "-f"), @("-NoProfile"))) {
    $result = Invoke-Host -Arguments (@($prefix) + @((Join-Path $bin "mvn.ps1"), "--fail"))
    Assert-Process $result $canReplace
  }
  $result = Invoke-Host -ExpectedExit 0 -Arguments @("-NoProfile", "-NoExit", "-File", (Join-Path $bin "mvn.ps1"), "--fail")
  Assert-Process $result $false

  $command = "& '" + (Join-Path $bin "mvn.ps1") + "' --fail; " + '$code = $LASTEXITCODE; Write-Output caller-resumed; exit $code'
  $caller = Join-Path $temporaryRoot "caller.ps1"
  [IO.File]::WriteAllText($caller, $command)
  $calls = @(
    @("-NoProfile", "-Command", $command),
    @("-NoProfile", "-EncodedCommand", [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($command))),
    @("-NoProfile", "-File", $caller)
  )
  foreach ($call in $calls) {
    $result = Invoke-Host -Arguments $call
    Assert-Process $result $false
    if (-not $result.Output.Contains("caller-resumed")) { throw "Caller did not resume" }
  }
  $result = Invoke-Host -Arguments @("-NoProfile", "-File", "-") -InputText ($command + [Environment]::NewLine)
  Assert-Process $result $false
  if (-not $result.Output.Contains("caller-resumed")) { throw "Stdin caller did not resume" }
  Write-Output "[PASS] existing sessions, caller scripts, stdin scripts and -NoExit keep child-process execution"

  if ($canReplace) {
    $savedHome = $env:HOME
    try {
      $rcHome = Join-Path $temporaryRoot "rc home"
      $changedDirectory = Join-Path $temporaryRoot "changed directory"
      New-Item -ItemType Directory -Path $rcHome, $changedDirectory > $null
      [IO.File]::WriteAllText((Join-Path $rcHome ".mavenrc.ps1"), "Set-Location -LiteralPath '$changedDirectory'")
      $env:HOME = $rcHome
      $env:MAVEN_SKIP_RC = $null
      $result = Invoke-Host -Arguments @("-NoProfile", "-File", (Join-Path $bin "mvn.ps1"), "--fail")
      Assert-Process $result $true
      Assert-Line ([IO.File]::ReadAllLines($outputFile)) ("cwd=" + (Encode-Value $changedDirectory))
      Write-Output "[PASS] process replacement uses the PowerShell filesystem location after RC loading"
    }
    finally {
      $env:HOME = $savedHome
      $env:MAVEN_SKIP_RC = "true"
    }
  }

}
finally {
  if ($null -ne $savedPassing) { $PSNativeCommandArgumentPassing = $savedPassing }
  foreach ($name in $environmentNames) {
    [Environment]::SetEnvironmentVariable($name, $savedEnvironment[$name])
  }
  if (Test-Path -LiteralPath $temporaryRoot) {
    Remove-Item -LiteralPath $temporaryRoot -Recurse -Force
  }
}
