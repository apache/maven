package org.apache.maven.cli.logging.impl.gossip;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.PrintStream;

import com.planet57.gossip.listener.ConsoleListener;
import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.internal.CLibrary;

/**
 * Specialized {@link com.planet57.gossip.listener.Listener} which is aware of ANSI streams.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 3.4.0
 */
public class ColorConsoleListener
    extends ConsoleListener
{
    private PrintStream out;

    /**
     * Returns file descriptor identifier for the configured stream.
     */
    private int getFileno()
    {
        switch ( getStream() )
        {
            case OUT:
                return CLibrary.STDOUT_FILENO;

            case ERR:
                return CLibrary.STDERR_FILENO;

            default:
                throw new InternalError();
        }
    }

    /**
     * Returns an ANSI aware wrapped stream.
     *
     * Needed so that jansi (limited) logic to detect supported streams is applied and copes with
     * redirection of stream to file to strip out ANSI sequences.
     */
    @Override
    protected PrintStream getOut()
    {
        if ( out == null )
        {
            out = new PrintStream( AnsiConsole.wrapOutputStream( super.getOut(), getFileno() ) );
        }
        return out;
    }
}
