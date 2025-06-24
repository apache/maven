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
package org.apache.maven.execution;

/**
 * Describes whether a target should be activated or not, and if that is required or optional.
 *
 * @param active Should the target be active?
 * @param optional Should the build continue if the target is not present?
 * @param recurse Should the target be activated and its children be activated?
 */
public record ActivationSettings(boolean active, boolean optional, boolean recurse) {

    static ActivationSettings of(final boolean active, final boolean optional) {
        return of(active, optional, true);
    }

    static ActivationSettings of(final boolean active, final boolean optional, final boolean recursive) {
        return new ActivationSettings(active, optional, recursive);
    }

    static ActivationSettings activated() {
        return new ActivationSettings(true, false, true);
    }

    static ActivationSettings activatedOpt() {
        return new ActivationSettings(true, true, true);
    }

    static ActivationSettings deactivated() {
        return new ActivationSettings(false, false, false);
    }

    static ActivationSettings deactivatedOpt() {
        return new ActivationSettings(false, true, false);
    }
}
