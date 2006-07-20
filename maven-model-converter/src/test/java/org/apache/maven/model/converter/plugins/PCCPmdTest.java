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
public class PCCPmdTest
    extends AbstractPCCTest
{
    protected void setUp()
        throws Exception
    {
        super.setUp();

        pluginConfigurationConverter = new PCCPmd();
    }

    public void testBuildConfiguration()
    {
        try
        {
            projectProperties.load( getClassLoader().getResourceAsStream( "PCCPmdTest.properties" ) );

            pluginConfigurationConverter.buildConfiguration( configuration, v3Model, projectProperties );

            String value = configuration.getChild( "excludes" ).getValue();
            Assert.assertEquals( "check excludes value", "**/*PropertyListParser*", value );

            value = configuration.getChild( "failOnViolation" ).getValue();
            Assert.assertEquals( "check failOnViolation value", "true", value );

            value = configuration.getChild( "minimumTokens" ).getValue();
            Assert.assertEquals( "check minimumTokens value", "50", value );

            Xpp3Dom rulesets = configuration.getChild( "rulesets" );
            if ( rulesets.getChildCount() == 3 )
            {
                Xpp3Dom rulesetOne = rulesets.getChild( 0 );
                Assert.assertEquals( "check rulesets/ruleset value", "fileupload_basic.xml", rulesetOne.getValue() );

                Xpp3Dom rulesetTwo = rulesets.getChild( 1 );
                Assert.assertEquals( "check rulesets/ruleset value", "/rulesets/unusedcode.xml",
                                     rulesetTwo.getValue() );

                Xpp3Dom rulesetThree = rulesets.getChild( 2 );
                Assert.assertEquals( "check rulesets/ruleset value", "/rulesets/imports.xml", rulesetThree.getValue() );
            }
            else
            {
                Assert.fail( "Wrong number of ruleset elements" );
            }

            value = configuration.getChild( "targetJdk" ).getValue();
            Assert.assertEquals( "check targetJdk value", "1.4", value );
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
