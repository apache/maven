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

public class CredentialsChangeRequest
{
    private String resourceId;
    private AuthenticationInfo authInfo;
    private String oldPassword;

    public CredentialsChangeRequest()
    {
    }

    public CredentialsChangeRequest( String resourceId,
                                     AuthenticationInfo authInfo,
                                     String oldPassword )
    {
        super();
        this.resourceId = resourceId;
        this.authInfo = authInfo;
        this.oldPassword = oldPassword;
    }

    public String getResourceId()
    {
        return resourceId;
    }

    public void setResourceId( String resourceId )
    {
        this.resourceId = resourceId;
    }

    public AuthenticationInfo getAuthInfo()
    {
        return authInfo;
    }

    public void setAuthInfo( AuthenticationInfo authInfo )
    {
        this.authInfo = authInfo;
    }

    public String getOldPassword()
    {
        return oldPassword;
    }

    public void setOldPassword( String oldPassword )
    {
        this.oldPassword = oldPassword;
    }


}
