package org.apache.maven.it;

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

/**
 * This is a test set for <a href="https://issues.apache.org/jira/browse/MNG-6223">MNG-6223</a>:
 * check that extensions in <code>.mvn/</code> are found when Maven is run with <code>-f path/to/dir</code>.
 * @see MavenITmng5889FindBasedir
 */
public class MavenITmng6223FindBasedir
    extends MavenITmng5889FindBasedir
{
    public MavenITmng6223FindBasedir()
    {
        super( "[3.5.1,)" );
    }

    /**
     * check that <code>path/to/.mvn/</code> is found when path to POM set by <code>--file path/to/dir</code>
     *
     * @throws Exception in case of failure
     */
    public void testMvnFileLongOptionToDir()
        throws Exception
    {
        runCoreExtensionWithOptionToDir( "--file", null );
    }

    /**
     * check that <code>path/to/.mvn/</code> is found when path to POM set by <code>-f path/to/dir</code>
     *
     * @throws Exception in case of failure
     */
    public void testMvnFileShortOptionToDir()
        throws Exception
    {
        runCoreExtensionWithOptionToDir( "-f", null );
    }

    /**
     * check that <code>path/to/.mvn/</code> is found when path to POM set by <code>--file path/to/module</code>
     *
     * @throws Exception in case of failure
     */
    public void testMvnFileLongOptionModuleToDir()
        throws Exception
    {
        runCoreExtensionWithOptionToDir( "--file", "module" );
    }

    /**
     * check that <code>path/to/.mvn/</code> is found when path to POM set by <code>-f path/to/module</code>
     *
     * @throws Exception in case of failure
     */
    public void testMvnFileShortOptionModuleToDir()
        throws Exception
    {
        runCoreExtensionWithOptionToDir( "-f", "module" );
    }

    private void runCoreExtensionWithOptionToDir( String option, String subdir )
        throws Exception
    {
        runCoreExtensionWithOption( option, subdir, false );
    }

}
