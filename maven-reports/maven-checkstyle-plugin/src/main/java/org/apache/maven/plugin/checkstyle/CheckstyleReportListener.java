package org.apache.maven.plugin.checkstyle;

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

import org.codehaus.doxia.sink.Sink;
import org.codehaus.plexus.util.StringUtils;

import java.util.ResourceBundle;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.AutomaticBean;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;

/**
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id: DependenciesReport.java,v 1.2 2005/02/23 00:08:02 brett Exp $
 */
public class CheckstyleReportListener
    extends AutomaticBean
    implements AuditListener
{
    private Sink sink;

    private String sourceDirectory;

    private String currentFilename;

    private boolean fileInitialized;

    private ResourceBundle bundle;

    public CheckstyleReportListener( Sink sink, String sourceDirectory, ResourceBundle bundle )
    {
        this.sink = sink;
        this.sourceDirectory = sourceDirectory;
        this.bundle = bundle;
    }

    private String getTitle()
    {
        return bundle.getString( "report.checkstyle.title" );
    }

    public void auditStarted( AuditEvent event )
    {
        sink.head();
        sink.title();
        sink.text( getTitle() );
        sink.title_();
        sink.head_();

        sink.body();

        sink.section1();
        sink.sectionTitle1();
        sink.text( getTitle() );
        sink.sectionTitle1_();

        sink.paragraph();
        sink.text( bundle.getString( "report.checkstyle.checkstylelink" ) + " " );
        sink.link( "http://checkstyle.sourceforge.net/" );
        sink.text( "Checkstyle" );
        sink.link_();
        sink.paragraph_();

        // TODO overall summary

        sink.section1_();
        sink.sectionTitle1();
        sink.text( bundle.getString( "report.checkstyle.files" ) );
        sink.sectionTitle1_();

        // TODO files summary
    }

    public void auditFinished( AuditEvent event )
    {
        sink.section1_();
        sink.body_();
        sink.flush();
        sink.close();
    }

    public void fileStarted( AuditEvent event )
    {
        currentFilename = StringUtils.substring( event.getFileName(), sourceDirectory.length() + 1 );
        currentFilename = StringUtils.replace( currentFilename, "\\", "/" );
        fileInitialized = false;
    }

    public void fileFinished( AuditEvent event )
    {
        if ( fileInitialized )
        {
            sink.table_();
            sink.section2_();
        }
    }

    public void addError( AuditEvent event )
    {
        if ( !SeverityLevel.IGNORE.equals( event.getSeverityLevel() ) )
        {
            if ( !fileInitialized )
            {
                sink.section2();
                sink.sectionTitle2();
                sink.text( currentFilename );
                sink.sectionTitle2_();

                sink.table();
                sink.tableRow();
                sink.tableHeaderCell();
                sink.text( bundle.getString( "report.checkstyle.column.violation" ) );
                sink.tableHeaderCell_();
                sink.tableHeaderCell();
                sink.text( bundle.getString( "report.checkstyle.column.line" ) );
                sink.tableHeaderCell_();
                sink.tableRow_();

                fileInitialized = true;
            }

            sink.tableRow();
            sink.tableCell();
            sink.text( event.getMessage() );
            sink.tableCell_();
            sink.tableCell();
            sink.text( String.valueOf( event.getLine() ) );
            sink.tableCell_();
            sink.tableRow_();
        }
    }

    public void addException( AuditEvent event, Throwable throwable )
    {
        //Do Nothing
    }
}

