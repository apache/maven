package org.apache.maven.bootstrap.model;

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

import org.apache.maven.bootstrap.download.ArtifactResolver;
import org.apache.maven.bootstrap.download.DownloadFailedException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * Utility class for resolving Model dependencies.
 *
 */
public final class ProjectResolver
{
    private static Set inProgress = new HashSet();

    private ProjectResolver()
    {

    }

    public static void resolveDependencies( ArtifactResolver resolver, Model model,
                                            boolean resolveTransitiveDependencies, String inheritedScope, Set excluded )
        throws SAXException
    {
        for ( Iterator it = model.getDependencies().values().iterator(); it.hasNext(); )
        {
            Dependency dependency = (Dependency) it.next();

            if ( !excluded.contains( dependency.getConflictId() ) && !dependency.isOptional() )
            {
                if ( !dependency.getScope().equals( Dependency.SCOPE_TEST ) || inheritedScope == null )
                {
                    if ( dependency.getVersion() == null )
                    {
                        Dependency managedDependency = (Dependency) model.getManagedDependencies().get( dependency
                            .getConflictId() );

                        if ( managedDependency == null )
                        {
                            throw new NullPointerException( "[" + model.getId() + "] " + "Dependency "
                                + dependency.getConflictId()
                                + " is missing a version, and nothing is found in dependencyManagement. " );
                        }
                        dependency.setVersion( managedDependency.getVersion() );
                    }

                    if ( resolveTransitiveDependencies )
                    {
                        Set excluded2 = new HashSet( excluded );
                        excluded2.addAll( dependency.getExclusions() );

                        Model p = retrievePom( resolver, dependency.getGroupId(), dependency.getArtifactId(),
                                               dependency.getVersion(), dependency.getScope(),
                                               resolveTransitiveDependencies, excluded2, dependency.getChain() );

                        addDependencies( p.getAllDependencies(), model.getTransitiveDependencies(), dependency.getScope(),
                                         excluded2 );
                    }
                }
            }
        }
    }

    public static void addDependencies( Collection dependencies, Map target, String inheritedScope, Set excluded )
    {
        for ( Iterator i = dependencies.iterator(); i.hasNext(); )
        {
            Dependency d = (Dependency) i.next();

            // skip test deps
            if ( !Dependency.SCOPE_TEST.equals( d.getScope() ) )
            {
                // Do we care about runtime here?
                if ( Dependency.SCOPE_TEST.equals( inheritedScope ) )
                {
                    d.setScope( Dependency.SCOPE_TEST );
                }

                if ( !hasDependency( d, target ) && !excluded.contains( d.getConflictId() ) && !d.isOptional() )
                {
                    if ( !"plexus".equals( d.getGroupId() )
                        || ( !"plexus-utils".equals( d.getArtifactId() ) && !"plexus-container-default".equals( d
                            .getArtifactId() ) ) )
                    {
                        target.put( d.getConflictId(), d );
                    }
                }
            }
        }
    }

    private static boolean hasDependency( Dependency d, Map dependencies )
    {
        String conflictId = d.getConflictId();
        if ( dependencies.containsKey( conflictId ) )
        {
            // We only care about pushing in compile scope dependencies I think
            // if not, we'll need to be able to get the original and pick the appropriate scope
            if ( d.getScope().equals( Dependency.SCOPE_COMPILE ) )
            {
                dependencies.remove( conflictId );
            }
            else
            {
                return true;
            }
        }
        return false;
    }

    public static Model retrievePom( ArtifactResolver resolver, String groupId, String artifactId, String version,
                                     String inheritedScope, boolean resolveTransitiveDependencies, Set excluded,
                                     List chain )
        throws SAXException
    {
        String key = groupId + ":" + artifactId + ":" + version;

        if ( inProgress.contains( key ) )
        {
            throw new SAXException( "Circular dependency found, looking for " + key + "\nIn progress:" + inProgress );
        }

        inProgress.add( key );

        ModelReader p = new ModelReader( resolver, inheritedScope, resolveTransitiveDependencies, excluded );

        try
        {
            // download the POM
            Dependency pom = new Dependency( groupId, artifactId, version, "pom", chain );

            resolver.downloadDependencies( Collections.singletonList( pom ) );

            // Parse the POM from the local repository into a model
            Model model = p.parseModel( resolver.getArtifactFile( pom ), chain );

            inProgress.remove( key );

            return model;
        }
        catch ( IOException e )
        {
            throw new SAXException( "Error getting parent POM", e );
        }
        catch ( ParserConfigurationException e )
        {
            throw new SAXException( "Error getting parent POM", e );
        }
        catch ( DownloadFailedException e )
        {
            throw new SAXException( "Error getting parent POM", e );
        }
    }
}
