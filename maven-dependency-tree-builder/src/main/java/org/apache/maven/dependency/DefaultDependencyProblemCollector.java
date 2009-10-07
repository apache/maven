package org.apache.maven.dependency;

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
 * @author Benjamin Bentmann
 */
public class DefaultDependencyProblemCollector
    implements DependencyProblemCollector
{

    private List<DependencyProblem> problems;

    public DefaultDependencyProblemCollector( List<DependencyProblem> problems )
    {
        this.problems = ( problems != null ) ? problems : new ArrayList<DependencyProblem>();
    }

    public List<DependencyProblem> getProblems()
    {
        return problems;
    }

    public void addError( String message )
    {
        addError( message, null );
    }

    public void addError( String message, Exception cause )
    {
        // TODO Auto-generated method stub
    }

    public void addWarning( String message )
    {
        addWarning( message, null );
    }

    public void addWarning( String message, Exception cause )
    {
        // TODO Auto-generated method stub
    }

}
