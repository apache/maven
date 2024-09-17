# ==== END VALIDATION ====

$CLASSWORLDS_CONF = "$MAVEN_HOME\bin\m2.conf"

# Find the project basedir, i.e., the directory that contains the directory ".mvn".
# Fallback to current working directory if not found.

$WDIR = Get-Location

# Look for the --file switch and start the search for the .mvn directory from the specified
# POM location, if supplied.

$i = 0
$file_flag_found = $false
foreach ($arg in $args) {
  if (($arg -ceq "-f") -or ($arg -ceq "--file")) {
    $file_flag_found = $true
    break
  }
  $i += 1
}

function IsNotRoot {
  param (
    [String] $path
  )
  $root_path_base_name = ((get-item $path).Root.BaseName
  $path_base_name = ((get-item $path).BaseName

  return $root_path_base_name -ne $path_base_name
}

function RetrieveContentsJvmConfig {
  param (
    [String] $path
  )

  $jvm_config = $path + "\.mvn\jvm.config"

  if (Test-Path $jvm_config) {
    return $env:MAVEN_OPTS + " " + -Join (Get-Content $jvm_config)
  }
  return $env:MAVEN_OPTS
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
    if (Test-Path "$WDIR\.mvn") {
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

