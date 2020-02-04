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
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.classrealm.ClassRealmRequest.RealmType;
import org.apache.maven.extension.internal.CoreExportsProvider;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.MutablePlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.artifact.Artifact;

/**
 * Manages the class realms used by Maven. <strong>Warning:</strong> This is an internal utility class that is only
 * public for technical reasons, it is not part of the public API. In particular, this class can be changed or deleted
 * without prior notice.
 *
 * @author Benjamin Bentmann
 */
@Named
@Singleton
public class DefaultClassRealmManager
    implements ClassRealmManager
{
    public static final String API_REALMID = "maven.api";

    /**
     * During normal command line build, ClassWorld is loaded by jvm system classloader, which only includes
     * plexus-classworlds jar and possibly javaagent classes, see https://issues.apache.org/jira/browse/MNG-4747.
     * <p>
     * Using ClassWorld to determine plugin/extensions realm parent classloaders gives m2e and integration test harness
     * flexibility to load multiple version of maven into dedicated classloaders without assuming state of jvm system
     * classloader.
     */
    private static final ClassLoader PARENT_CLASSLOADER = ClassWorld.class.getClassLoader();

    private final Logger logger;

    private final ClassWorld world;

    private final ClassRealm containerRealm;

    // this is a live injected collection
    private final List<ClassRealmManagerDelegate> delegates;

    private final ClassRealm mavenApiRealm;

    /**
     * Patterns of artifacts provided by maven core and exported via maven api realm. These artifacts are filtered from
     * plugin and build extensions realms to avoid presence of duplicate and possibly conflicting classes on classpath.
     */
    private final Set<String> providedArtifacts;

    @Inject
    public DefaultClassRealmManager( Logger logger, PlexusContainer container,
                                     List<ClassRealmManagerDelegate> delegates, CoreExportsProvider exports )
    {
        this.logger = logger;
        this.world = ( (MutablePlexusContainer) container ).getClassWorld();
        this.containerRealm = container.getContainerRealm();
        this.delegates = delegates;

        Map<String, ClassLoader> foreignImports = exports.get().getExportedPackages();

        this.mavenApiRealm =
            createRealm( API_REALMID, RealmType.Core, null /* parent */, null /* parentImports */,
                         foreignImports, null /* artifacts */ );

        this.providedArtifacts = exports.get().getExportedArtifacts();
    }

    private ClassRealm newRealm( String id )
    {
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

    public ClassRealm getMavenApiRealm()
    {
        return mavenApiRealm;
    }

    /**
     * Creates a new class realm with the specified parent and imports.
     *
     * @param baseRealmId The base id to use for the new realm, must not be {@code null}.
     * @param type The type of the class realm, must not be {@code null}.
     * @param parent The parent realm for the new realm, may be {@code null}.
     * @param parentImports The packages/types to import from the parent realm, may be {@code null}.
     * @param foreignImports The packages/types to import from foreign realms, may be {@code null}.
     * @param artifacts The artifacts to add to the realm, may be {@code null}. Unresolved artifacts (i.e. with a
     *            missing file) will automatically be excluded from the realm.
     * @return The created class realm, never {@code null}.
     */
    private ClassRealm createRealm( String baseRealmId, RealmType type, ClassLoader parent, List<String> parentImports,
                                    Map<String, ClassLoader> foreignImports, List<Artifact> artifacts )
    {
        Set<String> artifactIds = new LinkedHashSet<>();

        List<ClassRealmConstituent> constituents = new ArrayList<>();

        if ( artifacts != null )
        {
            for ( Artifact artifact : artifacts )
            {
                if ( !isProvidedArtifact( artifact ) )
                {
                    artifactIds.add( getId( artifact ) );
                    if ( artifact.getFile() != null )
                    {
                        constituents.add( new ArtifactClassRealmConstituent( artifact ) );
                    }
                }
            }
        }

        if ( parentImports != null )
        {
            parentImports = new ArrayList<>( parentImports );
        }
        else
        {
            parentImports = new ArrayList<>();
        }

        if ( foreignImports != null )
        {
            foreignImports = new TreeMap<>( foreignImports );
        }
        else
        {
            foreignImports = new TreeMap<>();
        }

        ClassRealm classRealm = newRealm( baseRealmId );

        if ( parent != null )
        {
            classRealm.setParentClassLoader( parent );
        }

        callDelegates( classRealm, type, parent, parentImports, foreignImports, constituents );

        wireRealm( classRealm, parentImports, foreignImports );

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

    public ClassRealm getCoreRealm()
    {
        return containerRealm;
    }

    public ClassRealm createProjectRealm( Model model, List<Artifact> artifacts )
    {
        Objects.requireNonNull( model, "model cannot be null" );

        ClassLoader parent = getMavenApiRealm();

        return createRealm( getKey( model ), RealmType.Project, parent, null, null, artifacts );
    }

    private static String getKey( Model model )
    {
        return "project>" + model.getGroupId() + ":" + model.getArtifactId() + ":" + model.getVersion();
    }

    public ClassRealm createExtensionRealm( Plugin plugin, List<Artifact> artifacts )
    {
        Objects.requireNonNull( plugin, "plugin cannot be null" );

        Map<String, ClassLoader> foreignImports =
            Collections.<String, ClassLoader>singletonMap( "", getMavenApiRealm() );

        return createRealm( getKey( plugin, true ), RealmType.Extension, PARENT_CLASSLOADER, null,
                foreignImports, artifacts );
    }

    private boolean isProvidedArtifact( Artifact artifact )
    {
        return providedArtifacts.contains( artifact.getGroupId() + ":" + artifact.getArtifactId() );
    }

    public ClassRealm createPluginRealm( Plugin plugin, ClassLoader parent, List<String> parentImports,
                                         Map<String, ClassLoader> foreignImports, List<Artifact> artifacts )
    {
        Objects.requireNonNull( plugin, "plugin cannot be null" );

        if ( parent == null )
        {
            parent = PARENT_CLASSLOADER;
        }

        return createRealm( getKey( plugin, false ), RealmType.Plugin, parent, parentImports, foreignImports,
                            artifacts );
    }

    private static String getKey( Plugin plugin, boolean extension )
    {
        String version = ArtifactUtils.toSnapshotVersion( plugin.getVersion() );
        return ( extension ? "extension>" : "plugin>" ) + plugin.getGroupId() + ":" + plugin.getArtifactId() + ":"
            + version;
    }

    private static String getId( Artifact artifact )
    {
        return getId( artifact.getGroupId(), artifact.getArtifactId(), artifact.getExtension(),
                      artifact.getClassifier(), artifact.getBaseVersion() );
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

    private void callDelegates( ClassRealm classRealm, RealmType type, ClassLoader parent, List<String> parentImports,
                                Map<String, ClassLoader> foreignImports, List<ClassRealmConstituent> constituents )
    {
        List<ClassRealmManagerDelegate> delegates = new ArrayList<>( this.delegates );

        if ( !delegates.isEmpty() )
        {
            ClassRealmRequest request =
                new DefaultClassRealmRequest( type, parent, parentImports, foreignImports, constituents );

            for ( ClassRealmManagerDelegate delegate : delegates )
            {
                try
                {
                    delegate.setupRealm( classRealm, request );
                }
                catch ( Exception e )
                {
                    logger.error( delegate.getClass().getName() + " failed to setup class realm " + classRealm + ": "
                        + e.getMessage(), e );
                }
            }
        }
    }

    private Set<String> populateRealm( ClassRealm classRealm, List<ClassRealmConstituent> constituents )
    {
        Set<String> includedIds = new LinkedHashSet<>();

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

    private void wireRealm( ClassRealm classRealm, List<String> parentImports, Map<String, ClassLoader> foreignImports )
    {
        if ( foreignImports != null && !foreignImports.isEmpty() )
        {
            if ( logger.isDebugEnabled() )
            {
                logger.debug( "Importing foreign packages into class realm " + classRealm.getId() );
            }

            for ( Map.Entry<String, ClassLoader> entry : foreignImports.entrySet() )
            {
                ClassLoader importedRealm = entry.getValue();
                String imp = entry.getKey();

                if ( logger.isDebugEnabled() )
                {
                    logger.debug( "  Imported: " + imp + " < " + getId( importedRealm ) );
                }

                classRealm.importFrom( importedRealm, imp );
            }
        }

        if ( parentImports != null && !parentImports.isEmpty() )
        {
            if ( logger.isDebugEnabled() )
            {
                logger.debug( "Importing parent packages into class realm " + classRealm.getId() );
            }

            for ( String imp : parentImports )
            {
                if ( logger.isDebugEnabled() )
                {
                    logger.debug( "  Imported: " + imp + " < " + getId( classRealm.getParentClassLoader() ) );
                }

                classRealm.importFromParent( imp );
            }
        }
    }

    private String getId( ClassLoader classLoader )
    {
        if ( classLoader instanceof ClassRealm )
        {
            return ( (ClassRealm) classLoader ).getId();
        }
        return String.valueOf( classLoader );
    }

}
