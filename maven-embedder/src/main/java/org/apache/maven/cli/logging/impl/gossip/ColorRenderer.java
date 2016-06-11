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

import static org.fusesource.jansi.Ansi.ansi;

import com.planet57.gossip.Event;
import com.planet57.gossip.Level;

/**
 * Specialized {@link com.planet57.gossip.render.Renderer} which colorizes level and error rendering.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 3.4.0
 */
public class ColorRenderer
extends com.planet57.gossip.render.PatternRenderer
{
    protected static final String WARNING = "WARNING";

    @Override
    protected void renderLevel( final Event event, final StringBuilder buff )
    {
        assert event != null;
        assert buff != null;

        Level level = event.getLevel();
        switch ( level )
        {
            case TRACE:
            case DEBUG:
                buff.append( ansi().bold().fgCyan().a( level.name() ).reset() );
                break;

            case INFO:
                buff.append( ansi().bold().fgGreen().a( level.name() ).reset() );
                break;

            case WARN:
                // Maven uses WARNING instead of WARN
                buff.append( ansi().bold().fgYellow().a( WARNING ).reset() );
                break;

            case ERROR:
                buff.append( ansi().bold().fgRed().a( level.name() ).reset() );
                break;

            default:
                throw new InternalError();
        }
    }

    @Override
    protected void renderName( final Event event, final StringBuilder buff, final boolean shortName )
    {
        StringBuilder tmp = new StringBuilder();
        super.renderName( event, tmp, shortName );
        buff.append( ansi().fgGreen().a( tmp ).reset() );
    }


    @Override
    protected void renderCause( final Event event, final StringBuilder buff )
    {
        assert event != null;
        assert buff != null;

        Throwable cause = event.getCause();
        if ( cause == null )
        {
            return;
        }

        buff.append( ansi().bold().fgRed().a( cause.getClass().getName() ).reset() );
        if ( cause.getMessage() != null )
        {
            buff.append( ": " );
            buff.append( ansi().bold().fgRed().a( cause.getMessage() ).reset() );
        }
        renderNewLine( buff );

        while ( cause != null )
        {
            for ( StackTraceElement e : cause.getStackTrace() )
            {
                buff.append( "    " );
                buff.append( ansi().bold().a( "at" ).reset().a( " " )
                        .a( e.getClassName() ).a( "." ).a( e.getMethodName() ) );
                buff.append( ansi().a( " (" ).bold().a( getLocation( e ) ).reset().a( ")" ) );
                renderNewLine( buff );
            }

            cause = cause.getCause();
            if ( cause != null )
            {
                buff.append( ansi().bold().a( "Caused by" ).reset().a( ": " )
                        .a( cause.getClass().getName() ) );
                if ( cause.getMessage() != null )
                {
                    buff.append( ": " );
                    buff.append( ansi().bold().fgRed().a( cause.getMessage() ).reset() );
                }
                renderNewLine( buff );
            }
        }
    }
}
