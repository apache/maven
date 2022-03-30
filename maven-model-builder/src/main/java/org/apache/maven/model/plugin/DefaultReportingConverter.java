package org.apache.maven.model.plugin;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.maven.api.xml.Dom;
import org.apache.maven.internal.xml.Xpp3Dom;
import org.apache.maven.api.model.Build;
import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.InputSource;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.model.Plugin;
import org.apache.maven.api.model.PluginManagement;
import org.apache.maven.api.model.ReportPlugin;
import org.apache.maven.api.model.ReportSet;
import org.apache.maven.api.model.Reporting;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.model.building.ModelProblem.Version;

/**
 * Handles conversion of the <code>&lt;reporting&gt;</code> section into the configuration of Maven Site Plugin 3.x,
 * i.e. <code>reportPlugins</code> and <code>outputDirectory</code> parameters.
 *
 * @author Benjamin Bentmann
 */
@Named
@Singleton
public class DefaultReportingConverter
    implements ReportingConverter
{
    private final InputLocation location;
    {
        String modelId = "org.apache.maven:maven-model-builder:"
            + this.getClass().getPackage().getImplementationVersion() + ":reporting-converter";
        InputSource inputSource = new InputSource( modelId, null );
        location = new InputLocation( -1, -1, inputSource );
    }

    @Override
    public Model convertReporting( Model model, ModelBuildingRequest request, ModelProblemCollector problems )
    {
        Reporting reporting = model.getReporting();

        if ( reporting == null )
        {
            return model;
        }

        Model.Builder builder = Model.newBuilder( model );

        Build build = model.getBuild();

        if ( build == null )
        {
            build = Build.newInstance();
            builder.location( "build", location );
        }

        Plugin sitePlugin = findSitePlugin( build );
        if ( sitePlugin == null )
        {
            sitePlugin = Plugin.newBuilder()
                    .artifactId( "maven-site-plugin" )
                    .location( "artifactId", location )
                    .build();
        }

        Dom configuration = sitePlugin.getConfiguration();
        if ( configuration == null )
        {
            configuration = newDom( "", "", location );
        }

        List<Dom> configChildren = new ArrayList<>( configuration.getChildren() );

        if ( configuration.getChild( "reportPlugins" ) != null )
        {
            // new-style report configuration already present: warn since this new style has been deprecated
            // in favor of classical reporting section MSITE-647 / MSITE-684
            problems.add( new ModelProblemCollectorRequest( Severity.WARNING, Version.BASE )
                    .setMessage( "Reporting configuration should be done in <reporting> section, "
                          + "not in maven-site-plugin <configuration> as reportPlugins parameter." )
                    .setLocation( sitePlugin.getLocation( "configuration" ) ) );
            return model;
        }

        if ( configuration.getChild( "outputDirectory" ) == null )
        {
            configChildren.add( newDom( "outputDirectory", reporting.getOutputDirectory(),
                    reporting.getLocation( "outputDirectory" ) ) );
        }

        List<Dom> reportPlugins = new ArrayList<>();

        boolean hasMavenProjectInfoReportsPlugin = false;

        /* waiting for MSITE-484 before deprecating <reporting> section
        if ( !reporting.getPlugins().isEmpty()
            && request.getValidationLevel() >= ModelBuildingRequest.VALIDATION_LEVEL_MAVEN_3_1 )
        {

            problems.add( new ModelProblemCollectorRequest( Severity.WARNING, Version.V31 )
                    .setMessage( "The <reporting> section is deprecated, please move the reports to the <configuration>"
                                 + " section of the new Maven Site Plugin." )
                    .setLocation( reporting.getLocation( "" ) ) );
        }*/

        for ( ReportPlugin plugin : reporting.getPlugins() )
        {
            Dom reportPlugin = convert( plugin );
            reportPlugins.add( reportPlugin );

            if ( !reporting.isExcludeDefaults() && !hasMavenProjectInfoReportsPlugin
                && "org.apache.maven.plugins".equals( plugin.getGroupId() )
                && "maven-project-info-reports-plugin".equals( plugin.getArtifactId() ) )
            {
                hasMavenProjectInfoReportsPlugin = true;
            }
        }

        if ( !reporting.isExcludeDefaults() && !hasMavenProjectInfoReportsPlugin )
        {
            Xpp3Dom dom = newDom( "reportPlugin",
                    Arrays.asList(
                            newDom( "groupId", "org.apache.maven.plugins", null ),
                            newDom( "artifactId", "maven-project-info-reports-plugin", null )
                    ),
                    location );

            reportPlugins.add( dom );
        }

        configChildren.add( newDom( "reportPlugins", reportPlugins, location ) );
        configuration = newDom( "configuration", configChildren, location );

        sitePlugin = sitePlugin.withConfiguration( configuration );

        PluginManagement pluginManagement = build.getPluginManagement();
        if ( pluginManagement == null )
        {
            pluginManagement = PluginManagement.newBuilder()
                    .plugins( Collections.singletonList( sitePlugin ) )
                    .build();
        }
        else
        {
            List<Plugin> plugins = new ArrayList<>( pluginManagement.getPlugins() );
            plugins.add( sitePlugin );
            pluginManagement = pluginManagement.withPlugins( plugins );
        }
        build = build.withPluginManagement( pluginManagement );

        return builder.build( build ).build();
    }

    private Plugin findSitePlugin( Build build )
    {
        for ( Plugin plugin : build.getPlugins() )
        {
            if ( isSitePlugin( plugin ) )
            {
                return plugin;
            }
        }

        PluginManagement pluginManagement = build.getPluginManagement();
        if ( pluginManagement != null )
        {
            for ( Plugin plugin : pluginManagement.getPlugins() )
            {
                if ( isSitePlugin( plugin ) )
                {
                    return plugin;
                }
            }
        }

        return null;
    }

    private boolean isSitePlugin( Plugin plugin )
    {
        return "maven-site-plugin".equals( plugin.getArtifactId() )
            && "org.apache.maven.plugins".equals( plugin.getGroupId() );
    }

    private Dom convert( ReportPlugin plugin )
    {
        List<Dom> children = new ArrayList<>();

        children.add( newDom( "groupId", plugin.getGroupId(), plugin.getLocation( "groupId" ) ) );
        children.add( newDom( "artifactId", plugin.getArtifactId(), plugin.getLocation( "artifactId" ) ) );
        children.add( newDom( "version", plugin.getVersion(), plugin.getLocation( "version" ) ) );

        Dom configuration = plugin.getConfiguration();
        if ( configuration != null )
        {
            children.add( configuration );
        }

        if ( !plugin.getReportSets().isEmpty() )
        {
            List<Dom> reportSets = new ArrayList<>();
            for ( ReportSet reportSet : plugin.getReportSets() )
            {
                Dom rs = convert( reportSet );
                reportSets.add( rs );
            }
            children.add( newDom( "reportSets", reportSets, plugin.getLocation( "reportSets" ) ) );
        }

        return newDom( "reportPlugin", children, plugin.getLocation( "" ) );
    }

    private Dom convert( ReportSet reportSet )
    {
        List<Dom> children = new ArrayList<>();

        if ( reportSet.getId() != null )
        {
            InputLocation idLocation = reportSet.getLocation( "id" );
            children.add( newDom( "id", reportSet.getId(), idLocation == null ? location : idLocation ) );
        }

        Dom configuration = reportSet.getConfiguration();
        if ( configuration != null )
        {
            children.add( configuration );
        }

        if ( !reportSet.getReports().isEmpty() )
        {
            InputLocation location = reportSet.getLocation( "reports" );
            List<Dom> reports = new ArrayList<>();
            for ( int n = 0; n < reportSet.getReports().size(); n++ )
            {
                String report = reportSet.getReports().get( n );
                reports.add( newDom( "report", report, location != null ? location.getLocation( n ) : null ) );
            }
            children.add( newDom( "reports", reports, location ) );
        }

        return newDom( "reportSet", children, reportSet.getLocation( "" ) );
    }

    private Xpp3Dom newDom( String name, String value, InputLocation location )
    {
        return new Xpp3Dom( name, value, null, null, location );
    }

    private Xpp3Dom newDom( String name, List<Dom> children, InputLocation location )
    {
        return new Xpp3Dom( name, null, null, children, location );
    }

}
