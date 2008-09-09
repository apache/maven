package org.apache.maven.shared.model;

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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ModelPropertyTest
{

    @Test
    public void isParent()
    {
        ModelProperty mp0 = new ModelProperty( "http://apache.org/maven/project/profiles#collection/profile/id", "1" );
        ModelProperty mp1 = new ModelProperty(
            "http://apache.org/maven/project/profiles#collection/profile/build/plugins/plugin/groupId", "org" );
        assertFalse( mp0.isParentOf( mp1 ) );
        assertTrue( mp0.getDepth() < mp1.getDepth() );
    }

    @Test
    public void isParent1()
    {
        ModelProperty mp0 = new ModelProperty( "http://apache.org/maven/project/profiles#collection/profile/id", "1" );
        ModelProperty mp1 =
            new ModelProperty( "http://apache.org/maven/project/profiles#collection/profile/id/a/b", "org" );
        assertFalse( mp0.isParentOf( mp1 ) );
    }
}
