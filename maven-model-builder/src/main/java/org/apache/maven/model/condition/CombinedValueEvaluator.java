package org.apache.maven.model.condition;

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

/**
 * @author Loïc B.
 */
public interface CombinedValueEvaluator
{

    /**
     * Parses a string representing a condition, and evaluates its result using simple boolean logic.
     * 
     * @param expression Boolean expression to be evaluated. Format : <br/>
     *            all(condition1,condition2,...) or and(condition1,condition2,...) : will be true if all conditions are
     *            true<br/>
     *            any(condition1,condition2,...) or or(condition1,condition2,...) : will be true if at least one of the
     *            conditions is true Nested operators are not supported.<br/>
     *            If there is no condition expressed, the string is simply passed to the evaluator as-is.
     * @param evaluator Actual evaluator for a single condition. Will be called once for each condition found in the
     *            expression
     * @return result of evaluation, depending on expression contents
     */
    boolean evaluate( String expression, SingleValueEvaluator evaluator );

}