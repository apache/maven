/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.maven.artifact.router;

import org.apache.maven.artifact.router.MirrorRoute;
import org.apache.maven.artifact.router.ArtifactRouter;

import junit.framework.TestCase;

public class MirrorRoutingTableTest
    extends TestCase
{
    
    public void testFindMirrorMatch()
    {
        String canonical = "http://repo1.maven.org/maven2";
        
        ArtifactRouter table = new ArtifactRouter();
        MirrorRoute route = new MirrorRoute( "test", "http://nowhere.com/mirror", 10, true, canonical );
        table.addMirror( route );
        
        MirrorRoute result = table.getMirror( canonical );
        
        assertEquals( route, result );
    }

    public void testMirrorMatchNotFound()
    {
        String canonical = "http://repo1.maven.org/maven3";
        
        ArtifactRouter table = new ArtifactRouter();
        MirrorRoute route = new MirrorRoute( "test", "http://nowhere.com/mirror", 10, true, "http://repo1.maven.org/maven2" );
        table.addMirror( route );
        
        MirrorRoute result = table.getMirror( canonical );
        
        assertNull( result );
    }

}
