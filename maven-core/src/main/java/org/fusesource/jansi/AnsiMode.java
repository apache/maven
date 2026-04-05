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
 * Compatibility mode enum kept for maven-shared-utils integration.
 */
@Deprecated
@SuppressWarnings("unused")
public enum AnsiMode {
    Strip,
    Default,
    Force;

    org.jline.jansi.AnsiMode toJline() {
        switch (this) {
            case Strip:
                return org.jline.jansi.AnsiMode.Strip;
            case Force:
                return org.jline.jansi.AnsiMode.Force;
            case Default:
            default:
                return org.jline.jansi.AnsiMode.Default;
        }
    }

    static AnsiMode fromJline(org.jline.jansi.AnsiMode mode) {
        if (mode == null) {
            return Default;
        }
        switch (mode) {
            case Strip:
                return Strip;
            case Force:
                return Force;
            case Default:
            default:
                return Default;
        }
    }
}
