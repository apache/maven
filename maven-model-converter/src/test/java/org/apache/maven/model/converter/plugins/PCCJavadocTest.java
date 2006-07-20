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
public class PCCJavadocTest
    extends AbstractPCCTest
{
    protected void setUp()
        throws Exception
    {
        super.setUp();

        pluginConfigurationConverter = new PCCJavadoc();
    }

    public void testBuildConfiguration()
    {
        try
        {
            projectProperties.load( getClassLoader().getResourceAsStream( "PCCJavadocTest1.properties" ) );

            pluginConfigurationConverter.buildConfiguration( configuration, v3Model, projectProperties );

            String value = configuration.getChild( "additionalparam" ).getValue();
            Assert.assertEquals( "check additionalparam value", "-J-showversion", value );

            value = configuration.getChild( "author" ).getValue();
            Assert.assertEquals( "check author value", "false", value );

            value = configuration.getChild( "bottom" ).getValue();
            Assert.assertEquals( "check bottom value", "Copyright", value );

            value = configuration.getChild( "destDir" ).getValue();
            Assert.assertEquals( "check destDir value", "apidocs", value );

            value = configuration.getChild( "docencoding" ).getValue();
            Assert.assertEquals( "check docencoding value", "UTF-8", value );

            value = configuration.getChild( "doclet" ).getValue();
            Assert.assertEquals( "check doclet value", "org.apache.MyDoclet", value );

            value = configuration.getChild( "docletPath" ).getValue();
            Assert.assertEquals( "check docletPath value", "/path/to/doclet", value );

            value = configuration.getChild( "doctitle" ).getValue();
            Assert.assertEquals( "check doctitle value", "The title", value );

            value = configuration.getChild( "encoding" ).getValue();
            Assert.assertEquals( "check encoding value", "ISO-8859-1", value );

            value = configuration.getChild( "excludePackageNames" ).getValue();
            Assert.assertEquals( "check excludePackageNames value", "org.apache.internal,org.apache.test", value );

            value = configuration.getChild( "footer" ).getValue();
            Assert.assertEquals( "check footer value", "The footer", value );

            value = configuration.getChild( "header" ).getValue();
            Assert.assertEquals( "check header value", "The header", value );

            value = configuration.getChild( "isOffline" ).getValue();
            Assert.assertEquals( "check isOffline value", "false", value );

            value = configuration.getChild( "links" ).getValue();
            Assert.assertEquals( "check links value", "http://java.sun.com/j2se/1.4/docs/api/", value );

            value = configuration.getChild( "locale" ).getValue();
            Assert.assertEquals( "check locale value", "en_US", value );

            value = configuration.getChild( "maxmemory" ).getValue();
            Assert.assertEquals( "check maxmemory value", "1024m", value );

            value = configuration.getChild( "offlineLinks" ).getValue();
            Assert.assertEquals( "check offlineLinks value", "/opt/java-apidoc/j2sdk1.4.2/docs/api/", value );

            value = configuration.getChild( "overview" ).getValue();
            Assert.assertEquals( "check overview value", "src/main/java/org/apache/overview.html", value );

            value = configuration.getChild( "source" ).getValue();
            Assert.assertEquals( "check source value", "1.3", value );

            value = configuration.getChild( "stylesheetfile" ).getValue();
            Assert.assertEquals( "check stylesheetfile value", "myStylesheet.css", value );

            value = configuration.getChild( "subpackages" ).getValue();
            Assert.assertEquals( "check subpackages value", "org.apache.maven", value );

            value = configuration.getChild( "taglet" ).getValue();
            Assert.assertEquals( "check taglet value", "org.apache.MyTaglet", value );

            value = configuration.getChild( "tagletpath" ).getValue();
            Assert.assertEquals( "check tagletpath value", "/path/to/taglet", value );

            Xpp3Dom tags = configuration.getChild( "tags" );
            if ( tags.getChildCount() == 2 )
            {
                Xpp3Dom tagOne = tags.getChild( 0 );

                value = tagOne.getChild( "head" ).getValue();
                Assert.assertEquals( "check tags/tag/head value", "To Do:", value );

                value = tagOne.getChild( "name" ).getValue();
                Assert.assertEquals( "check tags/tag/name value", "todo", value );

                value = tagOne.getChild( "placement" ).getValue();
                Assert.assertEquals( "check tags/tag/placement value", "a", value );

                Xpp3Dom tagTwo = tags.getChild( 1 );

                value = tagTwo.getChild( "head" ).getValue();
                Assert.assertEquals( "check tags/tag/head value", "Task:", value );

                value = tagTwo.getChild( "name" ).getValue();
                Assert.assertEquals( "check tags/tag/name value", "task", value );

                value = tagTwo.getChild( "placement" ).getValue();
                Assert.assertEquals( "check tags/tag/placement value", "Xa", value );
            }
            else
            {
                Assert.fail( "Wrong number of tag elements" );
            }

            value = configuration.getChild( "use" ).getValue();
            Assert.assertEquals( "check use value", "true", value );

            value = configuration.getChild( "version" ).getValue();
            Assert.assertEquals( "check version value", "true", value );

            value = configuration.getChild( "windowtitle" ).getValue();
            Assert.assertEquals( "check windowtitle value", "The title", value );
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

    public void testBuildConfigurationShow1()
    {
        try
        {
            projectProperties.load( getClassLoader().getResourceAsStream( "PCCJavadocTest1.properties" ) );

            pluginConfigurationConverter.buildConfiguration( configuration, v3Model, projectProperties );

            String value = configuration.getChild( "show" ).getValue();
            Assert.assertEquals( "check show value", "package", value );
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

    public void testBuildConfigurationShow2()
    {
        try
        {
            projectProperties.load( getClassLoader().getResourceAsStream( "PCCJavadocTest2.properties" ) );

            pluginConfigurationConverter.buildConfiguration( configuration, v3Model, projectProperties );

            String value = configuration.getChild( "show" ).getValue();
            Assert.assertEquals( "check show value", "private", value );
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

    public void testBuildConfigurationShow3()
    {
        try
        {
            projectProperties.load( getClassLoader().getResourceAsStream( "PCCJavadocTest3.properties" ) );

            pluginConfigurationConverter.buildConfiguration( configuration, v3Model, projectProperties );

            String value = configuration.getChild( "show" ).getValue();
            Assert.assertEquals( "check show value", "public", value );
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
