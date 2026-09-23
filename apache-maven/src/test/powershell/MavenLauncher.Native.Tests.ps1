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
    if (@($lines | Where-Object { $_.StartsWith("arg=") }).Count -ne $values.Count) {
      throw "Java received the wrong argument count in $mode mode"
    }
    foreach ($value in $values) { Assert-Line $lines ("arg=" + (Encode-Value $value)) }
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
