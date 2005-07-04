package org.apache.maven.plugin.ear.module;

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

import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * The {@link EarModule} implementation for an Web application module.
 *
 * @author Stephane Nicoll <stephane.nicoll@gmail.com>
 * @author $Author: sni $ (last edit)
 * @version $Revision: 1.2 $
 */
public class WebModule
    extends AbstractEarModule
{
    protected static final String WEB_MODULE = "web";

    protected static final String WEB_URI_FIELD = "web-uri";

    protected static final String CONTEXT_ROOT_FIELD = "context-root";

    private final String contextRoot;

    WebModule( String uri, Artifact a, String contextRoot )
    {
        super( uri, a );
        this.contextRoot = contextRoot;
    }

    public void appendModule( XMLWriter writer, String version )
    {
        writer.startElement( MODULE_ELEMENT );
        writer.startElement( WEB_MODULE );
        writer.startElement( WEB_URI_FIELD );
        writer.writeText( getUri() );
        writer.endElement(); // web-uri
        writer.startElement( CONTEXT_ROOT_FIELD );
        writer.writeText( getContextRoot() );
        writer.endElement(); // context-root
        writer.endElement(); // web
        writer.endElement(); // module
    }

    public String getContextRoot()
    {
        return contextRoot;
    }
}
