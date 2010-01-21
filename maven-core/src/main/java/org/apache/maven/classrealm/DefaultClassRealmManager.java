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

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.classrealm.ClassRealmRequest.RealmType;
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
import org.codehaus.plexus.util.StringUtils;

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

    /**
     * Creates a new class realm with the specified parent and imports.
     * 
     * @param baseRealmId The base id to use for the new realm, must not be {@code null}.
     * @param type The type of the class realm, must not be {@code null}.
     * @param parent The parent realm for the new realm, may be {@code null} to use the Maven core realm.
     * @param imports The packages/types to import from the parent realm, may be {@code null}.
     * @param artifacts The artifacts to add to the realm, may be {@code null}. Unresolved artifacts (i.e. with a
     *            missing file) will automatically be excluded from the realm.
     * @return The created class realm, never {@code null}.
     */
    private ClassRealm createRealm( String baseRealmId, RealmType type, ClassLoader parent, List<String> imports,
                                    boolean importXpp3Dom, List<Artifact> artifacts )
    {
        Set<String> artifactIds = new HashSet<String>();

        List<ClassRealmConstituent> constituents = new ArrayList<ClassRealmConstituent>();

        if ( artifacts != null )
        {
            for ( Artifact artifact : artifacts )
            {
                artifactIds.add( artifact.getId() );
                if ( artifact.getFile() != null )
                {
                    constituents.add( new ArtifactClassRealmConstituent( artifact ) );
                }
            }
        }

        if ( imports != null )
        {
            imports = new ArrayList<String>( imports );
        }
        else
        {
            imports = new ArrayList<String>();
        }

        ClassRealmRequest request = new DefaultClassRealmRequest( type, parent, imports, constituents );

        ClassRealm classRealm;

        ClassWorld world = getClassWorld();

        synchronized ( world )
        {
            String realmId = baseRealmId;

            Random random = new Random();

            while ( true )
            {
                try
                {
                    classRealm = world.newRealm( realmId, null );

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
        }

        if ( parent != null )
        {
            classRealm.setParentClassLoader( parent );
        }
        else
        {
            classRealm.setParentRealm( getCoreRealm() );
            importMavenApi( classRealm );
        }

        for ( ClassRealmManagerDelegate delegate : getDelegates() )
        {
            delegate.setupRealm( classRealm, request );
        }

        if ( importXpp3Dom )
        {
            importXpp3Dom( classRealm );
        }

        if ( !imports.isEmpty() )
        {
            ClassLoader importedRealm = classRealm.getParentClassLoader();

            if ( logger.isDebugEnabled() )
            {
                logger.debug( "Importing packages into class realm " + classRealm.getId() );
            }

            for ( String imp : imports )
            {
                if ( logger.isDebugEnabled() )
                {
                    logger.debug( "  Imported: " + imp );
                }

                classRealm.importFrom( importedRealm, imp );
            }
        }

        if ( logger.isDebugEnabled() )
        {
            logger.debug( "Populating class realm " + classRealm.getId() );
        }

        for ( ClassRealmConstituent constituent : constituents )
        {
            File file = constituent.getFile();

            String id = getId( constituent );
            artifactIds.remove( id );

            if ( logger.isDebugEnabled() )
            {
                logger.debug( "  Included: " + id );
            }

            try
            {
                classRealm.addURL( file.toURI().toURL() );
            }
            catch ( MalformedURLException e )
            {
                // Not going to happen
            }
        }

        if ( logger.isDebugEnabled() )
        {
            for ( String id : artifactIds )
            {
                logger.debug( "  Excluded: " + id );
            }
        }

        return classRealm;
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
        ClassRealm coreRealm = getCoreRealm();

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

        // classworlds (for legacy code)
        importingRealm.importFromParent( "org.codehaus.classworlds" );

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

    public ClassRealm getCoreRealm()
    {
        return container.getContainerRealm();
    }

    public ClassRealm createProjectRealm( Model model, List<Artifact> artifacts )
    {
        if ( model == null )
        {
            throw new IllegalArgumentException( "model missing" );
        }

        return createRealm( getKey( model ), RealmType.Project, null, null, false, artifacts );
    }

    private static String getKey( Model model )
    {
        return "project>" + model.getGroupId() + ":" + model.getArtifactId() + ":" + model.getVersion();
    }

    public ClassRealm createExtensionRealm( Plugin plugin, List<Artifact> artifacts )
    {
        if ( plugin == null )
        {
            throw new IllegalArgumentException( "extension plugin missing" );
        }

        return createRealm( getKey( plugin, true ), RealmType.Extension, null, null, true, artifacts );
    }

    public ClassRealm createPluginRealm( Plugin plugin, ClassLoader parent, List<String> imports,
                                         List<Artifact> artifacts )
    {
        if ( plugin == null )
        {
            throw new IllegalArgumentException( "plugin missing" );
        }

        return createRealm( getKey( plugin, false ), RealmType.Plugin, parent, imports, true, artifacts );
    }

    private static String getKey( Plugin plugin, boolean extension )
    {
        String version = ArtifactUtils.toSnapshotVersion( plugin.getVersion() );
        return ( extension ? "extension>" : "plugin>" ) + plugin.getGroupId() + ":" + plugin.getArtifactId() + ":"
            + version;
    }

    private static String getId( ClassRealmConstituent constituent )
    {
        return constituent.getGroupId() + ':' + constituent.getArtifactId() + ':' + constituent.getType()
            + ( StringUtils.isNotEmpty( constituent.getClassifier() ) ? ':' + constituent.getClassifier() : "" ) + ':'
            + constituent.getVersion();
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
