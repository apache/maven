package org.apache.maven.plugin.coreit;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;

import java.io.File;
import java.util.Collection;

/**
 * Provides common code for the install and deploy mojos.
 * 
 * @author Benjamin Bentmann
 *
 */
public abstract class AbstractRepoMojo
    extends AbstractMojo
{

    /**
     * The project's main artifact.
     * 
     * @parameter default-value="${project.artifact}"
     * @readonly
     * @required
     */
    protected Artifact mainArtifact;

    /**
     * The project's attached artifact.
     * 
     * @parameter default-value="${project.attachedArtifacts}"
     * @readonly
     * @required
     */
    protected Collection attachedArtifacts;

    /**
     * The packaging of the project.
     * 
     * @parameter default-value="${project.packaging}"
     * @required
     * @readonly
     */
    protected String packaging;

    /**
     * The POM file of the project.
     * 
     * @parameter default-value="${project.file}"
     * @required
     * @readonly
     */
    protected File pomFile;

    /**
     * The local repository.
     * 
     * @parameter default-value="${localRepository}"
     * @readonly
     * @required
     */
    protected ArtifactRepository localRepository;

    protected boolean isPomArtifact()
    {
        return "pom".equals( packaging );
    }

}
