package org.apache.maven.archiver;

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

/**
 * Capture common manifest configuration.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 * @todo is this general enough to be in Plexus Archiver?
 */
public class ManifestConfiguration
{
    private String mainClass;

    private String packageName;

    /**
     * @todo boolean instead
     */
    private String addClasspath;

    /**
     * @todo boolean instead
     */
    private String addExtensions;

    /**
     * This gets prefixed to all classpath entries.
     */
    private String classpathPrefix = "";

    public String getMainClass()
    {
        return mainClass;
    }

    public boolean isAddClasspath()
    {
        return addClasspath != null ? Boolean.valueOf( addClasspath ).booleanValue() : false;
    }

    public boolean isAddExtensions()
    {
        return addExtensions != null ? Boolean.valueOf( addExtensions ).booleanValue() : false;
    }

    public String getPackageName()
    {
        return packageName;
    }

    public String getClasspathPrefix()
    {
        String cpp = classpathPrefix.replaceAll( "\\\\", "/" );

        if ( cpp.length() != 0 && !cpp.endsWith("/") )
        {
            cpp += "/";
        }
        
        return cpp;
    }
}
