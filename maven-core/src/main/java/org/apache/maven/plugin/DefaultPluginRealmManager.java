package org.apache.maven.plugin;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * @author <a href="mailto:piotr@tabor.waw.pl">Piotr Tabor</a>
 */
public class DefaultPluginRealmManager
    extends AbstractLogEnabled
    implements PluginRealmManager
{
    private PlexusContainer container;

    public ClassRealm getOrCreateRealm( Plugin projectPlugin, Artifact pluginArtifact, Set artifacts )
        throws PluginManagerException
    {
        Set allArtifacts = new HashSet( artifacts );
        allArtifacts.add( pluginArtifact );

        List/* <URL> */pluginJars = generateJarsListForArtifacts( allArtifacts );

        String realmKey = generateChildContainerName( projectPlugin, allArtifacts );
        ClassRealm pluginRealm = container.getComponentRealm( realmKey );

        if ( ( pluginRealm != null ) && ( pluginRealm != container.getContainerRealm() ) )
        {
            getLogger().debug( "Realm already exists for: " + realmKey + ". Skipping addition..." );
            /*
             * we've already discovered this plugin, and configured it, so skip it this time.
             */
            return pluginRealm;
        }

        // ----------------------------------------------------------------------------
        // Realm creation for a plugin
        // ----------------------------------------------------------------------------

        ClassRealm componentRealm = null;

        try
        {
            // Now here we need the artifact coreArtifactFilter
            // stuff

            componentRealm = container.createComponentRealm( realmKey, pluginJars );

            /*
             * adding for MNG-3012 to try to work around problems with Xpp3Dom (from plexus-utils spawning a
             * ClassCastException when a mojo calls plugin.getConfiguration() from maven-model...
             */
            componentRealm.importFrom( componentRealm.getParentRealm().getId(), Xpp3Dom.class.getName() );
            componentRealm.importFrom( componentRealm.getParentRealm().getId(), "org.codehaus.plexus.util.xml.pull" );

            /*
             * Adding for MNG-2878, since maven-reporting-impl was removed from the internal list of artifacts managed
             * by maven, the classloader is different between maven-reporting-impl and maven-reporting-api...so this
             * resource is not available from the AbstractMavenReport since it uses: getClass().getResourceAsStream(
             * "/default-report.xml" ) (maven-reporting-impl version 2.0; line 134; affects: checkstyle plugin, and
             * probably others)
             */
            componentRealm.importFrom( componentRealm.getParentRealm().getId(), "/default-report.xml" );

        }
        catch ( PlexusContainerException e )
        {
            throw new PluginManagerException( "Failed to create realm for plugin '" + projectPlugin + ".", e );
        }
        catch ( NoSuchRealmException e )
        {
            throw new PluginManagerException( "Failed to import Xpp3Dom from parent realm for plugin: '" +
                projectPlugin + ".", e );
        }

        getLogger().debug( "Realm for plugin: " + realmKey + ":\n" + componentRealm );

        // ----------------------------------------------------------------------------
        // The PluginCollector will now know about the plugin we
        // are trying to load
        // ----------------------------------------------------------------------------

        return componentRealm;
    }

    List/* <URL> */generateJarsListForArtifacts( Set/* <Artifact> */artifacts )
    {
        List/* <URL> */jars = new ArrayList();

        for ( Iterator i = artifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = (Artifact) i.next();

            jars.add( artifact.getFile() );
        }

        return jars;
    }


    private static String generateChildContainerName( Plugin plugin, Set artifacts )
    {
        return plugin.getKey() + ":" + plugin.getVersion() + ":" + getHashOfArtifacts( artifacts );
    }

    static long getHashOfArtifacts( Set a )
    {
        long i = 1;
        Iterator/* <Artifact> */iterator = a.iterator();
        while ( iterator.hasNext() )
        {
            Artifact artifact = (Artifact) iterator.next();
            i = ( i * artifact.hashCode() ) % 2147483647 /* big prime */;
        }
        return i;
    }
}
