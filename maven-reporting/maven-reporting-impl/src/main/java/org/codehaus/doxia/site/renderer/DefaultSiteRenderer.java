package org.codehaus.doxia.site.renderer;

import org.apache.maven.doxia.site.renderer.SiteRenderer;
import org.codehaus.doxia.module.xhtml.XhtmlSink;

import java.io.File;
import java.io.InputStream;

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
 * 
 * @deprecated Use org.apache.maven.doxia.site.renderer.DefaultSiteRenderer instead.
 */
public class DefaultSiteRenderer
    implements org.codehaus.doxia.site.renderer.SiteRenderer
{

    private SiteRenderer siteRenderer;

    public XhtmlSink createSink( File moduleBasedir, String doc,
                                                                String outputDirectory, File siteDescriptor,
                                                                String flavour )
        throws Exception
    {
        return new org.codehaus.doxia.module.xhtml.XhtmlSink( siteRenderer.createSink( moduleBasedir, doc,
                                                                                       outputDirectory, siteDescriptor,
                                                                                       flavour ) );
    }

    public void render( String siteDirectory, String generatedSiteDirectory, String outputDirectory,
                       File resourcesDirectory )
        throws Exception
    {
        siteRenderer.render( siteDirectory, generatedSiteDirectory, outputDirectory, resourcesDirectory );
    }

    public void render( String siteDirectory, String generatedSiteDirectory, String outputDirectory, String flavour,
                       File resourcesDirectory )
        throws Exception
    {
        siteRenderer.render( siteDirectory, generatedSiteDirectory, outputDirectory, flavour, resourcesDirectory );
    }

    public void render( String siteDirectory, String generatedSiteDirectory, String outputDirectory, String flavour,
                       String siteDescriptorName, File resourcesDirectory )
        throws Exception
    {
        siteRenderer.render( siteDirectory, generatedSiteDirectory, outputDirectory, flavour, siteDescriptorName,
                             resourcesDirectory );
    }

    public void render( String siteDirectory, String generatedSiteDirectory, String outputDirectory, String flavour,
                       InputStream siteDescriptor, File resourcesDirectory )
        throws Exception
    {
        siteRenderer.render( siteDirectory, generatedSiteDirectory, outputDirectory, flavour, siteDescriptor,
                             resourcesDirectory );
    }

    public XhtmlSink createSink( File moduleBasedir, String doc,
                                                                String outputDirectory, InputStream siteDescriptor,
                                                                String flavour )
        throws Exception
    {
        return new org.codehaus.doxia.module.xhtml.XhtmlSink( siteRenderer.createSink( moduleBasedir, doc,
                                                                                       outputDirectory, siteDescriptor,
                                                                                       flavour ) );
    }

    public void copyResources( String outputDirectory, String flavour )
        throws Exception
    {
        siteRenderer.copyResources( outputDirectory, flavour );
    }
}
