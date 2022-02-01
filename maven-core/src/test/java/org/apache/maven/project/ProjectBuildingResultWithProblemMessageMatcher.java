package org.apache.maven.project;

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

import org.apache.maven.model.building.ModelProblem;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import static java.util.stream.Collectors.joining;

/**
 * Hamcrest matcher to help create fluent assertions about {@link ProjectBuildingResult} instances.
 */
class ProjectBuildingResultWithProblemMessageMatcher extends BaseMatcher<ProjectBuildingResult>
{
    private final String problemMessage;

    ProjectBuildingResultWithProblemMessageMatcher( String problemMessage ) {
        this.problemMessage = problemMessage;
    }

    @Override
    public boolean matches( Object o )
    {
        if ( !( o instanceof ProjectBuildingResult ) ) {
            return false;
        }

        final ProjectBuildingResult r = (ProjectBuildingResult) o;

        return r.getProblems().stream()
                .anyMatch( p -> p.getMessage().contains( problemMessage ) );
    }

    @Override
    public void describeTo( Description description )
    {
        description.appendText("a ProjectBuildingResult with message ")
                .appendValue(problemMessage);
    }

    @Override
    public void describeMismatch(final Object o, final Description description)
    {
        if ( !( o instanceof ProjectBuildingResult ) ) {
            super.describeMismatch( o, description );
        }
        else
        {
            final ProjectBuildingResult r = (ProjectBuildingResult) o;
            description.appendText( "was a ProjectBuildingResult with messages " );
            String messages = r.getProblems().stream()
                    .map( ModelProblem::getMessage )
                    .map( m -> "\"" + m + "\"" + System.lineSeparator() )
                    .collect( joining( ", ") );
            description.appendText( messages );
        }
    }

    static Matcher<ProjectBuildingResult> projectBuildingResultWithProblemMessage( String message ) {
        return new ProjectBuildingResultWithProblemMessageMatcher( message );
    }
}
