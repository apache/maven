package org.apache.maven.eventspy;

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

import java.util.Map;

/**
 * A core extension to monitor Maven's execution. Typically, such an extension gets loaded into Maven by specifying the
 * system property {@code maven.ext.class.path} on the command line. As soon as dependency injection is setup, Maven
 * looks up all implementators of this interface and calls their {@link #init(Context)} method. <em>Note:</em>
 * Implementors are strongly advised to inherit from {@link AbstractEventSpy} instead of directly implementing this
 * interface.
 */
public interface EventSpy
{

    interface Context
    {

        Map<String, Object> getData();

    }

    void init( Context context )
        throws Exception;

    void onEvent( Object event )
        throws Exception;

    void close()
        throws Exception;

}
