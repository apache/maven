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
public class PCCWarTest
    extends AbstractPCCTest
{
    protected void setUp()
        throws Exception
    {
        super.setUp();

        pluginConfigurationConverter = new PCCWar();
    }

    public void testBuildConfiguration1()
    {
        try
        {
            projectProperties.load( getClassLoader().getResourceAsStream( "PCCWarTest1.properties" ) );

            pluginConfigurationConverter.buildConfiguration( configuration, v3Model, projectProperties );

            String value = configuration.getChild( "warSourceDirectory" ).getValue();
            Assert.assertEquals( "check warSourceDirectory value", "myWebappDirectory", value );
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
            projectProperties.load( getClassLoader().getResourceAsStream( "PCCWarTest2.properties" ) );

            pluginConfigurationConverter.buildConfiguration( configuration, v3Model, projectProperties );

            String value = configuration.getChild( "warSourceDirectory" ).getValue();
            Assert.assertEquals( "check warSourceDirectory value", "myWebappDirectory", value );
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
            projectProperties.load( getClassLoader().getResourceAsStream( "PCCWarTest3.properties" ) );

            pluginConfigurationConverter.buildConfiguration( configuration, v3Model, projectProperties );

            Xpp3Dom child = configuration.getChild( "warSourceDirectory" );
            Assert.assertEquals( "check warSourceDirectory element", null, child );
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
