package org.apache.maven.project.inheritance.t00;

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

import org.apache.maven.project.MavenProject;
import org.apache.maven.project.inheritance.AbstractProjectInheritanceTestCase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A test which demonstrates maven's recursive inheritance where
 * a distinct value is taken from each parent contributing to the
 * the final model of the project being assembled. There is no
 * overriding going on amongst the models being used in this test:
 * each model in the lineage is providing a value that is not present
 * anywhere else in the lineage. We are just making sure that values
 * down in the lineage are bubbling up where they should.
 *
 * @author Jason van Zyl
 */
public class ProjectInheritanceTest
    extends AbstractProjectInheritanceTestCase
{
    // ----------------------------------------------------------------------
    //
    // p4 inherits from p3
    // p3 inherits from p2
    // p2 inherits from p1
    // p1 inherits from p0
    // p0 inherits from super model
    //
    // or we can show it graphically as:
    //
    // p4 ---> p3 ---> p2 ---> p1 ---> p0 --> super model
    //
    // ----------------------------------------------------------------------

    @Test
    public void testProjectInheritance()
        throws Exception
    {
        MavenProject p4 = getProject( projectFile( "p4" ) );

        assertEquals( "p4", p4.getName() );

        // ----------------------------------------------------------------------
        // Value inherited from p3
        // ----------------------------------------------------------------------

        assertEquals( "2000", p4.getInceptionYear() );

        // ----------------------------------------------------------------------
        // Value taken from p2
        // ----------------------------------------------------------------------

        assertEquals( "mailing-list", p4.getMailingLists().get( 0 ).getName() );

        // ----------------------------------------------------------------------
        // Value taken from p1
        // ----------------------------------------------------------------------

        assertEquals( "scm-url/p2/p3/p4", p4.getScm().getUrl() );

        // ----------------------------------------------------------------------
        // Value taken from p4
        // ----------------------------------------------------------------------

        assertEquals( "Codehaus", p4.getOrganization().getName() );

        // ----------------------------------------------------------------------
        // Value taken from super model
        // ----------------------------------------------------------------------

        assertEquals( "4.0.0", p4.getModelVersion() );

        assertEquals( "4.0.0", p4.getModelVersion() );
    }
}
