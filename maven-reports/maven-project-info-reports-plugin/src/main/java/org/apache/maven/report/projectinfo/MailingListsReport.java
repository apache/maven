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

import org.apache.maven.model.MailingList;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.doxia.site.renderer.SiteRenderer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id: MailingListsReport.java,v 1.4 2005/02/23 00:08:03 brett Exp $
 * @goal mailing-list
 */
public class MailingListsReport
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
     * @see org.apache.maven.reporting.MavenReport#getName()
     */
    public String getName()
    {
        return "Mailing Lists";
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getCategoryName()
     */
    public String getCategoryName()
    {
        return CATEGORY_PROJECT_INFORMATION;
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getDescription()
     */
    public String getDescription()
    {
        return "This document provides subscription and archive information for this project's mailing lists.";
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
        try
        {
            MailingListsRenderer r = new MailingListsRenderer( getSink(), getConfiguration().getModel() );

            r.render();
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Can't write the report " + getOutputName(), e );
        }
    }

    /**
     * @see org.apache.maven.reporting.MavenReport#getOutputName()
     */
    public String getOutputName()
    {
        return "mail-lists";
    }

    static class MailingListsRenderer
        extends AbstractMavenReportRenderer
    {
        private Model model;

        public MailingListsRenderer( Sink sink, Model model )
        {
            super( sink );

            this.model = model;
        }

        // How to i18n these ...
        public String getTitle()
        {
            return "Project Mailing Lists";
        }

        public void renderBody()
        {
            startSection( getTitle() );

            if ( model.getMailingLists().isEmpty() )
            {
                // TODO: should the report just be excluded?
                paragraph( "There are no mailing lists currently associated with this project." );
            }
            else
            {
                paragraph( "These are the mailing lists that have been established for this project. For each list, " +
                           "there is a subscribe, unsubscribe, and an archive link." );

                startTable();

                // To beautify the display
                boolean otherArchives = false;
                for ( Iterator i = model.getMailingLists().iterator(); i.hasNext(); )
                {
                    MailingList m = (MailingList) i.next();
                    if ( ( ( m.getOtherArchives() != null ) ) && ( m.getOtherArchives().size() > 0 ) )
                    {
                        otherArchives = true;
                    }
                }

                if ( otherArchives )
                {
                    tableHeader( new String[]{"Name", "Subscribe", "Unsubscribe", "Archive", "Other Archives"} );
                }
                else
                {
                    tableHeader( new String[]{"Name", "Subscribe", "Unsubscribe", "Archive"} );
                }

                for ( Iterator i = model.getMailingLists().iterator(); i.hasNext(); )
                {
                    MailingList m = (MailingList) i.next();

                    // Verify that subsribe and unsubsribe lists are valid?
                    // Same for archive?
                    if ( ( ( m.getOtherArchives() != null ) ) && ( m.getOtherArchives().size() > 0 ) )
                    {
                        List textRow = new ArrayList();
                        List hrefRow = new ArrayList();

                        textRow.add( m.getName() );
                        hrefRow.add( null );

                        textRow.add( "Subscribe" );
                        hrefRow.add( m.getSubscribe() );

                        textRow.add( "Unsubscribe" );
                        hrefRow.add( m.getUnsubscribe() );

                        textRow.add( getArchiveServer( m.getArchive() ) );
                        hrefRow.add( m.getArchive() );
                        
                        // For the first line
                        Iterator it = m.getOtherArchives().iterator();
                        String otherArchive = (String) it.next();

                        textRow.add( getArchiveServer( otherArchive ) );
                        hrefRow.add( otherArchive );
                        tableRowWithLink( (String[]) textRow.toArray( new String[0] ),
                                          (String[]) hrefRow.toArray( new String[0] ) );

                        // Other lines...
                        while ( it.hasNext() )
                        {
                            otherArchive = (String) it.next();
                            
                            // Reinit the list to beautify the display
                            textRow = new ArrayList();
                            hrefRow = new ArrayList();

                            textRow.add( "" );
                            hrefRow.add( null );

                            textRow.add( "" );
                            hrefRow.add( null );

                            textRow.add( "" );
                            hrefRow.add( null );

                            textRow.add( "" );
                            hrefRow.add( null );

                            textRow.add( getArchiveServer( otherArchive ) );
                            hrefRow.add( otherArchive );

                            tableRowWithLink( (String[]) textRow.toArray( new String[0] ),
                                              (String[]) hrefRow.toArray( new String[0] ) );
                        }
                    }
                    else
                    {
                        if ( otherArchives )
                        {
                            List textRow = new ArrayList();
                            List hrefRow = new ArrayList();

                            textRow.add( m.getName() );
                            hrefRow.add( null );

                            textRow.add( "Subscribe" );
                            hrefRow.add( m.getSubscribe() );

                            textRow.add( "Unsubscribe" );
                            hrefRow.add( m.getUnsubscribe() );

                            textRow.add( getArchiveServer( m.getArchive() ) );
                            hrefRow.add( m.getArchive() );

                            textRow.add( "" );
                            hrefRow.add( null );

                            tableRowWithLink( (String[]) textRow.toArray( new String[0] ),
                                              (String[]) hrefRow.toArray( new String[0] ) );
                        }
                        else
                        {
                            List textRow = new ArrayList();
                            List hrefRow = new ArrayList();

                            textRow.add( m.getName() );
                            hrefRow.add( null );

                            textRow.add( "Subscribe" );
                            hrefRow.add( m.getSubscribe() );

                            textRow.add( "Unsubscribe" );
                            hrefRow.add( m.getUnsubscribe() );

                            textRow.add( getArchiveServer( m.getArchive() ) );
                            hrefRow.add( m.getArchive() );

                            tableRowWithLink( (String[]) textRow.toArray( new String[0] ),
                                              (String[]) hrefRow.toArray( new String[0] ) );
                        }
                    }
                }

                endTable();
            }
            endSection();
        }
    }

    /**
     * Convenience method to return the name of a web-based mailing list archive server.
     * <br>
     * For instance, if the archive uri is <code>http://www.mail-archive.com/dev@maven.apache.org</code>,
     * this method return <code>www.mail-archive.com</code>
     *
     * @param uri
     * @return the server name of a web-based mailing list archive server
     */
    private static String getArchiveServer( String uri )
    {
        if ( uri == null )
        {
            return "???UNKWOWN???";
        }

        int at = uri.indexOf( "//" );
        int from = uri.indexOf( "/", at >= 0 ? ( uri.lastIndexOf( "/", at - 1 ) >= 0 ? 0 : at + 2 ) : 0 );

        return uri.substring( at + 2, from );
    }
}
