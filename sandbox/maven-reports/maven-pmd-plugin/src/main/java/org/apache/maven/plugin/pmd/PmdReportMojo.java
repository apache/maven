package org.apache.maven.plugin.pmd;

/*
 * Copyright 2005 The Apache Software Foundation.
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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportConfiguration;
import org.apache.maven.reporting.MavenReportException;

import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;

/**
 * @goal pmd
 * @description A Maven2 plugin which generates a PMD report
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id: PmdReportMojo.java,v 1.4 2005/02/23 00:08:54 brett Exp $
 */
public class PmdReportMojo
    extends AbstractMojo
{
    /**
     * @parameter alias="workingDirectory" expression="${project.build.directory}/site-generated"
     * @required
     */
    private String outputDirectory;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    public void execute()
        throws MojoExecutionException
    {
        MavenReportConfiguration config = new MavenReportConfiguration();

        config.setModel( project.getModel() );

        config.setOutputDirectory( new File( outputDirectory ) );

        MavenReport report = new PmdReport();

        report.setConfiguration( config );

        try
        {
            report.generate();
        }
        catch ( MavenReportException e )
        {
            throw new MojoExecutionException( "An error is occurred in the PMD report generation.", e );
        }
    }
}
