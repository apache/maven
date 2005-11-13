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
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.xml.XMLWriter;

import java.util.Set;

/**
 * The {@link EarModule} implementation for a J2EE client module.
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id$
 */
public class JavaModule
    extends AbstractEarModule
{
    protected static final String JAVA_MODULE = "java";

    private Boolean includeInApplicationXml = Boolean.FALSE;

    public JavaModule()
    {

    }

    public JavaModule( Artifact a, String defaultJavaBundleDir )
    {
        super( a );
        setJavaBundleDir( defaultJavaBundleDir );

    }

    public void appendModule( XMLWriter writer, String version )
    {
        // Generates an entry in the application.xml only if
        // includeInApplicationXml is set
        if ( includeInApplicationXml.booleanValue() )
        {
            writer.startElement( MODULE_ELEMENT );
            writer.startElement( JAVA_MODULE );
            writer.writeText( getUri() );
            writer.endElement();
            writer.endElement();
        }
    }

    public void resolveArtifact( Set artifacts, String defaultJavaBundleDir )
        throws MojoFailureException
    {
        // Let's resolve the artifact
        super.resolveArtifact( artifacts, defaultJavaBundleDir );

        // If the defaultJavaBundleDir is set and no bundle dir is
        // set, set the default as bundle dir
        setJavaBundleDir( defaultJavaBundleDir );
    }

    protected String getType()
    {
        return "jar";
    }

    private void setJavaBundleDir( String defaultJavaBundleDir )
    {
        if ( defaultJavaBundleDir != null && bundleDir == null )
        {
            this.bundleDir = defaultJavaBundleDir;
        }
    }
}
