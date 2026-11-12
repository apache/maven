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

import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.annotations.Consumer;
import org.apache.maven.api.annotations.Experimental;
import org.apache.maven.api.annotations.Nonnull;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.services.MavenException;

/**
 * Allows build extensions to transform project models after all reactor projects have been read
 * and before the reactor is sorted for execution. This is the v4 API equivalent of
 * {@code AbstractMavenLifecycleParticipant.afterProjectsRead()}.
 * <p>
 * Transformers are called in sequence for each reactor project; the model returned by one transformer
 * is passed as input to the next. The final model is written back into the project by the runtime.
 * <p>
 * Implementations may return a modified model built via {@code Model.newBuilder(model)...build()},
 * or return the input model unmodified for a no-op. Throwing aborts the build.
 *
 * @since 4.2.0
 */
@Experimental
@Consumer
public interface ProjectTransformer extends SpiService {

    /**
     * Transforms the model of a single reactor project.
     *
     * @param session the build session (provides access to all reactor projects)
     * @param project the project being transformed (reflects the original state before any transformer ran)
     * @param model   the model as modified by preceding transformers in the chain
     * @return the transformed model; must not be {@code null}
     * @throws MavenException if the transformation fails and the build cannot continue
     */
    @Nonnull
    Model transformProject(@Nonnull Session session, @Nonnull Project project, @Nonnull Model model)
            throws MavenException;
}
