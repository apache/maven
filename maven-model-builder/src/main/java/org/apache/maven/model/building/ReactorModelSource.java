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
package org.apache.maven.model.building;

import java.io.File;

/**
 * A {@link FileModelSource} for the POM of a project in the reactor, as opposed to one resolved from a
 * repository. A {@link org.apache.maven.model.resolution.ModelResolver} returns it so that the model builder
 * builds an imported reactor BOM as a project rather than as a repository (external) model.
 *
 * @since 3.10.3
 */
public class ReactorModelSource extends FileModelSource {

    /**
     * Creates a new model source backed by the specified reactor POM file.
     *
     * @param pomFile The POM file, must not be {@code null}.
     */
    public ReactorModelSource(File pomFile) {
        super(pomFile);
    }
}
