#!/bin/sh
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# -----------------------------------------------------------------------------
# Tests how bin/mvn hands the MAVEN_ARGS environment variable over to Maven.
#
# The test runs on any POSIX platform: a throw-away Maven home is populated with
# the real bin/mvn, and uname(1), dirname(1) and java(1) are stubbed out so that
# the JVM command line can be asserted upon, one bracketed token per argument.
#
# The launcher is always run with a PATH holding nothing but those stubs, so it
# may only rely on shell built-ins, as the stubs themselves do.
#
# Usage: sh apache-maven/src/test/scripts/test-mvn-args.sh
#
# Exits with 0 when all assertions pass, 1 otherwise.
# -----------------------------------------------------------------------------

set -e

script_dir=`cd "\`dirname "$0"\`" && pwd`
mvn_script="$script_dir/../../bin/mvn"

if [ ! -f "$mvn_script" ]; then
  echo "Cannot locate the mvn script at $mvn_script" >&2
  exit 1
fi

# Absolute path to the shell: it has to be invoked while PATH holds the stubs only.
sh_bin=`unset -f command; command -v sh`

work_dir=`mktemp -d "${TMPDIR:-/tmp}/mvn-args.XXXXXX"`
work_dir=`cd "$work_dir" && pwd`
trap 'rm -rf "$work_dir"' EXIT INT TERM

stub_dir="$work_dir/stubs"
maven_home="$work_dir/maven-home"
project_dir="$work_dir/project"

mkdir -p "$stub_dir" "$maven_home/bin" "$maven_home/boot" "$project_dir"

cp "$mvn_script" "$maven_home/bin/mvn"
touch "$maven_home/bin/m2.conf" "$maven_home/boot/plexus-classworlds-9.9.9.jar"

# Files matching a glob, to prove that MAVEN_ARGS is never expanded as a pattern.
touch "$project_dir/one.txt" "$project_dir/two.txt"

cat > "$stub_dir/uname" <<'STUB'
#!/bin/sh
echo Linux
STUB

# dirname(1) stub: bin/mvn needs it to locate the Maven home.
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

# java(1) stub: answers version probes, otherwise echoes the arguments it was
# invoked with, one bracketed token per argument.
cat > "$stub_dir/java" <<'STUB'
#!/bin/sh
case " $* " in
  *" -version "*) exit 0 ;;
esac

for arg in "$@"; do printf '[%s]' "$arg"; done
printf '\n'
STUB

chmod +x "$stub_dir/uname" "$stub_dir/dirname" "$stub_dir/java" "$maven_home/bin/mvn"

failures=0

# The text following the launcher class: the arguments Maven itself receives.
launcher='[org.codehaus.plexus.classworlds.launcher.Launcher]'

# maven_args <MAVEN_ARGS value> [args...]
# Prints the arguments handed over to Maven.
maven_args() {
  value="$1"
  shift
  output=`cd "$project_dir" &&
    MAVEN_ARGS="$value" PATH="$stub_dir" JAVA_HOME= MAVEN_SKIP_RC=1 \
      "$sh_bin" "$maven_home/bin/mvn" "$@" 2>/dev/null || :`
  printf '%s' "${output#*"$launcher"}"
}

# maven_args_stderr <MAVEN_ARGS value> [args...]
# Prints what the launcher wrote to the standard error.
maven_args_stderr() {
  value="$1"
  shift
  ( cd "$project_dir" &&
    MAVEN_ARGS="$value" PATH="$stub_dir" JAVA_HOME= MAVEN_SKIP_RC=1 \
      "$sh_bin" "$maven_home/bin/mvn" "$@" 2>&1 >/dev/null || : )
}

# assert_equals <description> <expected> <actual>
assert_equals() {
  if [ "$2" = "$3" ]; then
    printf 'ok - %s\n' "$1"
  else
    printf 'FAILED - %s\n' "$1"
    printf '  expected: %s\n' "$2"
    printf '  actual:   %s\n' "$3"
    failures=$((failures + 1))
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
      failures=$((failures + 1))
      ;;
  esac
}

