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

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Represents a parsed phase identifier.
 */
public class PhaseId
{
    /**
     * Interned {@link PhaseId} instances.
     */
    private static final Map<String, PhaseId> INSTANCES = new WeakHashMap<>();

    /**
     * The execution point of this {@link PhaseId}.
     */
    private final PhaseExecutionPoint executionPoint;

    /**
     * The static phase that this dynamic phase belongs to.
     */
    private final String phase;

    /**
     * The priority of this dynamic phase within the static phase.
     */
    private final int priority;

    /**
     * Parses the phase identifier.
     *
     * @param phase the phase identifier.
     * @return the {@link PhaseId}.
     */
    public static synchronized PhaseId of( String phase )
    {
        PhaseId result = INSTANCES.get( phase );
        if ( result == null )
        {
            result = new PhaseId( phase );
            INSTANCES.put( phase, result );
        }
        return result;
    }

    /**
     * Constructor.
     *
     * @param phase the phase identifier string.
     */
    private PhaseId( String phase )
    {
        int executionPointEnd = phase.indexOf( ':' );
        int phaseStart;
        if ( executionPointEnd == -1 )
        {
            executionPoint = PhaseExecutionPoint.AS;
            phaseStart = 0;
        }
        else
        {
            switch ( phase.substring( 0, executionPointEnd ) )
            {
                case "before":
                    executionPoint = PhaseExecutionPoint.BEFORE;
                    phaseStart = executionPointEnd + 1;
                    break;
                case "after":
                    executionPoint = PhaseExecutionPoint.AFTER;
                    phaseStart = executionPointEnd + 1;
                    break;
                default:
                    executionPoint = PhaseExecutionPoint.AS;
                    phaseStart = 0;
                    break;
            }
        }
        int phaseEnd = phase.indexOf( '[' );
        if ( phaseEnd == -1 )
        {
            priority = 0;
            this.phase = phase.substring( phaseStart );
        }
        else
        {
            int priorityEnd = phase.lastIndexOf( ']' );
            boolean havePriority;
            int priority;
            if ( priorityEnd < phaseEnd + 1 || priorityEnd != phase.length() - 1 )
            {
                priority = 0;
                havePriority = false;
            }
            else
            {
                try
                {
                    priority = Integer.parseInt( phase.substring( phaseEnd + 1, priorityEnd ) );
                    havePriority = true;
                }
                catch ( NumberFormatException e )
                {
                    // priority must be an integer
                    priority = 0;
                    havePriority = false;
                }
            }
            if ( havePriority )
            {
                this.phase = phase.substring( phaseStart, phaseEnd );
                this.priority = priority;
            }
            else
            {
                this.phase = phase.substring( phaseStart );
                this.priority = 0;
            }
        }
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        PhaseId phaseId = (PhaseId) o;

        if ( priority() != phaseId.priority() )
        {
            return false;
        }
        if ( executionPoint() != phaseId.executionPoint() )
        {
            return false;
        }
        return phase().equals( phaseId.phase() );
    }

    @Override
    public int hashCode()
    {
        int result = executionPoint().hashCode();
        result = 31 * result + phase().hashCode();
        result = 31 * result + priority();
        return result;
    }

    @Override
    public String toString()
    {
        return executionPoint().prefix() + phase() + ( priority() != 0 ? "[" + priority() + ']' : "" );
    }

    public PhaseExecutionPoint executionPoint()
    {
        return executionPoint;
    }

    public String phase()
    {
        return phase;
    }

    public int priority()
    {
        return priority;
    }
}
