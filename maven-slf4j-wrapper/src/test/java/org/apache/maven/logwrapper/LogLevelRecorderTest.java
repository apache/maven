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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.junit.Test;
import org.slf4j.event.Level;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;

public class LogLevelRecorderTest
{
    @Test
    public void createsLogLevelRecorder()
    {
        LogLevelRecorder logLevelRecorder = new LogLevelRecorder( "WARN" );
        logLevelRecorder.record( Level.ERROR );

        assertTrue( logLevelRecorder.metThreshold() );
    }

    @Test( expected = IllegalArgumentException.class )
    public void failsOnLowerThanWarn ()
    {
        new LogLevelRecorder( "INFO" );
    }

    @Test
    public void failsOnUnknownLogLevel ()
    {
        Throwable thrown = assertThrows( IllegalArgumentException.class, () -> new LogLevelRecorder( "SEVERE" ) );
        String message = thrown.getMessage();
        assertThat( message, containsString( "SEVERE is not a valid log severity threshold" ) );
        assertThat( message, containsString( "WARN" ) );
        assertThat( message, containsString( "ERROR" ) );
    }
}