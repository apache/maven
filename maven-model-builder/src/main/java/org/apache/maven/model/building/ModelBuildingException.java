package org.apache.maven.model.building;

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

/**
 * Signals one ore more errors during model building. The model builder tries to collect as many problems as possible
 * before eventually failing to provide callers with rich error information. Use {@link #getProblems()} to query the
 * details of the failure.
 * 
 * @author Benjamin Bentmann
 */
public class ModelBuildingException
    extends Exception
{

    private final List<ModelProblem> problems;

    /**
     * Creates a new exception with the specified problems.
     * 
     * @param problems The problems that causes this exception, may be {@code null}.
     */
    public ModelBuildingException( List<ModelProblem> problems )
    {
        super( toMessage( problems ) );

        this.problems = new ArrayList<ModelProblem>();
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
    public List<ModelProblem> getProblems()
    {
        return this.problems;
    }

    private static String toMessage( List<ModelProblem> problems )
    {
        StringWriter buffer = new StringWriter( 1024 );

        PrintWriter writer = new PrintWriter( buffer );

        writer.print( problems.size() );
        writer.print( ( problems.size() == 1 ) ? " problem was " : " problems were " );
        writer.println( "encountered during construction of the effective model:" );

        for ( ModelProblem problem : problems )
        {
            writer.print( "o " );
            writer.println( problem.getMessage() );
        }

        return buffer.toString();
    }

}
