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
package org.apache.maven.model.plugin;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.model.Build;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.ReportSet;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblem.Severity;
import org.apache.maven.model.building.ModelProblem.Version;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Handles conversion of the <code>&lt;reporting&gt;</code> section into the configuration of Maven Site Plugin 3.x,
 * i.e. <code>reportPlugins</code> and <code>outputDirectory</code> parameters.
 *
 * @author Benjamin Bentmann
 */
@Named
@Singleton
public class DefaultReportingConverter implements ReportingConverter {
    private final InputLocation location;

    {
        String modelId = "org.apache.maven:maven-model-builder:"
                + this.getClass().getPackage().getImplementationVersion() + ":reporting-converter";
        InputSource inputSource = new InputSource();
        inputSource.setModelId(modelId);
        location = new InputLocation(-1, -1, inputSource);
        location.setLocation(0, location);
    }

    @Override
    public void convertReporting(Model model, ModelBuildingRequest request, ModelProblemCollector problems) {
        Reporting reporting = model.getReporting();

        if (reporting == null) {
            return;
        }

        Build build = model.getBuild();

        if (build == null) {
            build = new Build();
            model.setBuild(build);
            model.setLocation("build", location);
        }

        Plugin sitePlugin = findSitePlugin(build);

        if (sitePlugin == null) {
            sitePlugin = new Plugin();
            sitePlugin.setArtifactId("maven-site-plugin");
            sitePlugin.setLocation("artifactId", location);
            PluginManagement pluginManagement = build.getPluginManagement();
            if (pluginManagement == null) {
                pluginManagement = new PluginManagement();
                build.setPluginManagement(pluginManagement);
            }
            pluginManagement.addPlugin(sitePlugin);
        }

        Xpp3Dom configuration = (Xpp3Dom) sitePlugin.getConfiguration();

        if (configuration == null) {
            configuration = new Xpp3Dom("configuration", location);
            sitePlugin.setConfiguration(configuration);
        }

        Xpp3Dom reportPlugins = configuration.getChild("reportPlugins");

        if (reportPlugins != null) {
            // new-style report configuration already present: warn since this new style has been deprecated
            // in favor of classical reporting section MSITE-647 / MSITE-684
            problems.add(new ModelProblemCollectorRequest(Severity.WARNING, Version.BASE)
                    .setMessage("Reporting configuration should be done in <reporting> section, "
                            + "not in maven-site-plugin <configuration> as reportPlugins parameter.")
                    .setLocation(sitePlugin.getLocation("configuration")));
            return;
        }

        if (configuration.getChild("outputDirectory") == null) {
            addDom(
                    configuration,
                    "outputDirectory",
                    reporting.getOutputDirectory(),
                    reporting.getLocation("outputDirectory"));
        }

        reportPlugins = new Xpp3Dom("reportPlugins", location);
        configuration.addChild(reportPlugins);

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

        for (ReportPlugin plugin : reporting.getPlugins()) {
            Xpp3Dom reportPlugin = convert(plugin);
            reportPlugins.addChild(reportPlugin);

            if (!reporting.isExcludeDefaults()
                    && !hasMavenProjectInfoReportsPlugin
                    && "org.apache.maven.plugins".equals(plugin.getGroupId())
                    && "maven-project-info-reports-plugin".equals(plugin.getArtifactId())) {
                hasMavenProjectInfoReportsPlugin = true;
            }
        }

        if (!reporting.isExcludeDefaults() && !hasMavenProjectInfoReportsPlugin) {
            Xpp3Dom dom = new Xpp3Dom("reportPlugin", location);

            addDom(dom, "groupId", "org.apache.maven.plugins");
            addDom(dom, "artifactId", "maven-project-info-reports-plugin");

            reportPlugins.addChild(dom);
        }
    }

    private Plugin findSitePlugin(Build build) {
        for (Plugin plugin : build.getPlugins()) {
            if (isSitePlugin(plugin)) {
                return plugin;
            }
        }

        PluginManagement pluginManagement = build.getPluginManagement();
        if (pluginManagement != null) {
            for (Plugin plugin : pluginManagement.getPlugins()) {
                if (isSitePlugin(plugin)) {
                    return plugin;
                }
            }
        }

        return null;
    }

    private boolean isSitePlugin(Plugin plugin) {
        return "maven-site-plugin".equals(plugin.getArtifactId())
                && "org.apache.maven.plugins".equals(plugin.getGroupId());
    }

    private Xpp3Dom convert(ReportPlugin plugin) {
        Xpp3Dom dom = new Xpp3Dom("reportPlugin", plugin.getLocation(""));

        addDom(dom, "groupId", plugin.getGroupId(), plugin.getLocation("groupId"));
        addDom(dom, "artifactId", plugin.getArtifactId(), plugin.getLocation("artifactId"));
        addDom(dom, "version", plugin.getVersion(), plugin.getLocation("version"));

        Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
        if (configuration != null) {
            configuration = new Xpp3Dom(configuration);
            dom.addChild(configuration);
        }

        if (!plugin.getReportSets().isEmpty()) {
            Xpp3Dom reportSets = new Xpp3Dom("reportSets", plugin.getLocation("reportSets"));
            for (ReportSet reportSet : plugin.getReportSets()) {
                Xpp3Dom rs = convert(reportSet);
                reportSets.addChild(rs);
            }
            dom.addChild(reportSets);
        }

        return dom;
    }

    private Xpp3Dom convert(ReportSet reportSet) {
        Xpp3Dom dom = new Xpp3Dom("reportSet", reportSet.getLocation(""));

        InputLocation idLocation = reportSet.getLocation("id");
        addDom(dom, "id", reportSet.getId(), idLocation == null ? location : idLocation);

        Xpp3Dom configuration = (Xpp3Dom) reportSet.getConfiguration();
        if (configuration != null) {
            configuration = new Xpp3Dom(configuration);
            dom.addChild(configuration);
        }

        if (!reportSet.getReports().isEmpty()) {
            InputLocation location = reportSet.getLocation("reports");
            Xpp3Dom reports = new Xpp3Dom("reports", location);
            int n = 0;
            for (String report : reportSet.getReports()) {
                addDom(reports, "report", report, (location == null) ? null : location.getLocation(n++));
            }
            dom.addChild(reports);
        }

        return dom;
    }

    private void addDom(Xpp3Dom parent, String childName, String childValue) {
        addDom(parent, childName, childValue, location);
    }

    private void addDom(Xpp3Dom parent, String childName, String childValue, InputLocation location) {
        if (StringUtils.isNotEmpty(childValue)) {
            parent.addChild(newDom(childName, childValue, location));
        }
    }

    private Xpp3Dom newDom(String name, String value, InputLocation location) {
        Xpp3Dom dom = new Xpp3Dom(name, location);
        dom.setValue(value);
        return dom;
    }
}
