/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.fusesource.jansi;

/**
 * Mirrors the real JAnsi {@code AnsiMode} enum so code compiled against JAnsi, such as
 * maven-shared-utils, keeps resolving against this type at runtime.
 *
 * This class comes from Jansi and is provided for backward compatibility with maven-shared-utils,
 * while Maven has migrated to JLine (into which Jansi has been merged since JLine 3.25.0).
 */
@Deprecated
public enum AnsiMode {
    Strip,
    Default,
    Force
}
