package org.apache.maven.it.cli;

/*
 * The MIT License
 *
 * Copyright (c) 2004, The Codehaus
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
 * Chicago, IL 60661 USA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     + Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     + Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
 *       names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/

/* ====================================================================
 * Copyright 2003-2004 The Apache Software Foundation.
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
 * ====================================================================
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;

/**
 * Class to pump the error stream during Process's runtime. Copied from the Ant
 * built-in task.
 *
 * @author <a href="mailto:fvancea@maxiq.com">Florin Vancea </a>
 * @author <a href="mailto:pj@thoughtworks.com">Paul Julius </a>
 * @since June 11, 2001
 */
public class StreamPumper
    extends Thread
{
    private BufferedReader in;

    private StreamConsumer consumer = null;

    private PrintWriter out = null;

    private static final int SIZE = 1024;

    boolean done;

    public StreamPumper( InputStream in )
    {
        this.in = new BufferedReader( new InputStreamReader( in ), SIZE );
    }

    public StreamPumper( InputStream in, StreamConsumer consumer )
    {
        this( in );

        this.consumer = consumer;
    }

    public StreamPumper( InputStream in, PrintWriter writer )
    {
        this( in );

        out = writer;
    }

    public StreamPumper( InputStream in, PrintWriter writer, StreamConsumer consumer )
    {
        this( in );
        this.out = writer;
        this.consumer = consumer;
    }

    public void run()
    {
        try
        {
            String s = in.readLine();

            while ( s != null )
            {
                consumeLine( s );

                if ( out != null )
                {
                    out.println( s );

                    out.flush();
                }

                s = in.readLine();
            }
        }
        catch ( Throwable e )
        {
            // Catched everything so the streams will be closed and flagged as done.
        }
        finally
        {
            closeInputStream( in );

            done = true;

            synchronized ( this )
            {
                this.notifyAll();
            }
        }
    }

    public void flush()
    {
        if ( out != null )
        {
            out.flush();
        }
    }

    public void close()
    {
        closeOutputStream( out );
    }

    protected void closeInputStream( Reader in )
    {
        if ( in == null )
        {
            return;
        }

        try
        {
            in.close();
        }
        catch ( IOException e )
        {
            // do nothing
        }
    }

    protected void closeOutputStream( PrintWriter out )
    {
        if ( out == null )
        {
            return;
        }

        out.close();
    }


    public boolean isDone()
    {
        return done;
    }

    private void consumeLine( String line )
    {
        if ( consumer != null )
        {
            consumer.consumeLine( line );
        }
    }
}
