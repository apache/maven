#!/usr/bin/env bash
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
# Reads the Mimir version from the <mimirVersion> property of the root POM and
# exports it as MIMIR_VERSION, which .github/ci-extensions.xml interpolates to
# pick the Mimir core extension. The extension and the eu.maveniverse.maven.mimir
# artifacts on the test classpath talk to the same daemon and refuse to run at
# different versions, so both have to come from that one property.
#
# Runs on the runner's bash, including Git bash on Windows: no XML tooling and no
# Maven, because the extension has to be resolved before the first Maven start.

set -eu

POM="${1:-pom.xml}"

VERSION=$(sed -n 's:^[[:space:]]*<mimirVersion>\([^<]*\)</mimirVersion>[[:space:]]*$:\1:p' "$POM")
COUNT=$(printf '%s\n' "$VERSION" | sed '/^$/d' | wc -l | tr -d '[:space:]')

if [ "$COUNT" != "1" ]; then
  echo "::error::expected exactly one <mimirVersion> property in $POM, found $COUNT" >&2
  exit 1
fi

echo "Mimir version: $VERSION"

if [ -n "${GITHUB_ENV:-}" ]; then
  echo "MIMIR_VERSION=$VERSION" >> "$GITHUB_ENV"
fi
