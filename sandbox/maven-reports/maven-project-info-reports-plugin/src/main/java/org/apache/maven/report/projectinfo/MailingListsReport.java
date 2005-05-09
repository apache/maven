package org.apache.maven.reports.projectinfo;

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
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;

import java.io.IOException;
import java.util.Iterator;

/**
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id: MailingListsReport.java,v 1.4 2005/02/23 00:08:03 brett Exp $
 */
public class MailingListsReport
    extends AbstractMavenReport
{
    public String getName()
    {
        return "Mailing Lists";
    }

    public String getCategoryName()
    {
        return CATEGORY_PROJECT_INFORMATION;
    }

    public String getDescription()
    {
        return "This document provides subscription and archive information for this project's mailing lists.";
    }

    public void execute()
        throws MavenReportException
    {
        try
        {
            MailingListsRenderer r = new MailingListsRenderer( getSink(), getConfiguration().getModel() );

            r.render();
        }
        catch( IOException e )
        {
            throw new MavenReportException( "Can't write the report " + getOutputName(), e );
        }
    }

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

                tableHeader( new String[]{"Name", "Subscribe", "Unsubscribe", "Archive"} );

                for ( Iterator i = model.getMailingLists().iterator(); i.hasNext(); )
                {
                    MailingList m = (MailingList) i.next();

                    // TODO: render otherArchives?
                    tableRow( new String[]{m.getName(), m.getSubscribe(), m.getUnsubscribe(), m.getArchive()} );
                }

                endTable();
            }
            endSection();
        }

    }
}
