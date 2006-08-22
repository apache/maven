package org.apache.maven.model.converter;

/*
 * Copyright 2005-2005 The Apache Software Foundation.
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

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Test rewriter.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class V3PomRewriterTest
    extends PlexusTestCase
{
    private V3PomRewriter rewriter;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        rewriter = (V3PomRewriter) lookup( V3PomRewriter.ROLE );
    }

    public void testCurrentVersionExpressionConversion()
        throws Exception
    {
        String pom =
            "<project><dependencies><dependency><groupId>g</groupId><artifactId>a</artifactId><version>${pom.currentVersion}</version></dependency></dependencies></project>";

        Writer to = new StringWriter();
        rewriter.rewrite( new StringReader( pom ), to, false, null, null, null, null );

        Xpp3Dom dom = Xpp3DomBuilder.build( new StringReader( to.toString() ) );
        String version = dom.getChild( "dependencies" ).getChild( 0 ).getChild( "version" ).getValue();
        assertEquals( "check new version expression", "${project.version}", version );
    }

}
