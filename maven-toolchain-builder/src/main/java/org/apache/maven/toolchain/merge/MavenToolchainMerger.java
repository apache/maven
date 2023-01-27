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
package org.apache.maven.toolchain.merge;

import org.apache.maven.toolchain.model.PersistedToolchains;

/**
 *
 * @author Robert Scholte
 * @since 3.2.4
 */
public class MavenToolchainMerger {

    public void merge(PersistedToolchains dominant, PersistedToolchains recessive, String recessiveSourceLevel) {
        if (dominant == null || recessive == null) {
            return;
        }

        recessive.setSourceLevel(recessiveSourceLevel);

        dominant.update(new org.apache.maven.toolchain.v4.MavenToolchainsMerger()
                .merge(dominant.getDelegate(), recessive.getDelegate(), true, null));
    }
}
