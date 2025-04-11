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
package org.apache.maven.api.cli.cisupport;

import org.apache.maven.api.annotations.Nonnull;

/**
 * CI support: this class contains gathered information and more from CI that Maven process runs on.
 */
public interface CISupport {
    /**
     * Short distinct name of CI system: "GH", "Jenkins", etc.
     */
    @Nonnull
    String name();

    /**
     * Returns a message that will be logged by Maven (why it was detected and possibly more).
     */
    @Nonnull
    String message();

    /**
     * Returns {@code true} if Maven runs on CI and the job is running in "debug" (or any equivalent) mode.
     */
    boolean isDebug();
}
