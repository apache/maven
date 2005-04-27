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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.tools.repoclean.artifact.metadata.ProjectMetadata;
import org.apache.maven.tools.repoclean.digest.DigestVerifier;
import org.apache.maven.tools.repoclean.discover.ArtifactDiscoverer;
import org.apache.maven.tools.repoclean.index.ArtifactIndexer;
import org.apache.maven.tools.repoclean.report.FileReporter;
import org.apache.maven.tools.repoclean.report.PathLister;
import org.apache.maven.tools.repoclean.report.ReportWriteException;
import org.apache.maven.tools.repoclean.report.Reporter;
import org.apache.maven.tools.repoclean.rewrite.ArtifactPomRewriter;
import org.apache.maven.tools.repoclean.transaction.RewriteTransaction;
import org.apache.maven.tools.repoclean.transaction.RollbackException;
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
import org.codehaus.plexus.util.IOUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * @author jdcasey
 */
public class RepositoryCleaner extends AbstractLogEnabled implements Contextualizable
{

    public static final String ROLE = RepositoryCleaner.class.getName();

    private static final String REPORTS_DIR_DATE_FORMAT = "dd-MMM-yyyy_hh.mm.ss";

    private DigestVerifier digestVerifier;

    private ArtifactRepositoryLayout bridgingLayout;

    private MailSender mailSender;

    private ArtifactIndexer artifactIndexer;

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

                ArtifactDiscoverer artifactDiscoverer = null;

                List artifacts = null;

                PathLister kickoutLister = null;
                PathLister excludeLister = null;

                try
                {
                    artifactDiscoverer = (ArtifactDiscoverer) container.lookup( ArtifactDiscoverer.ROLE,
                                                                                configuration.getSourceRepositoryLayout() );

                    if ( logger.isInfoEnabled() )
                    {
                        logger.info( "Discovering artifacts." );
                    }

                    try
                    {
                        File kickoutsList = new File( reportsBase, "kickouts.txt" );
                        File excludesList = new File( reportsBase, "excludes.txt" );

                        kickoutLister = new PathLister( kickoutsList );
                        excludeLister = new PathLister( excludesList );

                        artifacts = artifactDiscoverer.discoverArtifacts( sourceRepositoryBase, repoReporter,
                                                                          configuration.getBlacklistedPatterns(),
                                                                          excludeLister, kickoutLister );
                    }
                    catch ( Exception e )
                    {
                        repoReporter.error( "Error discovering artifacts in source repository.", e );
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

                if ( artifacts != null )
                {
                    ArtifactRepositoryLayout sourceLayout = null;
                    ArtifactRepositoryLayout targetLayout = null;
                    try
                    {
                        sourceLayout = (ArtifactRepositoryLayout) container.lookup( ArtifactRepositoryLayout.ROLE,
                                                                                    configuration.getSourceRepositoryLayout() );

                        ArtifactRepository sourceRepo = new ArtifactRepository( "source", "file://" +
                                                                                          sourceRepositoryBase.getAbsolutePath(),
                                                                                sourceLayout );

                        targetLayout = (ArtifactRepositoryLayout) container.lookup( ArtifactRepositoryLayout.ROLE,
                                                                                    configuration.getTargetRepositoryLayout() );

                        ArtifactRepository targetRepo = new ArtifactRepository( "target", "file://" +
                                                                                          targetRepositoryBase.getAbsolutePath(),
                                                                                targetLayout );

                        if ( logger.isInfoEnabled() )
                        {
                            logger.info( "Rewriting POMs and artifact files." );
                        }

                        artifactIndexer.writeAritfactIndex( artifacts, targetRepositoryBase );

                        rewriteArtifactsAndPoms( artifacts, sourceRepo, targetRepo, configuration, reportsBase,
                                                 sourceRepositoryBase, targetRepositoryBase, repoReporter );
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

                if ( repoReporter.hasWarning() && logger.isWarnEnabled() )
                {
                    logger.warn(
                        "Warning encountered while rewriting one or more artifacts from source repository to target repository." );
                }
            }
            finally
            {
                if ( repoReporter != null )
                {
                    repoReporter.close();
                }
            }

            // if we wrote a repository report, and the configuration says to email the report, then do it.
            if ( repoReporter.hasError() && configuration.mailErrorReport() )
            {
                logger.info( "Sending error report to " + configuration.getErrorReportToName() + " via email." );

                MailMessage message = new MailMessage();

                StringBuffer contentBuffer = new StringBuffer();

                contentBuffer.append( "Errors occurred while performing maven-1 to maven-2 repository conversion.\n\n" +
                                      "For more details, see:\n\n" );

                contentBuffer.append( configuration.getErrorReportLink().replaceAll("#date", dateSubdir) );

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
                }
            }
        }

    }

