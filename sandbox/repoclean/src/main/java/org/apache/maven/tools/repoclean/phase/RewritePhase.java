package org.apache.maven.tools.repoclean.phase;

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
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.apache.maven.tools.repoclean.RepositoryCleanerConfiguration;
import org.apache.maven.tools.repoclean.digest.DigestException;
import org.apache.maven.tools.repoclean.digest.DigestVerifier;
import org.apache.maven.tools.repoclean.digest.Digestor;
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
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

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
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RewritePhase
    extends AbstractLogEnabled
    implements Contextualizable
{
    private DigestVerifier digestVerifier;

    private Digestor artifactDigestor;

    private ArtifactRepositoryLayout bridgingLayout;

    private PlexusContainer container;

    public List execute( List artifacts, ArtifactRepository sourceRepo, ArtifactRepository targetRepo,
                         RepositoryCleanerConfiguration configuration, File reportsBase, Reporter repoReporter )
        throws ReportWriteException
    {
        Logger logger = getLogger();

        List rewritten = new ArrayList();

        File sourceBase;
        try
        {
            sourceBase = new File( new URL( sourceRepo.getUrl() ).getPath() );
        }
        catch ( MalformedURLException e )
        {
            repoReporter.error( "Cannot construct source repository base File for: " + sourceRepo, e );

            return null;
        }

        File targetBase;
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

            try
            {
                File artifactSource = new File( sourceRepo.getBasedir(), sourceRepo.pathOf( artifact ) );
                File artifactTarget = new File( targetRepo.getBasedir(),
                                                targetRepo.pathOf( artifact ).replace( '+', '-' ) );

                transaction.addFile( artifactTarget );

                artifact.setFile( artifactSource );

                boolean targetMissingOrOlder = !artifactTarget.exists() ||
                    artifactTarget.lastModified() < artifactSource.lastModified();

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
                            logger.debug( "Copying artifact[" + artifact.getId() + "] from \'" + artifactSource +
                                "\' to \'" + artifactTarget + "\'." );
                        }

                        copyArtifact( artifact, artifactTarget );
                    }

                    if ( logger.isDebugEnabled() )
                    {
                        logger.debug( "working on digest for artifact[" + artifact.getId() + "] with groupId: \'" +
                            artifact.getGroupId() + "\'" );
                    }

                    digestVerifier.verifyDigest( artifactSource, artifactTarget, transaction, repoReporter,
                                                 configuration.reportOnly() );

                    rewriteMetadata( artifact, transaction, sourceBase, sourceRepo, targetBase, targetRepo,
                                     repoReporter, configuration.reportOnly() );

                    rewritten.add( artifact );
                }
                else if ( !targetMissingOrOlder )
                {
                    repoReporter.warn( "Target file for artifact is present and not stale. (Artifact: \'" +
                        artifact.getId() + "\' in path: \'" + artifactSource + "\' with target path: " +
                        artifactTarget + ")...SKIPPING" );
                }
                else
                {
                    repoReporter.warn( "Cannot find source file for artifact: \'" + artifact.getId() +
                        "\' under path: \'" + artifactSource + "\'...SKIPPING" );
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
                        repoReporter.error(
                            "Error rolling back conversion transaction (artifact: " + artifact.getId() + ").", re );
                    }
                }
                else
                {
                    repoReporter.warn( "NOT Rolling back conversion for: " + artifact + "; we are in --force mode." );
                }

                repoReporter.error( "Error while rewriting file or POM for artifact: \'" + artifact.getId() + "\'.",
                                    e );
            }
        }

        logger.info( "Actual number of artifacts rewritten: " + rewritten.size() + " (" + rewritten.size() * 2 +
            " including POMs)." );

        return rewritten;
    }

    private void rewriteMetadata( Artifact artifact, RewriteTransaction transaction, File sourceBase,
                                  ArtifactRepository sourceRepo, File targetBase, ArtifactRepository targetRepo,
                                  Reporter artifactReporter, boolean reportOnly )
        throws Exception
    {
        ArtifactMetadata metadata = new ArtifactRepositoryMetadata( artifact );

        File metadataSource = new File( sourceBase, sourceRepo.pathOfRemoteRepositoryMetadata( metadata ) );
        File metadataTarget = new File( targetBase, targetRepo.pathOfRemoteRepositoryMetadata( metadata ) );

        Metadata sourceMetadata = readMetadata( metadataSource, artifact );
        if ( sourceMetadata.getVersioning() == null )
        {
            sourceMetadata.setVersioning( new Versioning() );
        }
        if ( !sourceMetadata.getVersioning().getVersions().contains( artifact.getBaseVersion() ) )
        {
            sourceMetadata.getVersioning().addVersion( artifact.getBaseVersion() );
        }
        mergeMetadata( sourceMetadata, metadataTarget, reportOnly );

        metadata = new SnapshotArtifactRepositoryMetadata( artifact );

        metadataSource = new File( sourceBase, sourceRepo.pathOfRemoteRepositoryMetadata( metadata ) );
        metadataTarget = new File( targetBase, targetRepo.pathOfRemoteRepositoryMetadata( metadata ) );

        sourceMetadata = readMetadata( metadataSource, artifact );
        if ( artifact.isSnapshot() )
        {
            if ( sourceMetadata.getVersioning() == null )
            {
                sourceMetadata.setVersioning( new Versioning() );
            }
            if ( sourceMetadata.getVersioning().getSnapshot() == null )
            {
                sourceMetadata.getVersioning().setSnapshot( new Snapshot() );
            }

            int i = artifact.getVersion().indexOf( '-' );
            if ( i >= 0 )
            {
                Snapshot snapshot = sourceMetadata.getVersioning().getSnapshot();
                snapshot.setTimestamp( artifact.getVersion().substring( 0, i ) );
                snapshot.setBuildNumber( Integer.valueOf( artifact.getVersion().substring( i + 1 ) ).intValue() );
            }
        }
        mergeMetadata( sourceMetadata, metadataTarget, reportOnly );

        // The rest is for POM metadata - translation and bridging of locations in the target repo may be required.
        ArtifactMetadata pom = new ProjectArtifactMetadata( artifact, null );

        File sourcePom = new File( sourceBase, sourceRepo.pathOfRemoteRepositoryMetadata( pom ) );
        File targetPom = new File( targetBase, targetRepo.pathOfRemoteRepositoryMetadata( pom ).replace( '+', '-' ) );

        String pomContents = null;

        boolean shouldRewritePom = true;

        if ( sourcePom.exists() )
        {
            pomContents = readPomContents( sourcePom );

            if ( pomContents.indexOf( "modelVersion" ) > -1 )
            {
                shouldRewritePom = false;

                copyMetadata( sourcePom, targetPom, transaction, artifactReporter, reportOnly );
            }
        }
        else if ( targetPom.exists() )
        {
            // we have a target pom for this artifact already, and we'll only be making up a new pom.
            // let's leave the existing one alone.
            shouldRewritePom = false;
        }

        File bridgedTargetPom = null;

        boolean wroteBridge = false;

        if ( shouldRewritePom )
        {
            ArtifactPomRewriter artifactPomRewriter = null;

            try
            {
                artifactPomRewriter = (ArtifactPomRewriter) container.lookup( ArtifactPomRewriter.ROLE,
                                                                              ArtifactPomRewriter.V3_POM );

                transaction.addFile( targetPom );

                bridgedTargetPom = new File( targetBase,
                                             bridgingLayout.pathOfRemoteRepositoryMetadata( pom ).replace( '+', '-' ) );

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

                wroteBridge = bridgePomLocations( targetPom, bridgedTargetPom, artifactReporter, transaction,
                                                  reportOnly );
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

        digestVerifier.verifyDigest( sourcePom, targetPom, transaction, artifactReporter, reportOnly );

        if ( wroteBridge )
        {
            digestVerifier.verifyDigest( sourcePom, bridgedTargetPom, transaction, artifactReporter, reportOnly );
        }
    }

    private void mergeMetadata( Metadata sourceMetadata, File target, boolean reportOnly )
        throws IOException, DigestException, XmlPullParserException, NoSuchAlgorithmException
    {
        boolean changed = false;
        Metadata targetMetadata = null;

        if ( target.exists() )
        {
            Reader reader = null;

            try
            {
                reader = new FileReader( target );

                MetadataXpp3Reader mappingReader = new MetadataXpp3Reader();

                targetMetadata = mappingReader.read( reader );
            }
            finally
            {
                IOUtil.close( reader );
            }

            changed = targetMetadata.merge( sourceMetadata );
        }
        else
        {
            changed = true;
            targetMetadata = sourceMetadata;
        }
        if ( changed )
        {
            Writer writer = null;
            try
            {
                target.getParentFile().mkdirs();
                writer = new FileWriter( target );

                MetadataXpp3Writer mappingWriter = new MetadataXpp3Writer();

                mappingWriter.write( writer, targetMetadata );

                if ( !reportOnly )
                {
                    File digestFile = artifactDigestor.getDigestFile( target, Digestor.MD5 );
                    artifactDigestor.createArtifactDigest( target, digestFile, Digestor.MD5 );
                    digestFile = artifactDigestor.getDigestFile( target, Digestor.SHA );
                    artifactDigestor.createArtifactDigest( target, digestFile, Digestor.SHA );
                }
            }
            finally
            {
                IOUtil.close( writer );
            }
        }
    }

    private Metadata readMetadata( File source, Artifact artifact )
        throws IOException, XmlPullParserException
    {
        Metadata sourceMetadata = null;

        if ( source.exists() )
        {
            Reader reader = null;

            try
            {
                reader = new FileReader( source );

                MetadataXpp3Reader mappingReader = new MetadataXpp3Reader();

                sourceMetadata = mappingReader.read( reader );
            }
            finally
            {
                IOUtil.close( reader );
            }
        }

        if ( sourceMetadata == null )
        {
            sourceMetadata = new Metadata();

            sourceMetadata.setGroupId( artifact.getGroupId() );
            sourceMetadata.setArtifactId( artifact.getArtifactId() );
            sourceMetadata.setVersion( artifact.getBaseVersion() );
        }
        return sourceMetadata;
    }

    private void copyMetadata( File source, File target, RewriteTransaction transaction, Reporter artifactReporter,
                               boolean reportOnly )
        throws IOException, DigestException, ReportWriteException
    {
        if ( source.exists() && !source.getCanonicalFile().equals( target.getCanonicalFile() ) )
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

    private void copyArtifact( Artifact artifact, File artifactTarget )
        throws IOException
    {
        File artifactSource = artifact.getFile();

        if ( artifactSource.getCanonicalFile().equals( artifactTarget.getCanonicalFile() ) )
        {
            return;
        }

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

    private boolean bridgePomLocations( File targetPom, File bridgedTargetPom, Reporter reporter,
                                        RewriteTransaction transaction, boolean reportOnly )
        throws IOException, ReportWriteException, DigestException
    {
        if ( targetPom.equals( bridgedTargetPom ) )
        {
            reporter.warn( "Cannot create legacy-compatible copy of POM at: " + targetPom +
                "; legacy-compatible path is the same as the converted POM itself." );

            return false;
        }

        copyMetadata( targetPom, bridgedTargetPom, transaction, reporter, reportOnly );

        return true;
    }

    public void contextualize( Context context )
        throws ContextException
    {
        this.container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }

}
