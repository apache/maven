package org.apache.maven.plugin.coreit;

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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Check that we correctly use the implementation parameter. See MNG-2293
 *
 * @goal param-implementation
 * @description Prints out the name of the implementation of the bla field.
 */
public class ParameterImplementationMojo
    extends AbstractMojo
{

    /**
     * @parameter implementation="org.apache.maven.plugin.coreit.sub.MyBla"
     * @required
     */
    private Bla bla;

    /**
     * The expected value of bla.toString().
     *
     * @parameter
     * @required
     */
    private String expected;

    public void execute()
        throws MojoExecutionException
    {

        getLog().info( "bla: " + bla );

        if ( ! expected.equals( bla.toString() ) )
        {
            throw new MojoExecutionException( "Expected '" + expected + "'; found '" + bla + "'" );
        }
    }

}
