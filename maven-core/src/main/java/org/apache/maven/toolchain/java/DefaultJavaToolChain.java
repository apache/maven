package org.apache.maven.toolchain.java;

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

import org.apache.maven.toolchain.model.ToolchainModel;
import org.codehaus.plexus.logging.Logger;

/**
 * Provides backwards compatibility with Maven 3.2.3 and earlier. Clients that do not require compatibility with Maven
 * 3.2.3 and earlier are encouraged to use {@link JavaToolchainImpl}.
 * <strong>Note:</strong> This is an internal component whose interface can change without prior notice.
 *
 * @deprecated clients that do not require compatibility with Maven 3.2.3 and earlier should link to
 *             {@link JavaToolchainImpl} instead.
 */
public class DefaultJavaToolChain
    extends JavaToolchainImpl
{
    public static final String KEY_JAVAHOME = JavaToolchainImpl.KEY_JAVAHOME;

    public DefaultJavaToolChain( ToolchainModel model, Logger logger )
    {
        super( model, logger );
    }

    @Override
    public String getJavaHome()
    {
        return super.getJavaHome();
    }

    @Override
    public void setJavaHome( String javaHome )
    {
        super.setJavaHome( javaHome );
    }

}