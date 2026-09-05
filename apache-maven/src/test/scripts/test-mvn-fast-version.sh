#!/bin/sh

# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# -----------------------------------------------------------------------------
# Tests the fast version path in bin/mvn that renders the version banner
# without starting Maven itself.
#
# The test runs on any POSIX platform: a throw-away Maven home is populated with
# a fake java stub that responds to -XshowSettings with synthetic properties and
# a maven.version.properties file.  The script then asserts that -v/--version
# prints the expected banner, -q/--quiet prints only the version, -V prints the
# banner but also launches Maven (via the -Dmaven.version.printed flag), and
# that -X or --enc bypasses the fast path.
#
# Usage: sh apache-maven/src/test/scripts/test-mvn-fast-version.sh
#
# Exits with 0 when all assertions pass, 1 otherwise.
# -----------------------------------------------------------------------------

set -e

script_dir=`cd "\`dirname "$0"\`" && pwd`
mvn_script="$script_dir/../../assembly/maven/bin/mvn"

if [ ! -f "$mvn_script" ]; then
  echo "Cannot locate the mvn script at $mvn_script" >&2
  exit 1
fi

sh_bin=`unset -f command; command -v sh`

work_dir=`mktemp -d "${TMPDIR:-/tmp}/mvn-fast-version.XXXXXX"`
work_dir=`cd "$work_dir" && pwd`
trap 'rm -rf "$work_dir"' EXIT INT TERM

maven_home="$work_dir/maven-home"
mkdir -p "$maven_home/bin" "$maven_home/boot" "$maven_home/lib"

cp "$mvn_script" "$maven_home/bin/mvn"
touch "$maven_home/boot/plexus-classworlds-9.9.9.jar"
# Minimal jar for -v --enc fallback (not used in fast path, but needed for
# the non-fast-path case where MAVEN_MAIN_CLASS is set).
touch "$maven_home/lib/maven-core-9.9.9.jar"

# Create the precomputed version properties file.
cat > "$maven_home/bin/maven.version.properties" <<'EOF'
buildNumber=abc123def
version=4.1.0-SNAPSHOT
distributionId=apache-maven
distributionShortName=Maven
distributionName=Apache Maven
EOF

# java stub: responds to -XshowSettings:properties -version by writing synthetic
# JVM properties to stderr (matching the real format), and to -version by
# exiting 0.  When invoked for a full Maven launch it prints the arguments so
# assertions can verify what was passed.
cat > "$maven_home/bin/java" <<'STUB'
#!/bin/sh
# Full settings dump on stderr (real format: "    key = value").
case " $* " in
  *" -XshowSettings:properties -version "*)
    printf '    java.version = 17.0.20.1\n' >&2
    printf '    java.vendor = Azul Systems, Inc.\n' >&2
    printf '    java.home = /usr/lib/jvm/zulu17\n' >&2
    printf '    user.language = en\n' >&2
    printf '    user.country = US\n' >&2
    printf '    file.encoding = UTF-8\n' >&2
    printf '    os.name = Linux\n' >&2
    printf '    os.version = 6.8.0-45-generic\n' >&2
    printf '    os.arch = amd64\n' >&2
    exit 0 ;;
  *" -version "*) exit 0 ;;
esac

# Fallback: echo arguments for full-Maven launch assertions.
for arg in "$@"; do printf '[%s]' "$arg"; done
printf '\n'
STUB

chmod +x "$maven_home/bin/java" "$maven_home/bin/mvn"

failures=0

# run_mvn <extra args...>
run_mvn() {
  ( cd "$work_dir" &&
    PATH="$maven_home/bin:$PATH" JAVA_HOME= MAVEN_SKIP_RC=1 \
      "$sh_bin" "$maven_home/bin/mvn" "$@" 2>/dev/null )
}

# run_mvn_stderr <extra args...>
# Same, but captures stderr (the version banner goes to stdout via the script).
run_mvn_stderr() {
  ( cd "$work_dir" &&
    PATH="$maven_home/bin:$PATH" JAVA_HOME= MAVEN_SKIP_RC=1 \
      "$sh_bin" "$maven_home/bin/mvn" "$@" )
}

# assert_eq <description> <expected> <actual>
assert_eq() {
  if [ "$2" = "$3" ]; then
    printf 'ok - %s\n' "$1"
  else
    printf 'FAILED - %s\n' "$1"
    printf '  expected: %s\n' "$2"
    printf '  actual:   %s\n' "$3"
    failures=`expr $failures + 1`
  fi
}

# assert_contains <description> <haystack> <needle>
assert_contains() {
  case "$2" in
    *"$3"*) printf 'ok - %s\n' "$1" ;;
    *)
      printf 'FAILED - %s\n' "$1"
      printf '  expected to contain: %s\n' "$3"
      printf '  actual: %s\n' "$2"
      failures=`expr $failures + 1`
      ;;
  esac
}

