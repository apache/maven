package org.apache.maven.project.artifact;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.transform.ArtifactTransformation;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.DefaultProjectBuilderConfiguration;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.project.interpolation.ModelInterpolationException;
import org.apache.maven.project.interpolation.StringSearchModelInterpolator;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.InterpolationPostProcessor;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.RecursionInterceptor;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
import org.codehaus.plexus.interpolation.ValueSource;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class VersionExpressionTransformation
    extends StringSearchModelInterpolator
    implements Initializable, ArtifactTransformation
{

    public void transformForDeployment( Artifact artifact, ArtifactRepository remoteRepository,
                                        ArtifactRepository localRepository )
        throws ArtifactDeploymentException
    {
        ProjectArtifactMetadata metadata = ArtifactWithProject.getProjectArtifactMetadata( artifact );
        File pomFile;
        boolean pomArtifact = false;
        if ( "pom".equals( artifact.getType() ) )
        {
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( "On Deploy: Using artifact file for POM: " + artifact );
            }
            pomFile = artifact.getFile();
            pomArtifact = true;
        }
        // FIXME: We can't be this smart (yet) since the deployment step transforms from the 
        // original POM once again and re-installs over the top of the install step.
//        else if ( metadata == null || metadata.isVersionExpressionsResolved() )
//        {
//            return;
//        }
        else if ( metadata != null )
        {
            pomFile = metadata.getFile();
        }
        else
        {
            return;
        }

        try
        {
            File outFile = transformVersions( pomFile, artifact, localRepository );
            
            if ( pomArtifact )
            {
                // FIXME: We need a way to mark a POM artifact as resolved WRT version expressions, so we don't reprocess...
                artifact.setFile( outFile );
            }
            else
            {
                metadata.setFile( outFile );
                metadata.setVersionExpressionsResolved( true );
            }
        }
        catch ( IOException e )
        {
            throw new ArtifactDeploymentException( "Failed to read or write POM for version transformation.", e );
        }
        catch ( ModelInterpolationException e )
        {
            throw new ArtifactDeploymentException( "Failed to interpolate POM versions.", e );
        }
    }

    public void transformForInstall( Artifact artifact, ArtifactRepository localRepository )
        throws ArtifactInstallationException
    {
        ProjectArtifactMetadata metadata = (ProjectArtifactMetadata) artifact.getMetadata( ProjectArtifactMetadata.class );
        File pomFile;
        boolean pomArtifact = false;
        if ( "pom".equals( artifact.getType() ) )
        {
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( "On Install: Using artifact file for POM: " + artifact );
            }
            pomFile = artifact.getFile();
            pomArtifact = true;
        }
        // FIXME: We can't be this smart (yet) since the deployment step transforms from the 
        // original POM once again and re-installs over the top of the install step.
