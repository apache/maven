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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CLIManagerTest
{
    private CLIManager cliManager;

    @BeforeEach
    void beforeEach()
    {
        cliManager = new CLIManager();
    }

    @Test
    void spacedOptions() throws ParseException
    {
        CommandLine cmdLine = cliManager.parse( "-X -Dx=1 -D y=2 test".split( " " ) );
        assertThat( cmdLine.hasOption( CLIManager.DEBUG ) ).isTrue();
        assertThat( cmdLine.getOptionValues( CLIManager.SET_SYSTEM_PROPERTY )[0] ).isEqualTo( "x=1" );
        assertThat( cmdLine.getOptionValues( CLIManager.SET_SYSTEM_PROPERTY )[1] ).isEqualTo( "y=2" );
    }

}
