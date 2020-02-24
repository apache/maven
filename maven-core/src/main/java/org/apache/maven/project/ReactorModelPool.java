package org.apache.maven.project;

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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.apache.maven.model.Model;

/**
 * Holds all Models that are known to the reactor. This allows the project builder to resolve imported Models from the
 * reactor when building another project's effective model.
 *
 * @author Benjamin Bentmann
 * @Robert Scholte
 */
class ReactorModelPool
{
    private final Map<GAKey, List<Model>> modelsByGa = new HashMap<>();

    private final Map<Path, Model> modelsByPath = new HashMap<>();

    /**
     * This used to be the only method, which  
     *  
     * @param groupId, never {@code null}
     * @param artifactId, never {@code null}
     * @param version, might be {@code null}
     * @return
     * @throws IllegalStateException if version was null and multiple modules share the same groupId + artifactId
     * @throws NoSuchElementException if model could not be found
     */
    public Model get( String groupId, String artifactId, String version )
        throws IllegalStateException, NoSuchElementException
    {
        // TODO DefaultModelBuilder.readParentExternally still tries to use the ReactorModelPool, should be fixed
        // For now, use getOrDefault/orElse instead of get 
        return modelsByGa.getOrDefault( new GAKey( groupId, artifactId ), Collections.emptyList() ).stream()
                        .filter( m -> version == null || version.equals( m.getVersion() ) )
                        .reduce( ( a, b ) -> 
                        {
                            throw new IllegalStateException( "Multiple modules with key "
                                + a.getGroupId() + ':' + a.getArtifactId() );
                        } ).orElse( null );
    }

    public Model get( Path path )
    {
        final Path pomFile;
        if ( Files.isDirectory( path ) )
        {
            pomFile = path.resolve( "pom.xml" );
        }
        else
        {
            pomFile = path;
        }
        return modelsByPath.get( pomFile );
    }

    static class Builder
    {
        private ReactorModelPool pool = new ReactorModelPool();
        
        Builder put( Path pomFile, Model model )
        {
            pool.modelsByPath.put( pomFile, model );
            pool.modelsByGa.computeIfAbsent( new GAKey( model.getGroupId(), model.getArtifactId() ),
                                             k -> new ArrayList<Model>() ).add( model );
            return this;
        }
        
        ReactorModelPool build() 
        {
            return pool;
        }
    }

    private static final class GAKey
    {

        private final String groupId;

        private final String artifactId;

        private final int hashCode;

        GAKey( String groupId, String artifactId )
        {
            this.groupId = ( groupId != null ) ? groupId : "";
            this.artifactId = ( artifactId != null ) ? artifactId : "";

            hashCode = Objects.hash( this.groupId, this.artifactId );
        }

        @Override
        public boolean equals( Object obj )
        {
            if ( this == obj )
            {
                return true;
            }

            GAKey that = (GAKey) obj;

            return artifactId.equals( that.artifactId ) && groupId.equals( that.groupId );
        }

        @Override
        public int hashCode()
        {
            return hashCode;
        }

        @Override
        public String toString()
        {
            StringBuilder buffer = new StringBuilder( 128 );
            buffer.append( groupId ).append( ':' ).append( artifactId );
            return buffer.toString();
        }
    }

}
