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
 * @description The archetype creation goal looks for an archetype with a given groupId, artifactId, and
 * version and retrieves it from the remote repository. Once the archetype is retrieve it is process against
 * a set of user parameters to create a working Maven project.
 * @requiresProject false
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
     * @parameter expression="${archetypeGroupId}" default-value="org.apache.maven.archetypes"
     * @required
     */
    private String archetypeGroupId;

    /**
     * @parameter expression="${archetypeArtifactId}" default-value="maven-archetype-quickstart"
     * @required
     */
    private String archetypeArtifactId;

    /**
     * @parameter expression="${archetypeVersion}" default-value="RELEASE"
     * @required
     */
    private String archetypeVersion;

    /**
     * @parameter expression="${groupId}"
     * @required
     */
    private String groupId;

    /**
     * @parameter expression="${artifactId}"
     * @required
     */
    private String artifactId;

    /**
     * @parameter expression="${version}" default-value="1.0-SNAPSHOT"
     * @required
     */
    private String version;

    /**
     * @parameter expression="${packageName}" alias="package"
     */
    private String packageName;

    /**
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @required
     */
    private List remoteRepositories;

    public void execute()
        throws MojoExecutionException
    {
        // TODO: prompt for missing values
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

        if ( packageName == null )
        {
            getLog().info( "Defaulting package to group ID: " + groupId );

            packageName = groupId;
        }

        // TODO: context mojo more appropriate?
        Map map = new HashMap();

        map.put( "basedir", basedir );

        map.put( "package", packageName );

        map.put( "packageName", packageName );

        map.put( "groupId", groupId );

        map.put( "artifactId", artifactId );

        map.put( "version", version );

        try
        {
            archetype.createArchetype( archetypeGroupId, archetypeArtifactId, archetypeVersion, localRepository, remoteRepositories, map );
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
