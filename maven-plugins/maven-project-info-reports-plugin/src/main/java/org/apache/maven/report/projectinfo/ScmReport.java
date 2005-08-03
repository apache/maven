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
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.clearcase.repository.ClearCaseScmProviderRepository;
import org.apache.maven.scm.provider.cvslib.repository.CvsScmProviderRepository;
import org.apache.maven.scm.provider.perforce.repository.PerforceScmProviderRepository;
import org.apache.maven.scm.provider.starteam.repository.StarteamScmProviderRepository;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.StringUtils;

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
     * @parameter expression="${component.org.apache.maven.scm.manager.ScmManager}"
     * @required
     * @readonly
     */
    protected ScmManager scmManager;

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
        ScmRenderer r = new ScmRenderer( scmManager, getSink(), getProject().getModel(), locale );

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

        private ScmManager scmManager;

        /** To support more SCM */
        private String anonymousConnection;

        private String devConnection;

        public ScmRenderer( ScmManager scmManager, Sink sink, Model model, Locale locale )
        {
            super( sink );

            this.scmManager = scmManager;

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

            anonymousConnection = scm.getConnection();
            devConnection = scm.getDeveloperConnection();

            ScmRepository anonymousRepository = getScmRepository( anonymousConnection );
            ScmRepository devRepository = getScmRepository( devConnection );

            // Overview section
            renderOverViewSection( anonymousRepository );

            // Web access section
            renderWebAccesSection( scm.getUrl() );

            // Anonymous access section if needed
            renderAnonymousAccessSection( anonymousRepository );

            // Developer access section
            renderDeveloperAccessSection( devRepository );

            // Access from behind a firewall section if needed
            renderAccessBehindFirewallSection( devRepository );

            // Access through a proxy section if needed
            renderAccessThroughProxySection( anonymousRepository, devRepository );
        }

        /**
         * Render the overview section
         * 
         * @param anonymousRepository the anonymous repository
         */
        private void renderOverViewSection( ScmRepository anonymousRepository )
        {
            startSection( getBundle( locale ).getString( "report.scm.overview.title" ) );

            if ( isScmSystem( anonymousRepository, "clearcase" ) )
            {
                linkPatternedText( getBundle( locale ).getString( "report.scm.clearcase.intro" ) );
            }
            else if ( isScmSystem( anonymousRepository, "cvs" ) )
            {
                linkPatternedText( getBundle( locale ).getString( "report.scm.cvs.intro" ) );
            }
            else if ( isScmSystem( anonymousRepository, "perforce" ) )
            {
                linkPatternedText( getBundle( locale ).getString( "report.scm.perforce.intro" ) );
            }
            else if ( isScmSystem( anonymousRepository, "starteam" ) )
            {
                linkPatternedText( getBundle( locale ).getString( "report.scm.starteam.intro" ) );
            }
            else if ( isScmSystem( anonymousRepository, "svn" ) )
            {
                linkPatternedText( getBundle( locale ).getString( "report.scm.svn.intro" ) );
            }
            else
            {
                paragraph( getBundle( locale ).getString( "report.scm.general.intro" ) );
            }

            endSection();
        }

        /**
         * Render the web access section
         * 
         * @param scmUrl The URL to the project's browsable repository.
         */
        private void renderWebAccesSection( String scmUrl )
        {
            startSection( getBundle( locale ).getString( "report.scm.webaccess.title" ) );

            if ( scmUrl == null )
            {
                paragraph( getBundle( locale ).getString( "report.scm.webaccess.nourl" ) );
            }
            else
            {
                paragraph( getBundle( locale ).getString( "report.scm.webaccess.url" ) );

                verbatimLink( scmUrl, scmUrl );
            }

            endSection();
        }

        /**
         * Render the anonymous access section depending the repository.
         * <p>Note: ClearCase, Starteam et Perforce seems to have no anonymous access.</>
         * 
         * @param anonymousRepository the anonymous repository
         */
        private void renderAnonymousAccessSection( ScmRepository anonymousRepository )
        {
            if ( ( isScmSystem( anonymousRepository, "clearcase" ) )
                || ( isScmSystem( anonymousRepository, "perforce" ) )
                || ( isScmSystem( anonymousRepository, "starteam" ) ) )
            {
                return;
            }

            startSection( getBundle( locale ).getString( "report.scm.anonymousaccess.title" ) );

            if ( ( anonymousRepository != null ) && ( isScmSystem( anonymousRepository, "cvs" ) ) )
            {
                CvsScmProviderRepository cvsRepo = (CvsScmProviderRepository) anonymousRepository
                    .getProviderRepository();

                anonymousAccessCVS( cvsRepo );
            }
            else if ( ( anonymousRepository != null ) && ( isScmSystem( anonymousRepository, "svn" ) ) )
            {
                SvnScmProviderRepository svnRepo = (SvnScmProviderRepository) anonymousRepository
                    .getProviderRepository();

                anonymousAccessSVN( svnRepo );
            }
            else
            {
                paragraph( getBundle( locale ).getString( "report.scm.anonymousaccess.general.intro" ) );

                verbatimText( anonymousConnection.substring( 4 ) );
            }

            endSection();
        }

        /**
         * Render the developer access section
         * 
         * @param devRepository the dev repository
         */
        private void renderDeveloperAccessSection( ScmRepository devRepository )
        {
            startSection( getBundle( locale ).getString( "report.scm.devaccess.title" ) );

            if ( ( devRepository != null ) && ( isScmSystem( devRepository, "clearcase" ) ) )
            {
                ClearCaseScmProviderRepository clearCaseRepo = (ClearCaseScmProviderRepository) devRepository
                    .getProviderRepository();

                developerAccessClearCase( clearCaseRepo );
            }
            else if ( ( devRepository != null ) && ( isScmSystem( devRepository, "cvs" ) ) )
            {
                CvsScmProviderRepository cvsRepo = (CvsScmProviderRepository) devRepository.getProviderRepository();

                developerAccessCVS( cvsRepo );
            }
            else if ( ( devRepository != null ) && ( isScmSystem( devRepository, "perforce" ) ) )
            {
                PerforceScmProviderRepository perforceRepo = (PerforceScmProviderRepository) devRepository
                    .getProviderRepository();

                developerAccessPerforce( perforceRepo );
            }
            else if ( ( devRepository != null ) && ( isScmSystem( devRepository, "starteam" ) ) )
            {
                StarteamScmProviderRepository starteamRepo = (StarteamScmProviderRepository) devRepository
                    .getProviderRepository();

                developerAccessStarteam( starteamRepo );
            }
            else if ( ( devRepository != null ) && ( isScmSystem( devRepository, "svn" ) ) )
            {
                SvnScmProviderRepository svnRepo = (SvnScmProviderRepository) devRepository.getProviderRepository();

                developerAccessSVN( svnRepo );
            }
            else
            {
                paragraph( getBundle( locale ).getString( "report.scm.devaccess.general.intro" ) );

                verbatimText( devConnection.substring( 4 ) );
            }

            endSection();
        }

        /**
         * Render the access from behind a firewall section
         * 
         * @param devRepository the dev repository
         */
        private void renderAccessBehindFirewallSection( ScmRepository devRepository )
        {
            startSection( getBundle( locale ).getString( "report.scm.accessbehindfirewall.title" ) );

            if ( ( devRepository != null ) && ( isScmSystem( devRepository, "svn" ) ) )
            {
                SvnScmProviderRepository svnRepo = (SvnScmProviderRepository) devRepository.getProviderRepository();

                paragraph( getBundle( locale ).getString( "report.scm.accessbehindfirewall.svn.intro" ) );

                StringBuffer sb = new StringBuffer();
                sb.append( "$>svn checkout " ).append( svnRepo.getUrl() ).append( " " ).append( model.getArtifactId() );
                verbatimText( sb.toString() );
            }
            else if ( ( devRepository != null ) && ( isScmSystem( devRepository, "cvs" ) ) )
            {
                linkPatternedText( getBundle( locale ).getString( "report.scm.accessbehindfirewall.cvs.intro" ) );
            }
            else
            {
                paragraph( getBundle( locale ).getString( "report.scm.accessbehindfirewall.general.intro" ) );
            }

            endSection();
        }

        /**
         * Render the access from behind a firewall section
         * 
         * @param anonymousRepository the anonymous repository
         * @param devRepository the dev repository
         */
        private void renderAccessThroughProxySection( ScmRepository anonymousRepository, ScmRepository devRepository )
        {
            if ( ( isScmSystem( anonymousRepository, "svn" ) ) || ( isScmSystem( devRepository, "svn" ) ) )
            {
                startSection( getBundle( locale ).getString( "report.scm.accessthroughtproxy.title" ) );

                paragraph( getBundle( locale ).getString( "report.scm.accessthroughtproxy.svn.intro1" ) );
                paragraph( getBundle( locale ).getString( "report.scm.accessthroughtproxy.svn.intro2" ) );
                paragraph( getBundle( locale ).getString( "report.scm.accessthroughtproxy.svn.intro3" ) );

                StringBuffer sb = new StringBuffer();
                sb.append( "[global]" );
                sb.append( "\n" );
                sb.append( "http-proxy-host = your.proxy.name" ).append( "\n" );
                sb.append( "http-proxy-port = 3128" ).append( "\n" );
                verbatimText( sb.toString() );

                endSection();
            }
        }

        // Clearcase

        /**
         * Create the documentation to provide an developer access with a <code>Clearcase</code> SCM.
         * For example, generate the following command line:
         * <p>cleartool checkout module</p>
         * 
         * @param clearCaseRepo
         */
        private void developerAccessClearCase( ClearCaseScmProviderRepository clearCaseRepo )
        {
            paragraph( getBundle( locale ).getString( "report.scm.devaccess.clearcase.intro" ) );

            StringBuffer command = new StringBuffer();
            command.append( "$>cleartool checkout " ).append( clearCaseRepo.getModule() );

            verbatimText( command.toString() );
        }

        // CVS

        /**
         * Create the documentation to provide an anonymous access with a <code>CVS</code> SCM.
         * For example, generate the following command line:
         * <p>cvs -d :pserver:anoncvs@cvs.apache.org:/home/cvspublic login</p>
         * <p>cvs -z3 -d :pserver:anoncvs@cvs.apache.org:/home/cvspublic co maven-plugins/dist</p>
         * 
         * @see <a href="https://www.cvshome.org/docs/manual/cvs-1.12.12/cvs_16.html#SEC115">https://www.cvshome.org/docs/manual/cvs-1.12.12/cvs_16.html#SEC115</a>
         * 
         * @param cvsRepo
         */
        private void anonymousAccessCVS( CvsScmProviderRepository cvsRepo )
        {
            paragraph( getBundle( locale ).getString( "report.scm.anonymousaccess.cvs.intro" ) );

            StringBuffer command = new StringBuffer();
            command.append( "$>cvs -d " ).append( cvsRepo.getCvsRoot() ).append( " login" );
            command.append( "\n" );
            command.append( "$>cvs -z3 -d " ).append( cvsRepo.getCvsRoot() );
            command.append( " co " ).append( cvsRepo.getModule() );

            verbatimText( command.toString() );
        }

        /**
         * Create the documentation to provide an developer access with a <code>CVS</code> SCM.
         * For example, generate the following command line:
         * <p>cvs -d :pserver:username@cvs.apache.org:/home/cvs login</p>
         * <p>cvs -z3 -d :ext:username@cvs.apache.org:/home/cvs co maven-plugins/dist</p>
         * 
         * @see <a href="https://www.cvshome.org/docs/manual/cvs-1.12.12/cvs_16.html#SEC115">https://www.cvshome.org/docs/manual/cvs-1.12.12/cvs_16.html#SEC115</a>
         * 
         * @param cvsRepo
         */
        private void developerAccessCVS( CvsScmProviderRepository cvsRepo )
        {
            paragraph( getBundle( locale ).getString( "report.scm.devaccess.cvs.intro" ) );

            // Safety: remove the username if present
            String cvsRoot = StringUtils.replace( cvsRepo.getCvsRoot(), cvsRepo.getUser(), "username" );

            StringBuffer command = new StringBuffer();
            command.append( "$>cvs -d " ).append( cvsRoot ).append( " login" );
            command.append( "\n" );
            command.append( "$>cvs -z3 -d " ).append( cvsRoot ).append( " co " ).append( cvsRepo.getModule() );

            verbatimText( command.toString() );
        }

        // Perforce

        /**
         * Create the documentation to provide an developer access with a <code>Perforce</code> SCM.
         * For example, generate the following command line:
         * <p>p4 -H hostname -p port -u username -P password path</p>
         * <p>p4 -H hostname -p port -u username -P password path submit -c changement</p>
         * 
         * @see <a href="http://www.perforce.com/perforce/doc.051/manuals/cmdref/index.html">http://www.perforce.com/perforce/doc.051/manuals/cmdref/index.html</>
         * 
         * @param perforceRepo
         */
        private void developerAccessPerforce( PerforceScmProviderRepository perforceRepo )
        {
            paragraph( getBundle( locale ).getString( "report.scm.devaccess.perforce.intro" ) );

            StringBuffer command = new StringBuffer();
            command.append( "$>p4" );
            if ( !StringUtils.isEmpty( perforceRepo.getHost() ) )
            {
                command.append( " -H " ).append( perforceRepo.getHost() );
            }
            if ( perforceRepo.getPort() > 0 )
            {
                command.append( " -p " + perforceRepo.getPort() );
            }
            command.append( " -u username" );
            command.append( " -P password" );
            command.append( " " );
            command.append( perforceRepo.getPath() );
            command.append( "\n" );
            command.append( "$>p4 submit -c \"A comment\"" );

            verbatimText( command.toString() );
        }

        // Starteam

        /**
         * Create the documentation to provide an developer access with a <code>Starteam</code> SCM.
         * For example, generate the following command line:
         * <p>stcmd co -x -nologo -stop -p myusername:mypassword@myhost:1234/projecturl -is</p>
         * <p>stcmd ci -x -nologo -stop -p myusername:mypassword@myhost:1234/projecturl -f NCI -is</p>
         * 
         * @param starteamRepo
         */
        private void developerAccessStarteam( StarteamScmProviderRepository starteamRepo )
        {
            paragraph( getBundle( locale ).getString( "report.scm.devaccess.starteam.intro" ) );

            StringBuffer command = new StringBuffer();

            // Safety: remove the username/password if present
            String fullUrl = StringUtils.replace( starteamRepo.getFullUrl(), starteamRepo.getUser(), "username" );
            fullUrl = StringUtils.replace( fullUrl, starteamRepo.getPassword(), "password" );

            command.append( "$>stcmd co -x -nologo -stop -p " );
            command.append( fullUrl );
            command.append( " -is" );
            command.append( "\n" );
            command.append( "$>stcmd ci -x -nologo -stop -p " );
            command.append( fullUrl );
            command.append( " -f NCI -is" );

            verbatimText( command.toString() );
        }

        // SVN

        /**
         * Create the documentation to provide an anonymous access with a <code>SVN</code> SCM.
         * For example, generate the following command line:
         * <p>svn checkout http://svn.apache.org/repos/asf/maven/components/trunk maven</p>
         * 
         * @see <a href="http://svnbook.red-bean.com/">http://svnbook.red-bean.com/</a>
         * 
         * @param svnRepo
         */
        private void anonymousAccessSVN( SvnScmProviderRepository svnRepo )
        {
            paragraph( getBundle( locale ).getString( "report.scm.anonymousaccess.svn.intro" ) );

            StringBuffer sb = new StringBuffer();
            sb.append( "$>svn checkout " ).append( svnRepo.getUrl() ).append( " " ).append( model.getArtifactId() );

            verbatimText( sb.toString() );
        }

        /**
         * Create the documentation to provide an developer access with a <code>SVN</code> SCM.
         * For example, generate the following command line:
         * <p>svn checkout https://svn.apache.org/repos/asf/maven/components/trunk maven</p>
         * <p>svn commit --username your-username -m "A message"</p>
         * 
         * @see <a href="http://svnbook.red-bean.com/">http://svnbook.red-bean.com/</a>
         * 
         * @param svnRepo
         */
        private void developerAccessSVN( SvnScmProviderRepository svnRepo )
        {
            paragraph( getBundle( locale ).getString( "report.scm.devaccess.svn.intro1" ) );

            StringBuffer sb = new StringBuffer();

            sb.append( "$>svn checkout " ).append( svnRepo.getUrl() ).append( " " ).append( model.getArtifactId() );

            verbatimText( sb.toString() );

            paragraph( getBundle( locale ).getString( "report.scm.devaccess.svn.intro2" ) );

            sb = new StringBuffer();
            sb.append( "$>svn commit --username your-username -m \"A message\"" );

            verbatimText( sb.toString() );
        }

        /**
         * Return a <code>SCM repository</code> defined by a given url
         * 
         * @param scmUrl an SCM URL
         * @return a valid SCM repository or null
         */
        public ScmRepository getScmRepository( String scmUrl )
        {
            if ( scmUrl == null )
            {
                return null;
            }

            try
            {
                return scmManager.makeScmRepository( scmUrl );
            }
            catch ( NoSuchScmProviderException e )
            {
                return null;
            }
            catch ( ScmRepositoryException e )
            {
                return null;
            }
        }

        /**
         * Convenience method that return true is the defined <code>SCM repository</code> is a known provider.
         * <p>Actually, we fully support Clearcase, CVS, Perforce, Starteam, SVN by the maven-scm-providers component.</p>
         * 
         * @see <a href="http://svn.apache.org/repos/asf/maven/scm/trunk/maven-scm-providers/">maven-scm-providers</a>
         * 
         * @param scmRepository a SCM repository 
         * @param scmProvider a SCM provider name 
         * @return true if the provider of the given SCM repository is equal to the given scm provider.
         */
        private static boolean isScmSystem( ScmRepository scmRepository, String scmProvider )
        {
            if ( StringUtils.isEmpty( scmProvider ) )
            {
                return false;
            }

            if ( ( scmRepository != null ) && ( scmProvider.equalsIgnoreCase( scmRepository.getProvider() ) ) )
            {
                return true;
            }

            return false;
        }
    }

    private static ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "project-info-report", locale, ScmReport.class.getClassLoader() );
    }
}