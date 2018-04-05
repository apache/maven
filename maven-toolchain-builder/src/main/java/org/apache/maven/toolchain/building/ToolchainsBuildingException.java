package org.apache.maven.toolchain.building;

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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.building.Problem;

/**
 * @author Robert Scholte
 * @since 3.3.0
 */
public class ToolchainsBuildingException
    extends Exception
{

    private final List<Problem> problems;

    /**
     * Creates a new exception with the specified problems.
     *
     * @param problems The problems that causes this exception, must not be {@code null}.
     */
    public ToolchainsBuildingException( List<Problem> problems )
    {
        super( toMessage( problems ) );

        this.problems = new ArrayList<>();
        if ( problems != null )
        {
            this.problems.addAll( problems );
        }
    }

    /**
     * Gets the problems that caused this exception.
     *
     * @return The problems that caused this exception, never {@code null}.
     */
    public List<Problem> getProblems()
    {
        return problems;
    }

    private static String toMessage( List<Problem> problems )
    {
        StringWriter buffer = new StringWriter( 1024 );

        PrintWriter writer = new PrintWriter( buffer );

        writer.print( problems.size() );
        writer.print( ( problems.size() == 1 ) ? " problem was " : " problems were " );
        writer.print( "encountered while building the effective toolchains" );
        writer.println();

        for ( Problem problem : problems )
        {
            writer.print( "[" );
            writer.print( problem.getSeverity() );
            writer.print( "] " );
            writer.print( problem.getMessage() );
            String location = problem.getLocation();
            if ( !location.isEmpty() )
            {
                writer.print( " @ " );
                writer.print( location );
            }
            writer.println();
        }

        return buffer.toString();
    }
}
