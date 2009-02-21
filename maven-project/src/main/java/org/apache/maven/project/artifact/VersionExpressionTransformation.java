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
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.DefaultProjectBuilderConfiguration;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.apache.maven.project.interpolation.ModelInterpolationException;
import org.apache.maven.project.interpolation.StringSearchModelInterpolator;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.InterpolationPostProcessor;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.ValueSource;
import org.codehaus.plexus.interpolation.object.FieldBasedObjectInterpolator;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class VersionExpressionTransformation
    extends StringSearchModelInterpolator
    implements Initializable, ArtifactTransformation
{

    private static Set BLACKLISTED_FIELD_NAMES;
    
    private static final Set WHITELISTED_FIELD_NAMES;
    
    static
    {
        Set whitelist = new HashSet();
        
        whitelist.add( "version" );
        whitelist.add( "dependencies" );
        whitelist.add( "build" );
        whitelist.add( "plugins" );
        whitelist.add( "reporting" );
        whitelist.add( "parent" );
        
        WHITELISTED_FIELD_NAMES = whitelist;
    }

    public void transformForDeployment( Artifact artifact, ArtifactRepository remoteRepository,
                                        ArtifactRepository localRepository )
        throws ArtifactDeploymentException
    {
        ProjectArtifactMetadata metadata = ArtifactWithProject.getProjectArtifactMetadata( artifact );
        File pomFile;
        boolean pomArtifact = false;
        if ( "pom".equals( artifact.getType() ) )
        {
            getLogger().debug( "On Deploy: Using artifact file for POM: " + artifact );
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
        catch ( XmlPullParserException e )
        {
            throw new ArtifactDeploymentException(
                                                   "Failed to parse POM for version transformation. Error was in file: "
                                                       + pomFile + ", at line: " + e.getLineNumber() + ", column: "
                                                       + e.getColumnNumber(), e );
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
            getLogger().debug( "On Install: Using artifact file for POM: " + artifact );
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
        catch ( XmlPullParserException e )
        {
            throw new ArtifactInstallationException(
                                                     "Failed to parse POM for version transformation. Error was in file: "
                                                         + pomFile + ", at line: " + e.getLineNumber() + ", column: "
                                                         + e.getColumnNumber(), e );
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
        throws IOException, XmlPullParserException, ModelInterpolationException
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
            getLogger().info(
                              "Artifact: " + artifact
                                  + " does not have project-builder metadata (ProjectBuilderConfiguration) associated with it.\n"
                                  + "Cannot access CLI properties for version transformation." );
            
            pbConfig = new DefaultProjectBuilderConfiguration().setLocalRepository( localRepository );
            projectDir = pomFile.getAbsoluteFile().getParentFile();
            outputFile = new File( projectDir, "target/pom-transformed.xml" );
        }

        Reader reader = null;
        Model model;
        try
        {
            reader = new FileReader( pomFile );
            model = new MavenXpp3Reader().read( reader );
        }
        finally
        {
            IOUtil.close( reader );
        }
        
        interpolateVersions( model, projectDir, pbConfig );
        
        Writer writer = null;
        try
        {
            outputFile.getParentFile().mkdirs();
            
            writer = new FileWriter( outputFile );
            
            new MavenXpp3Writer().write( writer, model );
        }
        finally
        {
            IOUtil.close( writer );
        }
        
        return outputFile;
    }

    protected void interpolateVersions( Model model, File projectDir, ProjectBuilderConfiguration config )
        throws ModelInterpolationException
    {
        boolean debugEnabled = getLogger().isDebugEnabled();

        Interpolator interpolator = getInterpolator();

        List valueSources = createValueSources( model, projectDir, config );
        List postProcessors = createPostProcessors( model, projectDir, config );

        synchronized ( this )
        {
            for ( Iterator it = valueSources.iterator(); it.hasNext(); )
            {
                ValueSource vs = (ValueSource) it.next();
                interpolator.addValueSource( vs );
            }

            for ( Iterator it = postProcessors.iterator(); it.hasNext(); )
            {
                InterpolationPostProcessor postProcessor = (InterpolationPostProcessor) it.next();

                interpolator.addPostProcessor( postProcessor );
            }

            try
            {
                FieldBasedObjectInterpolator objInterpolator =
                    new FieldBasedObjectInterpolator( BLACKLISTED_FIELD_NAMES,
                                                      FieldBasedObjectInterpolator.DEFAULT_BLACKLISTED_PACKAGE_PREFIXES );

                try
                {
                    objInterpolator.interpolate( model, getInterpolator(), getRecursionInterceptor() );
                }
                catch ( InterpolationException e )
                {
                    throw new ModelInterpolationException( e.getMessage(), e );
                }

                if ( debugEnabled )
                {
                    List feedback = new ArrayList();
                    if ( objInterpolator.hasWarnings() )
                    {
                        feedback.addAll( objInterpolator.getWarnings() );
                    }

                    List internalFeedback = interpolator.getFeedback();
                    if ( internalFeedback != null && !internalFeedback.isEmpty() )
                    {
                        feedback.addAll( internalFeedback );
                    }

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
                    interpolator.removeValuesSource( vs );
                }

                for ( Iterator iterator = postProcessors.iterator(); iterator.hasNext(); )
                {
                    InterpolationPostProcessor postProcessor = (InterpolationPostProcessor) iterator.next();
                    interpolator.removePostProcessor( postProcessor );
                }

                getInterpolator().clearAnswers();
            }
        }

        // if ( error != null )
        // {
        // throw error;
        // }
    }

    public void initialize()
        throws InitializationException
    {
        super.initialize();
        
        synchronized ( VersionExpressionTransformation.class )
        {
            if ( BLACKLISTED_FIELD_NAMES == null )
            {
                Set fieldNames = new HashSet();

                Class[] classes = { Model.class, Dependency.class, Plugin.class, ReportPlugin.class };
                for ( int i = 0; i < classes.length; i++ )
                {
                    Field[] fields = classes[i].getDeclaredFields();
                    for ( int j = 0; j < fields.length; j++ )
                    {
                        if ( !WHITELISTED_FIELD_NAMES.contains( fields[j].getName() ) )
                        {
                            fieldNames.add( fields[j].getName() );
                        }
                    }
                }

                BLACKLISTED_FIELD_NAMES = fieldNames;
            }
        }
    }

}
