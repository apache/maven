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
import org.codehaus.plexus.interpolation.SimpleRecursionInterceptor;
import org.codehaus.plexus.interpolation.ValueSource;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.XmlStreamWriter;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class VersionExpressionTransformation
    extends StringSearchModelInterpolator
    implements Initializable, ArtifactTransformation
{

    private static final List<String> VERSION_INTERPOLATION_TARGET_XPATHS;

    static
    {
        List<String> targets = new ArrayList<String>();

        targets.add( "/project/parent/version/text()" );
        targets.add( "/project/version/text()" );
        
        targets.add( "/project/dependencies/dependency/version/text()" );
        targets.add( "/project/dependencyManagement/dependencies/dependency/version/text()" );
        
        targets.add( "/project/build/plugins/plugin/version/text()" );
        targets.add( "/project/build/pluginManagement/plugins/plugin/version/text()" );
        targets.add( "/project/build/plugins/plugin/dependencies/dependency/version/text()" );
        targets.add( "/project/build/pluginManagement/plugins/plugin/dependencies/dependency/version/text()" );
        
        targets.add( "/project/reporting/plugins/plugin/version/text()" );

        targets.add( "/project/profiles/profile/dependencies/dependency/version/text()" );
        targets.add( "/project/profiles/profile/dependencyManagement/dependencies/dependency/version/text()" );
        
        targets.add( "/project/profiles/profile/build/plugins/plugin/version/text()" );
        targets.add( "/project/profiles/profile/build/pluginManagement/plugins/plugin/version/text()" );
        targets.add( "/project/profiles/profile/build/plugins/plugin/dependencies/dependency/version/text()" );
        targets.add( "/project/profiles/profile/build/pluginManagement/plugins/plugin/dependencies/dependency/version/text()" );
        
        targets.add( "/project/profiles/profile/reporting/plugins/plugin/version/text()" );

        targets = Collections.unmodifiableList( targets );

        VERSION_INTERPOLATION_TARGET_XPATHS = targets;
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
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( "On Deploy: Using artifact file for POM: " + artifact );
            }
            pomFile = artifact.getFile();
            pomArtifact = true;
        }
        // FIXME: We can't be this smart (yet) since the deployment step transforms from the
        // original POM once again and re-installs over the top of the install step.
        // else if ( metadata == null || metadata.isVersionExpressionsResolved() )
        // {
        // return;
        // }
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
                // FIXME: We need a way to mark a POM artifact as resolved WRT version expressions, so we don't
                // reprocess...
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
        ProjectArtifactMetadata metadata =
            (ProjectArtifactMetadata) artifact.getMetadata( ProjectArtifactMetadata.class );
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
        // else if ( metadata == null || metadata.isVersionExpressionsResolved() )
        // {
        // return;
        // }
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
                // FIXME: We need a way to mark a POM artifact as resolved WRT version expressions, so we don't
                // reprocess...
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

    public void transformForResolve( Artifact artifact, List<ArtifactRepository> remoteRepositories,
                                     ArtifactRepository localRepository )
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
                                   "WARNING: Artifact: "
                                       + artifact
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

            String detail =
                "\n\nNOTE: Error was in file: " + pomFile + ", at line: " + e.getLineNumber() + ", column: "
                    + e.getColumnNumber();

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

    @SuppressWarnings("unchecked")
    protected void interpolateVersions( File pomFile, File outputFile, Model model, File projectDir,
                                        ProjectBuilderConfiguration config )
        throws ModelInterpolationException
    {
        boolean debugEnabled = getLogger().isDebugEnabled();

        // NOTE: We want to interpolate version expressions ONLY, and want to do so without requiring the
        // use of the XPP3 Model reader/writers, which have a tendency to lose XML comments and such.
        // SOOO, we're using a two-stage string interpolation here. The first stage selects all XML 'version'
        // elements, and subjects their values to interpolation in the second stage.
        XPathInterpolator interpolator = new XPathInterpolator( getLogger() );

        // The second-stage interpolator is the 'normal' one used in all Model interpolation throughout
        // maven-project.
        Interpolator secondaryInterpolator = getInterpolator();

        // We'll just reuse the recursion interceptor...not sure it makes any difference.
        RecursionInterceptor recursionInterceptor = getRecursionInterceptor();

        // This is a ValueSource implementation that simply delegates to the second-stage "real" interpolator
        // once we've isolated the version elements from the input XML.
        interpolator.addValueSource( new SecondaryInterpolationValueSource( secondaryInterpolator, recursionInterceptor ) );

        List<ValueSource> valueSources = createValueSources( model, projectDir, config );
        List<InterpolationPostProcessor> postProcessors = createPostProcessors( model, projectDir, config );

        synchronized ( this )
        {
            for ( ValueSource vs : valueSources )
            {
                secondaryInterpolator.addValueSource( vs );
            }

            for ( InterpolationPostProcessor postProcessor : postProcessors )
            {
                secondaryInterpolator.addPostProcessor( postProcessor );
            }

            String pomContents;
            try
            {
                XmlStreamReader reader = null;
                try
                {
                    reader = ReaderFactory.newXmlReader( pomFile );
                    pomContents = IOUtil.toString( reader );
                    interpolator.setEncoding( reader.getEncoding() );
                }
                catch ( IOException e )
                {
                    throw new ModelInterpolationException( "Error reading POM for version-expression interpolation: "
                        + e.getMessage(), e );
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
                    List<Object> feedback = (List<Object>) interpolator.getFeedback();
                    if ( feedback != null && !feedback.isEmpty() )
                    {
                        getLogger().debug( "Maven encountered the following problems while transforming POM versions:" );

                        Object last = null;
                        for ( Object next : feedback )
                        {
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
                for ( ValueSource vs : valueSources )
                {
                    secondaryInterpolator.removeValuesSource( vs );
                }

                for ( InterpolationPostProcessor postProcessor : postProcessors )
                {
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
                throw new ModelInterpolationException( "Failed to write transformed POM: "
                    + outputFile.getAbsolutePath(), e );
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

        private List<Object> localFeedback = new ArrayList<Object>();

        public SecondaryInterpolationValueSource( Interpolator secondary, RecursionInterceptor recursionInterceptor )
        {
            this.secondary = secondary;
            this.recursionInterceptor = recursionInterceptor;
        }

        public void clearFeedback()
        {
            secondary.clearFeedback();
        }

        @SuppressWarnings("unchecked")
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

    private static final class XPathInterpolator
        implements Interpolator
    {

        private List<InterpolationPostProcessor> postProcessors = new ArrayList<InterpolationPostProcessor>();

        private List<ValueSource> valueSources = new ArrayList<ValueSource>();

        private Map<String, Object> answers = new HashMap<String, Object>();

        private List<Object> feedback = new ArrayList<Object>();

        private final Logger logger;

        private String encoding;

        public XPathInterpolator( Logger logger )
        {
            this.logger = logger;
        }

        public void setEncoding( String encoding )
        {
            this.encoding = encoding;
        }

        public String interpolate( String input, RecursionInterceptor recursionInterceptor )
            throws InterpolationException
        {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            TransformerFactory txFactory = TransformerFactory.newInstance();
            XPathFactory xpFactory = XPathFactory.newInstance();
            
            DocumentBuilder builder;
            Transformer transformer;
            XPath xpath;
            try
            {
                builder = dbFactory.newDocumentBuilder();
                transformer = txFactory.newTransformer();
                xpath = xpFactory.newXPath();
            }
            catch ( ParserConfigurationException e )
            {
                throw new InterpolationException( "Failed to construct XML DocumentBuilder: " + e.getMessage(), "-NONE-", e );
            }
            catch ( TransformerConfigurationException e )
            {
                throw new InterpolationException( "Failed to construct XML Transformer: " + e.getMessage(), "-NONE-", e );
            }
            
            Document document;
            try
            {
                document = builder.parse( new InputSource( new StringReader( input ) ) );
            }
            catch ( SAXException e )
            {
                throw new InterpolationException( "Failed to parse XML: " + e.getMessage(), "-NONE-", e );
            }
            catch ( IOException e )
            {
                throw new InterpolationException( "Failed to parse XML: " + e.getMessage(), "-NONE-", e );
            }
            
            inteprolateInternal( document, xpath );
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            XmlStreamWriter writer;
            try
            {
                writer = WriterFactory.newXmlWriter( baos );
            }
            catch ( IOException e )
            {
                throw new InterpolationException( "Failed to get XML writer: " + e.getMessage(), "-NONE-", e );
            }
            
            StreamResult r = new StreamResult( writer );
            DOMSource s = new DOMSource( document );
            
            try
            {
                if ( encoding != null )
                {
                    logger.info( "Writing transformed POM using encoding: " + encoding );
                    transformer.setOutputProperty( OutputKeys.ENCODING, encoding );
                }
                else
                {
                    logger.info( "Writing transformed POM using default encoding" );
                }
                
                transformer.transform( s, r );
            }
            catch ( TransformerException e )
            {
                throw new InterpolationException( "Failed to render interpolated XML: " + e.getMessage(), "-NONE-", e );
            }
            
            try
            {
                return baos.toString( writer.getEncoding() );
            }
            catch ( UnsupportedEncodingException e )
            {
                throw new InterpolationException( "Failed to render interpolated XML: " + e.getMessage(), "-NONE-", e );
            }
        }

        private void inteprolateInternal( Document document, XPath xp )
            throws InterpolationException
        {
            for ( String expr : VERSION_INTERPOLATION_TARGET_XPATHS )
            {
                NodeList nodes;
                try
                {
                    XPathExpression xpath = xp.compile( expr );
                    nodes = (NodeList) xpath.evaluate( document, XPathConstants.NODESET );
                }
                catch ( XPathExpressionException e )
                {
                    throw new InterpolationException( "Failed to evaluate XPath: " + expr + " (" + e.getMessage() + ")", "-NONE-", e );
                }
                
                if ( nodes != null )
                {
                    for( int idx = 0; idx < nodes.getLength(); idx++ )
                    {
                        Node node = nodes.item( idx );
                        Object value = node.getNodeValue();
                        if ( value == null )
                        {
                            continue;
                        }
                        
                        for ( ValueSource vs : valueSources )
                        {
                            if ( vs != null )
                            {
                                value = vs.getValue( value.toString() );
                                if ( value != null && !value.equals( node.getNodeValue() ) )
                                {
                                    break;
                                }
                                else if ( value == null )
                                {
                                    value = node.getNodeValue();
                                }
                            }
                        }
                        
                        if ( value != null && !value.equals( node.getNodeValue() ) )
                        {
                            for ( InterpolationPostProcessor postProcessor : postProcessors )
                            {
                                if ( postProcessor != null )
                                {
                                    value = postProcessor.execute( node.getNodeValue(), value );
                                }
                            }
                            
                            node.setNodeValue( String.valueOf( value ) );
                        }
                    }
                }
            }
        }

        public void addPostProcessor( InterpolationPostProcessor postProcessor )
        {
            postProcessors.add( postProcessor );
        }

        public void addValueSource( ValueSource valueSource )
        {
            valueSources.add( valueSource );
        }

        public void clearAnswers()
        {
            answers.clear();
        }

        public void clearFeedback()
        {
            feedback.clear();
        }

        @SuppressWarnings( "unchecked" )
        public List getFeedback()
        {
            return feedback;
        }

        public String interpolate( String input )
            throws InterpolationException
        {
            return interpolate( input, new SimpleRecursionInterceptor() );
        }

        public String interpolate( String input, String thisPrefixPattern )
            throws InterpolationException
        {
            return interpolate( input, new SimpleRecursionInterceptor() );
        }

        public String interpolate( String input, String thisPrefixPattern, RecursionInterceptor recursionInterceptor )
            throws InterpolationException
        {
            return interpolate( input, recursionInterceptor );
        }

        public boolean isCacheAnswers()
        {
            return true;
        }

        public void removePostProcessor( InterpolationPostProcessor postProcessor )
        {
            postProcessors.remove( postProcessor );
        }

        public void removeValuesSource( ValueSource valueSource )
        {
            valueSources.remove( valueSource );
        }

        public void setCacheAnswers( boolean cacheAnswers )
        {
        }
    }

}
