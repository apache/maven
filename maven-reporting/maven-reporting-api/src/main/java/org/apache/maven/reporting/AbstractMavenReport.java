package org.apache.maven.reporting;

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

import org.codehaus.doxia.sink.Sink;
import org.codehaus.doxia.module.xdoc.XdocSink;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public abstract class AbstractMavenReport
    implements MavenReport
{
    private MavenReportConfiguration config;

    public MavenReportConfiguration getConfiguration()
    {
        return config;
    }

    public void setConfiguration( MavenReportConfiguration config )
    {
        this.config = config;
    }

    public void generate()
        throws MavenReportException
    {
        if ( config == null )
        {
            throw new MavenReportException( "You must specify a report configuration." );
        }

        execute();
    }

    protected abstract void execute()
        throws MavenReportException;

    public Sink getSink()
        throws IOException
    {
        return getSink( getOutputName() );
    }

    public Sink getSink( String outputName )
        throws IOException
    {
        FileWriter writer = new FileWriter( new File( config.getOutputDirectory(), "xdoc/" + outputName + ".xml" ) );

        return new XdocSink( writer );
    }
}