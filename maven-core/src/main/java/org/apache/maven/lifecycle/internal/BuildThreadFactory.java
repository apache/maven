package org.apache.maven.lifecycle.internal;

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

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple {@link ThreadFactory} implementation that ensures the corresponding threads have a meaningful name.
 */
public class BuildThreadFactory
    implements ThreadFactory
{
    private final AtomicInteger id = new AtomicInteger();

    private static final String PREFIX = "BuilderThread";

    public Thread newThread( Runnable r )
    {
        return new Thread( r, String.format( "%s-%d", PREFIX, id.getAndIncrement() ) );
    }
}
