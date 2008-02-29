package org.apache.maven.embedder;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.codehaus.plexus.logging.AbstractLoggerManager;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.LoggerManager;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;

/**
 * This is a simple logger manager that will only write the logging statements to the console.
 * <p/>
 * Sample configuration:
 * <pre>
 * <logging>
 *   <implementation>org.codehaus.plexus.logging.ConsoleLoggerManager</implementation>
 *   <logger>
 *     <threshold>DEBUG</threshold>
 *   </logger>
 * </logging>
 * </pre>
 *
 * @author Jason van Zyl
 * @author <a href="mailto:trygvis@inamo.no">Trygve Laugst&oslash;l</a>
 * @version $Id$
 */
public class MavenEmbedderLoggerManager
    extends AbstractLoggerManager
    implements LoggerManager, Initializable
{
    /**
     * Message of this level or higher will be logged.
     * <p/>
     * This field is set by the plexus container thus the name is 'threshold'. The field
     * currentThreshold contains the current setting of the threshold.
     */
    private String threshold = "info";

    private int currentThreshold;

    private Logger logger;

    public MavenEmbedderLoggerManager( Logger logger )
    {
        this.logger = logger;
    }

    public void initialize()
    {
        debug( "Initializing ConsoleLoggerManager: " + this.hashCode() + "." );

        currentThreshold = parseThreshold( threshold );

        if ( currentThreshold == -1 )
        {
            debug( "Could not parse the threshold level: '" + threshold + "', setting to debug." );
            currentThreshold = Logger.LEVEL_DEBUG;
        }
    }

    public void setThreshold( int currentThreshold )
    {
        this.currentThreshold = currentThreshold;
    }

    public void setThresholds( int currentThreshold )
    {
        this.currentThreshold = currentThreshold;

        logger.setThreshold( currentThreshold );
    }

    /** @return Returns the threshold. */
    public int getThreshold()
    {
        return currentThreshold;
    }

    public void setThreshold( String role,
                              String roleHint,
                              int threshold )
    {
    }

    public int getThreshold( String role,
                             String roleHint )
    {
        return currentThreshold;
    }

    public Logger getLoggerForComponent( String role,
                                         String roleHint )
    {
        return logger;
    }

    public void returnComponentLogger( String role,
                                       String roleHint )
    {
    }

    public int getActiveLoggerCount()
    {
        return 1;
    }

    private int parseThreshold( String text )
    {
        text = text.trim().toLowerCase();

        if ( text.equals( "debug" ) )
        {
            return ConsoleLogger.LEVEL_DEBUG;
        }
        else if ( text.equals( "info" ) )
        {
            return ConsoleLogger.LEVEL_INFO;
        }
        else if ( text.equals( "warn" ) )
        {
            return ConsoleLogger.LEVEL_WARN;
        }
        else if ( text.equals( "error" ) )
        {
            return ConsoleLogger.LEVEL_ERROR;
        }
        else if ( text.equals( "fatal" ) )
        {
            return ConsoleLogger.LEVEL_FATAL;
        }

        return -1;
    }

    /**
     * Remove this method and all references when this code is verified.
     *
     * @param msg
     */
    private void debug( String msg )
    {
    }
}
