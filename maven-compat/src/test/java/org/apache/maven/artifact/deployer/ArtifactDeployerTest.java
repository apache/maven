package org.apache.maven.artifact.deployer;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;

import org.apache.maven.artifact.AbstractArtifactComponentTestCase;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.shared.utils.io.FileUtils;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 */
public class ArtifactDeployerTest
    extends AbstractArtifactComponentTestCase
{
    private ArtifactDeployer artifactDeployer;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        artifactDeployer = (ArtifactDeployer) lookup( ArtifactDeployer.ROLE );
    }

    protected String component()
    {
        return "deployer";
    }

    public void testArtifactInstallation()
        throws Exception
    {
        String artifactBasedir = new File( getBasedir(), "src/test/resources/artifact-install" ).getAbsolutePath();

        Artifact artifact = createArtifact( "artifact", "1.0" );

        File file = new File( artifactBasedir, "artifact-1.0.jar" );
        assertEquals( "dummy", FileUtils.fileRead( file, "UTF-8" ).trim() );

        artifactDeployer.deploy( file, artifact, remoteRepository(), localRepository() );

        ArtifactRepository remoteRepository = remoteRepository();
        File deployedFile = new File( remoteRepository.getBasedir(), remoteRepository.pathOf( artifact ) );
        assertTrue( deployedFile.exists() );
        assertEquals( "dummy", FileUtils.fileRead( deployedFile, "UTF-8" ).trim() );
    }
}