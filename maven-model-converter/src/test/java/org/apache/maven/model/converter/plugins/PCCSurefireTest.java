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
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.IOException;

/**
 * @author Dennis Lundberg
 * @version $Id$
 */
public class PCCSurefireTest
    extends AbstractPCCTest
{
    protected void setUp()
        throws Exception
    {
        super.setUp();

        pluginConfigurationConverter = new PCCSurefire();
    }

    public void testBuildConfiguration()
    {
        try
        {
            projectProperties.load( getClassLoader().getResourceAsStream( "PCCSureFireTest1.properties" ) );

            pluginConfigurationConverter.buildConfiguration( configuration, v3Model, projectProperties );

            String value = configuration.getChild( "reportFormat" ).getValue();
            Assert.assertEquals( "check reportFormat value", "xml", value );

            value = configuration.getChild( "jvm" ).getValue();
            Assert.assertEquals( "check jvm value", "java", value );

            value = configuration.getChild( "argLine" ).getValue();
            Assert.assertEquals( "check argLine value", "-Xmx160m -verbose", value );

            value = configuration.getChild( "printSummary" ).getValue();
            Assert.assertEquals( "check printSummary value", "false", value );

            Xpp3Dom systemProperties = configuration.getChild( "systemProperties" );
            if ( systemProperties.getChildCount() == 2 )
            {
                Xpp3Dom systemPropertyOne = systemProperties.getChild( 0 );
                Assert.assertEquals( "check systemProperties/prop1 name", "prop1", systemPropertyOne.getName() );
                Assert.assertEquals( "check systemProperties/prop1 value", "your value", systemPropertyOne.getValue() );

                Xpp3Dom systemPropertyTwo = systemProperties.getChild( 1 );
                Assert.assertEquals( "check systemProperties/prop2 name", "basedir", systemPropertyTwo.getName() );
                Assert.assertEquals( "check systemProperties/prop2 value", "${basedir}", systemPropertyTwo.getValue() );
            }
            else
            {
                Assert.fail( "Wrong number of system properties" );
            }

            value = configuration.getChild( "useFile" ).getValue();
            Assert.assertEquals( "check useFile value", "false", value );

            value = configuration.getChild( "testFailureIgnore" ).getValue();
            Assert.assertEquals( "check testFailureIgnore value", "true", value );

            value = configuration.getChild( "skip" ).getValue();
            Assert.assertEquals( "check skip value", "true", value );
        }
        catch ( ProjectConverterException e )
        {
            Assert.fail();
        }
        catch ( IOException e )
        {
            Assert.fail( "Unable to find the requested resource." );
        }
    }

    public void testBuildConfigurationFork1()
    {
        try
        {
            projectProperties.load( getClassLoader().getResourceAsStream( "PCCSureFireTest1.properties" ) );

            pluginConfigurationConverter.buildConfiguration( configuration, v3Model, projectProperties );

            String value = configuration.getChild( "forkMode" ).getValue();
            Assert.assertEquals( "check forkMode value", "once", value );
        }
        catch ( ProjectConverterException e )
        {
            Assert.fail();
        }
        catch ( IOException e )
        {
            Assert.fail( "Unable to find the requested resource." );
        }
    }

    public void testBuildConfigurationFork2()
    {
        try
        {
            projectProperties.load( getClassLoader().getResourceAsStream( "PCCSureFireTest2.properties" ) );

            pluginConfigurationConverter.buildConfiguration( configuration, v3Model, projectProperties );

            String value = configuration.getChild( "forkMode" ).getValue();
            Assert.assertEquals( "check forkMode value", "once", value );
        }
        catch ( ProjectConverterException e )
        {
            Assert.fail();
        }
        catch ( IOException e )
        {
            Assert.fail( "Unable to find the requested resource." );
        }
    }

    public void testBuildConfigurationFork3()
    {
        try
        {
            projectProperties.load( getClassLoader().getResourceAsStream( "PCCSureFireTest3.properties" ) );

            pluginConfigurationConverter.buildConfiguration( configuration, v3Model, projectProperties );

            String value = configuration.getChild( "forkMode" ).getValue();
            Assert.assertEquals( "check forkMode value", "perTest", value );
        }
        catch ( ProjectConverterException e )
        {
            Assert.fail();
        }
        catch ( IOException e )
        {
            Assert.fail( "Unable to find the requested resource." );
        }
    }
}
