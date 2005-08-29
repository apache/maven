package org.apache.maven.plugin.plugin.metadata;

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

import org.apache.maven.artifact.repository.metadata.InvalidRepositoryMetadataException;
import org.apache.maven.artifact.repository.metadata.PluginMappingMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadataManagementException;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;

/**
 * @goal installMapping
 * @phase install
 */
public class PluginMappingInstallMojo
    extends AbstractPluginMappingMojo
{

    public void execute()
        throws MojoExecutionException
    {
        RepositoryMetadata metadata = new PluginMappingMetadata( getProject().getGroupId() );

        try
        {
            try
            {
                getRepositoryMetadataManager().resolveLocally( metadata, getLocalRepository() );
            }
            catch ( InvalidRepositoryMetadataException e )
            {
                getRepositoryMetadataManager().purgeLocalCopy( metadata, getLocalRepository() );
            }

            File metadataFile = updatePluginMap( metadata );

            if ( metadataFile != null )
            {
                getRepositoryMetadataManager().install( metadataFile, metadata, getLocalRepository() );
            }
        }
        catch ( RepositoryMetadataManagementException e )
        {
            throw new MojoExecutionException( "Failed to install " + metadata, e );
        }
    }

}
