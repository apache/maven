package org.apache.maven.tools.repoclean.phase;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.metadata.ReleaseArtifactMetadata;
import org.apache.maven.artifact.metadata.SnapshotArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.tools.repoclean.RepositoryCleanerConfiguration;
import org.apache.maven.tools.repoclean.artifact.metadata.ProjectMetadata;
import org.apache.maven.tools.repoclean.digest.DigestException;
import org.apache.maven.tools.repoclean.digest.DigestVerifier;
import org.apache.maven.tools.repoclean.report.ReportWriteException;
import org.apache.maven.tools.repoclean.report.Reporter;
import org.apache.maven.tools.repoclean.rewrite.ArtifactPomRewriter;
import org.apache.maven.tools.repoclean.transaction.RewriteTransaction;
import org.apache.maven.tools.repoclean.transaction.RollbackException;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLifecycleException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.codehaus.plexus.util.IOUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
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

public class RewritePhase
    extends AbstractLogEnabled
    implements Contextualizable
{
    private DigestVerifier digestVerifier;

    private ArtifactRepositoryLayout bridgingLayout;

    private PlexusContainer container;

    public List execute( List artifacts, ArtifactRepository sourceRepo, ArtifactRepository targetRepo,
                        RepositoryCleanerConfiguration configuration, File reportsBase, Reporter repoReporter )
        throws ReportWriteException
    {
        Logger logger = getLogger();

        List rewritten = new ArrayList();

        File sourceBase = null;
        try
        {
            sourceBase = new File( new URL( sourceRepo.getUrl() ).getPath() );
        }
        catch ( MalformedURLException e )
        {
            repoReporter.error( "Cannot construct source repository base File for: " + sourceRepo, e );

            return null;
        }

        File targetBase = null;
        try
        {
            targetBase = new File( new URL( targetRepo.getUrl() ).getPath() );
        }
        catch ( MalformedURLException e )
        {
            repoReporter.error( "Cannot construct target repository base File for: " + targetRepo, e );

            return null;
        }

        for ( Iterator it = artifacts.iterator(); it.hasNext(); )
        {
            Artifact artifact = (Artifact) it.next();
            
            RewriteTransaction transaction = new RewriteTransaction( artifact );

            String artifactReportPath = buildArtifactReportPath( artifact );

            try
            {
                boolean errorOccurred = false;

                File artifactSource = new File( sourceRepo.getBasedir(), sourceRepo.pathOf( artifact ) );
                File artifactTarget = new File( targetRepo.getBasedir(), targetRepo.pathOf( artifact ).replace( '+',
                                                                                                                '-' ) );

                transaction.addFile( artifactTarget );

                artifact.setFile( artifactSource );

                boolean targetMissingOrOlder = !artifactTarget.exists()
                    || artifactTarget.lastModified() < artifactSource.lastModified();

                if ( artifactSource.exists() && ( configuration.force() || targetMissingOrOlder ) )
                {
                    transaction.addFile( artifactTarget );

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
                            logger.debug( "Copying artifact[" + artifact.getId() + "] from \'" + artifactSource
                                + "\' to \'" + artifactTarget + "\'." );
                        }

                        copyArtifact( artifact, artifactTarget, repoReporter );
                    }

                    if ( logger.isDebugEnabled() )
                    {
                        logger.debug( "working on digest for artifact[" + artifact.getId() + "] with groupId: \'"
                            + artifact.getGroupId() + "\'" );
                    }

                    digestVerifier.verifyDigest( artifactSource, artifactTarget, transaction, repoReporter,
                                                 configuration.reportOnly() );

                    rewriteMetadata( artifact, transaction, sourceBase, sourceRepo, targetBase, targetRepo,
                                     repoReporter, configuration.reportOnly() );
                    
                    rewritten.add( artifact );
                }
                else if ( !targetMissingOrOlder )
                {
                    repoReporter.warn( "Target file for artifact is present and not stale. (Artifact: \'"
                        + artifact.getId() + "\' in path: \'" + artifactSource + "\' with target path: "
                        + artifactTarget + ")." );
                }
                else
                {
                    repoReporter.error( "Cannot find source file for artifact: \'" + artifact.getId()
                        + "\' under path: \'" + artifactSource + "\'" );
                }

                if ( repoReporter.hasError() )
                {
                    repoReporter.warn( "Error(s) occurred while rewriting artifact: \'" + artifact.getId()
                        + "\' or its POM." );
                }
            }
            catch ( Exception e )
            {
                if ( !configuration.force() )
                {
                    repoReporter.warn( "Rolling back conversion for: " + artifact );
                    try
                    {
                        transaction.rollback();
                    }
                    catch ( RollbackException re )
                    {
                        repoReporter.error( "Error rolling back conversion transaction.", re );
                    }
                }
                else
                {
                    repoReporter.warn( "NOT Rolling back conversion for: " + artifact + "; we are in --force mode." );
                }

                repoReporter.error( "Error while rewriting file or POM for artifact: \'" + artifact.getId()
                    + "\'.", e );
            }
        }

        logger.info( "Actual number of artifacts rewritten: " + rewritten.size() + " (" + ( rewritten.size() * 2 )
            + " including POMs)." );

        return rewritten;
    }

    private void rewriteMetadata( Artifact artifact, RewriteTransaction transaction, File sourceBase,
                                 ArtifactRepository sourceRepo, File targetBase, ArtifactRepository targetRepo,
                                 Reporter artifactReporter, boolean reportOnly )
        throws Exception
    {
        // SNAPSHOT metadata
        ArtifactMetadata snapshot = new SnapshotArtifactMetadata( artifact );

        File snapshotSource = new File( sourceBase, sourceRepo.pathOfMetadata( snapshot ) );
        File snapshotTarget = new File( targetBase, targetRepo.pathOfMetadata( snapshot ) );

        freshenSupplementalMetadata( snapshot, snapshotSource, snapshotTarget, transaction, artifactReporter,
                                     reportOnly );

        // RELEASE metadata
        ArtifactMetadata release = new ReleaseArtifactMetadata( artifact );

        File releaseSource = new File( sourceBase, sourceRepo.pathOfMetadata( release ) );
        File releaseTarget = new File( targetBase, targetRepo.pathOfMetadata( release ) );

        freshenSupplementalMetadata( release, releaseSource, releaseTarget, transaction, artifactReporter, reportOnly );

        // The rest is for POM metadata - translation and bridging of locations in the target repo may be required.
        ArtifactMetadata pom = new ProjectMetadata( artifact );

        File sourcePom = new File( sourceBase, sourceRepo.pathOfMetadata( pom ) );
        File targetPom = new File( targetBase, targetRepo.pathOfMetadata( pom ).replace( '+', '-' ) );

        String pomContents = null;

        boolean pomNeedsRewriting = true;

        if ( sourcePom.exists() )
        {
            pomContents = readPomContents( sourcePom );

            if ( pomContents.indexOf( "modelVersion" ) > -1 )
            {
                pomNeedsRewriting = false;

                freshenSupplementalMetadata( pom, sourcePom, targetPom, transaction, artifactReporter, reportOnly );
            }
        }

        if ( pomNeedsRewriting )
        {
            ArtifactPomRewriter artifactPomRewriter = null;

            try
            {
                artifactPomRewriter = (ArtifactPomRewriter) container.lookup( ArtifactPomRewriter.ROLE,
                                                                              ArtifactPomRewriter.V3_POM );

                transaction.addFile( targetPom );

                File bridgedTargetPom = new File( targetBase, bridgingLayout.pathOfMetadata( pom ).replace( '+', '-' ) );

                transaction.addFile( bridgedTargetPom );

                File targetPomParent = targetPom.getParentFile();
                if ( !targetPomParent.exists() )
                {
                    targetPomParent.mkdirs();
                }

                FileWriter to = null;
                try
                {
                    StringReader from = null;
                    if ( pomContents != null )
                    {
                        from = new StringReader( pomContents );
                    }

                    to = new FileWriter( targetPom );

                    artifactPomRewriter.rewrite( artifact, from, to, artifactReporter, reportOnly );
                }
                finally
                {
                    IOUtil.close( to );
                }

                boolean wroteBridge = bridgePomLocations( pom, targetPom, bridgedTargetPom, artifactReporter,
                                                          transaction, reportOnly );

                digestVerifier.verifyDigest( sourcePom, targetPom, transaction, artifactReporter, reportOnly );

                if ( wroteBridge )
                {
                    digestVerifier.verifyDigest( sourcePom, bridgedTargetPom, transaction, artifactReporter,
                                                 reportOnly );
                }
            }
            finally
            {
                if ( artifactPomRewriter != null )
                {
                    try
                    {
                        container.release( artifactPomRewriter );
                    }
                    catch ( ComponentLifecycleException e )
                    {
                    }
                }
            }
        }
    }

    private void freshenSupplementalMetadata( ArtifactMetadata metadata, File source, File target,
                                             RewriteTransaction transaction, Reporter artifactReporter,
                                             boolean reportOnly )
        throws IOException, DigestException, ReportWriteException
    {
        if ( source.exists() )
        {
            File targetParent = target.getParentFile();
            if ( !targetParent.exists() )
            {
                targetParent.mkdirs();
            }

            FileReader reader = null;
            FileWriter writer = null;

            try
            {
                reader = new FileReader( source );
                writer = new FileWriter( target );

                IOUtil.copy( reader, writer );
            }
            finally
            {
                IOUtil.close( reader );
                IOUtil.close( writer );
            }

            digestVerifier.verifyDigest( source, target, transaction, artifactReporter, reportOnly );

        }
    }

    private String readPomContents( File sourcePom )
        throws IOException
    {
        FileReader reader = null;
        try
        {
            StringBuffer buffer = new StringBuffer();

            reader = new FileReader( sourcePom );

            int read = -1;
            char[] cbuf = new char[16];

            while ( ( read = reader.read( cbuf ) ) > -1 )
            {
                buffer.append( cbuf, 0, read );
            }

            return buffer.toString();
        }
        finally
        {
            IOUtil.close( reader );
        }
    }

    private String buildArtifactReportPath( Artifact artifact )
    {
        String classifier = artifact.getClassifier();
        String groupId = artifact.getGroupId().replace( '.', '/' );
        String artifactId = artifact.getArtifactId();
        String type = artifact.getType();
        String version = artifact.getVersion();

        return groupId + "/" + artifactId + "/" + type + "/"
            + ( ( classifier != null ) ? ( classifier + "-" ) : ( "" ) ) + version + ".report.txt";
    }

    private void copyArtifact( Artifact artifact, File artifactTarget, Reporter reporter )
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

    private boolean bridgePomLocations( ArtifactMetadata pom, File targetPom, File bridgedTargetPom, Reporter reporter,
                                       RewriteTransaction transaction, boolean reportOnly )
        throws IOException, ReportWriteException, DigestException
    {
        if ( targetPom.equals( bridgedTargetPom ) )
        {
            reporter.warn( "Cannot create legacy-compatible copy of POM at: " + targetPom
                + "; legacy-compatible path is the same as the converted POM itself." );

            return false;
        }

        freshenSupplementalMetadata( pom, targetPom, bridgedTargetPom, transaction, reporter, reportOnly );

        return true;
    }

    public void contextualize( Context context )
        throws ContextException
    {
        this.container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

}
