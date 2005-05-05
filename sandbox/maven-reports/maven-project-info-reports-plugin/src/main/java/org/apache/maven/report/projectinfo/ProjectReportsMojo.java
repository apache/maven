package org.apache.maven.reports.projectinfo;

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

/**
 * @goal generate
 * @description A Maven2 plugin which generates the set of project reports
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id: ProjectReportsMojo.java,v 1.3 2005/02/23 00:08:03 brett Exp $
 */
public class ProjectReportsMojo
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

        MavenReport dependenciesReport = new DependenciesReport();

        dependenciesReport.setConfiguration( config );

        MavenReport mailingListsReport = new MailingListsReport();

        mailingListsReport.setConfiguration( config );

        try
        {
            dependenciesReport.generate();

            mailingListsReport.generate();
        }
        catch ( MavenReportException e )
        {
            throw new MojoExecutionException( "An error is occurred in the report generation.", e );
        }
    }
}
