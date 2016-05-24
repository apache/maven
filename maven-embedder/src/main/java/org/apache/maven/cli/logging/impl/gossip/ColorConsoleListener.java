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

import org.fusesource.jansi.AnsiConsole;

import com.planet57.gossip.Event;
import com.planet57.gossip.listener.ListenerSupport;

/**
 * Specialized {@link com.planet57.gossip.listener.Listener} which is aware of ANSI streams.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 3.4.0
 */
public class ColorConsoleListener
    extends ListenerSupport
{
    private PrintStream out;

    private PrintStream getOut()
    {
        if ( out == null )
        {
            // wrapping has logic which can detect, to some limited degree, if ansi is supported and strip if needed
            out = new PrintStream( AnsiConsole.wrapOutputStream( System.out ) );
        }
        return out;
    }

    @Override
    public void onEvent( final Event event ) throws Exception
    {
        assert event != null;

        if ( !isLoggable( event ) )
        {
            return;
        }

        PrintStream out = getOut();
        synchronized ( out )
        {
            out.print( render( event ) );
            out.flush();
        }
    }
}
