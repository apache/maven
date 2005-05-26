package org.apache.maven.reporting.sink;

import java.io.File;
import java.io.InputStream;

import org.codehaus.doxia.sink.Sink;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.StringInputStream;

/*
 * Copyright 2005 The Apache Software Foundation.
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
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id: MavenReport.java 163376 2005-02-23 00:06:06Z brett $
 */
public class SinkFactory
{
    private String outputDirectory;

    private String siteDirectory;

    private SiteRenderer siteRenderer;

    private InputStream siteDescriptor;

    private String flavour;

    public void setOutputDirectory( String outputDirectory )
    {
        this.outputDirectory = outputDirectory;
    }

    public void setSiteRenderer( SiteRenderer siteRenderer )
    {
        this.siteRenderer = siteRenderer;
    }

    public void setSiteDirectory( String siteDirectory )
    {
        this.siteDirectory = siteDirectory;
    }

    public void setFlavour( String flavour )
    {
        this.flavour = flavour;
    }

    public void setSiteDescriptor( InputStream siteDescriptor )
    {
        this.siteDescriptor = siteDescriptor;
    }

    public Sink getSink( String outputFileName )
        throws Exception
    {
        InputStream descriptor = siteDescriptor;
        if ( descriptor == null )
        {
            descriptor = new StringInputStream( "" );
        }

        return siteRenderer.createSink( new File( siteDirectory ), outputFileName, outputDirectory,
                                        descriptor, flavour );
    }
}
