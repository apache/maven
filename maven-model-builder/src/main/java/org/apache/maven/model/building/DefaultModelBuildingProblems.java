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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Collects the problems that were encountered during model building.
 * 
 * @author Benjamin Bentmann
 */
class DefaultModelBuildingProblems
    implements ModelBuildingProblems
{

    private List<ModelProblem> problems;

    public DefaultModelBuildingProblems( List<ModelProblem> problems )
    {
        this.problems = ( problems != null ) ? problems : new ArrayList<ModelProblem>();
    }

    public List<ModelProblem> getProblems()
    {
        return problems;
    }

    public void add( ModelProblem problem )
    {
        if ( problem == null )
        {
            throw new IllegalArgumentException( "model problem missing" );
        }

        problems.add( problem );
    }

    public void addAll( Collection<ModelProblem> problems )
    {
        if ( problems == null )
        {
            throw new IllegalArgumentException( "model problems missing" );
        }

        for ( ModelProblem problem : problems )
        {
            add( problem );
        }
    }

}
