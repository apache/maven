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
import java.util.Properties;

/** @author Jason van Zyl */
public class EmbedderUsingScmPluginTest
    extends AbstractEmbedderExecutionTestCase
{
    protected String getId()
    {
        return "scm-plugin-from-embedder";
    }

    public void testRunningScmPlugin()
        throws Exception
    {
        File svnDirectory = new File( getBasedir(), ".svn" );

        if ( svnDirectory.exists() )
        {
            Properties p = new Properties();

            File outputDirectory = new File( getBasedir(), "target/scm.diff" );

            p.setProperty( "outputDirectory", outputDirectory.getCanonicalPath() );

            p.setProperty( "connectionUrl", "scm:svn:http://svn.apache.org/repos/asf/maven/components/trunk/maven-embedder" );

            File basedir = runWithProject( "scm:diff", p );
        }
    }
}
