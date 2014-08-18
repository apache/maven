package org.apache.maven.cli.logging;

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

import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.LoggerManager;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

/**
 * Use an SLF4J {@link org.slf4j.ILoggerFactory} as a backing for a Plexus
 * {@link org.codehaus.plexus.logging.LoggerManager},
 * ignoring Plexus logger API parts that are not classical and probably not really used.
 *
 * @author Jason van Zyl
 * @since 3.1
 */
public class Slf4jLoggerManager
    implements LoggerManager
{

    private ILoggerFactory loggerFactory;

    public Slf4jLoggerManager()
    {
        loggerFactory = LoggerFactory.getILoggerFactory();
    }

    public Logger getLoggerForComponent( String role )
    {
        return new Slf4jLogger( loggerFactory.getLogger( role ) );
    }

    /**
     * The logger name for a component with a non-null hint is <code>role.hint</code>.
     * <b>Warning</b>: this does not conform to logger name as class name convention.
     * (and what about <code>null</code> and <code>default</code> hint equivalence?)
     */
    public Logger getLoggerForComponent( String role, String hint )
    {
        return ( null == hint
            ? getLoggerForComponent( role )
            : new Slf4jLogger( loggerFactory.getLogger( role + '.' + hint ) ) );
    }

    //
    // Trying to give loggers back is a bad idea. Ceki said so :-)
    // notice to self: what was this method supposed to do?
    //
    /**
     * <b>Warning</b>: ignored.
     */
    public void returnComponentLogger( String role )
    {
    }

    /**
     * <b>Warning</b>: ignored.
     */
    public void returnComponentLogger( String role, String hint )
    {
    }

    /**
     * <b>Warning</b>: ignored (always return <code>0</code>).
     */
    public int getThreshold()
    {
        return 0;
    }

    /**
     * <b>Warning</b>: ignored.
     */
    public void setThreshold( int threshold )
    {
    }

    /**
     * <b>Warning</b>: ignored.
     */
    public void setThresholds( int threshold )
    {
    }

    /**
     * <b>Warning</b>: ignored (always return <code>0</code>).
     */
    public int getActiveLoggerCount()
    {
        return 0;
    }

}
