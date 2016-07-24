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

import static org.apache.maven.shared.utils.logging.MessageUtils.buffer;

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
                buff.append( buffer().debug( level.name() ) );
                break;

            case INFO:
                buff.append( buffer().info( level.name() ) );
                break;

            case WARN:
                // Maven uses WARNING instead of WARN
                buff.append( buffer().warning( WARNING ) );
                break;

            case ERROR:
                buff.append( buffer().error( level.name() ) );
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
        buff.append( buffer().success( tmp ) );
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

        buff.append( buffer().failure( cause.getClass().getName() ) );
        if ( cause.getMessage() != null )
        {
            buff.append( ": " );
            buff.append( buffer().failure( cause.getMessage() ) );
        }
        renderNewLine( buff );

        while ( cause != null )
        {
            for ( StackTraceElement e : cause.getStackTrace() )
            {
                buff.append( "    " );
                buff.append( buffer().strong( "at" ).a( " " ).a( e.getClassName() ).a( "." ).a( e.getMethodName() ) );
                buff.append( buffer().a( " (" ).strong( getLocation( e ) ).a( ")" ) );
                renderNewLine( buff );
            }

            cause = cause.getCause();
            if ( cause != null )
            {
                buff.append( buffer().strong( "Caused by" ).a( ": " ).a( cause.getClass().getName() ) );
                if ( cause.getMessage() != null )
                {
                    buff.append( ": " );
                    buff.append( buffer().failure( cause.getMessage() ) );
                }
                renderNewLine( buff );
            }
        }
    }
}
