package org.apache.maven.model.converter.plugins;

/*
 * Copyright 2006 The Apache Software Foundation.
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

import junit.framework.Assert;
import org.apache.maven.model.converter.ProjectConverterException;

import java.io.IOException;

/**
 * @author Dennis Lundberg
 * @version $Id$
 */
public class PCCCompilerTest
    extends AbstractPCCTest
{
    protected void setUp()
        throws Exception
    {
        super.setUp();

        pluginConfigurationConverter = new PCCCompiler();
    }

    public void testBuildConfiguration()
    {
        try
        {
            projectProperties.load( getClassLoader().getResourceAsStream( "PCCCompilerTest.properties" ) );

            pluginConfigurationConverter.buildConfiguration( configuration, v3Model, projectProperties );

            String value = configuration.getChild( "debug" ).getValue();
            Assert.assertEquals( "check debug value", "true", value );

            value = configuration.getChild( "showDeprecation" ).getValue();
            Assert.assertEquals( "check deprecation value", "false", value );

            value = configuration.getChild( "encoding" ).getValue();
            Assert.assertEquals( "check encoding value", "UTF-8", value );

            value = configuration.getChild( "executable" ).getValue();
            Assert.assertEquals( "check executable value", "/usr/java/bin/javac-2", value );

            value = configuration.getChild( "fork" ).getValue();
            Assert.assertEquals( "check fork value", "true", value );

            value = configuration.getChild( "meminitial" ).getValue();
            Assert.assertEquals( "check meminitial value", "10m", value );

            value = configuration.getChild( "maxmem" ).getValue();
            Assert.assertEquals( "check maxmem value", "20m", value );

            value = configuration.getChild( "optimize" ).getValue();
            Assert.assertEquals( "check optimize value", "false", value );

            value = configuration.getChild( "showWarnings" ).getValue();
            Assert.assertEquals( "check showWarnings value", "false", value );

            value = configuration.getChild( "source" ).getValue();
            Assert.assertEquals( "check source value", "1.3", value );

            value = configuration.getChild( "target" ).getValue();
            Assert.assertEquals( "check target value", "1.1", value );

            value = configuration.getChild( "verbose" ).getValue();
            Assert.assertEquals( "check verbose value", "false", value );
        }
        catch ( ProjectConverterException e )
        {
            Assert.fail( e.getMessage() );
        }
        catch ( IOException e )
        {
            Assert.fail( "Unable to find the requested resource." );
        }
    }
}
