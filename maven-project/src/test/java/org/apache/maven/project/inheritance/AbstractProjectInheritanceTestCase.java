package org.apache.maven.project.inheritance;

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

import org.apache.maven.project.AbstractMavenProjectTestCase;

import java.io.File;

/**
 * @author Jason van Zyl
 * @version $Id$
 */
public abstract class AbstractProjectInheritanceTestCase
    extends AbstractMavenProjectTestCase
{
    protected String getTestSeries()
    {
        String className = getClass().getPackage().getName();

        return className.substring( className.lastIndexOf( "." ) + 1 );
    }

    protected File projectFile( String name )
    {
        return projectFile( "maven", name );
    }

    protected File projectFile( String groupId, String artifactId )
    {
        return new File( getLocalRepositoryPath(), "/" + groupId + "/poms/" + artifactId + "-1.0.pom" );
    }

    // ----------------------------------------------------------------------
    // The local repository for this category of tests
    // ----------------------------------------------------------------------

    protected File getLocalRepositoryPath()
    {
        return getTestFile("src/test/resources/inheritance-repo/" + getTestSeries() );
    }
}
