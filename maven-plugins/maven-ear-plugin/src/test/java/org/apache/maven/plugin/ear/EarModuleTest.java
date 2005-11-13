package org.apache.maven.plugin.ear;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import junit.framework.TestCase;

/**
 * Ear module test case.
 *
 * @author <a href="snicoll@apache.org">Stephane Nicoll</a>
 * @version $Id: AbstractEarModule.java 314956 2005-10-12 18:27:15 +0200 (Wed, 12 Oct 2005) brett $
 */
public class EarModuleTest extends TestCase
{

    public void testCleanBuildDir() {
        assertEquals("APP-INF/lib/", AbstractEarModule.cleanBundleDir( "APP-INF/lib"));
        assertEquals("APP-INF/lib/", AbstractEarModule.cleanBundleDir( "APP-INF/lib/"));
        assertEquals("APP-INF/lib/", AbstractEarModule.cleanBundleDir( "/APP-INF/lib"));
        assertEquals("APP-INF/lib/", AbstractEarModule.cleanBundleDir( "/APP-INF/lib/"));        
        assertEquals("", AbstractEarModule.cleanBundleDir( "/"));
    }
}
