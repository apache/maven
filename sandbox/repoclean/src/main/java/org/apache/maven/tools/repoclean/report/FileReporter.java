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

import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * @author jdcasey
 */
public class FileReporter
    extends AbstractReporter
{
    private File reportsFile;

    private Writer writer;

    public FileReporter( File reportsBase, String reportPath, boolean warningsEnabled )
    {
        super( warningsEnabled );

        this.reportsFile = new File( reportsBase, reportPath );

        File parentDir = reportsFile.getParentFile();
        if ( !parentDir.exists() )
        {
            parentDir.mkdirs();
        }

        if ( !parentDir.isDirectory() )
        {
            throw new IllegalArgumentException( "path: \'" + parentDir.getAbsolutePath() +
                "\' refers to a file, not a directory.\n" + "Cannot write report file: \'" +
                reportsFile.getAbsolutePath() + "\'." );
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

    protected void write( String message )
        throws ReportWriteException
    {
        try
        {
            if ( writer == null )
            {
                open();
            }

            writer.write( message );

            writer.write( '\n' );

            writer.flush();
        }
        catch ( IOException e )
        {
            throw new ReportWriteException( "Cannot write message: " + message + " due to an I/O error.", e );
        }
    }

}