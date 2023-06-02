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
package org.apache.maven.plugin.coreit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Lists the available/configured reports in a properties file.
 *
 * @author Benjamin Bentmann
 *
 */
@Mojo(name = "list", defaultPhase = LifecyclePhase.INITIALIZE, requiresReports = true)
public class ListMojo extends AbstractMojo {

    /**
     * The path to the properties file used to list the available reports. The properties file will have a key named
     * <code>reports</code> that gives the total count of reports. The keys <code>reports.0</code>,
     * <code>reports.1</code> etc. will be used to denote the qualified class names of the reports.
     */
    @Parameter(property = "site.properties", defaultValue = "target/reports.properties")
    private File reportsFile;

    /**
     * The reports configured for the current build.
     */
    @Parameter(defaultValue = "${reports}", required = true, readonly = true)
    private List<?> reports;

    /**
     * Runs this mojo.
     *
     * @throws MojoExecutionException If the output file could not be created.
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        Properties reportProperties = new Properties();

        reportProperties.setProperty("reports", "" + reports.size());

        for (int i = 0; i < reports.size(); i++) {
            Object report = reports.get(i);
            getLog().info("[MAVEN-CORE-IT-LOG] Listing report " + report);
            reportProperties.setProperty("reports." + i, report.getClass().getName());
        }

        getLog().info("[MAVEN-CORE-IT-LOG] Creating output file " + reportsFile);

        OutputStream out = null;
        try {
            reportsFile.getParentFile().mkdirs();
            out = new FileOutputStream(reportsFile);
            reportProperties.store(out, "MAVEN-CORE-IT-LOG");
        } catch (IOException e) {
            throw new MojoExecutionException("Output file could not be created: " + reportsFile, e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // just ignore
                }
            }
        }

        getLog().info("[MAVEN-CORE-IT-LOG] Created output file " + reportsFile);
    }
}
