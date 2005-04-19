package org.apache.maven.tools.repoclean;

import java.io.File;
import java.net.URL;

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

public final class TestSupport
{
    
    private static final String REPO_MARKER = "repo-marker.txt";
    private static final int MY_PACKAGE_TRIM = TestSupport.class.getPackage().getName().length() + 1;
    
    private TestSupport()
    {
    }
    
    public static String getMyRepositoryPath(Object testInstance)
    {
        Class testClass = testInstance.getClass();
        
        String myRepo = testClass.getName().substring(MY_PACKAGE_TRIM);
        
        return getRepositoryPath(myRepo);
    }

    public static String getRepositoryPath( String relativePath )
    {
        String base = relativePath.replace('.', '/');
        
        if(!base.endsWith("/"))
        {
            base += "/";
        }
        
        ClassLoader cloader = Thread.currentThread().getContextClassLoader();
        
        URL repoMarkerResource = cloader.getResource(base + REPO_MARKER);
        
        File repoMarker = new File(repoMarkerResource.getPath()).getAbsoluteFile();
        
        return repoMarker.getParentFile().getPath();
    }

}
