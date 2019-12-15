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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Karl Heinz Marbaise
 */
class CleanArgumentTest
{
    @Test
    void cleanArgsShouldRemoveWrongSurroundingQuotes()
    {
        String[] args = { "\"-Dfoo=bar", "\"-Dfoo2=bar two\"" };
        String[] cleanArgs = CleanArgument.cleanArgs( args );
        assertThat( cleanArgs.length ).isEqualTo( args.length );
        assertThat( cleanArgs[0] ).isEqualTo( "-Dfoo=bar" );
        assertThat( cleanArgs[1] ).isEqualTo( "-Dfoo2=bar two" );
    }

    @Test
    void testCleanArgsShouldNotTouchCorrectlyQuotedArgumentsUsingDoubleQuotes()
    {
        String information = "-Dinformation=\"The Information is important.\"";
        String[] args = { information };
        String[] cleanArgs = CleanArgument.cleanArgs( args );
        assertThat( cleanArgs.length ).isEqualTo( args.length );
        assertThat( cleanArgs[0] ).isEqualTo( information );
    }

    @Test
    public void testCleanArgsShouldNotTouchCorrectlyQuotedArgumentsUsingSingleQuotes()
    {
        String information = "-Dinformation='The Information is important.'";
        String[] args = { information };
        String[] cleanArgs = CleanArgument.cleanArgs( args );
        assertThat( cleanArgs.length ).isEqualTo( args.length );
        assertThat( cleanArgs[0] ).isEqualTo( information );
    }

}
