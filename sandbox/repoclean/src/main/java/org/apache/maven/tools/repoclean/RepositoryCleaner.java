package org.apache.maven.tools.repoclean;

/*
 * ====================================================================
 * Copyright 2001-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License. ====================================================================
 */

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.tools.repoclean.index.ArtifactIndexer;
import org.apache.maven.tools.repoclean.phase.DiscoveryPhase;
import org.apache.maven.tools.repoclean.phase.RewritePhase;
import org.apache.maven.tools.repoclean.report.FileReporter;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.mailsender.MailMessage;
import org.codehaus.plexus.mailsender.MailSender;
import org.codehaus.plexus.mailsender.MailSenderException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author jdcasey
 */
public class RepositoryCleaner
    extends AbstractLogEnabled
    implements Contextualizable
{

    public static final String ROLE = RepositoryCleaner.class.getName();

    private static final String REPORTS_DIR_DATE_FORMAT = "dd-MMM-yyyy_hh.mm.ss";

    private MailSender mailSender;

    private ArtifactIndexer artifactIndexer;

    private DiscoveryPhase discoveryPhase;

    private RewritePhase rewritePhase;

    private PlexusContainer container;

    private String dateSubdir;

    public void cleanRepository( RepositoryCleanerConfiguration configuration )
        throws Exception
    {
        File reportsBase = formatReportsBase( configuration.getReportsPath() );

        File sourceRepositoryBase = normalizeSourceRepositoryBase( configuration.getSourceRepositoryPath() );

        File targetRepositoryBase = normalizeTargetRepositoryBase( configuration.getTargetRepositoryPath() );

        // do not proceed if we cannot produce reports, or if the repository is
        // invalid.
        if ( reportsBase != null && sourceRepositoryBase != null && targetRepositoryBase != null )
        {
            Logger logger = getLogger();

            FileReporter repoReporter = null;
            try
            {
                repoReporter = new FileReporter( reportsBase, "repository.report.txt" );

                List artifacts;

                artifacts = discoveryPhase.execute( reportsBase, sourceRepositoryBase, configuration, repoReporter );

                if ( !artifacts.isEmpty() )
                {
                    ArtifactRepositoryLayout sourceLayout = null;
                    ArtifactRepositoryLayout targetLayout = null;
                    try
                    {
                        sourceLayout = (ArtifactRepositoryLayout) container.lookup( ArtifactRepositoryLayout.ROLE,
                                                                                    configuration
                                                                                        .getSourceRepositoryLayout() );

                        ArtifactRepository sourceRepo = new ArtifactRepository( "source", "file://"
                            + sourceRepositoryBase.getAbsolutePath(), sourceLayout );

                        targetLayout = (ArtifactRepositoryLayout) container.lookup( ArtifactRepositoryLayout.ROLE,
                                                                                    configuration
                                                                                        .getTargetRepositoryLayout() );

                        ArtifactRepository targetRepo = new ArtifactRepository( "target", "file://"
                            + targetRepositoryBase.getAbsolutePath(), targetLayout );

                        if ( logger.isDebugEnabled() )
                        {
                            logger.debug( "Rewriting POMs and artifact files." );
                        }

                        //                        List originalArtifacts = new ArrayList( artifacts );

                        List rewritten = rewritePhase.execute( artifacts, sourceRepo, targetRepo, configuration,
                                                               reportsBase, repoReporter );

                        artifactIndexer.writeAritfactIndex( rewritten, targetRepositoryBase );
                    }
                    finally
                    {
                        if ( sourceLayout != null )
                        {
                            container.release( sourceLayout );
                        }

                        if ( targetLayout != null )
                        {
                            container.release( targetLayout );
                        }
                    }
                }

                if ( repoReporter.hasError() && logger.isErrorEnabled() )
                {
                    logger.error( "Error encountered while converting source repository to target repository." );
                }

                if ( repoReporter.hasWarning() && logger.isDebugEnabled() )
                {
                    logger
                        .warn( "Warning encountered while rewriting one or more artifacts from source repository to target repository." );
                }
            }
            finally
            {
                if ( repoReporter != null )
                {
                    repoReporter.close();
                }
            }

            // if we wrote a repository report with an error in it, and the configuration says to email the report, 
            // then do it.
            if ( repoReporter.hasError() && configuration.mailErrorReport() )
            {
                logger.debug( "Sending error report to " + configuration.getErrorReportToName() + " via email." );

                MailMessage message = new MailMessage();

                StringBuffer contentBuffer = new StringBuffer();

                contentBuffer.append( "Errors occurred while performing maven-1 to maven-2 repository conversion.\n\n"
                    + "For more details, see:\n\n" );

                contentBuffer.append( configuration.getErrorReportLink().replaceAll( "#date", dateSubdir ) );

                message.setContent( contentBuffer.toString() );
                message.setSubject( configuration.getErrorReportSubject() );
                message.setFrom( configuration.getErrorReportFromAddress(), configuration.getErrorReportFromName() );
                message.setSendDate( new Date() );
                message.addTo( configuration.getErrorReportToAddress(), configuration.getErrorReportToName() );

                try
                {
                    mailSender.send( message );
                }
                catch ( MailSenderException e )
                {
                    logger.error( "An error occurred while trying to email repoclean report.", e );

                    throw e;
                }
            }
        }

    }

    private File normalizeTargetRepositoryBase( String targetRepositoryPath )
    {
        Logger logger = getLogger();

        File targetRepositoryBase = new File( targetRepositoryPath );

        logger.debug( "Target repository is at: \'" + targetRepositoryBase + "\'" );

        if ( !targetRepositoryBase.exists() )
        {
            logger.debug( "Creating target repository at: \'" + targetRepositoryBase + "\'." );

            targetRepositoryBase.mkdirs();
        }
        else if ( !targetRepositoryBase.isDirectory() )
        {
            logger.error( "Cannot write to target repository \'" + targetRepositoryBase
                + "\' because it is not a directory." );

            targetRepositoryBase = null;
        }

        return targetRepositoryBase;
    }

    private File normalizeSourceRepositoryBase( String sourceRepositoryPath )
    {
        Logger logger = getLogger();

        File sourceRepositoryBase = new File( sourceRepositoryPath );

        logger.debug( "Source repository is at: \'" + sourceRepositoryBase + "\'" );

        if ( !sourceRepositoryBase.exists() )
        {
            logger.error( "Cannot convert repository \'" + sourceRepositoryBase + "\' because it does not exist." );

            sourceRepositoryBase = null;
        }
        else if ( !sourceRepositoryBase.isDirectory() )
        {
            logger.error( "Cannot convert repository \'" + sourceRepositoryBase + "\' because it is not a directory." );

            sourceRepositoryBase = null;
        }

        return sourceRepositoryBase;
    }

    private File formatReportsBase( String reportsPath )
    {
        Logger logger = getLogger();

        SimpleDateFormat dateFormat = new SimpleDateFormat( REPORTS_DIR_DATE_FORMAT );

        this.dateSubdir = dateFormat.format( new Date() );

        File allReportsBase = new File( reportsPath );

        File reportsBase = new File( allReportsBase, dateSubdir );

        if ( reportsBase.exists() && !reportsBase.isDirectory() )
        {
            logger.error( "Cannot write reports to \'" + reportsBase + "\' because it is not a directory." );

            reportsBase = null;
        }
        else
        {
            logger.debug( "Creating reports directory: \'" + reportsBase + "\'" );

            reportsBase.mkdirs();
        }

        return reportsBase;
    }

    public void contextualize( Context context )
        throws ContextException
    {
        this.container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

}