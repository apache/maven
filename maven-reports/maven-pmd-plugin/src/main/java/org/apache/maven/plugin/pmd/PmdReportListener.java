package org.apache.maven.plugin.pmd;

/*
 * Copyright 2005 The Apache Software Foundation.
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

import net.sourceforge.pmd.ReportListener;
import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.stat.Metric;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;

/**
 * Handle events from PMD, converting them into Doxia events.
 *
 * @author Brett Porter
 * @version $Id: PmdReportListener.java,v 1.1.1.1 2005/02/17 07:16:22 brett Exp $
 */
public class PmdReportListener
    implements ReportListener
{
    private static final String TITLE = "PMD Results";

    private Sink sink;

    private String sourceDirectory;

    private String currentFilename;

    private boolean fileInitialized;

    public PmdReportListener( Sink sink, String sourceDirectory )
    {
        this.sink = sink;
        this.sourceDirectory = sourceDirectory;
    }

    public void ruleViolationAdded( RuleViolation ruleViolation )
    {
        if ( ! fileInitialized )
        {
            sink.section2();
            sink.sectionTitle2();
            sink.text( currentFilename );
            sink.sectionTitle2_();

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
        sink.text( ruleViolation.getDescription() );
        sink.tableCell_();
        sink.tableCell();
        // TODO: xref link the line number
        sink.text( String.valueOf( ruleViolation.getLine() ) );
        sink.tableCell_();
        sink.tableRow_();
    }

    public void metricAdded( Metric metric )
    {
        // TODO: metrics
    }

    public void beginDocument()
    {
        sink.head();
        sink.title();
        sink.text( TITLE );
        sink.title_();
        sink.head_();

        sink.body();

        sink.section1();
        sink.sectionTitle1();
        sink.text( TITLE );
        sink.sectionTitle1_();

        sink.paragraph();
        sink.text( "The following document contains the results of " );
        sink.link( "http://pmd.sourceforge.net/" );
        sink.text( "PMD" );
        sink.link_();
        sink.paragraph_();

        // TODO overall summary

        sink.section1_();
        sink.sectionTitle1();
        sink.text( "Files" );
        sink.sectionTitle1_();

        // TODO files summary
    }

    public void beginFile( File file )
    {
        currentFilename = StringUtils.substring( file.getAbsolutePath(), sourceDirectory.length() + 1 );
        currentFilename = StringUtils.replace( currentFilename, "\\", "/" );
        fileInitialized = false;
    }

    public void endFile( File file )
    {
        if ( fileInitialized )
        {
            sink.table_();
            sink.section2_();
        }
    }

    public void endDocument()
    {
        sink.section1_();

        sink.body_();

        sink.flush();

        sink.close();
    }
}
