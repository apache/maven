#!/bin/bash

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

#libreoffice --headless --convert-to svg src/site/xdoc/maven-deps.odg
# CLI export keeps full A3 page
# I prefer doing it by hand, limiting export to "selection" = avoids extra space

# svgo https://github.com/svg/svgo
svgo --config src/site/svgo.config.mjs maven-deps.svg -o maven-deps-optimized.svg

cat maven-deps-optimized.svg \
  | sed 's/a xlink:href/a target="_parent" xlink:href/' \
  | sed 's_file://_.._' \
  > src/site/resources/images/maven-deps.svg