# check <description> <expected> <MAVEN_ARGS value> [args...]
check() {
  description="$1"
  expected="$2"
  value="$3"
  shift 3
  actual=`maven_args "$value" "$@"`
  assert_equals "$description" "$expected" "$actual"
}

nl='
'
tab=`printf '\t'`
dq='"'
sq="'"

check "no MAVEN_ARGS: only the command line arguments" \
  '[validate]' '' validate

check "white space only: no argument" \
  '[validate]' '   ' validate

check "white space in quotes is an argument of its own" \
  '[ ][validate]' "${sq} ${sq}" validate

check "plain words are split on white space" \
  '[-B][-X][validate]' '-B -X' validate

check "any amount of white space separates the words" \
  '[-B][-X][validate]' '   -B     -X   ' validate

check "tabs and new lines separate the words" \
  '[-B][-X][-V][validate]' "-B${tab}-X${nl}-V" validate

check "double quotes keep a path with a space in one word (#13413)" \
  '[-Dmaven.repo.local=/path with space/repo][validate]' \
  "${dq}-Dmaven.repo.local=/path with space/repo${dq}" validate

check "single quotes keep a path with a space in one word" \
  '[--settings][/path with space/settings.xml][validate]' \
  "--settings ${sq}/path with space/settings.xml${sq}" validate

check "quoted and unquoted parts of a word are joined" \
  '[-Dname=John Smith Jr][validate]' \
  "-Dname=${dq}John Smith${dq}${sq} Jr${sq}" validate

check "the other kind of quote is kept inside a quoted word" \
  "[-Dmessage=it's fine][-Dother=say \"hi\"][validate]" \
  "${dq}-Dmessage=it${sq}s fine${dq} ${sq}-Dother=say ${dq}hi${dq}${sq}" validate

check "an empty quoted word is an empty argument" \
  '[-Dkey=][][validate]' "-Dkey= ${dq}${dq}" validate

check "MAVEN_ARGS comes before the command line arguments, which stay untouched" \
  '[-B][-Dfoo=a b][first arg][second][validate]' \
  "-B ${dq}-Dfoo=a b${dq}" 'first arg' second validate

check "a pattern is not expanded" \
  '[-pl][*.txt][validate]' '-pl *.txt' validate

check "a pattern in quotes is not expanded either" \
  '[-pl][*.txt][validate]' "-pl ${dq}*.txt${dq}" validate

check "backslashes are kept as they are" \
  '[-Dpath=C:\temp\repo][validate]' '-Dpath=C:\temp\repo' validate

check "placeholders are kept as they are" \
  '[-Dbase=${project.basedir}][-Dhome=$HOME][validate]' \
  '-Dbase=${project.basedir} -Dhome=$HOME' validate

check "no command is substituted" \
  '[-Dx=$(echo][hi)][-Dy=`echo][hi`][validate]' \
  '-Dx=$(echo hi) -Dy=`echo hi`' validate

check "no command is run when quoted" \
  '[-Dx=$(echo hi)][validate]' "${dq}-Dx=\$(echo hi)${dq}" validate

check "an unterminated quote makes the rest of the value part of the last word" \
  '[-Dx=a b][validate]' "-Dx=${dq}a b" validate

stderr=`maven_args_stderr "-Dx=${dq}a b" validate`
assert_contains "an unterminated quote is reported" \
  "$stderr" "Warning: MAVEN_ARGS has an unterminated ${dq} quote"

# The debug log shows the arguments the way they are handed over.
debug_output=`cd "$project_dir" &&
  MAVEN_ARGS="-B ${dq}-Dfoo=a b${dq}" PATH="$stub_dir" JAVA_HOME= MAVEN_SKIP_RC=1 \
    MAVEN_DEBUG_SCRIPT=1 "$sh_bin" "$maven_home/bin/mvn" validate 2>&1 >/dev/null`
assert_contains "debug log: MAVEN_ARGS words are shown one by one" \
  "$debug_output" ' "-B" "-Dfoo=a b" "validate"'

if [ "$failures" -ne 0 ]; then
  printf '%s assertion(s) failed\n' "$failures"
  exit 1
fi

printf 'All assertions passed\n'
