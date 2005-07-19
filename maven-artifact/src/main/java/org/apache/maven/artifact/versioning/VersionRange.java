package org.apache.maven.artifact.versioning;

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
 * Construct a version range from a specification.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class VersionRange
{
    private String version;

    public VersionRange( String spec )
    {
        if ( spec != null )
        {
            // temporary!
            if ( spec.startsWith( "[" ) )
            {
                spec = spec.substring( 1, spec.length() - 1 );
            }
        }

        this.version = spec;
    }

    public String getVersion()
    {
        return version;
    }
}
