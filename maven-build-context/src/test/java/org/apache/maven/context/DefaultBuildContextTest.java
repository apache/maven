package org.apache.maven.context;

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

import java.util.Collections;
import java.util.Map;

public class DefaultBuildContextTest
    extends AbstractBuildContextTest
{

    protected BuildContext newContext()
    {
        return new DefaultBuildContext();
    }
    
    public void testConstructor_ShouldThrowNPEWhenContextMapParameterIsNull()
    {
        try
        {
            new DefaultBuildContext( null );
            
            fail( "Should throw NPE when contextMap parameter is null." );
        }
        catch( NullPointerException e )
        {
            // should happen.
        }
    }

    public void testConstructor_ShouldRetrieveValueFromPreExistingContextMap()
    {
        String key = "key";
        String value = "value";
        
        Map contextMap = Collections.singletonMap( key, value );
        BuildContext ctx = new DefaultBuildContext( contextMap );
        
        assertEquals( value, ctx.get( key ) );
    }

}
