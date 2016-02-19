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

import static com.google.common.base.Predicates.in;
import static com.google.common.collect.Iterables.any;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.transform;
import static java.util.Collections.singleton;
import static java.util.EnumSet.of;
import static org.apache.maven.model.building.ModelProblem.Severity.ERROR;
import static org.apache.maven.model.building.ModelProblem.Severity.FATAL;

import java.util.Arrays;
import java.util.Collections;

import org.apache.maven.model.building.ModelProblem.Severity;

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

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
     * Success without warnings.
     */
    public static <T> Result<T> success( T model )
    {
        return success( model, Collections.<ModelProblem>emptyList() );
    }

    /**
     * Success with warnings.
     */
    public static <T> Result<T> success( T model, Iterable<? extends ModelProblem> problems )
    {
        assert !hasErrors( problems );
        return new Result<>( false, model, problems );
    }

    /**
     * Success with warnings.
     */
    public static <T> Result<T> success( T model, Result<?>... results )
    {
        return success( model, Iterables.concat( Iterables.transform( Arrays.asList( results ), GET_PROBLEMS ) ) );
    }

    /**
     * Error with problems describing the cause.
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
        return error( Iterables.concat( Iterables.transform( Arrays.asList( results ), GET_PROBLEMS ) ) );
    }

    /**
     * Error with partial result and problems describing the cause.
     */
    public static <T> Result<T> error( T model, Iterable<? extends ModelProblem> problems )
    {
        return new Result<>( true, model, problems );
    }

    /**
     * New result - determine whether error or success by checking problems for errors.
     */
    public static <T> Result<T> newResult( T model, Iterable<? extends ModelProblem> problems )
    {
        return new Result<>( hasErrors( problems ), model, problems );
    }

    /**
     * New result consisting of given result and new problem. Convenience for newResult(result.get(),
     * concat(result.getProblems(),problems)).
     */
    public static <T> Result<T> addProblem( Result<T> result, ModelProblem problem )
    {
        return addProblems( result, singleton( problem ) );
    }

    /**
     * New result that includes the given.
     */
    public static <T> Result<T> addProblems( Result<T> result, Iterable<? extends ModelProblem> problems )
    {
        return new Result<>( result.hasErrors() || hasErrors( problems ), result.get(), concat( result.getProblems(),
                                                                                                 problems ) );
    }

    public static <T> Result<T> addProblems( Result<T> result, Result<?>... results )
    {
        return addProblems( result, Iterables.concat( Iterables.transform( Arrays.asList( results ), GET_PROBLEMS ) ) );
    }

    /**
     * Turns the given results into a single result by combining problems and models into single collection.
     */
    public static <T> Result<Iterable<T>> newResultSet( Iterable<? extends Result<? extends T>> results )
    {
        final boolean hasErrors = any( transform( results, new Function<Result<?>, Boolean>()
        {
            @Override
            public Boolean apply( Result<?> input )
            {
                return input.hasErrors();
            }
        } ), Predicates.equalTo( true ) );
        final Iterable<T> models = transform( results, new Function<Result<? extends T>, T>()
        {
            @Override
            public T apply( Result<? extends T> input )
            {
                return input.get();
            }
        } );
        final Iterable<ModelProblem> problems = concat( transform( results, GET_PROBLEMS ) );
        return new Result<>( hasErrors, models, problems );
    }

    // helper to determine if problems contain error
    private static boolean hasErrors( Iterable<? extends ModelProblem> problems )
    {
        return any( transform( problems, new Function<ModelProblem, Severity>()
        {
            @Override
            public Severity apply( ModelProblem input )
            {
                return input.getSeverity();
            }
        } ), in( of( ERROR, FATAL ) ) );
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

    private static final Function<Result<?>, Iterable<? extends ModelProblem>> GET_PROBLEMS =
        new Function<Result<?>, Iterable<? extends ModelProblem>>()
        {
            @Override
            public Iterable<? extends ModelProblem> apply( Result<?> input )
            {
                return input.getProblems();
            }
        };
}