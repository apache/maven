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
# Tests that JVM options from .mvn/jvm.config are correctly parsed and that
# JVM notices printed to stderr (e.g. JAVA_TOOL_OPTIONS, JDK_JAVA_OPTIONS,
# _JAVA_OPTIONS) do not pollute MAVEN_OPTS.
#
# Usage: sh apache-maven/src/test/scripts/test-mvn-jvm-config.sh
# -----------------------------------------------------------------------------

set -e

script_dir=`cd "\`dirname "$0"\`" && pwd`
mvn_script="$script_dir/../../assembly/maven/bin/mvn"

if [ ! -f "$mvn_script" ]; then
  echo "Cannot locate the mvn script at $mvn_script" >&2
  exit 1
fi

sh_bin=`unset -f command; command -v sh`

work_dir=`mktemp -d "${TMPDIR:-/tmp}/mvn-jvm-config.XXXXXX"`
work_dir=`cd "$work_dir" && pwd`
trap 'rm -rf "$work_dir"' EXIT INT TERM

stub_dir="$work_dir/stubs"
maven_home="$work_dir/maven-home"
project_dir="$work_dir/project"

mkdir -p "$stub_dir" "$maven_home/bin" "$maven_home/boot" "$project_dir/.mvn"

cp "$mvn_script" "$maven_home/bin/mvn"
touch "$maven_home/bin/m2.conf" "$maven_home/boot/plexus-classworlds-9.9.9.jar"

# uname stub
cat > "$stub_dir/uname" <<'STUB'
#!/bin/sh
echo "Linux"
STUB

# dirname stub
cat > "$stub_dir/dirname" <<'STUB'
#!/bin/sh
path="$1"
case "$path" in
  */*)
    path="${path%/*}"
    [ -n "$path" ] || path="/"
    ;;
  *) path="." ;;
esac
printf '%s\n' "$path"
STUB

# java stub: simulates Java runtime notices on stderr when tool options env vars are set
cat > "$stub_dir/java" <<'STUB'
#!/bin/sh

# If tool options are set, print JVM notices on stderr just like real JDK
if [ -n "$JAVA_TOOL_OPTIONS" ]; then
  echo "Picked up JAVA_TOOL_OPTIONS: $JAVA_TOOL_OPTIONS" >&2
fi
if [ -n "$JDK_JAVA_OPTIONS" ]; then
  echo "NOTE: Picked up JDK_JAVA_OPTIONS: $JDK_JAVA_OPTIONS" >&2
fi
if [ -n "$_JAVA_OPTIONS" ]; then
  echo "Picked up _JAVA_OPTIONS: $_JAVA_OPTIONS" >&2
fi

case " $* " in
  *" -version "*) exit 0 ;;
esac

case "$1" in
  *JvmConfigParser.java)
    if [ "$FAIL_PARSER" = "1" ]; then
      echo "Error: simulated parser failure" >&2
      exit 1
    fi
    # Emulate JvmConfigParser output
    while IFS= read -r line || [ -n "$line" ]; do
      line="${line%%#*}"
      for token in $line; do
        printf '"%s" ' "$token"
      done
    done < "$2"
    printf '\n'
    exit 0 ;;
esac

# For the main launch, echo all arguments bracketed
for arg in "$@"; do printf '[%s]' "$arg"; done
printf '\n'
STUB

chmod +x "$stub_dir/uname" "$stub_dir/dirname" "$stub_dir/java" "$maven_home/bin/mvn"

echo '-Xmx512m -Dcustom.prop=hello' > "$project_dir/.mvn/jvm.config"

failures=0

assert_contains() {
  desc="$1"; haystack="$2"; needle="$3"
  case "$haystack" in
    *"$needle"*) printf 'ok - %s\n' "$desc" ;;
    *)
      printf 'FAILED - %s\n' "$desc"
      printf '  expected to contain: %s\n' "$needle"
      printf '  actual: %s\n' "$haystack"
      failures=`expr "$failures" + 1`
      ;;
  esac
}

assert_not_contains() {
  desc="$1"; haystack="$2"; needle="$3"
  case "$haystack" in
    *"$needle"*)
      printf 'FAILED - %s\n' "$desc"
      printf '  expected NOT to contain: %s\n' "$needle"
      printf '  actual: %s\n' "$haystack"
      failures=`expr "$failures" + 1`
      ;;
    *) printf 'ok - %s\n' "$desc" ;;
  esac
}

run_mvn() {
  ( cd "$project_dir" &&
    PATH="$stub_dir" JAVA_HOME= MAVEN_SKIP_RC=1 \
      "$sh_bin" "$maven_home/bin/mvn" validate 2>/dev/null )
}

run_mvn_stderr() {
  ( cd "$project_dir" &&
    PATH="$stub_dir" JAVA_HOME= MAVEN_SKIP_RC=1 \
      "$sh_bin" "$maven_home/bin/mvn" validate 2>&1 >/dev/null )
}

# Test 1: Clean environment - JVM options from jvm.config are present
out=`run_mvn`
assert_contains "clean env: jvm.config options present" "$out" "[-Xmx512m]"
assert_contains "clean env: custom prop present" "$out" "[-Dcustom.prop=hello]"

# Test 2: JAVA_TOOL_OPTIONS is set
out=`JAVA_TOOL_OPTIONS="-Dfoo=bar" run_mvn`
assert_contains "JAVA_TOOL_OPTIONS: jvm.config options present" "$out" "[-Xmx512m]"
assert_not_contains "JAVA_TOOL_OPTIONS: 'Picked' not in args" "$out" "[Picked]"
assert_not_contains "JAVA_TOOL_OPTIONS: 'up' not in args" "$out" "[up]"
assert_not_contains "JAVA_TOOL_OPTIONS: 'JAVA_TOOL_OPTIONS:' not in args" "$out" "[JAVA_TOOL_OPTIONS:]"

# Test 3: JDK_JAVA_OPTIONS is set
out=`JDK_JAVA_OPTIONS="-Dfoo=bar" run_mvn`
assert_contains "JDK_JAVA_OPTIONS: jvm.config options present" "$out" "[-Xmx512m]"
assert_not_contains "JDK_JAVA_OPTIONS: 'NOTE:' not in args" "$out" "[NOTE:]"

# Test 4: _JAVA_OPTIONS is set
out=`_JAVA_OPTIONS="-Dfoo=bar" run_mvn`
assert_contains "_JAVA_OPTIONS: jvm.config options present" "$out" "[-Xmx512m]"
assert_not_contains "_JAVA_OPTIONS: 'Picked' not in args" "$out" "[Picked]"

# Test 5: Parser failure displays stderr output
err_out=`FAIL_PARSER=1 run_mvn_stderr || true`
assert_contains "parser failure: error message displayed" "$err_out" "ERROR: JvmConfigParser failed with exit code 1"
assert_contains "parser failure: parser stderr displayed" "$err_out" "simulated parser failure"

if [ "$failures" -ne 0 ]; then
  printf '%s assertion(s) failed\n' "$failures"
  exit 1
fi

echo "All assertions passed"
