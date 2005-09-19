package org.apache.maven.tools.repoclean.report;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Base implementation of reporter.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public abstract class AbstractReporter
    implements Reporter
{
    protected static final String WARN_LEVEL = "[WARNING]";

    protected static final String ERROR_LEVEL = "[ERROR]";

    protected boolean hasError;

    protected boolean hasWarning;

    protected final boolean warningsEnabled;

    protected AbstractReporter( boolean warningsEnabled )
    {
        this.warningsEnabled = warningsEnabled;
    }

    public boolean hasWarning()
    {
        return hasWarning;
    }

    public boolean hasError()
    {
        return hasError;
    }

    protected String getSourceLine()
    {
        NullPointerException npe = new NullPointerException();

        StackTraceElement element = npe.getStackTrace()[2];

        return "Reported from: (" + element.getClassName() + "." + element.getMethodName() + "(..):" +
            element.getLineNumber() + ")\n";
    }

    protected String format( String level, String source, String message )
    {
        return format( level, source, message, null );
    }

    protected String format( String level, String source, String message, Throwable error )
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append( level );
        buffer.append( " " );
        buffer.append( source );
        buffer.append( " " );
        buffer.append( message );
        if ( error != null )
        {
            buffer.append( formatThrowable( error ) );
        }
        return buffer.toString();
    }

    private String formatThrowable( Throwable throwable )
    {
        StringWriter sWriter = new StringWriter();
        PrintWriter pWriter = new PrintWriter( sWriter );

        throwable.printStackTrace( pWriter );

        return sWriter.toString();
    }

    protected abstract void write( String message )
        throws ReportWriteException;

    public void warn( String message )
        throws ReportWriteException
    {
        if ( warningsEnabled )
        {
            hasWarning = true;
            String source = getSourceLine();
            write( format( WARN_LEVEL, source, message ) );
        }
    }

    public void error( String message, Throwable error )
        throws ReportWriteException
    {
        hasError = true;
        String source = getSourceLine();
        write( format( ERROR_LEVEL, source, message, error ) );
    }

    public void error( String message )
        throws ReportWriteException
    {
        hasError = true;
        String source = getSourceLine();
        write( format( ERROR_LEVEL, source, message ) );
    }
}
