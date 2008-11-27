package org.apache.maven.artifact;

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

import org.apache.maven.artifact.versioning.VersionRange;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

public final class ArtifactUtils
{
    private ArtifactUtils()
    {
    }

    public static boolean isSnapshot( String version )
    {
        return ( version != null ) &&
            ( version.toUpperCase().endsWith( Artifact.SNAPSHOT_VERSION ) || Artifact.VERSION_FILE_PATTERN.matcher( version )
                .matches() );
    }

    public static String toSnapshotVersion( String version )
    {
        Matcher m = Artifact.VERSION_FILE_PATTERN.matcher( version );
        if ( m.matches() )
        {
            return m.group( 1 ) + "-" + Artifact.SNAPSHOT_VERSION;
        }
        else
        {
            return version;
        }
    }

    public static String versionlessKey( Artifact artifact )
    {
        return versionlessKey( artifact.getGroupId(), artifact.getArtifactId() );
    }

    public static String versionlessKey( String groupId,
                                         String artifactId )
    {
        if ( groupId == null )
        {
            throw new NullPointerException( "groupId was null" );
        }
        if ( artifactId == null )
        {
            throw new NullPointerException( "artifactId was null" );
        }
        return groupId + ":" + artifactId;
    }

    public static String artifactId( String groupId,
                                     String artifactId,
                                     String type,
                                     String version )
    {
        return artifactId( groupId, artifactId, type, null, version );
    }

    public static String artifactId( String groupId,
                                     String artifactId,
                                     String type,
                                     String classifier,
                                     String baseVersion )
    {
        return groupId + ":" + artifactId + ":" + type + ( classifier != null ? ":" + classifier : "" ) + ":" +
            baseVersion;
    }

    public static Map<String,Artifact> artifactMapByVersionlessId( Collection<Artifact> artifacts )
    {
        Map<String,Artifact> artifactMap = new LinkedHashMap<String,Artifact>();

        if ( artifacts != null )
        {
            for (Artifact artifact : artifacts) {
                artifactMap.put(versionlessKey(artifact), artifact);
            }
        }

        return artifactMap;
    }

    public static Map<String,Artifact> artifactMapByArtifactId( Collection<Artifact> artifacts )
    {
        Map<String,Artifact> artifactMap = new LinkedHashMap<String,Artifact>();

        if ( artifacts != null )
        {
            for (Artifact artifact : artifacts) {
                artifactMap.put(artifact.getId(), artifact);
            }
        }

        return artifactMap;
    }

    public static Artifact copyArtifact( Artifact artifact )
    {
        VersionRange range = artifact.getVersionRange();

        // For some reason with the introduction of MNG-1577 we have the case in Yoko where a depMan section has
        // something like the following:
        //
        // <dependencyManagement>
        //     <dependencies>
        //         <!--  Yoko modules -->
        //         <dependency>
        //             <groupId>org.apache.yoko</groupId>
        //             <artifactId>yoko-core</artifactId>
        //             <version>${version}</version>
        //         </dependency>
        // ...
        //
        // And the range is not set so we'll check here and set it. jvz.

        if ( range == null )
        {
            range = VersionRange.createFromVersion( artifact.getVersion() );
        }

        DefaultArtifact clone = new DefaultArtifact( artifact.getGroupId(), artifact.getArtifactId(), range.cloneOf(),
            artifact.getScope(), artifact.getType(), artifact.getClassifier(),
            artifact.getArtifactHandler(), artifact.isOptional() );
        clone.setRelease( artifact.isRelease() );
        clone.setResolvedVersion( artifact.getVersion() );
        clone.setResolved( artifact.isResolved() );
        clone.setFile( artifact.getFile() );

        clone.setAvailableVersions( copyList( artifact.getAvailableVersions() ) );
        clone.setBaseVersion( artifact.getBaseVersion() );
        clone.setDependencyFilter( artifact.getDependencyFilter() );
        clone.setDependencyTrail( copyList( artifact.getDependencyTrail() ) );
        clone.setDownloadUrl( artifact.getDownloadUrl() );
        clone.setRepository( artifact.getRepository() );

        return clone;
    }

    private static <T> List<T> copyList( List<T> original )
    {
        List<T> copy = null;

        if ( original != null )
        {
            copy = new ArrayList<T>();

            if ( !original.isEmpty() )
            {
                copy.addAll( original );
            }
        }

        return copy;
    }

}
