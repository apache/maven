package org.apache.maven.tools.repoclean.report;

import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

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

public class PathLister
{
    
    private final File listFile;
    
    private Writer writer;

    public PathLister( File listFile )
    {
        this.listFile = listFile;
    }
    
    private synchronized void checkOpen() throws ReportWriteException
    {
        if(writer == null)
        {
            try
            {
                writer = new FileWriter(listFile);
            }
            catch ( IOException e )
            {
                throw new ReportWriteException( "Cannot open listFile for writing: " + listFile, e );
            }
        }
    }
    
    public void close()
    {
        IOUtil.close( writer );
    }
    
    public void addPath( String path ) throws ReportWriteException
    {
        checkOpen();
        
        try
        {
            writer.write( path + "\n" );
        }
        catch ( IOException e )
        {
            throw new ReportWriteException( "Cannot write path: " + path + " to listFile: " + listFile, e );
        }
    }

    public void addPath( File path ) throws ReportWriteException
    {
        checkOpen();
        
        try
        {
            writer.write( path + "\n" );
        }
        catch ( IOException e )
        {
            throw new ReportWriteException( "Cannot write path: " + path + " to listFile: " + listFile, e );
        }
    }

}
