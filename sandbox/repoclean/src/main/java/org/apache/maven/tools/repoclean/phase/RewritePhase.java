package org.apache.maven.tools.repoclean.phase;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.tools.repoclean.RepositoryCleanerConfiguration;
import org.apache.maven.tools.repoclean.artifact.metadata.ProjectMetadata;
import org.apache.maven.tools.repoclean.digest.DigestVerifier;
import org.apache.maven.tools.repoclean.report.FileReporter;
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
        throws Exception
    {
        Logger logger = getLogger();

        ArtifactPomRewriter artifactPomRewriter = null;

        List rewritten = new ArrayList();

        try
        {
            File sourceBase = new File( new URL( sourceRepo.getUrl() ).getPath() );

            File targetBase = new File( new URL( targetRepo.getUrl() ).getPath() );

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
                    File artifactTarget = new File( targetRepo.getBasedir(), targetRepo.pathOf( artifact )
                        .replace( '+', '-' ) );

                    transaction.addFile( artifactTarget );

                    artifact.setFile( artifactSource );

                    boolean targetMissingOrOlder = !artifactTarget.exists()
                        || artifactTarget.lastModified() < artifactSource.lastModified();

                    if ( artifactSource.exists() && ( configuration.force() || targetMissingOrOlder ) )
                    {
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
                                    logger.debug( "Copying artifact[" + artifact.getId() + "] from \'" + artifactSource
                                        + "\' to \'" + artifactTarget + "\'." );
                                }

                                copyArtifact( artifact, artifactTarget, artifactReporter );
                            }
                        }
                        catch ( Exception e )
                        {
                            repoReporter.error( "Error transferring artifact[" + artifact.getId()
                                + "] to the target repository.", e );

                            throw e;
                        }

                        if ( logger.isDebugEnabled() )
                        {
                            logger.debug( "working on digest for artifact[" + artifact.getId() + "] with groupId: \'"
                                + artifact.getGroupId() + "\'" );
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

                        File sourcePom = new File( sourceBase, sourceRepo.pathOfMetadata( pom ) );

                        String pomContents = null;
                        
                        String pomVersion = ArtifactPomRewriter.V3_POM;

                        if ( sourcePom.exists() )
                        {
                            pomContents = readPomContents( sourcePom );
                            
                            if ( pomContents.indexOf( "modelVersion" ) > -1 )
                            {
                                pomVersion = ArtifactPomRewriter.V4_POM;
                            }
                        }

                        artifactPomRewriter = (ArtifactPomRewriter) container.lookup( ArtifactPomRewriter.ROLE,
                                                                                      pomVersion );

                        File targetPom = new File( targetBase, targetRepo.pathOfMetadata( pom ).replace( '+', '-' ) );

                        transaction.addFile( targetPom );

                        File bridgedTargetPom = new File( targetBase, bridgingLayout.pathOfMetadata( pom )
                            .replace( '+', '-' ) );

                        transaction.addFile( bridgedTargetPom );

                        try
                        {
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

                                artifactPomRewriter.rewrite( artifact, from, to, artifactReporter, configuration
                                    .reportOnly() );
                            }
                            finally
                            {
                                IOUtil.close( to );
                            }

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
                            repoReporter.error( "Error rewriting POM for artifact[" + artifact.getId()
                                + "] into the target repository.\n Error message: " + e.getMessage() );

                            throw e;
                        }

                        rewritten.add( artifact );
                    }
                    else if ( !targetMissingOrOlder )
                    {
                        artifactReporter.warn( "Target file for artifact is present and not stale. (Artifact: \'"
                            + artifact.getId() + "\' in path: \'" + artifactSource + "\' with target path: "
                            + artifactTarget + ")." );
                    }
                    else
                    {
                        artifactReporter.error( "Cannot find source file for artifact: \'" + artifact.getId()
                            + "\' under path: \'" + artifactSource + "\'" );
                    }

                    if ( artifactReporter.hasError() )
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
                        repoReporter
                            .warn( "NOT Rolling back conversion for: " + artifact + "; we are in --force mode." );
                    }

                    artifactReporter.error( "Error while rewriting file or POM for artifact: \'" + artifact.getId()
                        + "\'. See report at: \'" + artifactReportPath + "\'.", e );
                }
                finally
                {
                    if ( artifactReporter != null )
                    {
                        artifactReporter.close();
                    }
                }
            }

            logger.info( "Actual number of artifacts rewritten: " + rewritten.size() + " (" + ( rewritten.size() * 2 )
                + " including POMs)." );
        }
        finally
        {
            if ( artifactPomRewriter != null )
            {
                container.release( artifactPomRewriter );
            }
        }

        return rewritten;
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

    private boolean bridgePomLocations( File targetPom, File bridgedTargetPom, Reporter reporter )
        throws IOException, ReportWriteException
    {
        if ( targetPom.equals( bridgedTargetPom ) )
        {
            reporter.warn( "Cannot create legacy-compatible copy of POM at: " + targetPom
                + "; legacy-compatible path is the same as the converted POM itself." );

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

    public void contextualize( Context context )
        throws ContextException
    {
        this.container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

}
