package org.apache.maven;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.request.ArtifactRequestTransformation;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

import java.util.Map;
import java.util.Set;

public class JarOverrideRequestTransformation
    implements ArtifactRequestTransformation
{
    public Artifact transform( Artifact artifact,
                               ArtifactRepository localRepository,
                               Set remoteRepositories,
                               Map parameters )
        throws Exception
    {
/* TODO: need an override in the POM
        MavenProject project = (MavenProject) parameters.get( "project" );

        boolean mavenJarOverride = project.getBooleanProperty( "maven.jar.override" );

        String mavenJarProperty = project.getProperty( "maven.jar." + project.getArtifactId() );

        if ( mavenJarOverride && StringUtils.isNotEmpty( mavenJarProperty ) )
        {
            Artifact transformedArtifact = new DefaultArtifact( artifact.getGroupId(),
                                                                artifact.getArtifactId(),
                                                                mavenJarProperty,
                                                                artifact.getType() );
            return transformedArtifact;
        }
*/
        return artifact;
    }
}
