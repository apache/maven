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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

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
import org.sonatype.aether.artifact.Artifact;

/**
 * Manages the class realms used by Maven. <strong>Warning:</strong> This is an internal utility class that is only
 * public for technical reasons, it is not part of the public API. In particular, this class can be changed or deleted
 * without prior notice.
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

    private ClassRealm mavenRealm;

    private ClassWorld getClassWorld()
    {
        return ( (MutablePlexusContainer) container ).getClassWorld();
    }

    private ClassRealm newRealm( String id )
    {
        ClassWorld world = getClassWorld();

        synchronized ( world )
        {
            String realmId = id;

            Random random = new Random();

            while ( true )
            {
                try
                {
                    ClassRealm classRealm = world.newRealm( realmId, null );

                    if ( logger.isDebugEnabled() )
                    {
                        logger.debug( "Created new class realm " + realmId );
                    }

                    return classRealm;
                }
                catch ( DuplicateRealmException e )
                {
                    realmId = id + '-' + random.nextInt();
                }
            }
        }
    }

    private synchronized ClassRealm getMavenRealm()
    {
        if ( mavenRealm == null )
        {
            mavenRealm = newRealm( "maven.api" );

            importMavenApi( mavenRealm );

            mavenRealm.setParentClassLoader( ClassLoader.getSystemClassLoader() );

            List<ClassRealmManagerDelegate> delegates = getDelegates();
            if ( !delegates.isEmpty() )
            {
                List<ClassRealmConstituent> constituents = new ArrayList<ClassRealmConstituent>();

                ClassRealmRequest request =
                    new DefaultClassRealmRequest( RealmType.Core, null, new ArrayList<String>(), constituents );

                for ( ClassRealmManagerDelegate delegate : delegates )
                {
                    delegate.setupRealm( mavenRealm, request );
                }

                populateRealm( mavenRealm, constituents );
            }
        }
        return mavenRealm;
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
        Set<String> artifactIds = new LinkedHashSet<String>();

        List<ClassRealmConstituent> constituents = new ArrayList<ClassRealmConstituent>();

        if ( artifacts != null )
        {
            for ( Artifact artifact : artifacts )
            {
                artifactIds.add( getId( artifact ) );
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

        ClassRealm classRealm = newRealm( baseRealmId );

        if ( parent != null )
        {
            classRealm.setParentClassLoader( parent );
        }
        else
        {
            classRealm.setParentRealm( getMavenRealm() );
        }

        List<ClassRealmManagerDelegate> delegates = getDelegates();
        if ( !delegates.isEmpty() )
        {
            ClassRealmRequest request = new DefaultClassRealmRequest( type, parent, imports, constituents );

            for ( ClassRealmManagerDelegate delegate : delegates )
            {
                delegate.setupRealm( classRealm, request );
            }
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

        Set<String> includedIds = populateRealm( classRealm, constituents );

        if ( logger.isDebugEnabled() )
        {
            artifactIds.removeAll( includedIds );

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
        ClassRealm coreRealm = getCoreRealm();

        // maven-*
        importingRealm.importFrom( coreRealm, "org.apache.maven" );

        // aether
        importingRealm.importFrom( coreRealm, "org.sonatype.aether" );

        // plexus-classworlds
        importingRealm.importFrom( coreRealm, "org.codehaus.plexus.classworlds" );

        // classworlds (for legacy code)
        importingRealm.importFrom( coreRealm, "org.codehaus.classworlds" );

        // plexus-container, plexus-component-annotations
        importingRealm.importFrom( coreRealm, "org.codehaus.plexus.component" );
        importingRealm.importFrom( coreRealm, "org.codehaus.plexus.configuration" );
        importingRealm.importFrom( coreRealm, "org.codehaus.plexus.container" );
        importingRealm.importFrom( coreRealm, "org.codehaus.plexus.context" );
        importingRealm.importFrom( coreRealm, "org.codehaus.plexus.lifecycle" );
        importingRealm.importFrom( coreRealm, "org.codehaus.plexus.logging" );
        importingRealm.importFrom( coreRealm, "org.codehaus.plexus.personality" );
        importingRealm.importFrom( coreRealm, "org.codehaus.plexus.ComponentRegistry" );
        importingRealm.importFrom( coreRealm, "org.codehaus.plexus.ContainerConfiguration" );
        importingRealm.importFrom( coreRealm, "org.codehaus.plexus.DefaultComponentRegistry" );
        importingRealm.importFrom( coreRealm, "org.codehaus.plexus.DefaultContainerConfiguration" );
        importingRealm.importFrom( coreRealm, "org.codehaus.plexus.DefaultPlexusContainer" );
        importingRealm.importFrom( coreRealm, "org.codehaus.plexus.DuplicateChildContainerException" );
        importingRealm.importFrom( coreRealm, "org.codehaus.plexus.MutablePlexusContainer" );
        importingRealm.importFrom( coreRealm, "org.codehaus.plexus.PlexusConstants" );
        importingRealm.importFrom( coreRealm, "org.codehaus.plexus.PlexusContainer" );
        importingRealm.importFrom( coreRealm, "org.codehaus.plexus.PlexusContainerException" );
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

    private static String getId( Artifact artifact )
    {
        return getId( artifact.getGroupId(), artifact.getArtifactId(), artifact.getExtension(), artifact.getClassifier(),
                      artifact.getBaseVersion() );
    }

    private static String getId( ClassRealmConstituent constituent )
    {
        return getId( constituent.getGroupId(), constituent.getArtifactId(), constituent.getType(),
                      constituent.getClassifier(), constituent.getVersion() );
    }

    private static String getId( String gid, String aid, String type, String cls, String ver )
    {
        return gid + ':' + aid + ':' + type + ( StringUtils.isNotEmpty( cls ) ? ':' + cls : "" ) + ':' + ver;
    }

    private List<ClassRealmManagerDelegate> getDelegates()
    {
        try
        {
            return container.lookupList( ClassRealmManagerDelegate.class );
        }
        catch ( ComponentLookupException e )
        {
            logger.error( "Failed to lookup class realm delegates: " + e.getMessage(), e );

            return Collections.emptyList();
        }
    }

    private Set<String> populateRealm( ClassRealm classRealm, List<ClassRealmConstituent> constituents )
    {
        Set<String> includedIds = new LinkedHashSet<String>();

        if ( logger.isDebugEnabled() )
        {
            logger.debug( "Populating class realm " + classRealm.getId() );
        }

        for ( ClassRealmConstituent constituent : constituents )
        {
            File file = constituent.getFile();

            String id = getId( constituent );
            includedIds.add( id );

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
                logger.error( e.getMessage(), e );
            }
        }

        return includedIds;
    }

}
