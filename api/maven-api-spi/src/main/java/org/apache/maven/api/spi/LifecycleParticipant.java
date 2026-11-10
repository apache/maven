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
package org.apache.maven.api.spi;

import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.annotations.ThreadSafe;

/**
 * Allows build extensions to participate in the Maven build lifecycle after all reactor
 * projects have been read. This is the v4 API equivalent of
 * {@code AbstractMavenLifecycleParticipant.afterProjectsRead()}.
 * <p>
 * Extensions may update project state through services such as
 * {@link org.apache.maven.api.services.ProjectManager}.
 *
 * @since 4.2.0
 */
@Experimental
@Consumer
@ThreadSafe
public interface LifecycleParticipant extends SpiService {

    /**
     * Invoked after all reactor projects have been read and before the reactor is sorted for execution.
     * Extensions may update project state through services such as
     * {@link org.apache.maven.api.services.ProjectManager}.
     *
     * @param session the build session
     * @throws LifecycleParticipantException if the build cannot continue
     */
    default void afterProjectsRead(@Nonnull Session session) throws LifecycleParticipantException {}
}
