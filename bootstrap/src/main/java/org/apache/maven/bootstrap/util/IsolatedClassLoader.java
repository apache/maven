package org.apache.maven.bootstrap.util;

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

import java.net.URL;
import java.net.URLClassLoader;

public class IsolatedClassLoader
    extends URLClassLoader
{
    private ClassLoader parent;

    public IsolatedClassLoader()
    {
        this( ClassLoader.getSystemClassLoader() );
    }

    public IsolatedClassLoader( ClassLoader parent )
    {
        super( new URL[0] );
        this.parent = parent;
    }

    public void addURL( URL url )
    {
        super.addURL( url );
    }

    public synchronized Class loadClass( String className )
        throws ClassNotFoundException
    {
        Class c = findLoadedClass( className );

        ClassNotFoundException ex = null;

        if ( c == null )
        {
            try
            {
                c = findClass( className );
            }
            catch ( ClassNotFoundException e )
            {
                ex = e;

                if ( parent != null )
                {
                    c = parent.loadClass( className );
                }
            }
        }

        if ( c == null )
        {
            throw ex;
        }

        return c;
    }
}
