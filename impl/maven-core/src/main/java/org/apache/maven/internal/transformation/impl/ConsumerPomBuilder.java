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
package org.apache.maven.internal.transformation.impl;

import javax.xml.stream.XMLStreamException;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.maven.api.model.Model;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;

/**
 * This interface is not public and the purpose is to allow easy unit testing
 * of {@link DefaultConsumerPomArtifactTransformer}.
 */
interface ConsumerPomBuilder {

    Model build(RepositorySystemSession session, MavenProject project, Path src)
            throws ModelBuildingException, IOException, XMLStreamException;
}
