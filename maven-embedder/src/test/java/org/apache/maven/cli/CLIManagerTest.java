package org.apache.maven.cli;

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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.ArrayMatching.hasItemInArray;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.cli.CommandLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CLIManagerTest
{
    private CLIManager cliManager;

    @BeforeEach
    public void setup()
    {
        cliManager = new CLIManager();
    }

    @Test
    public void spacedOptionsShort()
        throws Exception
    {
        CommandLine cmdLine = cliManager.parse( "-X -Dv -Dw=1 -D x=2 test".split( " " ) );

        assertTrue( cmdLine.hasOption( CLIManager.VERBOSE ) );

        String[] properties = cmdLine.getOptionValues( CLIManager.SET_SYSTEM_PROPERTY );
        assertThat( properties[0], is( "v" ) );
        assertThat( properties[1], is( "w" ) );
        assertThat( properties[2], is( "1" ) );
        assertThat( properties[3], is( "x" ) );
        assertThat( properties[4], is( "2" ) );
        assertThat( properties, not( hasItemInArray( "test" ) ) );
    }
}
