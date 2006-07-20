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
public class PCCJarTest
    extends AbstractPCCTest
{
    protected void setUp()
        throws Exception
    {
        super.setUp();

        pluginConfigurationConverter = new PCCJar();
    }

    public void testBuildConfiguration()
    {
        String value;

        try
        {
            projectProperties.load( getClassLoader().getResourceAsStream( "PCCJarTest.properties" ) );

            pluginConfigurationConverter.buildConfiguration( configuration, v3Model, projectProperties );

            Xpp3Dom archive = configuration.getChild( "archive" );
            if ( archive.getChildCount() > 0 )
            {
                value = archive.getChild( "compress" ).getValue();
                Assert.assertEquals( "check compress value", "false", value );

                value = archive.getChild( "index" ).getValue();
                Assert.assertEquals( "check index value", "true", value );

                Xpp3Dom manifest = archive.getChild( "manifest" );
                if ( manifest.getChildCount() > 0 )
                {
                    value = manifest.getChild( "addClasspath" ).getValue();
                    Assert.assertEquals( "check addClasspath value", "true", value );

                    value = manifest.getChild( "addExtensions" ).getValue();
                    Assert.assertEquals( "check addExtensions value", "true", value );

                    value = manifest.getChild( "mainClass" ).getValue();
                    Assert.assertEquals( "check mainClass value", "MyClass", value );
                }

                Xpp3Dom manifestEntries = archive.getChild( "manifestEntries" );
                if ( manifestEntries.getChildCount() > 0 )
                {
                    value = manifestEntries.getChild( "Bar-Attribute" ).getValue();
                    Assert.assertEquals( "check Bar-Attribute value", "I like toast and jam", value );

                    value = manifestEntries.getChild( "Foo-Attribute" ).getValue();
                    Assert.assertEquals( "check Foo-Attribute value", "I like bread and butter", value );
                }

                value = archive.getChild( "manifestFile" ).getValue();
                Assert.assertEquals( "check manifestFile value", "manifest.mf", value );
            }

            value = configuration.getChild( "finalName" ).getValue();
            Assert.assertEquals( "check finalName value", "my.jar", value );
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
