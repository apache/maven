package org.apache.maven.building;

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

import java.util.ArrayList;
import java.util.List;

/**
 * Collects problems that are encountered during settings building.
 *
 * @author Benjamin Bentmann
 * @author Robert Scholte
 */
class DefaultProblemCollector
    implements ProblemCollector
{

    private List<Problem> problems;

    private String source;

    DefaultProblemCollector( List<Problem> problems )
    {
        this.problems = ( problems != null ) ? problems : new ArrayList<>();
    }

    @Override
    public List<Problem> getProblems()
    {
        return problems;
    }

    @Override
    public void setSource( String source )
    {
        this.source = source;
    }

    @Override
    public void add( Problem.Severity severity, String message, int line, int column, Exception cause )
    {
        Problem problem = new DefaultProblem( message, severity, source, line, column, cause );

        problems.add( problem );
    }

}
