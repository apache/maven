package org.apache.maven.tools.repoclean.report;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author jdcasey
 */
public class FileReporter
    implements Reporter
{

    private static final String WARN_LEVEL = "[WARNING] ";

    private static final String ERROR_LEVEL = "[ERROR] ";

    private File reportsFile;

    private List messages = new ArrayList();

    private boolean hasError = false;

    private boolean hasWarning = false;

    private Writer writer;

    public FileReporter( File reportsBase, String reportPath )
    {
        this.reportsFile = new File( reportsBase, reportPath );

        File parentDir = reportsFile.getParentFile();
        if ( !parentDir.exists() )
        {
            parentDir.mkdirs();
        }

        if ( !parentDir.isDirectory() )
        {
            throw new IllegalArgumentException( "path: \'" + parentDir.getAbsolutePath()
                + "\' refers to a file, not a directory.\n" + "Cannot write report file: \'"
                + reportsFile.getAbsolutePath() + "\'." );
        }
    }

    public File getReportFile()
    {
        return reportsFile;
    }

    private void open()
        throws IOException
    {
        this.writer = new FileWriter( reportsFile );
    }

    public void close()
    {
        IOUtil.close( writer );
    }

    private void write( Object message )
        throws ReportWriteException
    {
        try
        {
            if ( writer == null )
            {
                open();
            }

            if ( message instanceof List )
            {
                writer.write( format( (List) message ).toString() );
            }
            else
            {
                writer.write( String.valueOf( message ) );
            }

            writer.write( '\n' );

            writer.flush();
        }
        catch ( IOException e )
        {
            throw new ReportWriteException( "Cannot write message: " + message + " due to an I/O error.", e );
        }
    }

    public boolean hasWarning()
    {
        return hasWarning;
    }

    public boolean hasError()
    {
        return hasError;
    }

    public void warn( String message )
        throws ReportWriteException
    {
        hasWarning = true;
        write( new AppendingList( 2 ).append( WARN_LEVEL ).append( message ) );
    }

    public void error( String message, Throwable error )
        throws ReportWriteException
    {
        hasError = true;
        write( new AppendingList( 3 ).append( ERROR_LEVEL ).append( message ).append( error ) );
    }

    public void error( String message )
        throws ReportWriteException
    {
        hasError = true;
        write( new AppendingList( 2 ).append( ERROR_LEVEL ).append( message ) );
    }

    private CharSequence format( List messageParts )
    {
        StringBuffer buffer = new StringBuffer();
        for ( Iterator it = messageParts.iterator(); it.hasNext(); )
        {
            Object part = it.next();
            if ( part instanceof Throwable )
            {
                part = formatThrowable( (Throwable) part );
            }

            buffer.append( part );
        }

        return buffer;
    }

    private String formatThrowable( Throwable throwable )
    {
        StringWriter sWriter = new StringWriter();
        PrintWriter pWriter = new PrintWriter( sWriter );

        throwable.printStackTrace( pWriter );

        return sWriter.toString();
    }

    private static class AppendingList
        extends ArrayList
    {
        public AppendingList()
        {
        }

        public AppendingList( int size )
        {
            super( size );
        }

        public AppendingList append( Object item )
        {
            super.add( item );
            return this;
        }
    }

}