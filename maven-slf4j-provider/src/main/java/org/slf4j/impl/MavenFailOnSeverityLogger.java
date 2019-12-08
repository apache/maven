package org.slf4j.impl;

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

import org.apache.maven.logwrapper.LogLevelRecorder;
import org.slf4j.event.Level;

/**
 * A proxy which enhances the MavenSimpleLogger with functionality to track whether a logging threshold is hit.
 * Currently only support WARN and ERROR states, since it's been used for the --fail-on-severity flag.
 */
public class MavenFailOnSeverityLogger extends MavenSimpleLogger
{
    private final LogLevelRecorder logLevelRecorder;

    MavenFailOnSeverityLogger( String name, LogLevelRecorder logLevelRecorder )
    {
        super( name );
        this.logLevelRecorder = logLevelRecorder;
    }

    /**
     * A simple implementation which always logs messages of level WARN
     * according to the format outlined above.
     */
    @Override
    public void warn( String msg )
    {
        super.warn( msg );
        logLevelRecorder.record( Level.WARN );
    }

    /**
     * Perform single parameter substitution before logging the message of level
     * WARN according to the format outlined above.
     */
    @Override
    public void warn( String format, Object arg )
    {
        super.warn( format, arg );
        logLevelRecorder.record( Level.WARN );
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * WARN according to the format outlined above.
     */
    @Override
    public void warn( String format, Object arg1, Object arg2 )
    {
        super.warn( format, arg1, arg2 );
        logLevelRecorder.record( Level.WARN );
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * WARN according to the format outlined above.
     */
    @Override
    public void warn( String format, Object... argArray )
    {
        super.warn( format, argArray );
        logLevelRecorder.record( Level.WARN );
    }

    /** Log a message of level WARN, including an exception. */
    @Override
    public void warn( String msg, Throwable t )
    {
        super.warn( msg, t );
        logLevelRecorder.record( Level.WARN );
    }

    /**
     * A simple implementation which always logs messages of level ERROR
     * according to the format outlined above.
     */
    @Override
    public void error( String msg )
    {
        super.error( msg );
        logLevelRecorder.record( Level.ERROR );
    }

    /**
     * Perform single parameter substitution before logging the message of level
     * ERROR according to the format outlined above.
     */
    @Override
    public void error( String format, Object arg )
    {
        super.error( format, arg );
        logLevelRecorder.record( Level.ERROR );
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * ERROR according to the format outlined above.
     */
    @Override
    public void error( String format, Object arg1, Object arg2 )
    {
        super.error( format, arg1, arg2 );
        logLevelRecorder.record( Level.ERROR );
    }

    /**
     * Perform double parameter substitution before logging the message of level
     * ERROR according to the format outlined above.
     */
    @Override
    public void error( String format, Object... argArray )
    {
        super.error( format, argArray );
        logLevelRecorder.record( Level.ERROR );
    }

    /** Log a message of level ERROR, including an exception. */
    @Override
    public void error( String msg, Throwable t )
    {
        super.error( msg, t );
        logLevelRecorder.record( Level.ERROR );
    }
}
