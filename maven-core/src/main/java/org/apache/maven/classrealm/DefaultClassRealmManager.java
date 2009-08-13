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

import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.MutablePlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
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

            classRealm.setParentRealm( container.getContainerRealm() );

            importXpp3Dom( classRealm );

            importMavenApi( classRealm );

            for ( ClassRealmManagerDelegate delegate : getDelegates() )
            {
                delegate.setupRealm( classRealm );
            }

            return classRealm;
        }
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
        ClassRealm coreRealm = container.getContainerRealm();

        importingRealm.importFrom( coreRealm, "org.codehaus.plexus.util.xml.Xpp3Dom" );
        importingRealm.importFrom( coreRealm, "org.codehaus.plexus.util.xml.pull.XmlPullParser" );
        importingRealm.importFrom( coreRealm, "org.codehaus.plexus.util.xml.pull.XmlPullParserException" );
        importingRealm.importFrom( coreRealm, "org.codehaus.plexus.util.xml.pull.XmlSerializer" );
    }

    /**
     * Imports the classes/resources constituting the Maven API into the specified realm.
     * 
     * @param importingRealm The realm into which to import the Maven API, must not be {@code null}.
     */
    private void importMavenApi( ClassRealm importingRealm )
    {
        // maven-*
        importingRealm.importFromParent( "org.apache.maven" );

        // plexus-classworlds
        importingRealm.importFromParent( "org.codehaus.plexus.classworlds" );

        // plexus-container, plexus-component-annotations
        importingRealm.importFromParent( "org.codehaus.plexus.component" );
        importingRealm.importFromParent( "org.codehaus.plexus.configuration" );
        importingRealm.importFromParent( "org.codehaus.plexus.container" );
        importingRealm.importFromParent( "org.codehaus.plexus.context" );
        importingRealm.importFromParent( "org.codehaus.plexus.lifecycle" );
        importingRealm.importFromParent( "org.codehaus.plexus.logging" );
        importingRealm.importFromParent( "org.codehaus.plexus.personality" );
        importingRealm.importFromParent( "org.codehaus.plexus.ComponentRegistry" );
        importingRealm.importFromParent( "org.codehaus.plexus.ContainerConfiguration" );
        importingRealm.importFromParent( "org.codehaus.plexus.DefaultComponentRegistry" );
        importingRealm.importFromParent( "org.codehaus.plexus.DefaultContainerConfiguration" );
        importingRealm.importFromParent( "org.codehaus.plexus.DefaultPlexusContainer" );
        importingRealm.importFromParent( "org.codehaus.plexus.DuplicateChildContainerException" );
        importingRealm.importFromParent( "org.codehaus.plexus.MutablePlexusContainer" );
        importingRealm.importFromParent( "org.codehaus.plexus.PlexusConstants" );
        importingRealm.importFromParent( "org.codehaus.plexus.PlexusContainer" );
        importingRealm.importFromParent( "org.codehaus.plexus.PlexusContainerException" );
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

    private List<ClassRealmManagerDelegate> getDelegates()
    {
        try
        {
            return container.lookupList( ClassRealmManagerDelegate.class );
        }
        catch ( ComponentLookupException e )
        {
            return Collections.emptyList();
        }
    }

}
