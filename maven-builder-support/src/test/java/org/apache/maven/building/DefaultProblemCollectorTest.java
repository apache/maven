package org.apache.maven.building;

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

import org.apache.maven.building.Problem.Severity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DefaultProblemCollectorTest
{

    @Test
    public void testGetProblems()
    {
        DefaultProblemCollector collector = new DefaultProblemCollector( null );
        assertNotNull( collector.getProblems() );
        assertEquals( 0, collector.getProblems().size() );

        collector.add( null, "MESSAGE1", -1, -1, null );

        Exception e2 = new Exception();
        collector.add( Severity.WARNING, null, 42, 127, e2 );

        assertEquals( 2, collector.getProblems().size() );

        Problem p1 = collector.getProblems().get(0);
        assertEquals( Severity.ERROR, p1.getSeverity() );
        assertEquals( "MESSAGE1",p1.getMessage() );
        assertEquals( -1, p1.getLineNumber() );
        assertEquals( -1, p1.getColumnNumber() );
        assertEquals( null, p1.getException() );

        Problem p2 = collector.getProblems().get(1);
        assertEquals( Severity.WARNING, p2.getSeverity() );
        assertEquals( "",p2.getMessage() );
        assertEquals( 42, p2.getLineNumber() );
        assertEquals( 127, p2.getColumnNumber() );
        assertEquals( e2, p2.getException() );
    }

    @Test
    public void testSetSource()
    {
        DefaultProblemCollector collector = new DefaultProblemCollector( null );

        collector.add( null, "PROBLEM1", -1, -1, null );

        collector.setSource( "SOURCE_PROBLEM2" );
        collector.add( null, "PROBLEM2", -1, -1, null );

        collector.setSource( "SOURCE_PROBLEM3" );
        collector.add( null, "PROBLEM3", -1, -1, null );

        assertEquals( "", collector.getProblems().get( 0 ).getSource() );
        assertEquals( "SOURCE_PROBLEM2", collector.getProblems().get( 1 ).getSource() );
        assertEquals( "SOURCE_PROBLEM3", collector.getProblems().get( 2 ).getSource() );
    }
}
