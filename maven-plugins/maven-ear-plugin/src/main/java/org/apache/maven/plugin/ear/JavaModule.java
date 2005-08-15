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

/**
 * The {@link EarModule} implementation for a J2EE client module.
 *
 * @author <a href="stephane.nicoll@gmail.com">Stephane Nicoll</a>
 * @version $Id$
 */
public class JavaModule
    extends AbstractEarModule
{
    protected static final String JAVA_MODULE = "java";

    private Boolean library = Boolean.FALSE;

    public JavaModule()
    {
    }

    public JavaModule( Artifact a )
    {
        super( a );
    }

    public void appendModule( XMLWriter writer, String version )
    {
        // Generates an entry in the application.xml only if this
        // module is not a library
        if (!isLibrary()) {
            writer.startElement( MODULE_ELEMENT );
            writer.startElement( JAVA_MODULE );
            writer.writeText( getUri() );
            writer.endElement();
            writer.endElement();
        }
    }

    protected String getType()
    {
        return "jar";
    }

    /**
     * Specify whether this Java module is a third party library or not.
     * <p/>
     * If <tt>true</tt>, the module will not be included in the generated
     * <tt>application.xml</tt>.
     *
     * @return true if the module is a third party library, false otherwise
     */
    public boolean isLibrary()
    {
        return library.booleanValue();
    }
}
