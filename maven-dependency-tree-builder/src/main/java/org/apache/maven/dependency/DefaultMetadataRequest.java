package org.apache.maven.dependency;

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

import org.apache.maven.model.Dependency;

/**
 * @author Benjamin Bentmann
 */
class DefaultMetadataRequest
    implements MetadataRequest
{

    private Dependency dependency;

    private String repositoryId;

    private DependencyProblemCollector problems;

    public DefaultMetadataRequest( DependencyProblemCollector problems )
    {
        this.problems = problems;
    }

    public DefaultMetadataRequest( Dependency dependency, String repositoryId, DependencyProblemCollector problems )
    {
        this.dependency = dependency;
        this.repositoryId = repositoryId;
        this.problems = problems;
    }

    public void set( Dependency dependency, String repositoryId )
    {
        this.dependency = dependency;
        this.repositoryId = repositoryId;
    }

    public String getGroupId()
    {
        return dependency.getGroupId();
    }

    public String getArtifactId()
    {
        return dependency.getArtifactId();
    }

    public String getVersion()
    {
        return dependency.getVersion();
    }

    public DependencyProblemCollector getProblems()
    {
        return problems;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

}
