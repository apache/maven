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
    private static final String TITLE = "Checkstyle Results";

    private Sink sink;

    private String sourceDirectory;

    private String currentFilename;

    private boolean fileInitialized;

    public CheckstyleReportListener( Sink sink, String sourceDirectory )
    {
        this.sink = sink;
        this.sourceDirectory = sourceDirectory;
    }

    public void auditStarted( AuditEvent event )
    {
        sink.head();
        sink.title();
        sink.text( TITLE );
        sink.title_();
        sink.head_();

        sink.body();

        sink.section1();
        sink.sectionTitle();
        sink.text( TITLE );
        sink.sectionTitle_();

        sink.paragraph();
        sink.text( "The following document contains the results of " );
        sink.link( "http://checkstyle.sourceforge.net/" );
        sink.text( "Checkstyle" );
        sink.link_();
        sink.paragraph_();

        // TODO overall summary

        sink.section1_();
        sink.sectionTitle();
        sink.text( "Files" );
        sink.sectionTitle_();

        // TODO files summary
    }

    public void auditFinished( AuditEvent event )
    {
        sink.section1_();
        sink.body_();
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
                sink.sectionTitle();
                sink.text( currentFilename );
                sink.sectionTitle_();

                sink.table();
                sink.tableRow();
                sink.tableHeaderCell();
                sink.text( "Violation" );
                sink.tableHeaderCell_();
                sink.tableHeaderCell();
                sink.text( "Line" );
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

