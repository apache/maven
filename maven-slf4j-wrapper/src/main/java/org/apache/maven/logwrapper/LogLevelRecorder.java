package org.apache.maven.logwrapper;

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

import org.slf4j.event.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Responsible for keeping state of whether the threshold of the --fail-on-severity flag has been hit.
 */
public class LogLevelRecorder
{
    private static final Map<String, Level> ACCEPTED_LEVELS = new HashMap<>();
    static
    {
        ACCEPTED_LEVELS.put( "WARN", Level.WARN );
        ACCEPTED_LEVELS.put( "WARNING", Level.WARN );
        ACCEPTED_LEVELS.put( "ERROR", Level.ERROR );
    }

    private final Level logThreshold;
    private boolean metThreshold = false;

    public LogLevelRecorder( String threshold )
    {
        logThreshold = determineThresholdLevel( threshold );
    }

    private Level determineThresholdLevel( String input )
    {
        final Level result = ACCEPTED_LEVELS.get( input );
        if ( result == null )
        {
            String message = String.format(
                    "%s is not a valid log severity threshold. Valid severities are WARN/WARNING and ERROR.",
                    input );
            throw new IllegalArgumentException( message );
        }
        return result;
    }

    public void record( Level logLevel )
    {
        if ( !metThreshold && logLevel.toInt() >= logThreshold.toInt() )
        {
            metThreshold = true;
        }
    }

    public boolean metThreshold()
    {
        return metThreshold;
    }
}
