package org.apache.maven.embedder.execution;

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

import java.io.File;

/** @author Jason van Zyl */
public class EmbedderUsingEclipsePluginTest
    extends AbstractEmbedderExecutionTestCase
{
    protected String getId()
    {
        return "eclipse-from-embedder";
    }

    public void testRunningEclipsePlugin()
        throws Exception
    {
        File basedir = runWithProject( "org.apache.maven.plugins:maven-eclipse-plugin:2.3:eclipse" );

        assertFileExists( new File( basedir, ".classpath" ) );

        assertFileExists( new File( basedir, ".project" ) );
    }
}
