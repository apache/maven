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
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CLIReportingUtilsTest
{
    @Test
    void testFormatDuration()
    {
        assertThat( CLIReportingUtils.formatDuration( 1 ) ).isEqualTo( "0.001 s" );
        assertThat( CLIReportingUtils.formatDuration( 1000 - 1 ) ).isEqualTo( "0.999 s" );
        assertThat( CLIReportingUtils.formatDuration( 1000 ) ).isEqualTo( "1.000 s" );
        assertThat( CLIReportingUtils.formatDuration( 60 * 1000 - 1 ) ).isEqualTo( "59.999 s" );
        assertThat( CLIReportingUtils.formatDuration( 60 * 1000 ) ).isEqualTo( "01:00 min" );
        assertThat( CLIReportingUtils.formatDuration( 60 * 60 * 1000 - 1 ) ).isEqualTo( "59:59 min" );
        assertThat( CLIReportingUtils.formatDuration( 60 * 60 * 1000 ) ).isEqualTo( "01:00 h" );
        assertThat( CLIReportingUtils.formatDuration( 24 * 60 * 60 * 1000 - 1 ) ).isEqualTo( "23:59 h" );
        assertThat( CLIReportingUtils.formatDuration( 24 * 60 * 60 * 1000 ) ).isEqualTo( "1 d 00:00 h" );
    }
}
