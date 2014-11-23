package org.apache.maven.configuration;

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

import java.io.File;

/**
 * A path translator that resolves relative paths against a specific base directory.
 *
 * @author Benjamin Bentmann
 */
public class BasedirBeanConfigurationPathTranslator
    implements BeanConfigurationPathTranslator
{

    private final File basedir;

    /**
     * Creates a new path translator using the specified base directory.
     *
     * @param basedir The base directory to resolve relative paths against, may be {@code null} to disable path
     *            translation.
     */
    public BasedirBeanConfigurationPathTranslator( File basedir )
    {
        this.basedir = basedir;
    }

    public File translatePath( File path )
    {
        File result = path;

        if ( path != null && basedir != null )
        {
            if ( path.isAbsolute() )
            {
                // path is already absolute, we're done
            }
            else if ( path.getPath().startsWith( File.separator ) )
            {
                // drive-relative Windows path, don't align with base dir but with drive root
                result = path.getAbsoluteFile();
            }
            else
            {
                // an ordinary relative path, align with base dir
                result = new File( new File( basedir, path.getPath() ).toURI().normalize() ).getAbsoluteFile();
            }
        }

        return result;
    }

}
