package org.apache.maven.classrealm;

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

import java.util.Random;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.MutablePlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

/**
 * Manages the class realms used by Maven. <strong>Warning:</strong> This is an internal utility class that is only
 * public for technical reasons, it is not part of the public API. In particular, this interface can be changed or
 * deleted without prior notice.
 * 
 * @author Benjamin Bentmann
 */
@Component( role = ClassRealmManager.class )
public class DefaultClassRealmManager
    implements ClassRealmManager
{

    @Requirement
    private Logger logger;

    @Requirement
    protected PlexusContainer container;

    private ClassWorld getClassWorld()
    {
        return ( (MutablePlexusContainer) container ).getClassWorld();
    }

    private ClassRealm createRealm( String baseRealmId )
    {
        ClassWorld world = getClassWorld();

        String realmId = baseRealmId;

        Random random = new Random();

        synchronized ( world )
        {
            ClassRealm classRealm;

            while ( true )
            {
                try
                {
                    classRealm = world.newRealm( realmId );

                    if ( logger.isDebugEnabled() )
                    {
                        logger.debug( "Created new class realm " + realmId );
                    }

                    break;
                }
                catch ( DuplicateRealmException e )
                {
                    realmId = baseRealmId + '-' + random.nextInt();
                }
            }

            classRealm.setParentRealm( getApiRealm() );

            importXpp3Dom( classRealm );

            return classRealm;
        }
    }

    /**
     * Gets the class realm that holds the Maven API classes that we intend to share with plugins and extensions. The
     * API realm is basically a subset of the core realm and hides internal utility/implementation classes from
     * plugins/extensions.
     * 
     * @return The class realm for the Maven API, never {@code null}.
     */
    private ClassRealm getApiRealm()
    {
        return container.getContainerRealm();

// TODO: MNG-4273, currently non-functional because the core artifact filter wipes out transitive plugin dependencies
//       like plexus-utils, too. We need to filter the result set of the plugin artifacts, not the graph.
//
//        ClassWorld world = getClassWorld();
//
//        String realmId = "maven.api";
//
//        ClassRealm apiRealm;
//
//        synchronized ( world )
//        {
//            apiRealm = world.getClassRealm( realmId );
//
//            if ( apiRealm == null )
//            {
//                try
//                {
//                    apiRealm = world.newRealm( realmId );
//                }
//                catch ( DuplicateRealmException e )
//                {
//                    throw new IllegalStateException( "Failed to create API realm " + realmId, e );
//                }
//
//                String coreRealmId = container.getContainerRealm().getId();
//                try
//                {
//                    // components.xml
//                    apiRealm.importFrom( coreRealmId, "META-INF/plexus" );
//
//                    // maven-*
//                    apiRealm.importFrom( coreRealmId, "org.apache.maven." );
//
//                    // plexus-classworlds
//                    apiRealm.importFrom( coreRealmId, "org.codehaus.plexus.classworlds" );
//
//                    // plexus-container, plexus-component-annotations
//                    apiRealm.importFrom( coreRealmId, "org.codehaus.plexus.component" );
//                    apiRealm.importFrom( coreRealmId, "org.codehaus.plexus.configuration" );
//                    apiRealm.importFrom( coreRealmId, "org.codehaus.plexus.container" );
//                    apiRealm.importFrom( coreRealmId, "org.codehaus.plexus.context" );
//                    apiRealm.importFrom( coreRealmId, "org.codehaus.plexus.lifecycle" );
//                    apiRealm.importFrom( coreRealmId, "org.codehaus.plexus.logging" );
//                    apiRealm.importFrom( coreRealmId, "org.codehaus.plexus.personality" );
//                    apiRealm.importFrom( coreRealmId, "org.codehaus.plexus.ComponentRegistry" );
//                    apiRealm.importFrom( coreRealmId, "org.codehaus.plexus.ContainerConfiguration" );
//                    apiRealm.importFrom( coreRealmId, "org.codehaus.plexus.DefaultComponentRegistry" );
//                    apiRealm.importFrom( coreRealmId, "org.codehaus.plexus.DefaultContainerConfiguration" );
//                    apiRealm.importFrom( coreRealmId, "org.codehaus.plexus.DefaultPlexusContainer" );
//                    apiRealm.importFrom( coreRealmId, "org.codehaus.plexus.DuplicateChildContainerException" );
//                    apiRealm.importFrom( coreRealmId, "org.codehaus.plexus.MutablePlexusContainer" );
//                    apiRealm.importFrom( coreRealmId, "org.codehaus.plexus.PlexusConstants" );
//                    apiRealm.importFrom( coreRealmId, "org.codehaus.plexus.PlexusContainer" );
//                    apiRealm.importFrom( coreRealmId, "org.codehaus.plexus.PlexusContainerException" );
//                }
//                catch ( NoSuchRealmException e )
//                {
//                    throw new IllegalStateException( e );
//                }
//
//                try
//                {
//                    container.discoverComponents( apiRealm );
//                }
//                catch ( Exception e )
//                {
//                    throw new IllegalStateException( "Failed to discover components in API realm " + realmId, e );
//                }
//            }
//        }
//
//        return apiRealm;
    }

    /**
     * Imports Xpp3Dom and associated types into the specified realm. Unlike the other archives that constitute the API
     * realm, plexus-utils is not excluded from the plugin/project realm, yet we must ensure this class is loaded from
     * the API realm and not from the plugin/project realm.
     * 
     * @param importingRealm The realm into which to import Xpp3Dom, must not be {@code null}.
     */
    private void importXpp3Dom( ClassRealm importingRealm )
    {
        String coreRealmId = container.getContainerRealm().getId();
        try
        {
            importingRealm.importFrom( coreRealmId, "org.codehaus.plexus.util.xml.Xpp3Dom" );
            importingRealm.importFrom( coreRealmId, "org.codehaus.plexus.util.xml.pull.XmlPullParser" );
            importingRealm.importFrom( coreRealmId, "org.codehaus.plexus.util.xml.pull.XmlPullParserException" );
            importingRealm.importFrom( coreRealmId, "org.codehaus.plexus.util.xml.pull.XmlSerializer" );
        }
        catch ( NoSuchRealmException e )
        {
            throw new IllegalStateException( e );
        }
    }

    public ClassRealm createProjectRealm( Model model )
    {
        if ( model == null )
        {
            throw new IllegalArgumentException( "model missing" );
        }

        return createRealm( getKey( model ) );
    }

    private String getKey( Model model )
    {
        return "project>" + model.getGroupId() + ":" + model.getArtifactId() + ":" + model.getVersion();
    }

    public ClassRealm createPluginRealm( Plugin plugin )
    {
        if ( plugin == null )
        {
            throw new IllegalArgumentException( "plugin missing" );
        }

        return createRealm( getKey( plugin ) );
    }

    private String getKey( Plugin plugin )
    {
        String version = ArtifactUtils.toSnapshotVersion( plugin.getVersion() );
        return "plugin>" + plugin.getGroupId() + ":" + plugin.getArtifactId() + ":" + version;
    }

}
