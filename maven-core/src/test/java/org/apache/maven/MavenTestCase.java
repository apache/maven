package org.apache.maven;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.PluginManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.codehaus.plexus.ArtifactEnabledPlexusTestCase;

import java.io.File;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 * @version $Id$
 */
public class MavenTestCase
    extends ArtifactEnabledPlexusTestCase
{
    protected PluginManager pluginManager;

    protected MavenProjectBuilder projectBuilder;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        pluginManager = (PluginManager) lookup( PluginManager.ROLE );

        projectBuilder = (MavenProjectBuilder) lookup( MavenProjectBuilder.ROLE );
    }

    // ----------------------------------------------------------------------
    // Local repository
    // ----------------------------------------------------------------------

    protected File getLocalRepositoryPath()
    {
        return getTestFile( "src/test/resources/local-repo" );
    }

    protected ArtifactRepository getLocalRepository()
    {
        ArtifactRepository r = new ArtifactRepository( "local", "file://" + getLocalRepositoryPath().getAbsolutePath() );

        return r;
    }

    // ----------------------------------------------------------------------
    // Project building
    // ----------------------------------------------------------------------

    protected MavenProject getProject( File pom, boolean transitive )
        throws Exception
    {
        return projectBuilder.build( pom, getLocalRepository(), transitive );
    }

    protected MavenProject getProject( File pom )
        throws Exception
    {
        return projectBuilder.build( pom, getLocalRepository() );
    }

}
