package org.codehaus.doxia.site.renderer;

import org.apache.maven.doxia.siterenderer.RendererException;
import org.codehaus.doxia.site.renderer.sink.SiteRendererSink;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Locale;
import java.util.Map;

/*
 * Copyright 2004-2005 The Apache Software Foundation.
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

/**
 * @author <a href="mailto:evenisse@codehaus.org">Emmanuel Venisse</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id:DefaultSiteRenderer.java 348612 2005-11-24 12:54:19 +1100 (Thu, 24 Nov 2005) brett $
 * @plexus.component role="org.apache.maven.doxia.siterenderer.Renderer"
 */
public class DefaultSiteRenderer
    extends org.apache.maven.doxia.siterenderer.DefaultSiteRenderer
    implements Renderer
{

    public void render( File siteDirectory, File outputDirectory, File siteDescriptor, String templateName,
                       Map templateProperties )
        throws org.codehaus.doxia.site.renderer.RendererException, IOException
    {
        try
        {
            super.render( siteDirectory, outputDirectory, siteDescriptor, templateName, templateProperties );
        }
        catch ( RendererException e )
        {
            throw new org.codehaus.doxia.site.renderer.RendererException( e );
        }
    }

    public void render( File siteDirectory, File outputDirectory, InputStream siteDescriptor, String templateName,
                       Map templateProperties )
        throws org.codehaus.doxia.site.renderer.RendererException, IOException
    {
        try
        {
            super.render( siteDirectory, outputDirectory, siteDescriptor, templateName, templateProperties );
        }
        catch ( RendererException e )
        {
            throw new org.codehaus.doxia.site.renderer.RendererException( e );
        }
    }

    public void render( File siteDirectory, File outputDirectory, String siteDescriptor, String templateName,
                       Map templateProperties )
        throws org.codehaus.doxia.site.renderer.RendererException, IOException
    {
        try
        {
            super.render( siteDirectory, outputDirectory, siteDescriptor, templateName, templateProperties );
        }
        catch ( RendererException e )
        {
            throw new org.codehaus.doxia.site.renderer.RendererException( e );
        }
    }

    public void render( File siteDirectory, File outputDirectory, File siteDescriptor, String templateName,
                       Map templateProperties, Locale locale )
        throws org.codehaus.doxia.site.renderer.RendererException, IOException
    {
        try
        {
            super.render( siteDirectory, outputDirectory, siteDescriptor, templateName, templateProperties, locale );
        }
        catch ( RendererException e )
        {
            throw new org.codehaus.doxia.site.renderer.RendererException( e );
        }
    }

    public void render( File siteDirectory, File outputDirectory, InputStream siteDescriptor, String templateName,
                       Map templateProperties, Locale locale )
        throws org.codehaus.doxia.site.renderer.RendererException, IOException
    {
        try
        {
            super.render( siteDirectory, outputDirectory, siteDescriptor, templateName, templateProperties, locale );
        }
        catch ( RendererException e )
        {
            throw new org.codehaus.doxia.site.renderer.RendererException( e );
        }
    }

    public void render( File siteDirectory, File outputDirectory, String siteDescriptor, String templateName,
                       Map templateProperties, Locale locale )
        throws org.codehaus.doxia.site.renderer.RendererException, IOException
    {
        try
        {
            super.render( siteDirectory, outputDirectory, siteDescriptor, templateName, templateProperties, locale );
        }
        catch ( RendererException e )
        {
            throw new org.codehaus.doxia.site.renderer.RendererException( e );
        }
    }

    public void render( File siteDirectory, File outputDirectory, InputStream siteDescriptor, String templateName,
                       Map templateProperties, Locale locale, String outputEncoding )
        throws org.codehaus.doxia.site.renderer.RendererException, IOException
    {
        try
        {
            super.render( siteDirectory, outputDirectory, siteDescriptor, templateName, templateProperties, locale,
                          outputEncoding );
        }
        catch ( RendererException e )
        {
            throw new org.codehaus.doxia.site.renderer.RendererException( e );
        }
    }

    public void render( File siteDirectory, File outputDirectory, String module, String moduleExtension,
                       String moduleParserId, String siteDescriptor, String templateName, Map templateProperties,
                       Locale locale, String outputEncoding )
        throws org.codehaus.doxia.site.renderer.RendererException, IOException
    {
        try
        {
            super.render( siteDirectory, outputDirectory, module, moduleExtension, moduleParserId, siteDescriptor,
                          templateName, templateProperties, locale, outputEncoding );
        }
        catch ( RendererException e )
        {
            throw new org.codehaus.doxia.site.renderer.RendererException( e );
        }
    }

    public void render( File siteDirectory, File outputDirectory, String module, String moduleExtension,
                       String moduleParserId, InputStream siteDescriptor, String templateName, Map templateProperties,
                       Locale locale, String outputEncoding )
        throws org.codehaus.doxia.site.renderer.RendererException, IOException
    {
        try
        {
            super.render( siteDirectory, outputDirectory, module, moduleExtension, moduleParserId, siteDescriptor,
                          templateName, templateProperties, locale, outputEncoding );
        }
        catch ( RendererException e )
        {
            throw new org.codehaus.doxia.site.renderer.RendererException( e );
        }
    }

    public void generateDocument( Writer writer, String templateName, Map templateProperties,
                                 org.apache.maven.doxia.siterenderer.sink.SiteRendererSink sink )
        throws org.codehaus.doxia.site.renderer.RendererException
    {
        try
        {
            super.generateDocument( writer, templateName, templateProperties, sink );
        }
        catch ( RendererException e )
        {
            throw new org.codehaus.doxia.site.renderer.RendererException( e );
        }
    }

    public void generateDocument( Writer writer, String templateName, Map templateProperties,
                                 org.apache.maven.doxia.siterenderer.sink.SiteRendererSink sink, Locale locale )
        throws org.codehaus.doxia.site.renderer.RendererException
    {
        try
        {
            super.generateDocument( writer, templateName, templateProperties, sink, locale );
        }
        catch ( RendererException e )
        {
            throw new org.codehaus.doxia.site.renderer.RendererException( e );
        }
    }

    public void generateDocument( Writer writer, String templateName, Map templateProperties, SiteRendererSink sink )
        throws org.codehaus.doxia.site.renderer.RendererException
    {
        try
        {
            super.generateDocument( writer, templateName, templateProperties, sink.getSinkDelegate() );
        }
        catch ( RendererException e )
        {
            throw new org.codehaus.doxia.site.renderer.RendererException( e );
        }
    }

    public void generateDocument( Writer writer, String templateName, Map templateProperties, SiteRendererSink sink,
                                 Locale locale )
        throws org.codehaus.doxia.site.renderer.RendererException
    {
        try
        {
            super.generateDocument( writer, templateName, templateProperties, sink.getSinkDelegate(), locale );
        }
        catch ( RendererException e )
        {
            throw new org.codehaus.doxia.site.renderer.RendererException( e );
        }
    }

    public org.apache.maven.doxia.siterenderer.sink.SiteRendererSink createSink( File moduleBaseDir, String document,
                                                                                File siteDescriptor )
        throws org.codehaus.doxia.site.renderer.RendererException, IOException
    {
        try
        {
            return new org.codehaus.doxia.site.renderer.sink.SiteRendererSink( super.createSink( moduleBaseDir,
                                                                                                 document,
                                                                                                 siteDescriptor ) );
        }
        catch ( RendererException e )
        {
            throw new org.codehaus.doxia.site.renderer.RendererException( e );
        }
    }

    public org.apache.maven.doxia.siterenderer.sink.SiteRendererSink createSink( File moduleBaseDir, String document,
                                                                                String siteDescriptor )
        throws org.codehaus.doxia.site.renderer.RendererException, IOException
    {
        try
        {
            return new org.codehaus.doxia.site.renderer.sink.SiteRendererSink( super.createSink( moduleBaseDir,
                                                                                                 document,
                                                                                                 siteDescriptor ) );
        }
        catch ( RendererException e )
        {
            throw new org.codehaus.doxia.site.renderer.RendererException( e );
        }
    }

    public org.apache.maven.doxia.siterenderer.sink.SiteRendererSink createSink( File moduleBaseDir, String document,
                                                                                InputStream siteDescriptor )
        throws org.codehaus.doxia.site.renderer.RendererException, IOException
    {
        try
        {
            return new org.codehaus.doxia.site.renderer.sink.SiteRendererSink( super.createSink( moduleBaseDir,
                                                                                                 document,
                                                                                                 siteDescriptor ) );
        }
        catch ( RendererException e )
        {
            throw new org.codehaus.doxia.site.renderer.RendererException( e );
        }
    }

}
