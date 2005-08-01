package org.apache.maven.report.projectinfo;

/*
 * Copyright 2004-2005 The Apache Software Foundation.
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

import org.apache.maven.model.Contributor;
import org.apache.maven.model.Developer;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.StringUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * Generates the Project Team report.
 * 
 * @goal project-team
 * 
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @version $Id$
 */
public class TeamListReport
    extends AbstractMavenReport
{
    /**
     * @parameter expression="${project.build.directory}/site"
     * @required
     */
    private String outputDirectory;

    /**
     * @parameter expression="${component.org.codehaus.doxia.site.renderer.SiteRenderer}"
     * @required
     * @readonly
     */
    private SiteRenderer siteRenderer;

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @see org.apache.maven.reporting.MavenReport#getName(java.util.Locale)
     */
    public String getName( Locale locale )
    {
        return getBundle( locale ).getString( "report.team-list.name" );
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getCategoryName()
     */
    public String getCategoryName()
    {
        return CATEGORY_PROJECT_INFORMATION;
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getDescription(java.util.Locale)
     */
    public String getDescription( Locale locale )
    {
        return getBundle( locale ).getString( "report.team-list.description" );
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
     * @see org.apache.maven.reporting.AbstractMavenReport#getSiteRenderer()
     */
    protected SiteRenderer getSiteRenderer()
    {
        return siteRenderer;
    }

    /**
     * @see org.apache.maven.reporting.AbstractMavenReport#executeReport(java.util.Locale)
     */
    public void executeReport( Locale locale )
        throws MavenReportException
    {
        TeamListRenderer r = new TeamListRenderer( getSink(), getProject().getModel(), locale );

        r.render();
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "team-list";
    }

    static class TeamListRenderer
        extends AbstractMavenReportRenderer
    {
        private Model model;

        private Locale locale;

        public TeamListRenderer( Sink sink, Model model, Locale locale )
        {
            super( sink );

            this.model = model;

            this.locale = locale;
        }

        /**
         * @see org.apache.maven.reporting.MavenReportRenderer#getTitle()
         */
        public String getTitle()
        {
            return getBundle( locale ).getString( "report.team-list.title" );
        }

        /**
         * @see org.apache.maven.reporting.AbstractMavenReportRenderer#renderBody()
         */
        public void renderBody()
        {
            startSection( getBundle( locale ).getString( "report.team-list.intro.title" ) );

            // To handle JS
            StringBuffer javascript = new StringBuffer( "function offsetDate(id, offset) {\n" )
                .append( "    var now = new Date();\n" )
                .append( "    var nowTime = now.getTime();\n" )
                .append( "    var localOffset = now.getTimezoneOffset();\n" )
                .append( "    var developerTime = nowTime + ( offset * 60 * 60 * 1000 ) + ( localOffset * 60 * 1000 );\n" )
                .append( "    var developerDate = new Date(developerTime);\n" ).append( "\n" )
                .append( "    document.getElementById(id).innerHTML = developerDate;\n" ).append( "}\n" )
                .append( "\n" )
                .append( "function init(){\n" );

            // Intoduction
            paragraph( getBundle( locale ).getString( "report.team-list.intro.description1" ) );
            paragraph( getBundle( locale ).getString( "report.team-list.intro.description2" ) );

            // Developer section
            List developers = model.getDevelopers();

            startSection( getBundle( locale ).getString( "report.team-list.developers.title" ) );

            if ( ( developers == null ) || ( developers.isEmpty() ) )
            {
                paragraph( getBundle( locale ).getString( "report.team-list.nodeveloper" ) );
            }
            else
            {
                paragraph( getBundle( locale ).getString( "report.team-list.developers.intro" ) );

                startTable();

                String id = getBundle( locale ).getString( "report.team-list.developers.id" );
                String name = getBundle( locale ).getString( "report.team-list.developers.name" );
                String email = getBundle( locale ).getString( "report.team-list.developers.email" );
                String url = getBundle( locale ).getString( "report.team-list.developers.url" );
                String organization = getBundle( locale ).getString( "report.team-list.developers.organization" );
                String organizationUrl = getBundle( locale ).getString( "report.team-list.developers.organizationurl" );
                String roles = getBundle( locale ).getString( "report.team-list.developers.roles" );
                String timeZone = getBundle( locale ).getString( "report.team-list.developers.timezone" );
                String actualTime = getBundle( locale ).getString( "report.team-list.developers.actualtime" );
                String properties = getBundle( locale ).getString( "report.team-list.developers.properties" );

                tableHeader( new String[] {
                    id,
                    name,
                    email,
                    url,
                    organization,
                    organizationUrl,
                    roles,
                    timeZone,
                    actualTime,
                    properties } );

                // To handle JS
                int developersRows = 0;
                for ( Iterator i = developers.iterator(); i.hasNext(); )
                {
                    Developer developer = (Developer) i.next();

                    // To handle JS
                    sink.tableRow();

                    tableCell( developer.getId() );

                    tableCell( developer.getName() );

                    tableCell( createLinkPatternedText( developer.getEmail(), developer.getEmail() ) );

                    tableCell( createLinkPatternedText( developer.getUrl(), developer.getUrl() ) );

                    tableCell( developer.getOrganization() );

                    tableCell( createLinkPatternedText( developer.getOrganizationUrl(), developer.getOrganizationUrl() ) );

                    if ( developer.getRoles() != null )
                    {
                        // Comma separated roles
                        tableCell( StringUtils.join( developer.getRoles().toArray( new String[0] ), ", " ) );
                    }
                    else
                    {
                        tableCell( null );
                    }

                    tableCell( developer.getTimezone() );

                    // To handle JS
                    sink.tableCell();
                    sink.rawText( "<span id=\"developer-" + developersRows + "\">" );
                    text( developer.getTimezone() );
                    if ( !StringUtils.isEmpty( developer.getTimezone() ) )
                    {
                        javascript.append( "    offsetDate('developer-" + developersRows + "', '" );
                        javascript.append( developer.getTimezone() );
                        javascript.append( "');\n" );
                    }
                    sink.rawText( "</span>" );
                    sink.tableCell_();

                    Properties props = developer.getProperties();
                    if ( props != null )
                    {
                        tableCell( propertiesToString( props ) );
                    }
                    else
                    {
                        tableCell( null );
                    }

                    sink.tableRow_();

                    developersRows++;
                }

                endTable();
            }

            endSection();

            // contributors section
            List contributors = model.getContributors();

            startSection( getBundle( locale ).getString( "report.team-list.contributors.title" ) );

            if ( ( contributors == null ) || ( contributors.isEmpty() ) )
            {
                paragraph( getBundle( locale ).getString( "report.team-list.nocontributor" ) );
            }
            else
            {
                paragraph( getBundle( locale ).getString( "report.team-list.contributors.intro" ) );

                startTable();

                String name = getBundle( locale ).getString( "report.team-list.contributors.name" );
                String email = getBundle( locale ).getString( "report.team-list.contributors.email" );
                String url = getBundle( locale ).getString( "report.team-list.contributors.url" );
                String organization = getBundle( locale ).getString( "report.team-list.contributors.organization" );
                String organizationUrl = getBundle( locale )
                    .getString( "report.team-list.contributors.organizationurl" );
                String roles = getBundle( locale ).getString( "report.team-list.contributors.roles" );
                String timeZone = getBundle( locale ).getString( "report.team-list.contributors.timezone" );
                String actualTime = getBundle( locale ).getString( "report.team-list.contributors.actualtime" );
                String properties = getBundle( locale ).getString( "report.team-list.contributors.properties" );

                tableHeader( new String[] {
                    name,
                    email,
                    url,
                    organization,
                    organizationUrl,
                    roles,
                    timeZone,
                    actualTime,
                    properties } );

                // To handle JS
                int contributorsRows = 0;
                for ( Iterator i = contributors.iterator(); i.hasNext(); )
                {
                    Contributor contributor = (Contributor) i.next();

                    sink.tableRow();

                    tableCell( contributor.getName() );

                    tableCell( createLinkPatternedText( contributor.getEmail(), contributor.getEmail() ) );

                    tableCell( createLinkPatternedText( contributor.getUrl(), contributor.getUrl() ) );

                    tableCell( contributor.getOrganization() );

                    tableCell( createLinkPatternedText( contributor.getOrganizationUrl(), contributor
                        .getOrganizationUrl() ) );

                    if ( contributor.getRoles() != null )
                    {
                        // Comma separated roles
                        tableCell( StringUtils.join( contributor.getRoles().toArray( new String[0] ), ", " ) );
                    }
                    else
                    {
                        tableCell( null );
                    }

                    tableCell( contributor.getTimezone() );

                    // To handle JS
                    sink.tableCell();
                    sink.rawText( "<span id=\"contributor-" + contributorsRows + "\">" );
                    text( contributor.getTimezone() );
                    if ( !StringUtils.isEmpty( contributor.getTimezone() ) )
                    {
                        javascript.append( "    offsetDate('contributor-" + contributorsRows + "', '" );
                        javascript.append( contributor.getTimezone() );
                        javascript.append( "');\n" );
                    }
                    sink.rawText( "</span>" );
                    sink.tableCell_();

                    Properties props = contributor.getProperties();
                    if ( props != null )
                    {
                        tableCell( propertiesToString( props ) );
                    }
                    else
                    {
                        tableCell( null );
                    }

                    sink.tableRow_();

                    contributorsRows++;
                }

                endTable();
            }

            endSection();

            endSection();

            // To handle JS
            javascript.append( "}\n" ).append( "\n" ).append( "window.onLoad = init();\n" );
            javaScript( javascript.toString() );
        }
    }

    private static ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "project-info-report", locale, TeamListReport.class.getClassLoader() );
    }
}