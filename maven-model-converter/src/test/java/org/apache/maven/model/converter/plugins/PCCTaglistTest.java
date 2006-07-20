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
public class PCCTaglistTest
    extends AbstractPCCTest
{
    protected void setUp()
        throws Exception
    {
        super.setUp();

        pluginConfigurationConverter = new PCCTaglist();
    }

    public void testBuildConfiguration()
    {
        try
        {
            projectProperties.load( getClassLoader().getResourceAsStream( "PCCTaglistTest.properties" ) );

            pluginConfigurationConverter.buildConfiguration( configuration, v3Model, projectProperties );

            String value = configuration.getChild( "tags" ).getValue();
            Assert.assertEquals( "check tags value", "@fixme", value );
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
