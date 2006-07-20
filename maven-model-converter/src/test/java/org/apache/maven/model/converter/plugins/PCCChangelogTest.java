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
public class PCCChangelogTest
    extends AbstractPCCTest
{
    protected void setUp()
        throws Exception
    {
        super.setUp();

        pluginConfigurationConverter = new PCCChangelog();
    }

    public void testBuildConfiguration1()
    {
        try
        {
            projectProperties.load( getClassLoader().getResourceAsStream( "PCCChangelogTest1.properties" ) );

            pluginConfigurationConverter.buildConfiguration( configuration, v3Model, projectProperties );

            String value = configuration.getChild( "commentFormat" ).getValue();
            Assert.assertEquals( "check commentFormat value", "%Sn - %c - Activity: %[activity]p", value );

            value = configuration.getChild( "dateFormat" ).getValue();
            Assert.assertEquals( "check dateFormat value", "yyyy-MM-dd", value );

            value = configuration.getChild( "outputEncoding" ).getValue();
            Assert.assertEquals( "check outputEncoding value", "ISO-8859-1", value );

            value = configuration.getChild( "tagBase" ).getValue();
            Assert.assertEquals( "check tagBase value", "http://svn.apache.org/repos/asf/maven/plugins/", value );

            value = configuration.getChild( "type" ).getValue();
            Assert.assertEquals( "check type value", "date", value );

            Xpp3Dom dates = configuration.getChild( "dates" );
            if ( dates.getChildCount() == 1 )
            {
                Xpp3Dom date = dates.getChild( 0 );
                Assert.assertEquals( "check dates/date value", "2005-01-01", date.getValue() );
            }
            else
            {
                Assert.fail( "Wrong number of date elements" );
            }
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
            projectProperties.load( getClassLoader().getResourceAsStream( "PCCChangelogTest2.properties" ) );

            pluginConfigurationConverter.buildConfiguration( configuration, v3Model, projectProperties );

            String value = configuration.getChild( "type" ).getValue();
            Assert.assertEquals( "check type value", "range", value );

            value = configuration.getChild( "range" ).getValue();
            Assert.assertEquals( "check range value", "120", value );
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
            projectProperties.load( getClassLoader().getResourceAsStream( "PCCChangelogTest3.properties" ) );

            pluginConfigurationConverter.buildConfiguration( configuration, v3Model, projectProperties );

            String value = configuration.getChild( "type" ).getValue();
            Assert.assertEquals( "check type value", "tag", value );

            Xpp3Dom tags = configuration.getChild( "tags" );
            if ( tags.getChildCount() == 1 )
            {
                Xpp3Dom tag = tags.getChild( 0 );
                Assert.assertEquals( "check tags/tag value", "RELEASE-1_0", tag.getValue() );
            }
            else
            {
                Assert.fail( "Wrong number of tag elements" );
            }
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
