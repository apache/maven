package org.apache.maven.execution;

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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

/**
 * @author Benjamin Bentmann
 */
public class DefaultMavenExecutionTest
{
    @Test
    public void testCopyDefault()
    {
        MavenExecutionRequest original = new DefaultMavenExecutionRequest();
        MavenExecutionRequest copy = DefaultMavenExecutionRequest.copy( original );
        assertNotNull( copy );
        assertNotSame( copy, original );
    }

    @Test
    public void testResultWithNullTopologicallySortedProjectsIsEmptyList()
    {
        MavenExecutionResult result = new DefaultMavenExecutionResult();
        result.setTopologicallySortedProjects( null );
        List<MavenProject> projects = result.getTopologicallySortedProjects();
        assertNotNull( projects );
        assertThat( projects, is( empty() ) );
    }

}
