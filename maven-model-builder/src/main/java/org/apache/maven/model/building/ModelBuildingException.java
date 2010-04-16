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

import org.apache.maven.model.Model;

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

    private final Model model;

    private final String modelId;

    private final List<ModelProblem> problems;

    /**
     * Creates a new exception with the specified problems.
     * 
     * @param model The model that could not be built, may be {@code null}.
     * @param modelId The identifier of the model that could not be built, may be {@code null}.
     * @param problems The problems that causes this exception, may be {@code null}.
     */
    public ModelBuildingException( Model model, String modelId, List<ModelProblem> problems )
    {
        super( toMessage( modelId, problems ) );

        this.model = model;
        this.modelId = ( modelId != null ) ? modelId : "";

        this.problems = new ArrayList<ModelProblem>();
        if ( problems != null )
        {
            this.problems.addAll( problems );
        }
    }

    /**
     * Gets the model that could not be built properly.
     * 
     * @return The erroneous model or {@code null} if not available.
     */
    public Model getModel()
    {
        return model;
    }

    /**
     * Gets the identifier of the POM whose effective model could not be built. The general format of the identifier is
     * {@code <groupId>:<artifactId>:<version>} but some of these coordinates may still be unknown at the point the
     * exception is thrown so this information is merely meant to assist the user.
     * 
     * @return The identifier of the POM or an empty string if not known, never {@code null}.
     */
    public String getModelId()
    {
        return modelId;
    }

    /**
     * Gets the problems that caused this exception.
     * 
     * @return The problems that caused this exception, never {@code null}.
     */
    public List<ModelProblem> getProblems()
    {
        return problems;
    }

    private static String toMessage( String modelId, List<ModelProblem> problems )
    {
        StringWriter buffer = new StringWriter( 1024 );

        PrintWriter writer = new PrintWriter( buffer );

        writer.print( problems.size() );
        writer.print( ( problems.size() == 1 ) ? " problem was " : " problems were " );
        writer.print( "encountered while building the effective model" );
        if ( modelId != null && modelId.length() > 0 )
        {
            writer.print( " for " );
            writer.print( modelId );
        }
        writer.println();

        for ( ModelProblem problem : problems )
        {
            writer.print( "[" );
            writer.print( problem.getSeverity() );
            writer.print( "] " );
            writer.print( problem.getMessage() );
            writer.print( " @ " );
            writer.println( ModelProblemUtils.formatLocation( problem, modelId ) );
        }

        return buffer.toString();
    }

}
