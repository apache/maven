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

import static org.fusesource.jansi.Ansi.Attribute.INTENSITY_BOLD;
import static org.fusesource.jansi.Ansi.Color.CYAN;
import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.Color.YELLOW;
import static org.fusesource.jansi.Ansi.ansi;

import org.sonatype.gossip.Event;
import org.sonatype.gossip.render.PatternRenderer;

/**
 * Specialized {@link org.sonatype.gossip.render.Renderer} which is aware of basic Maven log messages to colorize.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 3.4.0
 */
public class ColorRenderer
    extends PatternRenderer
{
    @Override
    protected void renderLevel( final Event event, final StringBuilder buff )
    {
        assert event != null;
        assert buff != null;

        switch ( event.getLevel() )
        {
            case TRACE:
            case DEBUG:
                buff.append( ansi().a( INTENSITY_BOLD ).fg( YELLOW ).a( event.getLevel().name() ).reset() );
                break;

            case INFO:
                buff.append( ansi().a( INTENSITY_BOLD ).fg( GREEN ).a( event.getLevel().name() ).reset() );
                break;

            case WARN:
            case ERROR:
                buff.append( ansi().a( INTENSITY_BOLD ).fg( RED ).a( event.getLevel().name() ).reset() );
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
        buff.append( ansi().fg( GREEN ).a( tmp ).reset() );
    }

    @Override
    protected void renderMessage( final Event event, final StringBuilder buff )
    {
        final String message = event.getMessage();

        if ( message.startsWith( "------------------------------------------------------------------------" ) )
        {
            buff.append( ansi().a( INTENSITY_BOLD ).a( message ).reset() );
        }
        else if ( message.startsWith( "---" ) && message.endsWith( " ---" ) )
        {
            String[] items = message.split( "\\s" );
            buff.append( ansi().a( INTENSITY_BOLD ).a( "---" ).reset() );
            buff.append( " " );
            buff.append( ansi().fg( GREEN ).a( items[1] ).reset() );
            buff.append( " " );
            buff.append( ansi().a( INTENSITY_BOLD ).a( items[2] ).reset() );
            buff.append( " @ " );
            buff.append( ansi().fg( CYAN ).a( items[4] ).reset() );
            buff.append( ansi().a( INTENSITY_BOLD ).a( " ---" ).reset() );
        }
        else if ( message.contains( "ERROR" ) || message.contains( "FAILURE" ) || message.contains( "FAILED" ) )
        {
            buff.append( ansi().a( INTENSITY_BOLD ).fg( RED ).a( message ).reset() );
        }
        else if ( message.contains( "BUILD SUCCESS" ) )
        {
            buff.append( ansi().a( INTENSITY_BOLD ).fg( GREEN ).a( message ).reset() );
        }
        else if ( message.contains( "SUCCESS" ) )
        {
            buff.append( ansi().fg( GREEN ).a( message ).reset() );
        }
        else
        {
            buff.append( message );
        }
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

        buff.append( ansi().a( INTENSITY_BOLD ).fg( RED ).a( cause.getClass().getName() ).reset() );
        if ( cause.getMessage() != null )
        {
            buff.append( ": " );
            buff.append( ansi().a( INTENSITY_BOLD ).fg( RED ).a( cause.getMessage() ).reset() );
        }
        renderNewLine( buff );

        while ( cause != null )
        {
            for ( StackTraceElement e : cause.getStackTrace() )
            {
                buff.append( "    " );
                buff.append( ansi().a( INTENSITY_BOLD ).a( "at" ).reset().a( " " )
                        .a( e.getClassName() ).a( "." ).a( e.getMethodName() ) );
                buff.append( ansi().a( " (" ).a( INTENSITY_BOLD ).a( getLocation( e ) ).reset().a( ")" ) );
                renderNewLine( buff );
            }

            cause = cause.getCause();
            if ( cause != null )
            {
                buff.append( ansi().a( INTENSITY_BOLD ).a( "Caused by" ).reset().a( " " )
                        .a( cause.getClass().getName() ) );
                if ( cause.getMessage() != null )
                {
                    buff.append( ": " );
                    buff.append( ansi().a( INTENSITY_BOLD ).fg( RED ).a( cause.getMessage() ).reset() );
                }
                renderNewLine( buff );
            }
        }
    }
}
