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

import static java.util.Collections.singleton;
import static org.apache.maven.model.building.ModelProblem.Severity.ERROR;
import static org.apache.maven.model.building.ModelProblem.Severity.FATAL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * There are various forms of results that are represented by this class:
 * <ol>
 * <li>success - in which case only the model field is set
 * <li>success with warnings - model field + non-error model problems
 * <li>error - no model, but diagnostics
 * <li>error - (partial) model and diagnostics
 * </ol>
 * Could encode these variants as subclasses, but kept in one for now
 *
 * @author bbusjaeger
 * @param <T>
 */
public class Result<T>
{

    /**
     * Success without warnings
     *
     * @param model
     */
    public static <T> Result<T> success( T model )
    {
        return success( model, Collections.<ModelProblem>emptyList() );
    }

    /**
     * Success with warnings
     *
     * @param model
     * @param problems
     */
    public static <T> Result<T> success( T model, Iterable<? extends ModelProblem> problems )
    {
        assert !hasErrors( problems );
        return new Result<>( false, model, problems );
    }

    /**
     * Success with warnings
     *
     * @param model
     * @param results
     */
    public static <T> Result<T> success( T model, Result<?>... results )
    {
        final List<ModelProblem> problemsList = new ArrayList<>();

        for ( Result<?> result1 : results )
        {
            for ( ModelProblem modelProblem : result1.getProblems() )
            {
                problemsList.add( modelProblem );
            }
        }

        return success( model, problemsList );
    }

    /**
     * Error with problems describing the cause
     *
     * @param problems
     */
    public static <T> Result<T> error( Iterable<? extends ModelProblem> problems )
    {
        return error( null, problems );
    }

    public static <T> Result<T> error( T model )
    {
        return error( model, Collections.<ModelProblem>emptyList() );
    }

    public static <T> Result<T> error( Result<?> result )
    {
        return error( result.getProblems() );
    }

    public static <T> Result<T> error( Result<?>... results )
    {
        final List<ModelProblem> problemsList = new ArrayList<>( );

        for ( Result<?> result1 : results )
        {
            for ( ModelProblem modelProblem : result1.getProblems( ) )
            {
                problemsList.add( modelProblem );
            }
        }

        return error( problemsList );
    }

    /**
     * Error with partial result and problems describing the cause
     *
     * @param model
     * @param problems
     */
    public static <T> Result<T> error( T model, Iterable<? extends ModelProblem> problems )
    {
        return new Result<>( true, model, problems );
    }

    /**
     * New result - determine whether error or success by checking problems for errors
     *
     * @param model
     * @param problems
     */
    public static <T> Result<T> newResult( T model, Iterable<? extends ModelProblem> problems )
    {
        return new Result<>( hasErrors( problems ), model, problems );
    }

    /**
     * New result consisting of given result and new problem. Convenience for newResult(result.get(),
     * concat(result.getProblems(),problems)).
     *
     * @param result
     * @param problem
     */
    public static <T> Result<T> addProblem( Result<T> result, ModelProblem problem )
    {
        return addProblems( result, singleton( problem ) );
    }

    /**
     * New result that includes the given
     *
     * @param result
     * @param problems
     */
    public static <T> Result<T> addProblems( Result<T> result, Iterable<? extends ModelProblem> problems )
    {
        Collection<ModelProblem> list = new ArrayList<>();
        for ( ModelProblem item : problems )
        {
            list.add( item );
        }
        for ( ModelProblem item : result.getProblems() )
        {
            list.add( item );
        }
        return new Result<>( result.hasErrors() || hasErrors( problems ), result.get(), list );
    }

    public static <T> Result<T> addProblems( Result<T> result, Result<?>... results )
    {
        final List<ModelProblem> problemsList = new ArrayList<>();

        for ( Result<?> result1 : results )
        {
            for ( ModelProblem modelProblem : result1.getProblems( ) )
            {
                problemsList.add( modelProblem );
            }
        }
        return addProblems( result, problemsList );
    }

    /**
     * Turns the given results into a single result by combining problems and models into single collection.
     *
     * @param results
     */
    public static <T> Result<Iterable<T>> newResultSet( Iterable<? extends Result<? extends T>> results )
    {
        boolean hasErrors = false;
        List<T> modelsList = new ArrayList<>();
        List<ModelProblem> problemsList = new ArrayList<>();

        for ( Result<? extends T> result : results )
        {
            modelsList.add( result.get() );

            for ( ModelProblem modelProblem : result.getProblems() )
            {
                problemsList.add( modelProblem );
            }

            if ( result.hasErrors() )
            {
                hasErrors = true;
            }
        }
        return new Result<>( hasErrors, ( Iterable<T> ) modelsList, problemsList );
    }

    // helper to determine if problems contain error
    private static boolean hasErrors( Iterable<? extends ModelProblem> problems )
    {
        for ( ModelProblem input : problems )
        {
            if ( input.getSeverity().equals( ERROR ) || input.getSeverity().equals( FATAL ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Class definition
     */

    private final boolean errors;

    private final T value;

    private final Iterable<? extends ModelProblem> problems;

    private Result( boolean errors, T model, Iterable<? extends ModelProblem> problems )
    {
        this.errors = errors;
        this.value = model;
        this.problems = problems;
    }

    public Iterable<? extends ModelProblem> getProblems()
    {
        return problems;
    }

    public T get()
    {
        return value;
    }

    public boolean hasErrors()
    {
        return errors;
    }
}