package org.apache.maven.repository;

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

public class MavenArtifact
{

    private String repositoryUrl;

    private String name;

    private long contentLength;

    public MavenArtifact( String repositoryUrl, String name, long contentLength )
    {
        if ( repositoryUrl == null )
        {
            this.repositoryUrl = "";
        }
        else if ( !repositoryUrl.endsWith( "/" ) && repositoryUrl.length() > 0 )
        {
            this.repositoryUrl = repositoryUrl + '/';
        }
        else
        {
            this.repositoryUrl = repositoryUrl;
        }

        if ( name == null )
        {
            this.name = "";
        }
        else if ( name.startsWith( "/" ) )
        {
            this.name = name.substring( 1 );
        }
        else
        {
            this.name = name;
        }

        this.contentLength = contentLength;
    }

    /**
     * The base URL of the repository, e.g. "http://repo1.maven.org/maven2/". Unless the URL is unknown, it will be
     * terminated by a trailing slash.
     * 
     * @return The base URL of the repository or an empty string if unknown, never {@code null}.
     */
    public String getRepositoryUrl()
    {
        return repositoryUrl;
    }

    /**
     * The path of the artifact relative to the repository's base URL.
     * 
     * @return The path of the artifact, never {@code null}.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Gets the full URL of the artifact.
     * 
     * @return The full URL of the artifact, never {@code null}.
     */
    public String getUrl()
    {
        return getRepositoryUrl() + getName();
    }

    /**
     * The size of the artifact in bytes.
     * 
     * @return The of the artifact in bytes or a negative value if unknown.
     */
    public long getContentLength()
    {
        return contentLength;
    }

    @Override
    public String toString()
    {
        return getUrl();
    }

}
