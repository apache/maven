package org.apache.maven.plugin.logging;

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

import org.apache.maven.plugin.logging.internal.ILogFactory;

/**
 * This interface supplies {@link Log} instances to Mojos. Plugin code may freely use this log factory to obtain
 * loggers that will be abridged to Maven internal logging system.
 *
 * @since TBD
 */
public final class LogFactory
{
    private static ILogFactory bridge;

    private LogFactory()
    {
        // no instances of this can be created
    }

    /**
     * Initialized Mojo LogFactory with a bridge.
     */
    public static void initLogFactory( ILogFactory bridge )
    {
        LogFactory.bridge = bridge;
    }

    /**
     * Returns the {@link Log} instance for given class.
     */
    public static Log getLog( Class<?> clazz )
    {
        return bridge.getLog( clazz );
    }

    /**
     * Returns the {@link Log} instance for given name.
     */
    public static Log getLog( String name )
    {
        return bridge.getLog( name );
    }
}
