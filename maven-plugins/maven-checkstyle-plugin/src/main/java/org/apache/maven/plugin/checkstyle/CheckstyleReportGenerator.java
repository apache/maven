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

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Map;

import org.codehaus.doxia.sink.Sink;

public class CheckstyleReportGenerator
{
    private ResourceBundle bundle;
    
    private Sink sink;
    
    private SeverityLevel severityLevel;
    
    public CheckstyleReportGenerator( Sink sink, ResourceBundle bundle )
    {
        this.bundle = bundle;
        
        this.sink = sink;
    }
    
    private String getTitle()
    {
        String title;
        
        if ( getSeverityLevel() == null )
            title = bundle.getString( "report.checkstyle.title" );
        else
            title = bundle.getString( "report.checkstyle.severity_title" ) + severityLevel.getName();
                    
        return title;
    }

    public void generateReport( Map files )
    {
        doHeading();
        
        if ( getSeverityLevel() == null ) 
        {
            doSeveritySummary( files );

            doFilesSummary( files );
        }
        
        doDetails( files );
    }
    
    private void doHeading()
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
    }
    
    private void doSeveritySummary( Map files )
    {
        sink.section1();
        sink.sectionTitle1();
        sink.text( bundle.getString( "report.checkstyle.summary" ) );
        sink.sectionTitle1_();
        
        sink.table();
        
        sink.tableRow();
        sink.tableHeaderCell();
        sink.text( bundle.getString( "report.checkstyle.files" ) );
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        sink.text( "Infos" );
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        sink.text( "Warnings" );
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        sink.text( "Errors" );
        sink.tableHeaderCell_();
        sink.tableRow_();
        
        sink.tableRow();
        sink.tableCell();
        sink.text( String.valueOf( files.size() ) );
        sink.tableCell_();
        sink.tableCell();
        sink.text( countSeverity( files.values().iterator(), SeverityLevel.INFO ) );
        sink.tableCell_();
        sink.tableCell();
        sink.text( countSeverity( files.values().iterator(), SeverityLevel.WARNING ) );
        sink.tableCell_();
        sink.tableCell();
        sink.text( countSeverity( files.values().iterator(), SeverityLevel.ERROR ) );
        sink.tableCell_();
        sink.tableRow_();

        sink.table_();
        
        sink.section1_();
    }
    
    private String countSeverity( Iterator files, SeverityLevel level )
    {
        long count = 0;
        
        while ( files.hasNext() )
        {
            List errors = (List) files.next();
            
            for( Iterator error = errors.iterator(); error.hasNext(); )
            {
                AuditEvent event = (AuditEvent) error.next();
                
                if ( event.getSeverityLevel().equals( level ) ) count++;
            }
        }
        
        return String.valueOf( count );
    }
    
    private void doFilesSummary( Map filesMap )
    {
        sink.section1();
        sink.sectionTitle1();
        sink.text( bundle.getString( "report.checkstyle.files" ) );
        sink.sectionTitle1_();
        
        sink.table();
        
        sink.tableRow();
        sink.tableHeaderCell();
        sink.text( bundle.getString( "report.checkstyle.files" ) );
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        sink.text( "I" );
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        sink.text( "W" );
        sink.tableHeaderCell_();
        sink.tableHeaderCell();
        sink.text( "E" );
        sink.tableHeaderCell_();
        sink.tableRow_();
        
        for( Iterator files = filesMap.keySet().iterator(); files.hasNext(); )
        {
            String filename = (String) files.next();
            List errors = (List) filesMap.get( filename );
            
            sink.tableRow();
            
            sink.tableCell();
            sink.link( "#" + filename.replace( '/', '.' ) );
            sink.text( filename );
            sink.link_();
            sink.tableCell_();
            
            sink.tableCell();
            sink.text( countSeverity( Collections.singletonList( errors ).iterator(), SeverityLevel.INFO ) );
            sink.tableCell_();
            
            sink.tableCell();
            sink.text( countSeverity( Collections.singletonList( errors ).iterator(), SeverityLevel.WARNING ) );
            sink.tableCell_();
            
            sink.tableCell();
            sink.text( countSeverity( Collections.singletonList( errors ).iterator(), SeverityLevel.ERROR ) );
            sink.tableCell_();
            
            sink.tableRow_();
        }
        
        sink.table_();
        sink.section1_();
    }
    
    private void doDetails( Map filesMap )
    {
        Iterator files = filesMap.keySet().iterator();
        
        while ( files.hasNext() )
        {
            String file = (String) files.next();
            List eventList = (List) filesMap.get( file );
            
            sink.section1();
            sink.sectionTitle1();
            sink.anchor( file.replace( '/', '.' ) );
            sink.text( file );
            sink.anchor_();
            sink.sectionTitle1_();
            
            sink.table();
            sink.tableRow();
            sink.tableHeaderCell();
            sink.text( bundle.getString( "report.checkstyle.column.violation" ) );
            sink.tableHeaderCell_();
            sink.tableHeaderCell();
            sink.text( "Message" );
            sink.tableHeaderCell_();
            sink.tableHeaderCell();
            sink.text( "Line" );
            sink.tableHeaderCell_();
            sink.tableRow_();

            doFileEvents( eventList);
            
            sink.table_();
            sink.section1_();
        }
    }
    
    private void doFileEvents( List eventList )
    {
        Iterator events = eventList.iterator();
        while ( events.hasNext() )
        {
            AuditEvent event = (AuditEvent) events.next();
            SeverityLevel level = event.getSeverityLevel();
            
            if ( getSeverityLevel() != null )
                if ( !getSeverityLevel().equals( level ) ) continue;

            sink.tableRow();
            
            sink.tableCell();
            sink.figure();
            sink.figureCaption();
            sink.text( level.getName() );
            sink.figureCaption_();
            
            if ( SeverityLevel.INFO.equals( level ) )
                sink.figureGraphics( "images/icon_info_sml.gif" );
            else if ( SeverityLevel.WARNING.equals( level ) )
                sink.figureGraphics( "images/icon_warning_sml.gif" );
            else if ( SeverityLevel.ERROR.equals( level ) )
                sink.figureGraphics( "images/icon_error_sml.gif" );

            sink.figure_();
            sink.tableCell_();
            
            sink.tableCell();
            sink.text( event.getMessage() );
            sink.tableCell_();
            
            sink.tableCell();
            sink.text( String.valueOf( event.getLine() ) );
            sink.tableCell_();

            sink.tableRow_();
        }
    }
    
    private Iterator getCheckstyleEvents( Iterator events, SeverityLevel level )
    {
        LinkedList filtered = new LinkedList();
        
        while( events.hasNext() )
        {
            AuditEvent event = (AuditEvent) events.next();
            
            if ( event.getSeverityLevel().equals( level ) ) filtered.add( event );
        }
        
        return filtered.iterator();
    }

    public SeverityLevel getSeverityLevel()
    {
        return severityLevel;
    }

    public void setSeverityLevel(SeverityLevel severityLevel)
    {
        this.severityLevel = severityLevel;
    }
}
