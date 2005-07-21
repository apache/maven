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

import org.apache.commons.validator.UrlValidator;
import org.apache.maven.model.License;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Generates the Project License report.
 * 
 * @goal license
 * 
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton </a>
 * @version $Id$
 */
public class LicenseReport
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
        return getBundle( locale ).getString( "report.license.name" );
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
        return getBundle( locale ).getString( "report.license.description" );
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
        LicenseRenderer r = new LicenseRenderer( getSink(), getProject(), locale );

        r.render();
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "license";
    }

    static class LicenseRenderer
        extends AbstractMavenReportRenderer
    {
        private MavenProject project;

        private Locale locale;

        public LicenseRenderer( Sink sink, MavenProject project, Locale locale )
        {
            super( sink );

            this.project = project;

            this.locale = locale;
        }

        /**
         * @see org.apache.maven.reporting.MavenReportRenderer#getTitle()
         */
        public String getTitle()
        {
            return getBundle( locale ).getString( "report.license.title" );
        }

        /**
         * @see org.apache.maven.reporting.AbstractMavenReportRenderer#renderBody()
         */
        public void renderBody()
        {
            List licenses = project.getModel().getLicenses();

            if ( licenses.isEmpty() )
            {
                startSection( getTitle() );

                paragraph( getBundle( locale ).getString( "report.license.nolicense" ) );

                endSection();

                return;
            }

            // Overview
            startSection( getBundle( locale ).getString( "report.license.overview.title" ) );

            paragraph( getBundle( locale ).getString( "report.license.overview.intro" ) );

            endSection();

            // License
            startSection( getBundle( locale ).getString( "report.license.title" ) );

            for ( Iterator i = licenses.iterator(); i.hasNext(); )
            {
                License license = (License) i.next();

                String name = license.getName();
                String url = license.getUrl();
                String distribution = license.getDistribution();
                String comments = license.getComments();

                String licenseContent = null;

                URL licenseUrl = null;
                UrlValidator urlValidator = new UrlValidator( UrlValidator.ALLOW_ALL_SCHEMES );
                if ( urlValidator.isValid( url ) )
                {
                    try
                    {
                        licenseUrl = new URL( url );
                    }
                    catch ( MalformedURLException e )
                    {
                        throw new MissingResourceException( "The license url [" + url + "] seems to be invalid: "
                            + e.getMessage(), null, null );
                    }
                }
                else
                {
                    File licenseFile = new File( project.getBasedir(), url );
                    if ( !licenseFile.exists() )
                    {
                        throw new MissingResourceException( "Maven can't find the file " + licenseFile
                            + " on the system.", null, null );
                    }
                    try
                    {
                        licenseUrl = licenseFile.toURL();
                    }
                    catch ( MalformedURLException e )
                    {
                        throw new MissingResourceException( "The license url [" + url + "] seems to be invalid: "
                            + e.getMessage(), null, null );
                    }
                }

                InputStream in = null;
                try
                {
                    in = licenseUrl.openStream();
                    // All licenses are supposed in English...
                    licenseContent = IOUtil.toString( in, "ISO-8859-1" );
                }
                catch ( IOException e )
                {
                    throw new MissingResourceException( "Can't read the url [" + url + "] : " + e.getMessage(), null,
                                                        null );
                }
                finally
                {
                    IOUtil.close( in );
                }

                startSection( name );

                if ( !StringUtils.isEmpty( comments ) )
                {
                    paragraph( comments );
                }

                verbatimText( licenseContent );

                endSection();
            }

            endSection();
        }
    }

    private static ResourceBundle getBundle( Locale locale )
    {
        return ResourceBundle.getBundle( "project-info-report", locale, LicenseReport.class.getClassLoader() );
    }
}