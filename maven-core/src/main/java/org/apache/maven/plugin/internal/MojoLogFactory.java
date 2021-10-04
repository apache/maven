package org.apache.maven.plugin.internal;

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

import javax.inject.Named;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.LogFactory;
import org.apache.maven.plugin.logging.internal.ILogFactory;
import org.eclipse.sisu.EagerSingleton;
import org.slf4j.LoggerFactory;

/**
 * Bridge implementation for Mojo {@link Log}.
 *
 * @since TBD
 */
@EagerSingleton
@Named
public class MojoLogFactory
    implements ILogFactory
{
    public MojoLogFactory()
    {
        LogFactory.initLogFactory( this ); // init Mojo Logging
    }

    @Override
    public Log getLog( final Class<?> clazz )
    {
        return new MojoLog( LoggerFactory.getLogger( clazz ) );
    }

    @Override
    public Log getLog( final String name )
    {
        return new MojoLog( LoggerFactory.getLogger( name ) );
    }
}
