package org.apache.maven.extension;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Extension;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.StringUtils;

import java.util.Collections;
import java.util.Iterator;

/**
 * Used to locate extensions.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class DefaultExtensionManager
    implements ExtensionManager, Contextualizable
{
    private ArtifactFactory artifactFactory;

    private ArtifactResolver artifactResolver;

    private ArtifactMetadataSource artifactMetadataSource;

    private PlexusContainer container;

    public void addExtension( Extension extension, MavenProject project, ArtifactRepository localRepository )
        throws ArtifactResolutionException, PlexusContainerException, InvalidVersionSpecificationException
    {
        // TODO: this is duplicated with DefaultMavenProjectBuilder. Push into artifact factory.
        String version;

        if ( StringUtils.isEmpty( extension.getVersion() ) )
        {
            version = "RELEASE";
        }
        else
        {
            version = extension.getVersion();
        }

        VersionRange versionRange = VersionRange.createFromVersionSpec( version );
        Artifact artifact = artifactFactory.createExtensionArtifact( extension.getGroupId(), extension.getArtifactId(),
                                                                     versionRange );

        if ( artifact != null )
        {
            ArtifactResolutionResult result = artifactResolver.resolveTransitively( Collections.singleton( artifact ),
                                                                                    project.getArtifact(),
                                                                                    project.getRemoteArtifactRepositories(),
                                                                                    localRepository,
                                                                                    artifactMetadataSource );
            for ( Iterator i = result.getArtifacts().iterator(); i.hasNext(); )
            {
                Artifact a = (Artifact) i.next();
                container.addJarResource( a.getFile() );
            }
        }
    }

    public void contextualize( Context context )
        throws ContextException
    {
        this.container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }
}
