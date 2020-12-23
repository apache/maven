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
 * "Throw" a parameter into the plugin context, for the "catch" mojo to
 * pickup and process.
 *
 * @goal throw
 */
public class ThrowMojo
    extends AbstractMojo
{

    public static final String THROWN_PARAMETER = "throw-parameter";

    /**
     * @parameter property="value" default-value="thrown"
     */
    private String value;

    public void setValue( String value )
    {
        this.value = value;
    }

    public String getValue()
    {
        return value;
    }

    public void execute()
        throws MojoExecutionException
    {
        getPluginContext().put( THROWN_PARAMETER, value );
    }

}
