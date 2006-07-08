package org.apache.maven.reporting.sink;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.RendererException;

import java.io.File;
import java.io.IOException;

/**
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 */
public class SinkFactory
{
    private String siteDirectory;

    private Renderer siteRenderer;

    public void setSiteRenderer( Renderer siteRenderer )
    {
        this.siteRenderer = siteRenderer;
    }

    public void setSiteDirectory( String siteDirectory )
    {
        this.siteDirectory = siteDirectory;
    }

    public Sink getSink( String outputFileName )
        throws RendererException, IOException
    {
        return siteRenderer.createSink( new File( siteDirectory ), outputFileName );
    }
}
