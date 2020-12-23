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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;

/**
 * @goal test
 * @phase process-sources
 */
public class DerivedItMojo
    extends EvalMojo
{
    /**
     * The path to the output file for the properties with the expression values. For each expression given by the
     * parameter {@link #expressions} an similar named properties key will be used to save the expression value. If an
     * expression evaluated to <code>null</code>, there will be no corresponding key in the properties file.
     *
     * @parameter
     */
    private File file;

    public void execute()
        throws MojoFailureException, MojoExecutionException
    {
        if ( file != null )
        {
            super.setOutputFile( file );
        }

        super.execute();
    }

}
