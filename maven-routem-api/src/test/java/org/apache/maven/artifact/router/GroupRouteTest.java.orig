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

import static junit.framework.Assert.*;

import org.apache.maven.artifact.router.GroupPattern;
import org.apache.maven.artifact.router.GroupRoute;
import org.junit.Test;

public class GroupRouteTest
{

    @Test
    public void containsWithExactMatch()
    {
        assertTrue( new GroupRoute( "http://maven.apache.org", new GroupPattern( "org.apache.maven" ) ).contains( new GroupPattern(
                                                                                                                                    "org.apache.maven" ) ) );
    }

    @Test
    public void s_containsWithExactMatch()
    {
        assertTrue( new GroupRoute( "http://maven.apache.org", new GroupPattern( "org.apache.maven" ) ).contains( "org.apache.maven" ) );
    }

    @Test
    public void containsWithWildcardMatch()
    {
        assertTrue( new GroupRoute( "http://maven.apache.org", new GroupPattern( "org.apache.maven.*" ) ).contains( new GroupPattern(
                                                                                                                                      "org.apache.maven.artifact" ) ) );
    }

    @Test
    public void s_containsWithWildcardMatch()
    {
        assertTrue( new GroupRoute( "http://maven.apache.org", new GroupPattern( "org.apache.maven.*" ) ).contains( "org.apache.maven.artifact" ) );
    }

    @Test
    public void containsWithMatchAmongTwoWildcards()
    {
        assertTrue( new GroupRoute( "http://maven.apache.org", new GroupPattern( "org.apache.maven.plugins.*" ),
                                    new GroupPattern( "org.codehaus.mojo.*" ) ).contains( new GroupPattern(
                                                                                                            "org.apache.maven.plugins" ) ) );
    }
    
    @Test
    public void mergeToIncludeBroaderMatch()
    {
        String m = "org.apache.maven";
        String a = m + ".artifact";
        
        GroupRoute r = new GroupRoute( "http://maven.apache.org", new GroupPattern( "org.apache.maven" ) );
        assertTrue( "Before merge, " + m + " NOT matched!", r.contains( m ) );
        assertFalse( "Before merge, " + a + " matched!", r.contains( a ) );
        
        r.merge( new GroupPattern( "org.apache.maven.*" ) );
        assertTrue( "After merge, " + m + " NO LONGER matched!", r.contains( m ) );
        assertTrue( "After merge, " + a + " STILL not matched!", r.contains( a ) );
    }

    @Test
    public void mergeReplacesObsoletePattern()
    {
        GroupRoute r = new GroupRoute( "http://maven.apache.org", new GroupPattern( "org.apache.maven" ) );
        r.merge( new GroupPattern( "org.apache.maven.*" ) );
        
        assertEquals( "After merge, obsolete pattern NOT replaced!", 1, r.getGroupPatterns().size() );
    }

}
