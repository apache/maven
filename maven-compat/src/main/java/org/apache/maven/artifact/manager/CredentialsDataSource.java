package org.apache.maven.artifact.manager;

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

import org.apache.maven.wagon.authentication.AuthenticationInfo;

/**
 * A wrapper class around setting/retrieving/caching authentication
 * info by resource ID. Typical usage - accessing server authentication for
 * by it's ID
 *
 * @author <a href="oleg@codehaus.org">Oleg Gusakov</a>
 */

public interface CredentialsDataSource
{
    public static final String ROLE = CredentialsDataSource.class.getName();

    /**
     * find, if not found, prompt and create Authentication info
     * for a given resource
     *
     * @param resourceId resource ID for which authentication is required
     * @return resource AuthenticationInfo. Should always exist.
     * @throws CredentialsDataSourceException
     */
    AuthenticationInfo get( String resourceId )
        throws CredentialsDataSourceException;

    /**
     * set, if not found, prompt and create Authentication info
     * for a given resource. This one uses the old password
     * member of AuthenticationInfo
     * 
     * @throws CredentialsDataSourceException
     */
    void set( CredentialsChangeRequest req )
        throws CredentialsDataSourceException;
}
