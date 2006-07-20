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
public class PCCCheckstyleTest
    extends AbstractPCCTest
{
    protected void setUp()
        throws Exception
    {
        super.setUp();

        pluginConfigurationConverter = new PCCCheckstyle();
    }

    public void testBuildConfiguration1()
    {
        try
        {
            projectProperties.load( getClassLoader().getResourceAsStream( "PCCCheckstyleTest1.properties" ) );

            pluginConfigurationConverter.buildConfiguration( configuration, v3Model, projectProperties );

            String value = configuration.getChild( "cacheFile" ).getValue();
            Assert.assertEquals( "check cacheFile value", "target/checkstyle/myCachefile", value );

            value = configuration.getChild( "configLocation" ).getValue();
            Assert.assertEquals( "check configLocation value", "config/sun_checks.xml", value );

            value = configuration.getChild( "excludes" ).getValue();
            Assert.assertEquals( "check excludes value", "**/*.html", value );

            value = configuration.getChild( "failsOnError" ).getValue();
            Assert.assertEquals( "check failsOnError value", "true", value );

            value = configuration.getChild( "headerLocation" ).getValue();
            Assert.assertEquals( "check headerLocation value", "src/main/resources/HEADER.txt", value );

            value = configuration.getChild( "includes" ).getValue();
            Assert.assertEquals( "check includes value", "**/*.java", value );

            value = configuration.getChild( "outputFile" ).getValue();
            Assert.assertEquals( "check outputFile value", "target/checkstyle/checkstyle-raw-report.txt", value );

            value = configuration.getChild( "outputFileFormat" ).getValue();
            Assert.assertEquals( "check outputFileFormat value", "plain", value );

            value = configuration.getChild( "suppressionsLocation" ).getValue();
            Assert.assertEquals( "check suppressionsLocation value", "src/main/resources/mySuppressions.xml", value );

            value = configuration.getChild( "useFile" ).getValue();
            Assert.assertEquals( "check useFile value", "true", value );
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

    public void testBuildConfiguration2()
    {
        try
        {
            projectProperties.load( getClassLoader().getResourceAsStream( "PCCCheckstyleTest2.properties" ) );

            pluginConfigurationConverter.buildConfiguration( configuration, v3Model, projectProperties );

            String value = configuration.getChild( "configLocation" ).getValue();
            Assert.assertEquals( "check configLocation value",
                                 "http://svn.apache.org/repos/asf/maven/plugins/trunk/maven-checkstyle-plugin/src/main/resources/config/avalon_checks.xml",
                                 value );

            value = configuration.getChild( "outputFile" ).getValue();
            Assert.assertEquals( "check outputFile value", "target/checkstyle/checkstyle-raw-report.xml", value );

            value = configuration.getChild( "outputFileFormat" ).getValue();
            Assert.assertEquals( "check outputFileFormat value", "xml", value );
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

    public void testBuildConfiguration3()
    {
        try
        {
            projectProperties.load( getClassLoader().getResourceAsStream( "PCCCheckstyleTest3.properties" ) );

            pluginConfigurationConverter.buildConfiguration( configuration, v3Model, projectProperties );

            String value = configuration.getChild( "configLocation" ).getValue();
            Assert.assertEquals( "check configLocation value", "checkstyle.xml", value );
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
