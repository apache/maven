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

import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public class ProblemCollectorFactoryTest
{

    @Test
    public void testNewInstance()
    {
        ProblemCollector collector1 = ProblemCollectorFactory.newInstance( null );

        Problem problem = new DefaultProblem( "MESSAGE1", null, null, -1, -1, null );
        ProblemCollector collector2 = ProblemCollectorFactory.newInstance( Collections.singletonList( problem ) );

        assertNotSame( collector1, collector2 );
        assertEquals( 0, collector1.getProblems().size() );
        assertEquals( 1, collector2.getProblems().size() );
    }

}
