package org.apache.maven.model.resolution;

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

import org.apache.maven.model.ModelBuilder;
import org.apache.maven.model.ModelSource;
import org.apache.maven.model.Repository;

/**
 * Resolves a POM from its coordinates. During the build process, the {@link ModelBuilder} will add any relevant
 * repositories to the model resolver. In other words, the model resolver is stateful and should not be reused across
 * multiple model building requests.
 * 
 * @author Benjamin Bentmann
 */
public interface ModelResolver
{

    ModelSource resolveModel( String groupId, String artifactId, String version )
        throws UnresolvableModelException;

    void addRepository( Repository repository )
        throws InvalidRepositoryException;

}