# assert_not_contains <description> <haystack> <needle>
assert_not_contains() {
  case "$2" in
    *"$3"*)
      printf 'FAILED - %s\n' "$1"
      printf '  expected NOT to contain: %s\n' "$3"
      printf '  actual: %s\n' "$2"
      failures=`expr $failures + 1`
      ;;
    *) printf 'ok - %s\n' "$1" ;;
  esac
}

# ---------------------------------------------------------------------------
# Test: -v prints the full banner
# ---------------------------------------------------------------------------
output=`run_mvn -v`

assert_contains "-v: prints Maven name and version" "$output" "Apache Maven 4.1.0-SNAPSHOT"
assert_contains "-v: prints buildNumber" "$output" "abc123def"
assert_contains "-v: prints Maven home" "$output" "Maven home: $maven_home"
assert_contains "-v: prints Java version" "$output" "Java version: 17.0.20.1"
assert_contains "-v: prints Java vendor" "$output" "vendor: Azul Systems, Inc."
assert_contains "-v: prints Java runtime" "$output" "runtime: /usr/lib/jvm/zulu17"
assert_contains "-v: prints locale" "$output" "Default locale: en_US"
assert_contains "-v: prints encoding" "$output" "platform encoding: UTF-8"
assert_contains "-v: prints OS name" "$output" "OS name: \"linux\""
assert_contains "-v: prints OS arch" "$output" "arch: \"amd64\""
assert_contains "-v: prints OS family" "$output" "family: \"unix\""

# --version should produce identical output.
output_long=`run_mvn --version`
assert_eq "--version produces same output as -v" "$output" "$output_long"

# ---------------------------------------------------------------------------
# Test: -q/--quiet prints only the version string
# ---------------------------------------------------------------------------
output_q=`run_mvn -q -v`
assert_eq "-q -v: prints only the version string" "4.1.0-SNAPSHOT" "$output_q"

output_q_long=`run_mvn --quiet --version`
assert_eq "--quiet --version: prints only the version string" "4.1.0-SNAPSHOT" "$output_q_long"

# ---------------------------------------------------------------------------
# Test: -V/--show-version prints banner AND launches Maven (not exit)
# ---------------------------------------------------------------------------
output_sv=`run_mvn -V`
assert_contains "-V: prints banner" "$output_sv" "Apache Maven 4.1.0-SNAPSHOT"
assert_contains "-V: passes -Dmaven.version.printed=true" "$output_sv" "[-Dmaven.version.printed=true]"

output_sv_long=`run_mvn --show-version`
assert_contains "--show-version: prints banner" "$output_sv_long" "Apache Maven 4.1.0-SNAPSHOT"
assert_contains "--show-version: passes -Dmaven.version.printed=true" "$output_sv_long" "[-Dmaven.version.printed=true]"

# ---------------------------------------------------------------------------
# Test: -X/--debug bypasses the fast path (verbose = full JVM launch)
# ---------------------------------------------------------------------------
output_dbg=`run_mvn -v -X`
assert_not_contains "-v -X: does NOT print banner from script" "$output_dbg" "Apache Maven 4.1.0-SNAPSHOT"
assert_contains "-v -X: launches Maven with standard args" "$output_dbg" "[-Dmaven.home=$maven_home]"

# ---------------------------------------------------------------------------
# Test: --enc bypasses the fast path (MAIN_CLASS_OVERRIDE)
# ---------------------------------------------------------------------------
output_enc=`run_mvn -v --enc`
assert_not_contains "-v --enc: does NOT print banner from script" "$output_enc" "Apache Maven 4.1.0-SNAPSHOT"
assert_contains "-v --enc: uses MavenEncCling" "$output_enc" "MavenEncCling"

# ---------------------------------------------------------------------------
# Test: missing version file falls back to jar filename parsing
# ---------------------------------------------------------------------------
mv "$maven_home/bin/maven.version.properties" "$maven_home/bin/maven.version.properties.bak"
output_novf=`run_mvn -v`
assert_contains "missing version file: falls back to jar filename" "$output_novf" "Apache Maven 9.9.9"
assert_contains "missing version file: prints home" "$output_novf" "Maven home: $maven_home"
assert_not_contains "missing version file: no buildNumber" "$output_novf" "abc123def"
mv "$maven_home/bin/maven.version.properties.bak" "$maven_home/bin/maven.version.properties"

# ---------------------------------------------------------------------------
# Test: -v in quiet mode from the missing-version fallback
# ---------------------------------------------------------------------------
mv "$maven_home/bin/maven.version.properties" "$maven_home/bin/maven.version.properties.bak"
output_novf_q=`run_mvn -q -v`
assert_eq "missing version file + -q: prints jar-parsed version" "9.9.9" "$output_novf_q"
mv "$maven_home/bin/maven.version.properties.bak" "$maven_home/bin/maven.version.properties"

if [ "$failures" -ne 0 ]; then
  printf '%s assertion(s) failed\n' "$failures"
  exit 1
fi

echo "All fast-version assertions passed"
