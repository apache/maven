package org.apache.maven.feature.check;

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
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.WeakHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.Maven;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.logging.Logger;

/**
 * Enforces that the required version of Maven is running and enables the experimental features of that version.
 */
@Component( role = AbstractMavenLifecycleParticipant.class, hint = "experimental" )
public class MavenExperimentEnabler
    extends AbstractMavenLifecycleParticipant
{

    @Requirement
    private Logger log;

    @Requirement
    private PlexusContainer container;

    private final Map<MavenSession, Void> startedSessions = new WeakHashMap<>();

    @Override
    public void afterProjectsRead( MavenSession session )
        throws MavenExecutionException
    {
        if ( !startedSessions.containsKey( session ) )
        {
            log.error( "Experimental features cannot be enabled using project/build/extensions" );
            throw new MavenExecutionException( "Experimental features cannot be enabled using project/build/extensions",
                                               topLevelProjectFile( session ) );
        }
    }

    @Override
    public void afterSessionStart( MavenSession session )
        throws MavenExecutionException
    {
        startedSessions.put( session, null );
        log.debug( "Determining experimental feature version requirements" );
        String targetVersion;
        try
        {
            targetVersion = parse( getClass(), "/META-INF/maven/org.apache.maven/maven-experiments/pom.properties" );
        }
        catch ( IOException e )
        {
            throw new MavenExecutionException(
                "Cannot determine required version of Maven to enable experimental features",
                topLevelProjectFile( session ) );
        }
        if ( StringUtils.isBlank( targetVersion ) )
        {
            throw new MavenExecutionException(
                "Cannot determine required version of Maven to enable experimental features",
                topLevelProjectFile( session ) );
        }
        String activeVersion;
        try
        {
            activeVersion = parse( Maven.class, "/META-INF/maven/org.apache.maven/maven-core/pom.properties" );
        }
        catch ( IOException e )
        {
            throw new MavenExecutionException( "Cannot confirm executing version of Maven as " + targetVersion
                                                   + " which is required to enable the experimental features used"
                                                   + " by this project", topLevelProjectFile( session ) );
        }
        if ( Objects.equals( activeVersion, targetVersion ) )
        {
            log.info( "Enabling experimental features of Maven " + targetVersion );
        }
        else
        {
            throw new MavenExecutionException(
                "The project uses experimental features that require exactly Maven " + targetVersion,
                topLevelProjectFile( session ) );
        }
        try
        {
            Helper.enableFeatures( session, targetVersion, this.container, this.topLevelProjectFile( session ) );
        }
        catch ( LinkageError | ClassNotFoundException | ComponentLookupException e )
        {
            throw new MavenExecutionException(
                "The project uses experimental features that require exactly Maven " + targetVersion,
                topLevelProjectFile( session ) );
        }
    }

    private File topLevelProjectFile( MavenSession session )
    {
        return session.getTopLevelProject() != null ? session.getTopLevelProject().getFile() : null;
    }

    private static String parse( Class<?> clazz, String resource )
        throws IOException
    {
        Properties targetProperties = new Properties();
        try ( InputStream is = clazz.getResourceAsStream( resource ) )
        {
            if ( is != null )
            {
                targetProperties.load( is );
            }
        }
        return targetProperties.getProperty( "version" );
    }

    @Override
    public void afterSessionEnd( MavenSession session )
        throws MavenExecutionException
    {
        startedSessions.remove( session );
    }

}
