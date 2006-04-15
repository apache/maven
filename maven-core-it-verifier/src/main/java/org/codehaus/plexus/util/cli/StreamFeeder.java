package org.codehaus.plexus.util.cli;

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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class StreamFeeder
    extends Thread
{
    private InputStream input;

    private OutputStream output;

    private boolean done;

    public StreamFeeder( InputStream input, OutputStream output )
    {
        this.input = input;

        this.output = output;
    }

    // ----------------------------------------------------------------------
    // Runnable implementation
    // ----------------------------------------------------------------------

    public void run()
    {
        try
        {
            feed();
        }
        catch ( Throwable ex )
        {
            // Catched everything so the streams will be closed and flagged as done.
        }
        finally
        {
            close();

            done = true;

            synchronized ( this )
            {
                this.notifyAll();
            }
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    public void close()
    {
        if ( input != null )
        {
            synchronized ( input )
            {
                try
                {
                    input.close();
                }
                catch ( IOException ex )
                {
                    // ignore
                }

                input = null;
            }
        }

        if ( output != null )
        {
            synchronized ( output )
            {
                try
                {
                    output.close();
                }
                catch ( IOException ex )
                {
                    // ignore
                }

                output = null;
            }
        }
    }

    public boolean isDone()
    {
        return done;
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    private void feed()
        throws IOException
    {
        int data = input.read();

        while ( !done && data != -1 )
        {
            synchronized ( output )
            {
                output.write( data );

                data = input.read();
            }
        }
    }
}
