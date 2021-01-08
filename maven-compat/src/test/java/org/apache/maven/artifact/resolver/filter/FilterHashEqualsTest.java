package org.apache.maven.artifact.resolver.filter;

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

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Igor Fedorenko
 */
public class FilterHashEqualsTest
{

    @Test
    public void testIncludesExcludesArtifactFilter()
    {
        List<String> patterns = Arrays.asList( "c", "d", "e" );

        IncludesArtifactFilter f1 = new IncludesArtifactFilter( patterns );

        IncludesArtifactFilter f2 = new IncludesArtifactFilter( patterns );

        assertTrue( f1.equals(f2) );
        assertTrue( f2.equals(f1) );
        assertTrue( f1.hashCode() == f2.hashCode() );

        IncludesArtifactFilter f3 = new IncludesArtifactFilter( Arrays.asList( "d", "c", "e" ) );
        assertTrue( f1.equals( f3 ) );
        assertTrue( f1.hashCode() == f3.hashCode() );
    }
}
