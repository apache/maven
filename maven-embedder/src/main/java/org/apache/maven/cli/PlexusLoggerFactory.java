package org.apache.maven.cli;

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

import org.codehaus.plexus.logging.LoggerManager;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

/**
 * A Slf4j logger factory bridged onto a Plexus logger manager.
 */
public class PlexusLoggerFactory
    implements ILoggerFactory
{

    private LoggerManager loggerManager;

    public PlexusLoggerFactory( LoggerManager loggerManager )
    {
        this.loggerManager = loggerManager;
    }

    public void setLoggerManager( LoggerManager loggerManager )
    {
        this.loggerManager = loggerManager;
    }

    public Logger getLogger( String name )
    {
        return new PlexusLogger( loggerManager.getLoggerForComponent( name, null ) );
    }

}
