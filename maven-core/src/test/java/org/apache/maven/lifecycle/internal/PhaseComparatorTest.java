package org.apache.maven.lifecycle.internal;

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

import junit.framework.TestCase;

import java.util.Arrays;

public class PhaseComparatorTest
    extends TestCase
{
    public void testSmokes() throws Exception {
        PhaseComparator comparator = new PhaseComparator( Arrays.asList( "site", "site-deploy" ) );
        assertTrue( comparator.compare( "site", "site-deploy" ) < 0);
        assertTrue( comparator.compare( "site-deploy", "site" ) > 0);
        assertTrue( comparator.compare( "site", "site" ) == 0);
        assertTrue( comparator.compare( "site", "deploy" ) < 0);
        assertTrue( comparator.compare( "before:site", "site" ) < 0);
        assertTrue( comparator.compare( "after:site", "site-deploy" ) < 0);
        assertTrue( comparator.compare( "after:site", "site" ) > 0);
        assertTrue( comparator.compare( "site", "site[1]" ) > 0);
        assertTrue( comparator.compare( "site", "site[-1]" ) < 0);
        assertTrue( comparator.compare( "site[1000]", "site[500]" ) < 0);
    }
}
