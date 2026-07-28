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
# Tests the Cygwin/MinGW/MSYS2 path conversion performed by bin/mvn.
#
# The test runs on any POSIX platform: a throw-away Maven home is populated with
# the real bin/mvn, and uname(1), cygpath(1) and java(1) are stubbed out so that
# the Windows-only code paths can be exercised and the resulting JVM command
# line can be asserted upon.
#
# The launcher is always run with a PATH holding nothing but those stubs. That
# keeps the outcome identical on every platform, and it is what makes the
# missing-cygpath case meaningful: were the real PATH kept, the genuine
# cygpath(1) of an actual Cygwin/MSYS2 installation would be picked up and the
# fallback would never be exercised. Consequently the stubs may only rely on
# shell built-ins, no external command is reachable from them.
#
# Usage: sh apache-maven/src/test/scripts/test-mvn-path-conversion.sh
#
# Exits with 0 when all assertions pass, 1 otherwise, so it can be wired into a
# CI job or a git hook as-is.
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

work_dir=`mktemp -d "${TMPDIR:-/tmp}/mvn-path-conversion.XXXXXX"`
# Normalize the path: on macOS $TMPDIR ends with '/', producing a double
# slash that pwd(1) inside the launcher will collapse.  Resolving through
# cd/pwd keeps the test's reference paths in sync with the launcher output.
work_dir=`cd "$work_dir" && pwd`
trap 'rm -rf "$work_dir"' EXIT INT TERM

stub_dir="$work_dir/stubs"
maven_home="$work_dir/maven-home"
project_dir="$work_dir/project"

mkdir -p "$stub_dir" "$maven_home/bin" "$maven_home/boot" "$maven_home/lib/jansi-native" \
  "$project_dir/.mvn" "$project_dir/module"

cp "$mvn_script" "$maven_home/bin/mvn"
touch "$maven_home/bin/m2.conf" "$maven_home/boot/plexus-classworlds-9.9.9.jar"

# uname(1) stub: the emulated OS is taken from the FAKE_UNAME variable.
cat > "$stub_dir/uname" <<'STUB'
#!/bin/sh
echo "${FAKE_UNAME:-Linux}"
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

# tr(1) stub: concat_lines uses tr -s '\r\n' '  ' to flatten jvm.config.
cat > "$stub_dir/tr" <<'STUB'
#!/bin/sh
# Minimal tr that only handles the concat_lines usage: tr -s '\r\n' '  '
# Reads stdin and replaces \r and \n with spaces, squeezing repeats.
while IFS= read -r line || [ -n "$line" ]; do
  printf '%s ' "$line"
done
STUB

# cygpath(1) stub: --windows maps /foo/bar to C:\foo\bar, --unix does the reverse.
cat > "$stub_dir/cygpath" <<'STUB'
#!/bin/sh
for arg in "$@"; do path="$arg"; done

replace() {
  text="$1"; needle="$2"; value="$3"; out=""
  while :; do
    case "$text" in
      *"$needle"*)
        out="$out${text%%"$needle"*}$value"
        text="${text#*"$needle"}"
        ;;
      *) break ;;
    esac
  done
  printf '%s' "$out$text"
}

case " $* " in
  *" --windows "*) printf 'C:%s\n' "`replace "$path" '/' '\'`" ;;
  *" --unix "*) printf '%s\n' "`replace "${path#C:}" '\' '/'`" ;;
  *" --path "*)
    # --path with --windows
    case " $* " in
      *" --windows "*) printf 'C:%s\n' "`replace "$path" '/' '\'`" ;;
      *" --unix "*) printf '%s\n' "`replace "${path#C:}" '\' '/'`" ;;
      *) printf '%s\n' "$path" ;;
    esac ;;
  *) printf '%s\n' "$path" ;;
esac
STUB

# java(1) stub: answers version probes and otherwise echoes the arguments it was
# invoked with, one bracketed token per argument.
cat > "$stub_dir/java" <<'STUB'
#!/bin/sh
case " $* " in
  *" -version "*) exit 0 ;;
esac

for arg in "$@"; do printf '[%s]' "$arg"; done
printf '\n'
STUB

