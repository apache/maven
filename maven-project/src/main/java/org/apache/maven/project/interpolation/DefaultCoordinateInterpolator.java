package org.apache.maven.project.interpolation;

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

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.InterpolationPostProcessor;
import org.codehaus.plexus.interpolation.Interpolator;
import org.codehaus.plexus.interpolation.RecursionInterceptor;
import org.codehaus.plexus.interpolation.SimpleRecursionInterceptor;
import org.codehaus.plexus.interpolation.ValueSource;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.XmlStreamWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
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

public class DefaultCoordinateInterpolator
    extends StringSearchModelInterpolator
    implements CoordinateInterpolator
{

    private static final List<String> VERSION_INTERPOLATION_TARGET_XPATHS;

    static
    {
        List<String> targets = new ArrayList<String>();
        
        // packaging
        targets.add( "/project/packaging/text()" );

        // groupId
        targets.add( "/project/parent/groupId/text()" );
        targets.add( "/project/groupId/text()" );
        
        targets.add( "/project/dependencies/dependency/groupId/text()" );
        targets.add( "/project/dependencyManagement/dependencies/dependency/groupId/text()" );
        
        targets.add( "/project/build/plugins/plugin/groupId/text()" );
        targets.add( "/project/build/pluginManagement/plugins/plugin/groupId/text()" );
        targets.add( "/project/build/plugins/plugin/dependencies/dependency/groupId/text()" );
        targets.add( "/project/build/pluginManagement/plugins/plugin/dependencies/dependency/groupId/text()" );
        
        targets.add( "/project/reporting/plugins/plugin/groupId/text()" );

        targets.add( "/project/profiles/profile/dependencies/dependency/groupId/text()" );
        targets.add( "/project/profiles/profile/dependencyManagement/dependencies/dependency/groupId/text()" );
        
        targets.add( "/project/profiles/profile/build/plugins/plugin/groupId/text()" );
        targets.add( "/project/profiles/profile/build/pluginManagement/plugins/plugin/groupId/text()" );
        targets.add( "/project/profiles/profile/build/plugins/plugin/dependencies/dependency/groupId/text()" );
        targets.add( "/project/profiles/profile/build/pluginManagement/plugins/plugin/dependencies/dependency/groupId/text()" );
        
        targets.add( "/project/profiles/profile/reporting/plugins/plugin/groupId/text()" );

        // artifactId
        targets.add( "/project/parent/artifactId/text()" );
        targets.add( "/project/artifactId/text()" );
        
        targets.add( "/project/dependencies/dependency/artifactId/text()" );
        targets.add( "/project/dependencyManagement/dependencies/dependency/artifactId/text()" );
        
        targets.add( "/project/build/plugins/plugin/artifactId/text()" );
        targets.add( "/project/build/pluginManagement/plugins/plugin/artifactId/text()" );
        targets.add( "/project/build/plugins/plugin/dependencies/dependency/artifactId/text()" );
        targets.add( "/project/build/pluginManagement/plugins/plugin/dependencies/dependency/artifactId/text()" );
        
        targets.add( "/project/reporting/plugins/plugin/artifactId/text()" );

        targets.add( "/project/profiles/profile/dependencies/dependency/artifactId/text()" );
        targets.add( "/project/profiles/profile/dependencyManagement/dependencies/dependency/artifactId/text()" );
        
        targets.add( "/project/profiles/profile/build/plugins/plugin/artifactId/text()" );
        targets.add( "/project/profiles/profile/build/pluginManagement/plugins/plugin/artifactId/text()" );
        targets.add( "/project/profiles/profile/build/plugins/plugin/dependencies/dependency/artifactId/text()" );
        targets.add( "/project/profiles/profile/build/pluginManagement/plugins/plugin/dependencies/dependency/artifactId/text()" );
        
        targets.add( "/project/profiles/profile/reporting/plugins/plugin/artifactId/text()" );

        // version
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
        
        // other dependency-specific elements
        // classifier
        targets.add( "/project/dependencies/dependency/classifier/text()" );
        targets.add( "/project/dependencyManagement/dependencies/dependency/classifier/text()" );
        
        targets.add( "/project/build/plugins/plugin/dependencies/dependency/classifier/text()" );
        targets.add( "/project/build/pluginManagement/plugins/plugin/dependencies/dependency/classifier/text()" );
        
        targets.add( "/project/profiles/profile/dependencies/dependency/classifier/text()" );
        targets.add( "/project/profiles/profile/dependencyManagement/dependencies/dependency/classifier/text()" );
        
        targets.add( "/project/profiles/profile/build/plugins/plugin/dependencies/dependency/classifier/text()" );
        targets.add( "/project/profiles/profile/build/pluginManagement/plugins/plugin/dependencies/dependency/classifier/text()" );

        // type
        targets.add( "/project/dependencies/dependency/type/text()" );
        targets.add( "/project/dependencyManagement/dependencies/dependency/type/text()" );
        
        targets.add( "/project/build/plugins/plugin/dependencies/dependency/type/text()" );
        targets.add( "/project/build/pluginManagement/plugins/plugin/dependencies/dependency/type/text()" );
        
        targets.add( "/project/profiles/profile/dependencies/dependency/type/text()" );
        targets.add( "/project/profiles/profile/dependencyManagement/dependencies/dependency/type/text()" );
        
        targets.add( "/project/profiles/profile/build/plugins/plugin/dependencies/dependency/type/text()" );
        targets.add( "/project/profiles/profile/build/pluginManagement/plugins/plugin/dependencies/dependency/type/text()" );
        
        targets = Collections.unmodifiableList( targets );

        VERSION_INTERPOLATION_TARGET_XPATHS = targets;
    }

    public synchronized void interpolateArtifactCoordinates( MavenProject project )
        throws IOException, ModelInterpolationException
    {
        ProjectBuilderConfiguration config = project.getProjectBuilderConfiguration();
        Model model = project.getOriginalModel();
        File projectDir = project.getBasedir();
        File pomFile = project.getFile();
        
        File outputFile = new File( projectDir, COORDINATE_INTERPOLATED_POMFILE );
        outputFile.deleteOnExit();
        
        getLogger().debug( "POM artifact coordinates are being interpolated. New POM will be stored at: " + outputFile );

        List<ValueSource> valueSources = createValueSources( model, projectDir, config );
        List<InterpolationPostProcessor> postProcessors = createPostProcessors( model, projectDir, config );

        String pomContents = doInterpolation( pomFile, valueSources, postProcessors );

        Writer writer = null;
        try
        {
            if ( outputFile.getParentFile() != null )
            {
                outputFile.getParentFile().mkdirs();
            }

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
        
        project.setFile( outputFile );
    }

    private String doInterpolation( File pomFile, List<ValueSource> valueSources,
                                    List<InterpolationPostProcessor> postProcessors )
        throws ModelInterpolationException
    {
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

            giveFeedback( interpolator );

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
        
        return pomContents;
    }

    @SuppressWarnings("unchecked")
    private void giveFeedback( Interpolator interpolator )
    {
        if ( getLogger() == null || !getLogger().isDebugEnabled() )
        {
            return;
        }
        
        List<Object> feedback = interpolator.getFeedback();
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
                    logger.debug( "Writing transformed POM using encoding: " + encoding );
                    transformer.setOutputProperty( OutputKeys.ENCODING, encoding );
                }
                else
                {
                    logger.debug( "Writing transformed POM using default encoding" );
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
