package org.apache.maven.project.harness;

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

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathNotFoundException;
import org.apache.commons.jxpath.ri.JXPathContextReferenceImpl;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.builder.PomClassicDomainModel;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.model.ModelProperty;
import org.codehaus.plexus.util.WriterFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class PomTestWrapper
{

    private PomClassicDomainModel domainModel;

    private File pomFile;

    private JXPathContext context;

    private MavenProject mavenProject;

    static
    {
        JXPathContextReferenceImpl.addNodePointerFactory( new Xpp3DomPointerFactory() );
    }

    public PomTestWrapper( PomClassicDomainModel domainModel )
        throws IOException
    {
        this( null, domainModel );
    }

    public PomTestWrapper( File pomFile, PomClassicDomainModel domainModel )
        throws IOException
    {
        if ( domainModel == null )
        {
            throw new IllegalArgumentException( "domainModel: null" );
        }
        this.domainModel = domainModel;
        this.pomFile = pomFile;
        try {
            context = JXPathContext.newContext( new MavenXpp3Reader().read(domainModel.getInputStream()));
        } catch (XmlPullParserException e) {
            throw new IOException(e.getMessage());
        }
    }

    public PomTestWrapper( File pomFile, MavenProject mavenProject )
        throws IOException
    {
        if ( mavenProject == null )
        {
            throw new IllegalArgumentException( "mavenProject: null" );
        }
        this.mavenProject = mavenProject;
        this.pomFile = pomFile;
        context = JXPathContext.newContext( mavenProject.getModel() );
    }

    public PomTestWrapper( MavenProject mavenProject )
        throws IOException
    {
        if ( mavenProject == null )
        {
            throw new IllegalArgumentException( "mavenProject: null" );
        }
        this.mavenProject = mavenProject;
        context = JXPathContext.newContext( mavenProject.getModel() );
    }

    public PomTestWrapper( File file )
        throws IOException
    {
        if ( file == null )
        {
            throw new IllegalArgumentException( "file: null" );
        }

        this.domainModel = new PomClassicDomainModel( file );
        try {
            context = JXPathContext.newContext( new MavenXpp3Reader().read(domainModel.getInputStream()));
        } catch (XmlPullParserException e) {
            throw new IOException(e.getMessage());
        }
    }

    public MavenProject getMavenProject()
    {
        return mavenProject;
    }

    public PomClassicDomainModel getDomainModel()
    {
        if ( domainModel == null && mavenProject != null )
        {
            try
            {
                domainModel = convertToDomainModel( mavenProject.getModel() );
                int lineageCount = 1;
                for ( MavenProject parent = mavenProject.getParent(); parent != null; parent = parent.getParent() )
                {
                    lineageCount++;
                }
                domainModel.setLineageCount( lineageCount );
            }
            catch ( IOException e )
            {

            }
        }

        return this.domainModel;
    }

    private PomClassicDomainModel convertToDomainModel(Model model) throws IOException
    {
                if ( model == null )
        {
            throw new IllegalArgumentException( "model: null" );
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Writer out = null;
        MavenXpp3Writer writer = new MavenXpp3Writer();
        try
        {
            out = WriterFactory.newXmlWriter( baos );
            writer.write( out, model );
        }
        finally
        {
            if ( out != null )
            {
                out.close();
            }
        }
        return new PomClassicDomainModel(new ByteArrayInputStream(baos.toByteArray()));
    }

    public File getBasedir()
    {
        return ( pomFile != null ) ? pomFile.getParentFile() : null;
    }

    public String getValueOfProjectUri( String projectUri, boolean withResolvedValue )
        throws IOException
    {
        if ( projectUri.contains( "#collection" ) || projectUri.contains( "#set" ) )
        {
            throw new IllegalArgumentException( "projectUri: contains a collection or set" );
        }
        return asMap( withResolvedValue ).get( projectUri );
    }

    public void setValueOnModel( String expression, Object value )
    {
        context.setValue( expression, value );
    }

    /*
    public int containerCountForUri( String uri )
        throws IOException
    {
        if ( uri == null || uri.trim().equals( "" ) )
        {
            throw new IllegalArgumentException( "uri: null or empty" );
        }
        ModelDataSource source = new DefaultModelDataSource();
        source.init( domainModel.getModelProperties(), null );
        return source.queryFor( uri ).size();
    }
	*/

	public Iterator<?> getIteratorForXPathExpression( String expression )
    {
        return context.iterate( expression );
    }

    public boolean containsXPathExpression( String expression )
    {
        return context.getValue( expression ) != null;
    }

    public Object getValue( String expression )
    {
        try
        {
            return context.getValue( expression );
        }
        catch ( JXPathNotFoundException e )
        {
            return null;
        }
    }

    public boolean xPathExpressionEqualsValue( String expression, String value )
    {
        return context.getValue( expression ) != null && context.getValue( expression ).equals( value );
    }

    public Map<String, String> asMap( boolean withResolvedValues )
        throws IOException
    {
        Map<String, String> map = new HashMap<String, String>();
        for ( ModelProperty mp : domainModel.getModelProperties() )
        {
            if ( withResolvedValues )
            {
                map.put( mp.getUri(), mp.getResolvedValue() );
            }
            else
            {
                map.put( mp.getUri(), mp.getValue() );
            }

        }
        return map;
    }

    public boolean containsModelProperty( ModelProperty modelProperty )
        throws IOException
    {
        return domainModel.getModelProperties().contains( modelProperty );
    }

    public boolean containsAllModelPropertiesOf( List<ModelProperty> modelProperties )
        throws IOException
    {
        for ( ModelProperty mp : modelProperties )
        {
            if ( !containsModelProperty( mp ) )
            {
                return false;
            }
        }
        return true;
    }

    public boolean matchModelProperties( List<ModelProperty> hasProperties, List<ModelProperty> doesNotHaveProperties )
        throws IOException
    {
        return containsAllModelPropertiesOf( hasProperties ) && containNoModelPropertiesOf( doesNotHaveProperties );
    }

    public boolean matchUris( List<String> hasAllUris, List<String> doesNotHaveUris )
        throws IOException
    {
        return hasAllUris( hasAllUris ) && hasNoUris( doesNotHaveUris );
    }

    public boolean containNoModelPropertiesOf( List<ModelProperty> modelProperties )
        throws IOException
    {
        for ( ModelProperty mp : modelProperties )
        {
            if ( containsModelProperty( mp ) )
            {
                return false;
            }
        }
        return true;
    }

    public boolean hasUri( String uri )
        throws IOException
    {
        for ( ModelProperty mp : domainModel.getModelProperties() )
        {
            if ( mp.getValue().equals( uri ) )
            {
                return true;
            }
        }
        return false;
    }

    public boolean hasAllUris( List<String> uris )
        throws IOException
    {
        for ( String s : uris )
        {
            if ( !hasUri( s ) )
            {
                return false;
            }
        }
        return true;
    }

    public boolean hasNoUris( List<String> uris )
        throws IOException
    {
        for ( String s : uris )
        {
            if ( hasUri( s ) )
            {
                return false;
            }
        }
        return true;
    }

}
