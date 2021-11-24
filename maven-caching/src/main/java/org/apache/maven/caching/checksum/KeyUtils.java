package org.apache.maven.caching.checksum;

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

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.caching.xml.build.Artifact;
import org.apache.maven.project.MavenProject;

/**
 * KeyUtils
 */
public class KeyUtils
{

    private static final String SEPARATOR = ":";

    public static String getProjectKey( MavenProject project )
    {
        return getProjectKey( project.getGroupId(), project.getArtifactId(), project.getVersion() );
    }

    public static String getProjectKey( String groupId, String artifactId, String version )
    {
        return StringUtils.joinWith( SEPARATOR, groupId, artifactId, version );
    }

    public static String getVersionlessProjectKey( MavenProject project )
    {
        return StringUtils.joinWith( SEPARATOR, project.getGroupId(), project.getArtifactId() );
    }

    public static String getArtifactKey( Artifact artifact )
    {
        return getArtifactKey( artifact.getGroupId(), artifact.getArtifactId(), artifact.getType(),
                artifact.getClassifier(), artifact.getVersion() );
    }

    public static String getVersionlessArtifactKey( org.apache.maven.artifact.Artifact artifact )
    {
        return getVersionlessArtifactKey( artifact.getGroupId(), artifact.getArtifactId(), artifact.getType(),
                artifact.getClassifier() );
    }


    /**
     * Computes the key for the given artifact, using the given type instead of the one defined in the artifact.
     */
    public static String getArtifactKey( Artifact artifact, String type )
    {
        return getArtifactKey( artifact.getGroupId(), artifact.getArtifactId(), type,
                artifact.getClassifier(), artifact.getVersion() );
    }

    public static String getArtifactKey( org.apache.maven.artifact.Artifact artifact )
    {
        return getArtifactKey( artifact.getGroupId(), artifact.getArtifactId(), artifact.getType(),
                artifact.getClassifier(), artifact.getVersion() );
    }

    public static String getArtifactKey( String groupId, String artifactId, String type,
                                         String classifier, String version )
    {
        if ( classifier != null )
        {
            return StringUtils.joinWith( SEPARATOR, groupId, artifactId, type, classifier, version );
        }
        else
        {
            return StringUtils.joinWith( SEPARATOR, groupId, artifactId, type, version );
        }
    }

    public static String getVersionlessArtifactKey( String groupId, String artifactId, String type,
                                                    String classifier )
    {
        if ( classifier != null )
        {
            return StringUtils.joinWith( SEPARATOR, groupId, artifactId, type, classifier );
        }
        else
        {
            return StringUtils.joinWith( SEPARATOR, groupId, artifactId, type );
        }
    }
}
