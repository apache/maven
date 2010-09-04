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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Knows the phase the current thread is executing.
 * <p/>
 * This class is used in weave-mode only , there may be better ways of doing this once the dust settles.
 * 
 * @since 3.0
 * @author Kristian Rosenvold
 */
class CurrentPhaseForThread
{
    private static final InheritableThreadLocal<String> threadPhase = new InheritableThreadLocal<String>();


    public static void setPhase( String phase )
    {
        threadPhase.set( phase );
    }

    public static boolean isPhase( String phase )
    {
        return phase.equals( threadPhase.get() );
    }

}
