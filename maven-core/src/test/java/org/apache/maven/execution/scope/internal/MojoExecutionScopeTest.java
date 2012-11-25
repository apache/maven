/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.maven.execution.scope.internal;

import junit.framework.TestCase;

import com.google.inject.Key;

public class MojoExecutionScopeTest
    extends TestCase
{
    public void testNestedEnter()
        throws Exception
    {
        MojoExecutionScope scope = new MojoExecutionScope();

        scope.enter();

        Object o1 = new Object();
        scope.seed( Object.class, o1 );
        assertSame( o1, scope.scope( Key.get( Object.class ), null ).get() );

        scope.enter();
        Object o2 = new Object();
        scope.seed( Object.class, o2 );
        assertSame( o2, scope.scope( Key.get( Object.class ), null ).get() );

        scope.exit();
        assertSame( o1, scope.scope( Key.get( Object.class ), null ).get() );

        scope.exit();

        try
        {
            scope.exit();
        }
        catch ( IllegalStateException expected )
        {
        }
    }
}
