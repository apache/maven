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

import java.util.Comparator;
import java.util.List;

/**
 * Compares phases within the context of a specific lifecycle with secondary sorting based on the {@link PhaseId}.
 */
public class PhaseComparator
    implements Comparator<String>
{
    /**
     * The lifecycle phase ordering.
     */
    private final List<String> lifecyclePhases;

    /**
     * Constructor.
     *
     * @param lifecyclePhases the lifecycle phase ordering.
     */
    public PhaseComparator( List<String> lifecyclePhases )
    {
        this.lifecyclePhases = lifecyclePhases;
    }

    @Override
    public int compare( String o1, String o2 )
    {
        PhaseId p1 = PhaseId.of( o1 );
        PhaseId p2 = PhaseId.of( o2 );
        int i1 = lifecyclePhases.indexOf( p1.phase() );
        int i2 = lifecyclePhases.indexOf( p2.phase() );
        if ( i1 == -1 && i2 == -1 )
        {
            // unknown phases, leave in existing order
            return 0;
        }
        if ( i1 == -1 )
        {
            // second one is known, so it comes first
            return 1;
        }
        if ( i2 == -1 )
        {
            // first one is known, so it comes first
            return -1;
        }
        int rv = Integer.compare( i1, i2 );
        if ( rv != 0 )
        {
            return rv;
        }
        // same phase, now compare execution points
        i1 = p1.executionPoint().ordinal();
        i2 = p2.executionPoint().ordinal();
        rv = Integer.compare( i1, i2 );
        if ( rv != 0 )
        {
            return rv;
        }
        // same execution point, now compare priorities (highest wins, so invert)
        return -Integer.compare( p1.priority(), p2.priority() );
    }
}
