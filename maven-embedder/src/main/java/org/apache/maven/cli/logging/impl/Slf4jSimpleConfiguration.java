package org.apache.maven.cli.logging.impl;

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

import org.apache.maven.cli.logging.BaseSlf4jConfiguration;
import org.slf4j.MavenSlf4jFriend;
import org.slf4j.impl.MavenSlf4jSimpleFriend;

/**
 * Configuration for slf4j-simple.
 *
 * @author Hervé Boutemy
 * @since 3.1.0
 */
public class Slf4jSimpleConfiguration
    extends BaseSlf4jConfiguration
{
    @Override
    public void setRootLoggerLevel( Level level )
    {
        String value;
        switch ( level )
        {
            case DEBUG:
                value = "debug";
                break;

            case INFO:
                value = "info";
                break;

            default:
                value = "error";
                break;
        }
        System.setProperty( "org.slf4j.simpleLogger.defaultLogLevel", value );
    }

    @Override
    public void activate()
    {
        // property for root logger level or System.out redirection need to be taken into account
        MavenSlf4jFriend.reset();
        MavenSlf4jSimpleFriend.init();
    }
}
