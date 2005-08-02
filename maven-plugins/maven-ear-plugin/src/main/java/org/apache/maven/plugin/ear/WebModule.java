package org.apache.maven.plugin.ear;

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

import java.util.Set;

/**
 * The {@link EarModule} implementation for a Web application module.
 *
 * @author <a href="stephane.nicoll@gmail.com">Stephane Nicoll</a>
 * @version $Id$
 */
public class WebModule
    extends AbstractEarModule
{
    protected static final String WEB_MODULE = "web";

    protected static final String WEB_URI_FIELD = "web-uri";

    protected static final String CONTEXT_ROOT_FIELD = "context-root";

    private String contextRoot;

    public WebModule()
    {
    }

    public WebModule( Artifact a )
    {
        super( a );
        this.contextRoot = getDefaultContextRoot( a );
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

    public void resolveArtifact( Set artifacts )
        throws EarPluginException
    {
        // Let's resolve the artifact
        super.resolveArtifact( artifacts );

        // Context root has not been customized - using default
        if ( contextRoot == null )
        {
            contextRoot = getDefaultContextRoot( getArtifact() );
        }
    }

    /**
     * Returns the context root to use for the web module.
     * <p/>
     * Note that this might return <tt>null</tt> till the
     * artifact has been resolved.
     *
     * @return the context root
     */
    public String getContextRoot()
    {
        return contextRoot;
    }

    protected String getType()
    {
        return "war";
    }

    /**
     * Generates a default context root for the given artifact, based
     * on the <tt>artifactId</tt>.
     *
     * @param a the artifact
     * @return a context root for the artifact
     */
    private static String getDefaultContextRoot( Artifact a )
    {
        if ( a == null )
        {
            throw new NullPointerException( "Artifact could not be null." );
        }
        return "/" + a.getArtifactId();
    }
}