chmod +x "$stub_dir/uname" "$stub_dir/dirname" "$stub_dir/cygpath" "$stub_dir/java" \
  "$stub_dir/tr" "$maven_home/bin/mvn"

# The same stubs, without cygpath, to exercise the fallback.
nocygpath_dir="$work_dir/stubs-without-cygpath"
mkdir -p "$nocygpath_dir"
for stub in uname dirname java tr; do
  cp "$stub_dir/$stub" "$nocygpath_dir/$stub"
  chmod +x "$nocygpath_dir/$stub"
done

failures=0

# run_mvn <uname-output> <stub-dir>
run_mvn() {
  ( cd "$project_dir/module" &&
    FAKE_UNAME="$1" PATH="$2" JAVA_HOME= MAVEN_SKIP_RC=1 \
      "$sh_bin" "$maven_home/bin/mvn" verify 2>/dev/null )
}

# to_windows <posix-path>
to_windows() {
  text="$1"; out=""
  while :; do
    case "$text" in
      */*)
        out="$out${text%%/*}\\"
        text="${text#*/}"
        ;;
      *) break ;;
    esac
  done
  printf 'C:%s' "$out$text"
}

# contains <haystack> <needle>
# Plain substring check. A case/glob comparison cannot be used, the haystack
# holds Windows paths and backslashes are escape characters in glob patterns.
contains() {
  case "$1" in
    *"$2"*) return 0 ;;
    *) return 1 ;;
  esac
}

# assert_contains <description> <haystack> <needle>
assert_contains() {
  if contains "$2" "$3"; then
    printf 'ok - %s\n' "$1"
  else
    printf 'FAILED - %s\n' "$1"
    printf '  expected to contain: %s\n' "$3"
    printf '  actual: %s\n' "$2"
    failures=`expr $failures + 1`
  fi
}

# assert_not_contains <description> <haystack> <needle>
assert_not_contains() {
  if contains "$2" "$3"; then
    printf 'FAILED - %s\n' "$1"
    printf '  expected NOT to contain: %s\n' "$3"
    printf '  actual: %s\n' "$2"
    failures=`expr $failures + 1`
  else
    printf 'ok - %s\n' "$1"
  fi
}

# POSIX platforms must be left untouched.
output=`run_mvn Linux "$stub_dir"`
assert_contains "POSIX: multiModuleProjectDirectory stays a POSIX path" "$output" \
  "[-Dmaven.multiModuleProjectDirectory=$project_dir]"
assert_contains "POSIX: maven.home stays a POSIX path" "$output" \
  "[-Dmaven.home=$maven_home]"

# Every Windows POSIX emulation layer must receive native Windows paths.
for os in CYGWIN_NT-10.0 MINGW32_NT-6.2 MINGW64_NT-10.0 MSYS_NT-10.0; do
  output=`run_mvn "$os" "$stub_dir"`

  assert_contains "$os: multiModuleProjectDirectory is a native Windows path" "$output" \
    "[-Dmaven.multiModuleProjectDirectory=`to_windows "$project_dir"`]"
  assert_contains "$os: maven.home is a native Windows path" "$output" \
    "[-Dmaven.home=`to_windows "$maven_home"`]"
  assert_contains "$os: library.jansi.path is a native Windows path" "$output" \
    "[-Dlibrary.jansi.path=`to_windows "$maven_home/lib/jansi-native"`]"
  assert_not_contains "$os: no path mixes both separators" "$output" "\\/"
done

# Without cygpath the launcher must still work, using unconverted paths, rather
# than passing empty paths to the JVM.
output=`run_mvn MINGW64_NT-10.0 "$nocygpath_dir"`
assert_contains "missing cygpath: multiModuleProjectDirectory falls back to a POSIX path" "$output" \
  "[-Dmaven.multiModuleProjectDirectory=$project_dir]"
assert_contains "missing cygpath: maven.home falls back to a POSIX path" "$output" \
  "[-Dmaven.home=$maven_home]"
assert_not_contains "missing cygpath: no empty maven.home is passed" "$output" \
  "[-Dmaven.home=]"

if [ "$failures" -ne 0 ]; then
  printf '%s assertion(s) failed\n' "$failures"
  exit 1
fi

echo "All assertions passed"
