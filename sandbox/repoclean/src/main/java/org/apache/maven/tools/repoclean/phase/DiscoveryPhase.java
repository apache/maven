package org.apache.maven.tools.repoclean.phase;

import org.apache.maven.tools.repoclean.RepositoryCleanerConfiguration;
import org.apache.maven.tools.repoclean.discover.ArtifactDiscoverer;
import org.apache.maven.tools.repoclean.report.PathLister;
import org.apache.maven.tools.repoclean.report.Reporter;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

public class DiscoveryPhase
    extends AbstractLogEnabled
    implements Contextualizable
{

    private PlexusContainer container;

    public List execute( File reportsBase, File sourceRepositoryBase, RepositoryCleanerConfiguration configuration,
                         Reporter repoReporter )
        throws Exception
    {
        Logger logger = getLogger();

        ArtifactDiscoverer artifactDiscoverer = null;

        PathLister kickoutLister = null;
        PathLister excludeLister = null;

        List artifacts = new ArrayList();

        try
        {
            artifactDiscoverer = (ArtifactDiscoverer) container.lookup( ArtifactDiscoverer.ROLE, configuration
                .getSourceRepositoryLayout() );

            if ( logger.isDebugEnabled() )
            {
                logger.debug( "Discovering artifacts." );
            }

            try
            {
                File kickoutsList = new File( reportsBase, "kickouts.txt" );
                File excludesList = new File( reportsBase, "excludes.txt" );

                kickoutLister = new PathLister( kickoutsList );
                excludeLister = new PathLister( excludesList );

                artifacts = artifactDiscoverer.discoverArtifacts( sourceRepositoryBase, repoReporter,
                                                                  configuration.getBlacklistedPatterns(), excludeLister,
                                                                  kickoutLister, configuration.isConvertSnapshots() );
            }
            catch ( Exception e )
            {
                repoReporter.error( "Error discovering artifacts in source repository.", e );

                throw e;
            }

        }
        finally
        {
            if ( artifactDiscoverer != null )
            {
                container.release( artifactDiscoverer );
            }

            if ( excludeLister != null )
            {
                excludeLister.close();
            }

            if ( kickoutLister != null )
            {
                kickoutLister.close();
            }
        }

        return artifacts;
    }

    public void contextualize( Context context )
        throws ContextException
    {
        this.container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

}