//        else if ( metadata == null || metadata.isVersionExpressionsResolved() )
//        {
//            return;
//        }
        else if ( metadata != null )
        {
            pomFile = metadata.getFile();
        }
        else
        {
            return;
        }

        try
        {
            File outFile = transformVersions( pomFile, artifact, localRepository );
            
            if ( pomArtifact )
            {
                // FIXME: We need a way to mark a POM artifact as resolved WRT version expressions, so we don't reprocess...
                artifact.setFile( outFile );
            }
            else
            {
                metadata.setFile( outFile );
                metadata.setVersionExpressionsResolved( true );
            }
        }
        catch ( IOException e )
        {
            throw new ArtifactInstallationException( "Failed to read or write POM for version transformation.", e );
        }
        catch ( ModelInterpolationException e )
        {
            throw new ArtifactInstallationException( "Failed to interpolate POM versions.", e );
        }
    }

    public void transformForResolve( Artifact artifact, List remoteRepositories, ArtifactRepository localRepository )
        throws ArtifactResolutionException, ArtifactNotFoundException
    {
        return;
    }

    protected File transformVersions( File pomFile, Artifact artifact, ArtifactRepository localRepository )
        throws IOException, ModelInterpolationException
    {
        ProjectBuilderConfiguration pbConfig;
        File projectDir;
        File outputFile;
        if ( artifact instanceof ArtifactWithProject )
        {
            MavenProject project = ( (ArtifactWithProject) artifact ).getProject();

            projectDir = project.getBasedir();
            pbConfig = project.getProjectBuilderConfiguration();
            outputFile = new File( project.getBuild().getDirectory(), "pom-transformed.xml" );
        }
        else
        {
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug(
                                  "WARNING: Artifact: " + artifact
                                      + " does not have project-builder metadata (ProjectBuilderConfiguration) associated with it.\n"
                                      + "Cannot access CLI properties for version transformation." );
            }
            
            pbConfig = new DefaultProjectBuilderConfiguration().setLocalRepository( localRepository );
            projectDir = pomFile.getAbsoluteFile().getParentFile();
            outputFile = new File( projectDir, "target/pom-transformed.xml" );
        }

        Reader reader = null;
        Model model;
        try
        {
            reader = ReaderFactory.newXmlReader( pomFile );
            model = new MavenXpp3Reader().read( reader );
            
            interpolateVersions( pomFile, outputFile, model, projectDir, pbConfig );
        }
        catch ( XmlPullParserException e )
        {
            String message =
                "Failed to parse POM for version transformation. Proceeding with original (non-interpolated) POM file.";
            
            String detail = "\n\nNOTE: Error was in file: " + pomFile + ", at line: "
                    + e.getLineNumber() + ", column: " + e.getColumnNumber();
            
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( message + detail, e );
            }
            else
            {
                getLogger().warn( message + " See debug output for details." );
            }
            
            outputFile = pomFile;
        }
        finally
        {
            IOUtil.close( reader );
        }
        
        return outputFile;
    }

    protected void interpolateVersions( File pomFile, File outputFile, Model model, File projectDir, ProjectBuilderConfiguration config )
        throws ModelInterpolationException
    {
        boolean debugEnabled = getLogger().isDebugEnabled();

        // NOTE: We want to interpolate version expressions ONLY, and want to do so without requiring the
        // use of the XPP3 Model reader/writers, which have a tendency to lose XML comments and such.
        // SOOO, we're using a two-stage string interpolation here. The first stage selects all XML 'version'
        // elements, and subjects their values to interpolation in the second stage.
        Interpolator interpolator = new StringSearchInterpolator( "<version>", "</version>" );
        
        // The second-stage interpolator is the 'normal' one used in all Model interpolation throughout
        // maven-project.
        Interpolator secondaryInterpolator = getInterpolator();
        
        // We'll just reuse the recursion interceptor...not sure it makes any difference.
        RecursionInterceptor recursionInterceptor = getRecursionInterceptor();
        
        // This is a ValueSource implementation that simply delegates to the second-stage "real" interpolator
        // once we've isolated the version elements from the input XML.
        interpolator.addValueSource( new SecondaryInterpolationValueSource( secondaryInterpolator, recursionInterceptor ) );
        
        // The primary interpolator is searching for version XML elements, and interpolating their values. Since
        // '<version>' and '</version>' are the delimiters for this, the interpolator will remove these tokens
        // from the result. So, we need to put them back before including the interpolated result.
        interpolator.addPostProcessor( new VersionRestoringPostProcessor() );

        List valueSources = createValueSources( model, projectDir, config );
        List postProcessors = createPostProcessors( model, projectDir, config );

        synchronized ( this )
        {
            for ( Iterator it = valueSources.iterator(); it.hasNext(); )
            {
                ValueSource vs = (ValueSource) it.next();
                secondaryInterpolator.addValueSource( vs );
            }

            for ( Iterator it = postProcessors.iterator(); it.hasNext(); )
            {
                InterpolationPostProcessor postProcessor = (InterpolationPostProcessor) it.next();

                secondaryInterpolator.addPostProcessor( postProcessor );
            }

            String pomContents;
            try
            {
                Reader reader = null;
                try
                {
                    reader = ReaderFactory.newXmlReader( pomFile );
                    pomContents = IOUtil.toString( reader );
                }
                catch ( IOException e )
                {
                    throw new ModelInterpolationException( "Error reading POM for version-expression interpolation: " + e.getMessage(), e );
                }
                finally
                {
                    IOUtil.close( reader );
                }
                
                try
                {
                    pomContents = interpolator.interpolate( pomContents );
                }
                catch ( InterpolationException e )
                {
                    throw new ModelInterpolationException( e.getMessage(), e );
                }

                if ( debugEnabled )
                {
                    List feedback = interpolator.getFeedback();
                    if ( feedback != null && !feedback.isEmpty() )
                    {
                        getLogger().debug( "Maven encountered the following problems while transforming POM versions:" );

                        Object last = null;
                        for ( Iterator it = feedback.iterator(); it.hasNext(); )
                        {
                            Object next = it.next();

                            if ( next instanceof Throwable )
                            {
                                if ( last == null )
                                {
                                    getLogger().debug( "", ( (Throwable) next ) );
                                }
                                else
                                {
                                    getLogger().debug( String.valueOf( last ), ( (Throwable) next ) );
                                }
                            }
                            else
                            {
                                if ( last != null )
                                {
                                    getLogger().debug( String.valueOf( last ) );
                                }

                                last = next;
                            }
                        }

                        if ( last != null )
                        {
                            getLogger().debug( String.valueOf( last ) );
                        }
                    }
                }

                interpolator.clearFeedback();
            }
            finally
            {
                for ( Iterator iterator = valueSources.iterator(); iterator.hasNext(); )
                {
                    ValueSource vs = (ValueSource) iterator.next();
                    secondaryInterpolator.removeValuesSource( vs );
                }

                for ( Iterator iterator = postProcessors.iterator(); iterator.hasNext(); )
                {
                    InterpolationPostProcessor postProcessor = (InterpolationPostProcessor) iterator.next();
                    secondaryInterpolator.removePostProcessor( postProcessor );
                }

                getInterpolator().clearAnswers();
            }
            
            Writer writer = null;
            try
            {
                outputFile.getParentFile().mkdirs();
                
                writer = WriterFactory.newXmlWriter( outputFile );
                
                IOUtil.copy( pomContents, writer );
            }
            catch ( IOException e )
            {
                throw new ModelInterpolationException( "Failed to write transformed POM: " + outputFile.getAbsolutePath(), e );
            }
            finally
            {
                IOUtil.close( writer );
            }
        }
        
        // if ( error != null )
        // {
        // throw error;
        // }
    }

    private static final class SecondaryInterpolationValueSource
        implements ValueSource
    {
        
        private Interpolator secondary;
        private final RecursionInterceptor recursionInterceptor;
        private List localFeedback = new ArrayList();
        
        public SecondaryInterpolationValueSource( Interpolator secondary, RecursionInterceptor recursionInterceptor )
        {
            this.secondary = secondary;
            this.recursionInterceptor = recursionInterceptor;
        }

        public void clearFeedback()
        {
            secondary.clearFeedback();
        }

        public List getFeedback()
        {
            List result = secondary.getFeedback();
            if ( result != null )
            {
                result = new ArrayList( result );
            }
            
            result.addAll( localFeedback );
            
            return result;
        }

        public Object getValue( String expression )
        {
            try
            {
                return secondary.interpolate( expression, recursionInterceptor );
            }
            catch ( InterpolationException e )
            {
                localFeedback.add( "Error during version expression interpolation." );
                localFeedback.add( e );
            }
            
            return null;
        }
    }
    
    private static final class VersionRestoringPostProcessor
        implements InterpolationPostProcessor
    {

        public Object execute( String expression, Object value )
        {
            return "<version>" + value + "</version>";
        }
        
    }

}
