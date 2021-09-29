package org.apache.maven.toolchain.building;

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

import org.apache.maven.building.Source;

/**
 * Collects toolchains that control building of effective toolchains.
 *
 * @author Robert Scholte
 * @since 3.3.0
 */
public class DefaultToolchainsBuildingRequest
    implements ToolchainsBuildingRequest
{
    private Source globalToolchainsSource;

    private Source userToolchainsSource;

    @Override
    public Source getGlobalToolchainsSource()
    {
        return globalToolchainsSource;
    }

    @Override
    public ToolchainsBuildingRequest setGlobalToolchainsSource( Source globalToolchainsSource )
    {
        this.globalToolchainsSource = globalToolchainsSource;
        return this;
    }

    @Override
    public Source getUserToolchainsSource()
    {
        return userToolchainsSource;
    }

    @Override
    public ToolchainsBuildingRequest setUserToolchainsSource( Source userToolchainsSource )
    {
        this.userToolchainsSource = userToolchainsSource;
        return this;
    }

}
