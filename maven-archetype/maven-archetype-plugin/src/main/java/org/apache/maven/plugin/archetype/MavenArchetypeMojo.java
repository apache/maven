package org.apache.maven.plugin.archetype;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import org.apache.maven.archetype.Archetype;
import org.apache.maven.archetype.ArchetypeDescriptorException;
import org.apache.maven.archetype.ArchetypeNotFoundException;
import org.apache.maven.archetype.ArchetypeTemplateProcessingException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds archetype containers.
 *
 * @goal create
 */
public class MavenArchetypeMojo
    extends AbstractMojo
{
    /**
     * @parameter expression="${component.org.apache.maven.archetype.Archetype}"
     * @required
     */
    private Archetype archetype;

    /**
     * @parameter expression="${localRepository}"
     * @required
     */
    private ArtifactRepository localRepository;

    /**
     * @parameter expression="${archetypeGroupId}"
     * @required
     */
    private String archetypeGroupId = "org.apache.maven.archetypes";

    /**
     * @parameter expression="${archetypeArtifactId}"
     * @required
     */
    private String archetypeArtifactId = "maven-archetype-quickstart";

    /**
     * @parameter expression="${archetypeVersion}"
     * @required
     */
    private String archetypeVersion = "1.0-alpha-1-SNAPSHOT";

    /**
     * @parameter expression="${groupId}"
     * @required
     */
    private String groupId = "org.apache.maven.archetypes";

    /**
     * @parameter expression="${artifactId}"
     * @required
     */
    private String artifactId = "maven-archetype-quickstart";

    /**
     * @parameter expression="${version}"
     * @required
     */
    private String version = "1.0-SNAPSHOT";

    /**
     * @parameter expression="${packageName}"
     * @required
     */
    private String packageName = "com.mycompany.app";

    /**
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @required
     */
    private List remoteRepositories;

    public void execute()
        throws MojoExecutionException
    {
        // TODO: prompt for missing values
        // TODO: use new plugin type
        // TODO: configurable license

        // ----------------------------------------------------------------------
        // archetypeGroupId
        // archetypeArtifactId
        // archetypeVersion
        //
        // localRepository
        // remoteRepository
        // parameters
        // ----------------------------------------------------------------------

        String basedir = System.getProperty( "user.dir" );

        // TODO: allow this to be configured
        File outputDirectory = new File( basedir, artifactId );

        if ( outputDirectory.exists() )
        {
            throw new MojoExecutionException( outputDirectory.getName() +
                " already exists - please run from a clean directory" );
        }

        // TODO: context mojo more appropriate?
        Map map = new HashMap();
        map.put( "outputDirectory", outputDirectory.getAbsolutePath() );
        map.put( "package", packageName );
        map.put( "packageName", packageName );
        map.put( "groupId", groupId );
        map.put( "artifactId", artifactId );
        map.put( "version", version );

        try
        {
            archetype.createArchetype( archetypeGroupId, archetypeArtifactId, archetypeVersion, localRepository,
                                       remoteRepositories, map );
        }
        catch ( ArchetypeNotFoundException e )
        {
            throw new MojoExecutionException( "Error creating from archetype", e );
        }
        catch ( ArchetypeDescriptorException e )
        {
            throw new MojoExecutionException( "Error creating from archetype", e );
        }
        catch ( ArchetypeTemplateProcessingException e )
        {
            throw new MojoExecutionException( "Error creating from archetype", e );
        }
    }
}
