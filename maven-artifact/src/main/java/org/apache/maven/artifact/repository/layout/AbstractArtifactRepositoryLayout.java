package org.apache.maven.artifact.repository.layout;

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
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerNotFoundException;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.codehaus.plexus.util.StringUtils;

/**
 * @author jdcasey
 */
public abstract class AbstractArtifactRepositoryLayout
    implements ArtifactRepositoryLayout
{
    private ArtifactHandlerManager artifactHandlerManager;

    protected abstract String layoutPattern();

    protected abstract String metadataLayoutPattern();

    protected abstract String groupIdAsPath( String groupId );

    public String pathOf( Artifact artifact )
        throws ArtifactPathFormatException
    {
        String path = basicPathOf( artifact, layoutPattern() );

        if ( artifact.hasClassifier() )
        {
            path = StringUtils.replace( path, "${classifier}", artifact.getClassifier() );
        }
        else
        {
            path = StringUtils.replace( path, "-${classifier}", "" );
        }

        return path;
    }

    public String pathOfMetadata( ArtifactMetadata metadata )
        throws ArtifactPathFormatException
    {
        String path = basicPathOf( metadata.getArtifact(), metadataLayoutPattern() );

        path = StringUtils.replace( path, "${metadataSuffix}", metadata.getFilenameSuffix() );

        return path;
    }

    private String basicPathOf( Artifact artifact, String pattern )
        throws ArtifactPathFormatException
    {
        String path = pattern;

        String groupPath = groupIdAsPath( artifact.getGroupId() );

        path = StringUtils.replace( path, "${groupPath}", groupPath );

        path = StringUtils.replace( path, "${artifactId}", artifact.getArtifactId() );

        path = StringUtils.replace( path, "${version}", artifact.getVersion() );

        ArtifactHandler artifactHandler = null;
        try
        {
            artifactHandler = artifactHandlerManager.getArtifactHandler( artifact.getType() );
        }
        catch ( ArtifactHandlerNotFoundException e )
        {
            throw new ArtifactPathFormatException( "Cannot find ArtifactHandler for artifact: \'" + artifact.getId() +
                                                   "\'.", e );
        }

        path = StringUtils.replace( path, "${directory}", artifactHandler.directory() );

        path = StringUtils.replace( path, "${extension}", artifactHandler.extension() );
        return path;
    }

}