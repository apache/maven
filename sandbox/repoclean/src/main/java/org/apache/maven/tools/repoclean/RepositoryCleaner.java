package org.apache.maven.tools.repoclean;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.tools.repoclean.correlate.ArtifactMd5Correlator;
import org.apache.maven.tools.repoclean.correlate.ArtifactPomCorrelator;
import org.apache.maven.tools.repoclean.patch.V4ModelPatcher;
import org.apache.maven.tools.repoclean.report.Reporter;
import org.apache.maven.tools.repoclean.translate.PomV3ToV4Translator;
import org.apache.maven.tools.repoclean.validate.V4ModelIndependenceValidator;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author jdcasey
 */
public class RepositoryCleaner
    extends AbstractLogEnabled
{

    public static final String ROLE = RepositoryCleaner.class.getName();

    private ArtifactPomCorrelator artifactPomCorrelator;

    private ArtifactMd5Correlator artifactMd5Correlator;

    private PomV3ToV4Translator pomV3ToV4Translator;

    private V4ModelIndependenceValidator v4ModelIndependenceValidator;

    private V4ModelPatcher v4ModelPatcher;

    public void cleanRepository( String repositoryPath, String reportsDir, boolean reportOnly )
    {
        Logger logger = getLogger();

        File reportsBase = new File( reportsDir );
        if ( !reportsBase.exists() )
        {
            logger.info( "Creating reports directory: \'" + reportsDir + "\'" );
            reportsBase.mkdirs();
        }
        else if ( !reportsBase.isDirectory() )
        {
            logger.error( "Cannot write reports to \'" + reportsDir + "\' because it is not a directory." );

            reportsBase = null;
        }

        File repositoryBase = new File( repositoryPath );
        if ( !repositoryBase.exists() )
        {
            logger.error( "Cannot clean repository \'" + repositoryPath + "\' because it does not exist." );

            repositoryBase = null;
        }
        else if ( !repositoryBase.isDirectory() )
        {
            logger.error( "Cannot clean repository \'" + repositoryPath + "\' because it is not a directory." );

            repositoryBase = null;
        }

        // do not proceed if we cannot produce reports, or if the repository is 
        // invalid.
        if ( reportsBase != null && repositoryBase != null )
        {
            logger.info( "Scanning for POMs." );
            String[] poms = scanPoms( repositoryPath );

            logger.info( "Scanning for artifacts." );
            String[] artifacts = scanArtifacts( repositoryPath );

            Reporter repoReporter = new Reporter( reportsBase, "repository.report.txt" );

            logger.info( "Correlating artifacts to POMs." );
            artifactPomCorrelator.correlateArtifactsToPoms( poms, artifacts, repoReporter );

            logger.info( "Correlating artifacts to MD5 digest files." );
            artifactMd5Correlator.correlateArtifactsToMd5( repositoryBase, artifacts, repoReporter, reportOnly );

            logger.info( "Translating POMs to V4 format." );
            for ( int i = 0; i < poms.length; i++ )
            {
                String pom = poms[i];

                Reporter pomReporter = new Reporter( reportsBase, pom + ".report.txt" );

                logger.info( "Reading POM: \'" + pom + "\'" );
                org.apache.maven.model.v3_0_0.Model v3Model = null;
                try
                {
                    v3Model = readV3( repositoryBase, pom );
                }
                catch ( Exception e )
                {
                    logger.error( "Error reading POM: \'" + pom + "\'", e );
                }

                if ( v3Model != null )
                {
                    logger.info( "Translating POM: \'" + pom + "\'" );
                    org.apache.maven.model.v4_0_0.Model v4Model = pomV3ToV4Translator.translate( v3Model, pomReporter );

                    logger.info( "Performing validation on resulting v4 model for POM: \'" + pom + "\'" );
                    boolean isValid = v4ModelIndependenceValidator.validate( v4Model, pomReporter, true );

                    if ( !isValid )
                    {
                        logger.info( "Patching v4 model for POM: \'" + pom + "\' using information glean from path." );
                        v4ModelPatcher.patchModel( v4Model, pom, pomReporter );

                        logger.info( "Re-performing validation on patched v4 model for POM: \'" + pom + "\'" );
                        isValid = v4ModelIndependenceValidator.validate( v4Model, pomReporter, false );
                    }

                    if ( pomReporter.hasError() )
                    {
                        repoReporter.warn( "Translation of POM: \'" + pom + "\' encountered errors." );
                    }

                    if ( !reportOnly )
                    {
                        logger.info( "Writing POM: \'" + pom + "\'" );

                        try
                        {
                            writeV4( repositoryBase, pom, v4Model );
                        }
                        catch ( Exception e )
                        {
                            logger.error( "Error writing POM: \'" + pom + "\'", e );
                        }
                    }
                    else
                    {
                        logger.info( "NOT writing POM: \'" + pom + "\'; we are in report-only mode." );
                    }
                }
                else
                {
                    pomReporter.error( "Cannot translate pom. V3 model is null." );
                }

                try
                {
                    pomReporter.writeReport();
                }
                catch ( IOException e )
                {
                    logger.error( "Error writing report for POM: \'" + pom + "\'", e );
                }
            }

            try
            {
                repoReporter.writeReport();
            }
            catch ( IOException e )
            {
                logger.error( "Error writing report for repository", e );
            }
        }
    }

    private void writeV4( File repositoryBase, String pom, org.apache.maven.model.v4_0_0.Model model ) throws Exception
    {
        FileWriter writer = null;
        try
        {
            File pomFile = new File( repositoryBase, pom );
            writer = new FileWriter( pomFile );

            org.apache.maven.model.v4_0_0.io.xpp3.MavenXpp3Writer modelWriter = new org.apache.maven.model.v4_0_0.io.xpp3.MavenXpp3Writer();

            modelWriter.write( writer, model );
        }
        finally
        {
            IOUtil.close( writer );
        }
    }

    private org.apache.maven.model.v3_0_0.Model readV3( File repositoryBase, String pom ) throws Exception
    {
        org.apache.maven.model.v3_0_0.Model model = null;

        FileReader reader = null;
        try
        {
            File pomFile = new File( repositoryBase, pom );
            reader = new FileReader( pomFile );

            org.apache.maven.model.v3_0_0.io.xpp3.MavenXpp3Reader modelReader = new org.apache.maven.model.v3_0_0.io.xpp3.MavenXpp3Reader();
            model = modelReader.read( reader );
        }
        finally
        {
            IOUtil.close( reader );
        }

        return model;
    }

    private String[] scanPoms( String repositoryPath )
    {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( repositoryPath );
        scanner.setIncludes( new String[] { "**/poms/*.pom" } );

        scanner.scan();

        return scanner.getIncludedFiles();
    }

    private String[] scanArtifacts( String repositoryPath )
    {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir( repositoryPath );
        scanner.setExcludes( new String[] { "**/poms/*.pom", "**/*.md5" } );

        scanner.scan();

        return scanner.getIncludedFiles();
    }

}