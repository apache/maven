package org.apache.maven.model.interpolation;

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

import java.util.Properties;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.SimpleProblemCollector;

import junit.framework.TestCase;

/**
 * @author Benjamin Bentmann
 */
public class StringSearchModelInterpolatorTest
    extends TestCase
{

    public void testFinalFieldsExcludedFromInterpolation()
    {
        Properties props = new Properties();
        props.setProperty( "expression", "value" );
        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setUserProperties( props );

        SimpleProblemCollector problems = new SimpleProblemCollector();
        StringSearchModelInterpolator interpolator = new StringSearchModelInterpolator();
        interpolator.interpolateObject( new ClassWithFinalField(), new Model(), null, request, problems );

        assertTrue( problems.getFatals().toString(), problems.getFatals().isEmpty() );
        assertTrue( problems.getErrors().toString(), problems.getErrors().isEmpty() );
        assertTrue( problems.getWarnings().toString(), problems.getWarnings().isEmpty() );
    }

    static class ClassWithFinalField
    {

        public static final String CONSTANT = "${expression}";

    }

}
