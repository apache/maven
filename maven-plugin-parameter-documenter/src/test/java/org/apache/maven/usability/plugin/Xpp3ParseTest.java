package org.apache.maven.usability.plugin;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import junit.framework.TestCase;

import org.apache.maven.usability.plugin.io.xpp3.ParamdocXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Properties;

public class Xpp3ParseTest
    extends TestCase
{
    
    public void testParse() throws IOException, XmlPullParserException
    {
        InputStream testDocStream = getClass().getClassLoader().getResourceAsStream( "test.paramdoc.xml" );
        ParamdocXpp3Reader reader = new ParamdocXpp3Reader();
        
        ExpressionDocumentation documentation = reader.read(new InputStreamReader( testDocStream ) );
        
        Map exprs = documentation.getExpressionsBySyntax();
        
        Expression expr = (Expression) exprs.get( "localRepository" );
        
        assertNotNull( expr );
        
        Properties p = expr.getCliOptions();
        
        assertNotNull( p );
        
        assertEquals( 1, p.size() );
        
        assertEquals( "Override the local repository location on a per-build basis.", p.getProperty( "-Dmaven.repo.local=/path/to/local/repo" ) );
        
    }

}
