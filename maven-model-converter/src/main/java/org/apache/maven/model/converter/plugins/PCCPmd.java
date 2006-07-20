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

import org.apache.maven.model.converter.ProjectConverterException;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.Properties;
import java.util.StringTokenizer;

/**
 * A <code>PluginConfigurationConverter</code> for the maven-pmd-plugin.
 *
 * @plexus.component role="org.apache.maven.model.converter.plugins.PluginConfigurationConverter" role-hint="pmd"
 *
 * @author Dennis Lundberg
 * @version $Id$
 */
public class PCCPmd
    extends AbstractPluginConfigurationConverter
{
    /**
     * @see AbstractPluginConfigurationConverter#getArtifactId()
     */
    public String getArtifactId()
    {
        return "maven-pmd-plugin";
    }

    public String getType()
    {
        return TYPE_REPORT_PLUGIN;
    }

    protected void buildConfiguration( Xpp3Dom configuration, org.apache.maven.model.v3_0_0.Model v3Model,
                                       Properties projectProperties )
        throws ProjectConverterException
    {
        addConfigurationChild( configuration, projectProperties, "maven.pmd.excludes", "excludes" );

        addConfigurationChild( configuration, projectProperties, "maven.pmd.failonruleviolation", "failOnViolation" );

        addConfigurationChild( configuration, projectProperties, "maven.pmd.cpd.minimumtokencount", "minimumTokens" );

        String rulesetfiles = projectProperties.getProperty( "maven.pmd.rulesetfiles" );
        if ( rulesetfiles != null )
        {
            StringTokenizer tokenizer = new StringTokenizer( rulesetfiles, "," );
            if ( tokenizer.hasMoreTokens() )
            {
                Xpp3Dom rulesets = new Xpp3Dom( "rulesets" );
                while ( tokenizer.hasMoreTokens() )
                {
                    addConfigurationChild( rulesets, "ruleset", translate( tokenizer.nextToken() ) );
                }
                if ( rulesets.getChildCount() > 0 )
                {
                    configuration.addChild( rulesets );
                }
            }
        }

        addConfigurationChild( configuration, projectProperties, "maven.pmd.targetjdk", "targetJdk" );
    }

    /**
     * In the Maven 1 plugin the built-in rulesets where accessed by prefixing
     * them with "rulesets/", but in the Maven 2 plugin the prefix "/rulesets/"
     * is used.
     *
     * @param mavenOneRuleset A ruleset from the Maven 1 configuration
     * @return A ruleset suitable for the Maven 2 configuration
     */
    private String translate( String mavenOneRuleset )
    {
        if ( mavenOneRuleset != null && mavenOneRuleset.startsWith( "rulesets/" ) )
        {
            return "/" + mavenOneRuleset;
        }
        else
        {
            return mavenOneRuleset;
        }
    }
}
