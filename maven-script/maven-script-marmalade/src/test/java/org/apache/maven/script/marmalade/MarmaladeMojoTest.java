package org.apache.maven.script.marmalade;

/*
 * Copyright 2001-2004 The Apache Software Foundation.
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

import org.apache.maven.plugin.PluginExecutionRequest;
import org.apache.maven.plugin.PluginExecutionResponse;
import org.codehaus.marmalade.metamodel.ScriptBuilder;
import org.codehaus.marmalade.model.MarmaladeScript;
import org.codehaus.marmalade.parsing.DefaultParsingContext;
import org.codehaus.marmalade.parsing.MarmaladeParsingContext;
import org.codehaus.marmalade.parsing.ScriptParser;

import java.io.StringReader;
import java.util.Collections;

import junit.framework.TestCase;

/**
 * @author jdcasey
 */
public class MarmaladeMojoTest
    extends TestCase
{

    private static final String TEST_SCRIPT = "<set xmlns=\"marmalade:core\" var=\"testvar\" value=\"${param}/testval\" extern=\"true\"/>";

    public void testShouldProduceOutputWithRequest_Dot_ToStringInline() throws Exception
    {
        MarmaladeParsingContext parseContext = new DefaultParsingContext();
        parseContext.setInput( new StringReader( TEST_SCRIPT ) );
        parseContext.setInputLocation( "<embedded test script>" );

        ScriptBuilder builder = new ScriptParser().parse( parseContext );

        MarmaladeScript script = builder.build();

        MarmaladeMojo mojo = new MarmaladeMojo( script );

        PluginExecutionRequest request = new PluginExecutionRequest( Collections.EMPTY_MAP );
        request.setParameters( Collections.singletonMap( "param", "paramValue" ) );

        PluginExecutionResponse response = new PluginExecutionResponse();

        mojo.execute( request, response );

        Object result = request.getContextValue( "testvar" );

        assertEquals( "paramValue/testval", result );
    }

}