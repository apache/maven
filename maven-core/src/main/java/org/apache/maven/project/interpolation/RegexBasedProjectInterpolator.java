package org.apache.maven.project.interpolation;

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

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.util.introspection.ReflectionValueExtractor;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.Logger;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jdcasey Created on Feb 3, 2005
 */
public class RegexBasedProjectInterpolator
    extends AbstractLogEnabled
    implements ProjectInterpolator
{

    private static final Pattern EXPRESSION_PATTERN = Pattern.compile( "#([.A-Za-z]+)" );

    /**
     * Added: Feb 3, 2005 by jdcasey
     */
    public MavenProject interpolate( MavenProject project ) throws ProjectInterpolationException
    {
        StringWriter sWriter = new StringWriter();
        Model model = project.getModel();

        MavenXpp3Writer writer = new MavenXpp3Writer();
        try
        {
            writer.write( sWriter, model );
        }
        catch( Exception e )
        {
            throw new ProjectInterpolationException(
                "Cannot serialize project model for interpolation.", e );
        }

        String serializedModel = sWriter.toString();
        serializedModel = interpolateInternal( serializedModel, model );

        StringReader sReader = new StringReader( serializedModel );

        MavenXpp3Reader modelReader = new MavenXpp3Reader();
        try
        {
            model = modelReader.read( sReader );
        }
        catch( Exception e )
        {
            throw new ProjectInterpolationException(
                "Cannot read project model from interpolating filter of serialized version.", e );
        }

        MavenProject newProject = new MavenProject( model );
        newProject.setParent( project.getParent() );
        newProject.setFile( project.getFile() );
        newProject.setArtifacts( project.getArtifacts() );

        return new MavenProject( model );
    }

    /**
     * Added: Feb 3, 2005 by jdcasey
     * 
     * @throws Exception
     */
    private String interpolateInternal( String src, Model model )
    {
        String result = src;
        Matcher matcher = EXPRESSION_PATTERN.matcher( result );
        while( matcher.find() )
        {
            String wholeExpr = matcher.group( 0 );
            String realExpr = matcher.group( 1 );

            String value = null;
            try
            {
                value = String.valueOf( ReflectionValueExtractor.evaluate( realExpr, model ) );
            }
            catch( Exception e )
            {
                Logger logger = getLogger();
                if( logger != null )
                {
                    logger.debug( "POM interpolation cannot proceed with expression: " + wholeExpr
                        + ". Skipping...", e );
                }
            }

            if( value != null )
            {
                result = result.replaceAll( wholeExpr, value );
                matcher.reset( result );
            }
        }

        return result;
    }

}