    private void rewriteArtifactsAndPoms( List artifacts, ArtifactRepository sourceRepo, ArtifactRepository targetRepo,
                                          RepositoryCleanerConfiguration configuration, File reportsBase,
                                          File sourceRepositoryBase, File targetRepositoryBase,
                                          FileReporter repoReporter )
        throws Exception
    {
        Logger logger = getLogger();

        ArtifactPomRewriter artifactPomRewriter = null;

        try
        {
            logger.info( "Rewriting up to " + artifacts.size() + " artifacts (Should be " + ( artifacts.size() * 2 ) +
                         " rewrites including POMs)." );

            int actualRewriteCount = 0;
            for ( Iterator it = artifacts.iterator(); it.hasNext(); )
            {
                Artifact artifact = (Artifact) it.next();

                RewriteTransaction transaction = new RewriteTransaction( artifact );

                String artifactReportPath = buildArtifactReportPath( artifact );

                FileReporter artifactReporter = null;
                try
                {
                    artifactReporter = new FileReporter( reportsBase, artifactReportPath );

                    boolean errorOccurred = false;

                    File artifactSource = new File( sourceRepo.getBasedir(), sourceRepo.pathOf( artifact ) );
                    File artifactTarget = new File( targetRepo.getBasedir(), targetRepo.pathOf( artifact ).replace(
                        '+', '-' ) );

                    transaction.addFile( artifactTarget );

                    artifact.setFile( artifactSource );

                    boolean targetMissingOrOlder = !artifactTarget.exists() ||
                        artifactTarget.lastModified() < artifactSource.lastModified();

                    if ( artifactSource.exists() && ( configuration.force() || targetMissingOrOlder ) )
                    {
                        actualRewriteCount++;

                        transaction.addFile( artifactTarget );

                        try
                        {
                            if ( !configuration.reportOnly() )
                            {
                                if ( logger.isDebugEnabled() )
                                {
                                    logger.debug( "sourceRepo basedir is: \'" + sourceRepo.getBasedir() + "\'" );
                                    logger.debug( "targetRepo basedir is: \'" + targetRepo.getBasedir() + "\'" );
                                }

                                File targetParent = artifactTarget.getParentFile();
                                if ( !targetParent.exists() )
                                {
                                    targetParent.mkdirs();
                                }

                                if ( logger.isDebugEnabled() )
                                {
                                    logger.debug( "Copying artifact[" + artifact.getId() + "] from \'" +
                                                  artifactSource + "\' to \'" + artifactTarget + "\'." );
                                }

                                copyArtifact( artifact, artifactTarget, artifactReporter );
                            }
                        }
                        catch ( Exception e )
                        {
                            repoReporter.error( "Error transferring artifact[" + artifact.getId() +
                                                "] to the target repository.", e );

                            throw e;
                        }

                        if ( logger.isDebugEnabled() )
                        {
                            logger.debug( "working on digest for artifact[" + artifact.getId() + "] with groupId: \'" +
                                          artifact.getGroupId() + "\'" );
                        }

                        try
                        {
                            digestVerifier.verifyDigest( artifactSource, artifactTarget, transaction, artifactReporter,
                                                         configuration.reportOnly() );
                        }
                        catch ( Exception e )
                        {
                            repoReporter.error( "Error verifying digest for artifact[" + artifact.getId() + "]", e );

                            throw e;
                        }

                        ArtifactMetadata pom = new ProjectMetadata( artifact );

                        artifactPomRewriter = (ArtifactPomRewriter) container.lookup( ArtifactPomRewriter.ROLE,
                                                                                      configuration.getSourcePomVersion() );

                        File sourcePom = new File( sourceRepositoryBase, sourceRepo.pathOfMetadata( pom ) );

                        File targetPom = new File( targetRepositoryBase,
                                                   targetRepo.pathOfMetadata( pom ).replace( '+', '-' ) );

                        transaction.addFile( targetPom );

                        File bridgedTargetPom = new File( targetRepositoryBase, bridgingLayout.pathOfMetadata( pom ).replace(
                            '+', '-' ) );

                        transaction.addFile( bridgedTargetPom );

                        try
                        {
                            artifactPomRewriter.rewrite( artifact, sourcePom, targetPom, artifactReporter,
                                                         configuration.reportOnly() );

                            boolean wroteBridge = bridgePomLocations( targetPom, bridgedTargetPom, artifactReporter );

                            digestVerifier.verifyDigest( sourcePom, targetPom, transaction, artifactReporter,
                                                         configuration.reportOnly() );

                            if ( wroteBridge )
                            {
                                digestVerifier.verifyDigest( sourcePom, bridgedTargetPom, transaction,
                                                             artifactReporter, configuration.reportOnly() );
                            }

                        }
                        catch ( Exception e )
                        {
                            repoReporter.error( "Error rewriting POM for artifact[" + artifact.getId() +
                                                "] into the target repository.\n Error message: " + e.getMessage() );

                            throw e;
                        }

                    }
                    else if ( !targetMissingOrOlder )
                    {
                        artifactReporter.warn( "Target file for artifact is present and not stale. (Artifact: \'" +
                                               artifact.getId() + "\' in path: \'" + artifactSource +
                                               "\' with target path: " + artifactTarget + ")." );
                    }
                    else
                    {
                        artifactReporter.error( "Cannot find source file for artifact: \'" + artifact.getId() +
                                                "\' under path: \'" + artifactSource + "\'" );
                    }

                    if ( artifactReporter.hasError() )
                    {
                        repoReporter.warn( "Error(s) occurred while rewriting artifact: \'" + artifact.getId() +
                                           "\' or its POM." );
                    }
                }
                catch ( Exception e )
                {
                    repoReporter.warn( "Rolling back conversion for: " + artifact );
                    if ( !configuration.force() )
                    {
                        try
                        {
                            transaction.rollback();
                        }
                        catch ( RollbackException re )
                        {
                            repoReporter.error( "Error rolling back conversion transaction.", re );
                        }
                    }

                    artifactReporter.error( "Error while rewriting file or POM for artifact: \'" + artifact.getId() +
                                            "\'. See report at: \'" + artifactReportPath + "\'.", e );
                }
                finally
                {
                    if ( artifactReporter != null )
                    {
                        artifactReporter.close();
                    }
                }
            }

            logger.info( "Actual number of artifacts rewritten: " + actualRewriteCount + " (" +
                         ( actualRewriteCount * 2 ) + " including POMs)." );
        }
        finally
        {
            if ( artifactPomRewriter != null )
            {
                container.release( artifactPomRewriter );
            }
        }
    }

