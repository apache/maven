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

import net.sourceforge.pmd.PMD;
import net.sourceforge.pmd.PMDException;
import net.sourceforge.pmd.Report;
import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.RuleSet;
import net.sourceforge.pmd.RuleSetFactory;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

/**
 * Implement the PMD report.
 *
 * @author Brett Porter
 * @version $Id: PmdReport.java,v 1.3 2005/02/23 00:08:53 brett Exp $
 */
public class PmdReport
    extends AbstractMavenReport
{
    protected static final String[] DEFAULT_EXCLUDES = {// Miscellaneous typical temporary files
        "**/*~", "**/#*#", "**/.#*", "**/%*%", "**/._*",

        // CVS
        "**/CVS", "**/CVS/**", "**/.cvsignore",

        // SCCS
        "**/SCCS", "**/SCCS/**",

        // Visual SourceSafe
        "**/vssver.scc",

        // Subversion
        "**/.svn", "**/.svn/**",

        // Mac
        "**/.DS_Store"};

    public void execute()
        throws MavenReportException
    {
        Sink sink = null;
        try
        {
            sink = getSink();
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Can't obtain sink for PMD report.", e );
        }

        PMD pmd = new PMD();
        RuleContext ruleContext = new RuleContext();
        Report report = new Report();
        PmdReportListener reportSink = new PmdReportListener( sink );
        report.addListener( reportSink );
        ruleContext.setReport( report );

        RuleSetFactory ruleSetFactory = new RuleSetFactory();
        InputStream rulesInput = pmd.getClass().getResourceAsStream( "/rulesets/controversial.xml" );
        RuleSet ruleSet = ruleSetFactory.createRuleSet( rulesInput );

        reportSink.beginDocument();

        List files;
        try
        {
            files = getFilesToProcess( "**/*.java", null );
        }
        catch ( IOException e )
        {
            throw new MavenReportException( "Can't parse " + getConfiguration().getSourceDirectory(), e );
        }

        for ( Iterator i = files.iterator(); i.hasNext(); )
        {
            File file = (File) i.next();
            FileReader fileReader;
            try
            {
                fileReader = new FileReader( file );
            }
            catch ( FileNotFoundException e )
            {
                throw new MavenReportException( "Error opening source file: " + file, e );
            }

            try
            {
                // TODO: lazily call beginFile in case there are no rules

                reportSink.beginFile( file );
                pmd.processFile( fileReader, ruleSet, ruleContext );
                reportSink.endFile( file );
            }
            catch ( PMDException e )
            {
                Exception ex = e;
                if ( e.getReason() != null )
                {
                    ex = e.getReason();
                }
                throw new MavenReportException( "Failure executing PMD for: " + file, ex );
            }
            finally
            {
                try
                {
                    fileReader.close();
                }
                catch ( IOException e )
                {
                    throw new MavenReportException( "Error closing source file: " + file, e );
                }
            }
        }
        reportSink.endDocument();
    }

    public String getOutputName()
    {
        return "pmd";
    }

    private List getFilesToProcess( String includes, String excludes )
        throws IOException
    {
        StringBuffer excludesStr = new StringBuffer();
        if ( StringUtils.isNotEmpty( excludes ) )
        {
            excludesStr.append( excludes );
        }
        for ( int i = 0; i < DEFAULT_EXCLUDES.length; i++ )
        {
            if ( excludesStr.length() > 0 )
            {
                excludesStr.append( "," );
            }
            excludesStr.append( DEFAULT_EXCLUDES[i] );
        }

        return FileUtils.getFiles( new File( getConfiguration().getSourceDirectory() ), includes,
                                   excludesStr.toString() );
    }
}
