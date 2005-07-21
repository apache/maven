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

import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.StringUtils;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Generates the Project Source Configuration Management report.
 * 
 * @goal scm
 * 
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @version $Id$
 */
public class ScmReport
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
        return getBundle( locale ).getString( "report.scm.name" );
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
        return getBundle( locale ).getString( "report.scm.description" );
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
        ScmRenderer r = new ScmRenderer( getSink(), getProject().getModel(), locale );

        r.render();
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "source-repository";
    }

    static class ScmRenderer
        extends AbstractMavenReportRenderer
    {
        private Model model;

        private Locale locale;

        public ScmRenderer( Sink sink, Model model, Locale locale )
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
            return getBundle( locale ).getString( "report.scm.title" );
        }

        /**
         * @see org.apache.maven.reporting.AbstractMavenReportRenderer#renderBody()
         */
        public void renderBody()
        {
            Scm scm = model.getScm();
            if ( scm == null )
            {
                startSection( getTitle() );

                paragraph( getBundle( locale ).getString( "report.scm.noscm" ) );

                endSection();

                return;
            }

            String connection = scm.getConnection();
            String devConnection = scm.getDeveloperConnection();

            boolean isSvnConnection = isScmSystem( connection, "svn" );
            boolean isCvsConnection = isScmSystem( connection, "cvs" );
            boolean isVssConnection = isScmSystem( connection, "vss" );

            boolean isSvnDevConnection = isScmSystem( devConnection, "svn" );
            boolean isCvsDevConnection = isScmSystem( devConnection, "cvs" );
            boolean isVssDevConnection = isScmSystem( devConnection, "vss" );

            // Overview
            startSection( getBundle( locale ).getString( "report.scm.overview.title" ) );

            if ( isSvnConnection )
            {
                linkPatternedText( getBundle( locale ).getString( "report.scm.svn.intro" ) );

            }
            else if ( isCvsConnection )
            {
                linkPatternedText( getBundle( locale ).getString( "report.scm.cvs.intro" ) );
            }
            else if ( isVssConnection )
            {
                linkPatternedText( getBundle( locale ).getString( "report.scm.vss.intro" ) );
            }
            else
            {
                paragraph( getBundle( locale ).getString( "report.scm.general.intro" ) );
            }

            endSection();

            // Web access
            startSection( getBundle( locale ).getString( "report.scm.webaccess.title" ) );

            if ( scm.getUrl() == null )
            {
                paragraph( getBundle( locale ).getString( "report.scm.webaccess.nourl" ) );
            }
            else
            {
                paragraph( getBundle( locale ).getString( "report.scm.webaccess.url" ) );

                verbatimLink( scm.getUrl(), scm.getUrl() );
            }

            endSection();

            // Anonymous access
            if ( !StringUtils.isEmpty( connection ) )
            {
                // Validation
                validConnection( connection );

                startSection( getBundle( locale ).getString( "report.scm.anonymousaccess.title" ) );

                if ( isSvnConnection )
                {
                    paragraph( getBundle( locale ).getString( "report.scm.anonymousaccess.svn.intro" ) );

                    // Example:
                    // $> svn checkout
                    // http://svn.apache.org/repos/asf/maven/components/trunk
                    // maven

                    StringBuffer sb = new StringBuffer();
                    sb.append( "$>svn checkout " ).append( getSvnRoot( connection ) ).append( " " )
                        .append( model.getArtifactId() );
                    verbatimText( sb.toString() );
                }
                else if ( isCvsConnection )
                {
                    paragraph( getBundle( locale ).getString( "report.scm.anonymousaccess.cvs.intro" ) );

                    // Example:
                    // cvs -d :pserver:anoncvs@cvs.apache.org:/home/cvspublic
                    // login
                    // cvs -z3 -d
                    // :pserver:anoncvs@cvs.apache.org:/home/cvspublic co
                    // maven-plugins/dist

                    String[] connectionDef = StringUtils.split( connection, ":" );

                    StringBuffer command = new StringBuffer();
                    command.append( "$>cvs -d " ).append( getCvsRoot( connection, "" ) ).append( " login" );
                    command.append( "\n" );
                    command.append( "$>cvs -z3 -d " ).append( getCvsRoot( connection, "" ) ).append( " co " )
                        .append( getCvsModule( connection ) );
                    verbatimText( command.toString() );
                }
                else if ( isVssConnection )
                {
                    paragraph( getBundle( locale ).getString( "report.scm.anonymousaccess.vss.intro" ) );

                    verbatimText( getVssRoot( connection, "" ) );
                }
                else
                {
                    paragraph( getBundle( locale ).getString( "report.scm.anonymousaccess.general.intro" ) );

                    verbatimText( connection.substring( 4 ) );
                }

                endSection();
            }

            // Developer access
            if ( !StringUtils.isEmpty( devConnection ) )
            {
                // Validation
                validConnection( devConnection );

                startSection( getBundle( locale ).getString( "report.scm.devaccess.title" ) );

                if ( isSvnDevConnection )
                {
                    paragraph( getBundle( locale ).getString( "report.scm.devaccess.svn.intro1" ) );

                    // Example:
                    // $> svn checkout
                    // https://svn.apache.org/repos/asf/maven/components/trunk
                    // maven

                    StringBuffer sb = new StringBuffer();
                    sb.append( "$>svn checkout " ).append( getSvnRoot( devConnection ) ).append( " " )
                        .append( model.getArtifactId() );
                    verbatimText( sb.toString() );

                    paragraph( getBundle( locale ).getString( "report.scm.devaccess.svn.intro2" ) );

                    sb = new StringBuffer();
                    sb.append( "$>svn commit --username your-username -m \"A message\"" );
                    verbatimText( sb.toString() );
                }
                else if ( isCvsDevConnection )
                {
                    paragraph( getBundle( locale ).getString( "report.scm.devaccess.cvs.intro" ) );

                    // Example:
                    // cvs -d :pserver:username@cvs.apache.org:/home/cvs login
                    // cvs -z3 -d :ext:username@cvs.apache.org:/home/cvs co
                    // maven-plugins/dist

                    String[] connectionDef = StringUtils.split( devConnection, ":" );

                    StringBuffer command = new StringBuffer();
                    command.append( "$>cvs -d " ).append( getCvsRoot( devConnection, "username" ) ).append( " login" );
                    command.append( "\n" );
                    command.append( "$>cvs -z3 -d " ).append( getCvsRoot( devConnection, "username" ) ).append( " co " )
                        .append( getCvsModule( devConnection ) );
                    verbatimText( command.toString() );
                }
                else if ( isVssDevConnection )
                {
                    paragraph( getBundle( locale ).getString( "report.scm.devaccess.vss.intro" ) );

                    verbatimText( getVssRoot( connection, "username" ) );
                }
                else
                {
                    paragraph( getBundle( locale ).getString( "report.scm.devaccess.general.intro" ) );

                    verbatimText( connection.substring( 4 ) );
                }

                endSection();
            }

            // Access from behind a firewall
            startSection( getBundle( locale ).getString( "report.scm.accessbehindfirewall.title" ) );

            if ( isSvnDevConnection )
            {
                paragraph( getBundle( locale ).getString( "report.scm.accessbehindfirewall.svn.intro" ) );

                StringBuffer sb = new StringBuffer();
                sb.append( "$>svn checkout " ).append( getSvnRoot( devConnection ) ).append( " " )
                    .append( model.getArtifactId() );
                verbatimText( sb.toString() );
            }
            else if ( isCvsDevConnection )
            {
                paragraph( getBundle( locale ).getString( "report.scm.accessbehindfirewall.cvs.intro" ) );
            }
            else
            {
                paragraph( getBundle( locale ).getString( "report.scm.accessbehindfirewall.general.intro" ) );
            }

            endSection();

            // Access through a proxy
            if ( isSvnConnection || isSvnDevConnection )
            {
                startSection( getBundle( locale ).getString( "report.scm.accessthroughtproxy.title" ) );

                paragraph( getBundle( locale ).getString( "report.scm.accessthroughtproxy.svn.intro1" ) );
                paragraph( getBundle( locale ).getString( "report.scm.accessthroughtproxy.svn.intro2" ) );
                paragraph( getBundle( locale ).getString( "report.scm.accessthroughtproxy.svn.intro3" ) );

                StringBuffer sb = new StringBuffer();
                sb.append( "[global]" ).append( "\n" );
                sb.append( "http-proxy-host = your.proxy.name" ).append( "\n" );
                sb.append( "http-proxy-port = 3128" ).append( "\n" );
                verbatimText( sb.toString() );

                endSection();
            }
        }

        /**
         * Checks if a SCM connection is a SVN, CVS...
         * 
         * @return true if the SCM is a SVN, CVS server, false otherwise.
         */
        private static boolean isScmSystem( String connection, String scm )
        {
            if ( StringUtils.isEmpty( connection ) )
            {
                return false;
            }

            if ( StringUtils.isEmpty( scm ) )
            {
                return false;
            }

            if ( connection.toLowerCase().substring( 4 ).startsWith( scm ) )
            {
                return true;
            }

            return false;
        }

        /**
         * Get the SVN root from a connection
         * 
         * @param connection
         *            a valid SVN connection
         * @return the svn connection
         */
        private static String getSvnRoot( String connection )
        {
            if ( !isScmSystem( connection, "svn" ) )
            {
                throw new IllegalArgumentException( "Cannot get the SVN root from a none SVN SCM." );
            }

            String[] connectionDef = StringUtils.split( connection, ":" );

            if ( connectionDef.length != 4 )
            {
                throw new IllegalArgumentException( "The SVN repository connection is not valid." );
            }

            return connectionDef[2] + ":" + connectionDef[3];
        }

        /**
         * Get the CVS root from the connection
         * 
         * @param connection
         *            a valid CVS connection
         * @param username
         * @return the CVS root
         */
        private static String getCvsRoot( String connection, String username )
        {
            if ( !isScmSystem( connection, "cvs" ) )
            {
                throw new IllegalArgumentException( "Cannot get the CVS root from a none CVS SCM." );
            }

            String[] connectionDef = StringUtils.split( connection, ":" );

            if ( connectionDef.length != 6 )
            {
                throw new IllegalArgumentException( "The CVS repository connection is not valid." );
            }

            if ( connectionDef[3].indexOf( '@' ) >= 0 )
            {
                if ( StringUtils.isEmpty( username ) )
                {
                    username = connectionDef[3].substring( 0, connectionDef[3].indexOf( '@' ) );
                }
                connectionDef[3] = username + "@" + connectionDef[3].substring( connectionDef[3].indexOf( '@' ) + 1 );
            }

            return ":" + connectionDef[2] + ":" + connectionDef[3] + ":" + connectionDef[4];
        }

        /**
         * Get the CVS module from a connection
         * 
         * @param connection
         *            a valid CVS connection
         * @return the CVS module
         */
        private static String getCvsModule( String connection )
        {
            if ( !isScmSystem( connection, "cvs" ) )
            {
                throw new IllegalArgumentException( "Cannot get the CVS root from a none CVS SCM." );
            }
            String[] connectionDef = StringUtils.split( connection, ":" );

            if ( connectionDef.length != 6 )
            {
                throw new IllegalArgumentException( "The CVS repository connection is not valid." );
            }

            return connectionDef[5];
        }

        /**
         * Get a VSS root.
         * 
         * @param connection
         *            a valid VSS connection
         * @param username
         * @return the VSS root
         */
        private static String getVssRoot( String connection, String username )
        {
            if ( !isScmSystem( connection, "vss" ) )
            {
                throw new IllegalArgumentException( "Cannot get the VSS root from a none VSS SCM." );
            }

            String[] connectionDef = StringUtils.split( connection, ":" );

            if ( connectionDef.length != 5 )
            {
                throw new IllegalArgumentException( "The VSS repository connection is not valid." );
            }

            if ( StringUtils.isEmpty( username ) )
            {
                username = connectionDef[3];
            }

            return connectionDef[1] + ":" + connectionDef[2] + ":" + username + ":" + connectionDef[4];
        }

        /**
         * Convenience method that valid a given connection.
         * <p>
         * Throw an <code>IllegalArgumentException</code> if the connection is
         * not a valid one.
         * </p>
         * 
         * @param connection
         */
        private static void validConnection( String connection )
        {
            if ( StringUtils.isEmpty( connection ) )
            {
                throw new IllegalArgumentException( "The source repository connection could not be null." );
            }
            if ( connection.length() < 4 )
            {
                throw new IllegalArgumentException( "The source repository connection is too short." );
            }
            if ( !connection.startsWith( "scm" ) )
            {
                throw new IllegalArgumentException( "The source repository connection must start with scm." );
            }
        }
    }

    private static ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "project-info-report", locale, ScmReport.class.getClassLoader() );
    }
}