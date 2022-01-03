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
package org.apache.maven.artifact.repository.metadata.validator;

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

import org.apache.maven.artifact.repository.metadata.Metadata;

/**
 * Validates repository metadata
 *
 */
public interface MetadataValidator {

    /**
     * The different levels where repository metadata may occur in the
     * <a href="https://cwiki.apache.org/confluence/x/jYOV">repository layout</a>.
     */
    enum Level {
        GROUP_ID("groupId"),
        ARTIFACT_ID("artifactId"),
        VERSION("version");

        private final String label;

        Level(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * Validates the specified metadata with unknown level.
     * Applies some heuristic to determine which repository metadata level is probably specified through the given
     * metadata
     *
     * @param metadata The metadata to validate, must not be {@code null}.
     * @param problems The container used to collect problems that were encountered, must not be {@code null}.
     */
    void validate(Metadata metadata, MetadataProblemCollector problems);

    /**
     * Validates the specified metadata for the given level.
     *
     * @param metadata The metadata to validate, must not be {@code null}.
     * @param level The metadata level, must not be {@code null}.
     * @param isSnapshot {@code false} in case the metadata is only for snapshot versions, {@code true} in case it is
     * only for release versions or {@code null} if it is potentially for both snapshot and release versions.
     * @param problems The container used to collect problems that were encountered, must not be {@code null}.
     */
    void validate(Metadata metadata, Level level, Boolean isSnapshot, MetadataProblemCollector problems);
}
