package jar;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import java.io.File;
import java.util.Locale;

import org.apache.maven.doxia.siterenderer.DefaultSiteRenderer;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;

/**
 * Goal which touches a timestamp file.
 *
 * @goal report
 * @execute phase="initialize"
 */
public class ForkingReport
    extends AbstractMavenReport
{

    /**
     * @parameter default-value="${project.build.directory}/report"
     * @readonly
     */
    private File outputDirectory;

    /**
     * @parameter default-value="${project}"
     * @readonly
     */
    private MavenProject project;

    protected void executeReport( Locale locale )
        throws MavenReportException
    {
        getLog().info( "MNG-3743 report executed." );
    }

    protected String getOutputDirectory()
    {
        return outputDirectory.getAbsolutePath();
    }

    protected MavenProject getProject()
    {
        return project;
    }

    protected Renderer getSiteRenderer()
    {
        return new DefaultSiteRenderer();
    }

    public String getDescription( Locale locale )
    {
        return "mng3743";
    }

    public String getName( Locale locale )
    {
        return "mng3743";
    }

    public String getOutputName()
    {
        return "mng3743";
    }
}
