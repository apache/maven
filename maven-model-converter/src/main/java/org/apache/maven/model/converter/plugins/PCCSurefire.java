package org.apache.maven.model.converter.plugins;

/*
 * Copyright 2001-2006 The Apache Software Foundation.
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

import org.apache.maven.model.converter.ProjectConverterException;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * @plexus.component role="org.apache.maven.model.converter.plugins.PluginConfigurationConverter" role-hint="surefire"
 *
 * @author Fabrizio Giustina
 * @author Dennis Lundberg
 * @version $Id$
 */
public class PCCSurefire
    extends AbstractPluginConfigurationConverter
{
    /**
     * @see org.apache.maven.model.converter.plugins.AbstractPluginConfigurationConverter#getArtifactId()
     */
    public String getArtifactId()
    {
        return "maven-surefire-plugin";
    }

    public String getType()
    {
        return TYPE_BUILD_PLUGIN;
    }

    protected void buildConfiguration( Xpp3Dom configuration, org.apache.maven.model.v3_0_0.Model v3Model,
                                       Properties projectProperties )
        throws ProjectConverterException
    {
        addConfigurationChild( configuration, projectProperties, "maven.junit.jvmargs", "argLine" );

        String forkMode = projectProperties.getProperty( "maven.junit.forkmode" );
        if ( forkMode == null )
        {
            String fork = projectProperties.getProperty( "maven.junit.fork" );
            if ( fork != null )
            {
                boolean useFork = Boolean.valueOf( PropertyUtils.convertYesNoToBoolean( fork ) ).booleanValue();
                if ( useFork )
                {
                    // Use "once" here as that is the default forkMode
                    addConfigurationChild( configuration, "forkMode", "once" );
                }
            }
        }
        else
        {
            addConfigurationChild( configuration, projectProperties, "maven.junit.forkmode", "forkMode" );
        }

        addConfigurationChild( configuration, projectProperties, "maven.junit.jvm", "jvm" );

        addConfigurationChild( configuration, projectProperties, "maven.junit.printSummary", "printSummary" );

        addConfigurationChild( configuration, projectProperties, "maven.junit.format", "reportFormat" );

        addConfigurationChild( configuration, projectProperties, "maven.test.skip", "skip" );

        String sysproperties = projectProperties.getProperty( "maven.junit.sysproperties" );
        if ( sysproperties != null )
        {
            StringTokenizer tokenizer = new StringTokenizer( sysproperties );
            if ( tokenizer.hasMoreTokens() )
            {
                Xpp3Dom systemProperties = new Xpp3Dom( "systemProperties" );
                while ( tokenizer.hasMoreTokens() )
                {
                    String name = tokenizer.nextToken();
                    String value = projectProperties.getProperty( name );
                    addConfigurationChild( systemProperties, name, value );
                }
                if ( systemProperties.getChildCount() > 0 )
                {
                    configuration.addChild( systemProperties );
                }
            }
        }

        addConfigurationChild( configuration, projectProperties, "maven.test.failure.ignore", "testFailureIgnore" );

        addConfigurationChild( configuration, projectProperties, "maven.junit.usefile", "useFile" );

        if ( v3Model.getBuild() != null && v3Model.getBuild().getUnitTest() != null )
        {
            org.apache.maven.model.v3_0_0.UnitTest v3UnitTest = v3Model.getBuild().getUnitTest();

            List excludes = v3UnitTest.getExcludes();
            if ( excludes != null && excludes.size() > 0 )
            {
                Xpp3Dom excludesConf = new Xpp3Dom( "excludes" );
                for ( Iterator iter = excludes.iterator(); iter.hasNext(); )
                {
                    addConfigurationChild( excludesConf, "exclude", (String) iter.next() );
                }
                configuration.addChild( excludesConf );
            }

            List includes = v3UnitTest.getIncludes();
            if ( includes != null && includes.size() > 0 )
            {
                Xpp3Dom includesConf = new Xpp3Dom( "includes" );
                for ( Iterator iter = includes.iterator(); iter.hasNext(); )
                {
                    addConfigurationChild( includesConf, "include", (String) iter.next() );
                }
                configuration.addChild( includesConf );
            }
        }
    }
}
