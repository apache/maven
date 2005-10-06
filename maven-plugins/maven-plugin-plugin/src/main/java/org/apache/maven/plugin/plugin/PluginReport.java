package org.apache.maven.plugin.plugin;

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

import org.apache.maven.plugin.descriptor.InvalidPluginDescriptorException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.tools.plugin.extractor.ExtractionException;
import org.apache.maven.tools.plugin.generator.Generator;
import org.apache.maven.tools.plugin.generator.PluginXdocGenerator;
import org.apache.maven.tools.plugin.scanner.MojoScanner;
import org.apache.maven.tools.plugin.util.PluginUtils;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Generates the Plugin's documentation report.
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id $
 * @goal report
 */
public class PluginReport
    extends AbstractMavenReport
{
    /**
     * Report output directory.
     *
     * @parameter expression="${project.build.directory}/generated-site/xdoc"
     * @required
     */
    private String outputDirectory;

    /**
     * Doxia Site Renderer.
     *
     * @component
     */
    private SiteRenderer siteRenderer;

    /**
     * The Maven Project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Mojo scanner tools.
     *
     * @component
     */
    protected MojoScanner mojoScanner;

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getSiteRenderer()
     */
    protected SiteRenderer getSiteRenderer()
    {
        return siteRenderer;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getOutputDirectory()
     */
    protected String getOutputDirectory()
    {
        return outputDirectory;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#getProject()
     */
    protected MavenProject getProject()
    {
        return project;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    protected void executeReport( Locale locale )
        throws MavenReportException
    {
        if ( !project.getPackaging().equals( "maven-plugin" ) )
        {
            return;
        }

        String goalPrefix = PluginDescriptor.getGoalPrefixFromArtifactId( project.getArtifactId() );

        // TODO: could use this more, eg in the writing of the plugin descriptor!
        PluginDescriptor pluginDescriptor = new PluginDescriptor();

        pluginDescriptor.setGroupId( project.getGroupId() );

        pluginDescriptor.setArtifactId( project.getArtifactId() );

        pluginDescriptor.setVersion( project.getVersion() );

        pluginDescriptor.setGoalPrefix( goalPrefix );

        try
        {
            pluginDescriptor.setDependencies( PluginUtils.toComponentDependencies( project.getRuntimeDependencies() ) );

            mojoScanner.populatePluginDescriptor( project, pluginDescriptor );

            // Generate the plugin's documentation
            generatePluginDocumentation( pluginDescriptor );

            // Write the overview
            PluginOverviewRenderer r = new PluginOverviewRenderer( getSink(), pluginDescriptor, locale );
            r.render();
        }
        catch ( InvalidPluginDescriptorException e )
        {
            throw new MavenReportException( "Error extracting plugin descriptor: \'" + e.getLocalizedMessage() + "\'",
                                            e );
        }
        catch ( ExtractionException e )
        {
            throw new MavenReportException( "Error extracting plugin descriptor: \'" + e.getLocalizedMessage() + "\'",
                                            e );
        }
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.plugin.description" );
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.plugin.name" );
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "plugin-info";
    }

    private void generatePluginDocumentation( PluginDescriptor pluginDescriptor )
        throws MavenReportException
    {
        try
        {
            File outputDir = new File( getOutputDirectory() );
            outputDir.mkdirs();

            Generator generator = new PluginXdocGenerator();
            generator.execute( outputDir, pluginDescriptor );
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Error writing plugin documentation", e );
        }

    }

    private static ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "plugin-report", locale, PluginReport.class.getClassLoader() );
    }

    /**
     * Generates an overview page with the list of goals
     * and a link to the goal's page.
     */
    static class PluginOverviewRenderer
        extends AbstractMavenReportRenderer
    {
        private final PluginDescriptor pluginDescriptor;

        private final Locale locale;

        public PluginOverviewRenderer( Sink sink, PluginDescriptor pluginDescriptor, Locale locale )
        {
            super( sink );

            this.pluginDescriptor = pluginDescriptor;

            this.locale = locale;
        }

        /**
         * @see org.apache.maven.reporting.MavenReportRenderer#getTitle()
         */
        public String getTitle()
        {
            return getBundle( locale ).getString( "report.plugin.title" );
        }

        /**
         * @see org.apache.maven.reporting.AbstractMavenReportRenderer#renderBody()
         */
        public void renderBody()
        {
            startSection( getTitle() );

            paragraph( getBundle( locale ).getString( "report.plugin.goals.intro" ) );

            startTable();

            String goalColumnName = getBundle( locale ).getString( "report.plugin.goals.column.goal" );
            String descriptionColumnName = getBundle( locale ).getString( "report.plugin.goals.column.description" );

            tableHeader( new String[]{goalColumnName, descriptionColumnName} );

            for ( Iterator i = pluginDescriptor.getMojos().iterator(); i.hasNext(); )
            {
                MojoDescriptor mojo = (MojoDescriptor) i.next();

                String goalName = mojo.getFullGoalName();
                /*
                 * Added ./ to define a relative path
                 * @see AbstractMavenReportRenderer#getValidHref(java.lang.String)
                 */
                String goalDocumentationLink = "./" + mojo.getGoal() + "-mojo.html";
                String description = mojo.getDescription();
                if ( StringUtils.isEmpty( mojo.getDescription() ) )
                {
                    description = getBundle( locale ).getString( "report.plugin.goal.nodescription" );

                }

                sink.tableRow();
                tableCell( createLinkPatternedText( goalName, goalDocumentationLink ) );
                tableCell( description, true );
                sink.tableRow_();
            }

            endTable();

            endSection();
        }
    }
}