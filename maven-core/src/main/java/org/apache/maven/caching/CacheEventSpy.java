package org.apache.maven.caching;

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

import org.apache.maven.caching.xml.CacheConfig;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * Triggers cache report generation on build completion
 */
@Component( role = EventSpy.class )
public class CacheEventSpy extends AbstractEventSpy
{
    @Requirement
    private CacheConfig cacheConfig;

    @Requirement
    private CacheController cacheController;

    @Override
    public void onEvent( Object event ) throws Exception
    {
        if ( cacheConfig.isEnabled() )
        {
            if ( event instanceof ExecutionEvent )
            {
                ExecutionEvent executionEvent = (ExecutionEvent) event;
                if ( executionEvent.getType() == ExecutionEvent.Type.SessionEnded )
                {
                    cacheController.saveCacheReport( executionEvent.getSession() );
                }
            }
        }
    }
}