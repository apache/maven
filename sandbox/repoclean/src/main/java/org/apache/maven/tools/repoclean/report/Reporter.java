package org.apache.maven.tools.repoclean.report;

import org.codehaus.plexus.util.IOUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

/**
 * @author jdcasey
 */
public class Reporter
{

    private static final String WARN_LEVEL = "[WARNING] ";

    private static final String INFO_LEVEL = "[INFO] ";

    private static final String ERROR_LEVEL = "[ERROR] ";

    private File reportsFile;

    private List messages = new ArrayList();

    private boolean hasError = false;

    private boolean hasWarning = false;

    public Reporter( File reportsBase, String reportPath )
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

    public void writeReport() throws IOException
    {
        BufferedWriter writer = null;

        try
        {
            writer = new BufferedWriter( new FileWriter( reportsFile ) );

            for ( Iterator it = messages.iterator(); it.hasNext(); )
            {
                Object message = it.next();

                if ( message instanceof List )
                {
                    writer.write( format( (List) message ).toString() );
                }
                else
                {
                    writer.write( String.valueOf( message ) );
                }

                writer.newLine();
            }
        }
        finally
        {
            IOUtil.close( writer );
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
    {
        hasWarning = true;
        messages.add( new AppendingList( 2 ).append( WARN_LEVEL ).append( message ) );
    }

    public void info( String message )
    {
        messages.add( new AppendingList( 2 ).append( INFO_LEVEL ).append( message ) );
    }

    public void error( String message, Throwable error )
    {
        hasError = true;
        messages.add( new AppendingList( 3 ).append( ERROR_LEVEL ).append( message ).append( error ) );
    }

    public void error( String message )
    {
        hasError = true;
        messages.add( new AppendingList( 2 ).append( ERROR_LEVEL ).append( message ) );
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