    private boolean bridgePomLocations( File targetPom, File bridgedTargetPom, Reporter reporter )
        throws IOException, ReportWriteException
    {
        if ( targetPom.equals( bridgedTargetPom ) )
        {
            reporter.warn( "Cannot create legacy-compatible copy of POM at: " + targetPom +
                           "; legacy-compatible path is the same as the converted POM itself." );

            return false;
        }

        FileInputStream in = null;
        FileOutputStream out = null;

        try
        {
            in = new FileInputStream( targetPom );
            out = new FileOutputStream( bridgedTargetPom );

            IOUtil.copy( in, out );
        }
        finally
        {
            IOUtil.close( in );
            IOUtil.close( out );
        }

        return true;
    }

    private String buildArtifactReportPath( Artifact artifact )
    {
        String classifier = artifact.getClassifier();
        String groupId = artifact.getGroupId().replace( '.', '/' );
        String artifactId = artifact.getArtifactId();
        String type = artifact.getType();
        String version = artifact.getVersion();

        return groupId + "/" + artifactId + "/" + type + "/" +
            ( ( classifier != null ) ? ( classifier + "-" ) : ( "" ) ) + version + ".report.txt";
    }

    private void copyArtifact( Artifact artifact, File artifactTarget, FileReporter reporter )
        throws IOException
    {
        File artifactSource = artifact.getFile();

        InputStream inStream = null;
        OutputStream outStream = null;
        try
        {
            File targetParent = artifactTarget.getParentFile();
            if ( !targetParent.exists() )
            {
                targetParent.mkdirs();
            }

            inStream = new BufferedInputStream( new FileInputStream( artifactSource ) );
            outStream = new BufferedOutputStream( new FileOutputStream( artifactTarget ) );

            byte[] buffer = new byte[16];
            int read = -1;

            while ( ( read = inStream.read( buffer ) ) > -1 )
            {
                outStream.write( buffer, 0, read );
            }

            outStream.flush();
        }
        finally
        {
            IOUtil.close( inStream );
            IOUtil.close( outStream );
        }
    }

    private File normalizeTargetRepositoryBase( String targetRepositoryPath )
    {
        Logger logger = getLogger();

        File targetRepositoryBase = new File( targetRepositoryPath );

        logger.info( "Target repository is at: \'" + targetRepositoryBase + "\'" );

        if ( !targetRepositoryBase.exists() )
        {
            logger.info( "Creating target repository at: \'" + targetRepositoryBase + "\'." );

            targetRepositoryBase.mkdirs();
        }
        else if ( !targetRepositoryBase.isDirectory() )
        {
            logger.error( "Cannot write to target repository \'" + targetRepositoryBase +
                          "\' because it is not a directory." );

            targetRepositoryBase = null;
        }

        return targetRepositoryBase;
    }

    private File normalizeSourceRepositoryBase( String sourceRepositoryPath )
    {
        Logger logger = getLogger();

        File sourceRepositoryBase = new File( sourceRepositoryPath );

        logger.info( "Source repository is at: \'" + sourceRepositoryBase + "\'" );

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
            logger.info( "Creating reports directory: \'" + reportsBase + "\'" );

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