package org.apache.maven.plugin.coreit;

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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.reporting.MavenReport;
import org.codehaus.doxia.sink.Sink;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Locale;

/**
 * Generates the available/configured reports.
 *
 * @author Benjamin Bentmann
 *
 * @goal generate
 * @phase site
 * @requiresReports true
 */
public class GenerateMojo
    extends AbstractMojo
{

    /**
     * The path to the output directory of the site.
     *
     * @parameter default-value="${project.reporting.outputDirectory}"
     */
    private File outputDirectory;

    /**
     * The language for the reports.
     *
     * @parameter default-value="en"
     */
    private String language = "en";

    /**
     * A flag whether to ignore errors from reports and continue the generation.
     *
     * @parameter default-value="false"
     */
    private boolean ignoreErrors;

    /**
     * The reports configured for the current build.
     *
     * @parameter default-value="${reports}"
     * @required
     * @readonly
     */
    private List reports;

    /**
     * Runs this mojo.
     *
     * @throws MojoExecutionException If the output file could not be created.
     */
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        getLog().info( "[MAVEN-CORE-IT-LOG] Using output directory " + outputDirectory );

        Locale locale = new Locale( language );
        getLog().info( "[MAVEN-CORE-IT-LOG] Using locale " + locale );

        InvocationHandler handler = new InvocationHandler()
        {

            public Object invoke( Object proxy, Method method, Object[] args )
                throws Throwable
            {
                return null;
            }

        };
        Sink sink = (Sink) Proxy.newProxyInstance( getClass().getClassLoader(), new Class[]{ Sink.class }, handler );

        for ( Object report1 : reports )
        {
            MavenReport report = (MavenReport) report1;

            if ( report.canGenerateReport() )
            {
                getLog().info( "[MAVEN-CORE-IT-LOG] Generating report " + report );
                try
                {
                    report.setReportOutputDirectory( outputDirectory );
                    report.generate( sink, locale );
                }
                catch ( Throwable e )
                {
                    getLog().warn( "[MAVEN-CORE-IT-LOG]   " + e, e );
                    if ( !ignoreErrors )
                    {
                        throw new MojoExecutionException( "Failed to generate report " + report, e );
                    }
                }
            }
            else
            {
                getLog().info( "[MAVEN-CORE-IT-LOG] Skipping report " + report );
            }
        }
    }

}
