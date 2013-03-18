package org.slf4j.impl;

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

import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

/**
 * Utility for Maven to access Slf4j-Simple internals through package access.
 * Use with precaution, since this is not normally intended for production use.
 */
public class MavenSlf4jSimpleFriend
{
    public static void init()
    {
        SimpleLogger.init();
        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        if ( loggerFactory instanceof SimpleLoggerFactory )
        {
            ( (SimpleLoggerFactory) loggerFactory ).reset();
        }
    }
}
