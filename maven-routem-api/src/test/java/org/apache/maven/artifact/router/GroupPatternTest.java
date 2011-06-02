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
import org.junit.Test;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class GroupPatternTest
{

    @Test
    public void orgApacheMaven_doesNotImply_orgApacheMavenArtifact()
    {
        assertFalse( new GroupPattern( "org.apache.maven" ).implies( new GroupPattern( "org.apache.maven.artifact" ) ) );
    }

    @Test
    public void orgApacheMaven_wildcardImplies_orgApacheMavenArtifact()
    {
        assertTrue( new GroupPattern( "org.apache.maven.*" ).implies( new GroupPattern( "org.apache.maven.artifact" ) ) );
    }

    @Test
    public void orgApacheMaven_wildcardImplies_orgApacheMaven()
    {
        assertTrue( new GroupPattern( "org.apache.maven.*" ).implies( new GroupPattern( "org.apache.maven" ) ) );
    }

    @Test
    public void orgApacheMaven_impliesSelf()
    {
        GroupPattern pat = new GroupPattern( "org.apache.maven" );
        assertTrue( pat.implies( pat ) );
    }

    @Test
    public void orgApacheMaven_wildcardImpliesSelf()
    {
        GroupPattern pat = new GroupPattern( "org.apache.maven.*" );
        assertTrue( pat.implies( pat ) );
    }

    @Test
    public void s_orgApacheMaven_doesNotImply_orgApacheMavenArtifact()
    {
        assertFalse( new GroupPattern( "org.apache.maven" ).implies( "org.apache.maven.artifact" ) );
    }

    @Test
    public void s_orgApacheMaven_wildcardImplies_orgApacheMavenArtifact()
    {
        assertTrue( new GroupPattern( "org.apache.maven.*" ).implies( "org.apache.maven.artifact" ) );
    }

    @Test
    public void s_orgApacheMaven_wildcardImplies_orgApacheMaven()
    {
        assertTrue( new GroupPattern( "org.apache.maven.*" ).implies( "org.apache.maven" ) );
    }

    @Test
    public void s_orgApacheMaven_impliesSelf()
    {
        GroupPattern pat = new GroupPattern( "org.apache.maven" );
        assertTrue( pat.implies( pat.getPattern() ) );
    }

    @Test
    public void s_orgApacheMaven_wildcardImpliesSelf()
    {
        GroupPattern pat = new GroupPattern( "org.apache.maven.*" );
        assertTrue( pat.implies( pat.getPattern() ) );
    }

    @Test
    public void wildcardSortsLastWhenBasePatternsAreEqual()
    {
        String base = "org.apache.maven";
        String wc = base + ".*";

        GroupPattern one = new GroupPattern( base );
        GroupPattern two = new GroupPattern( wc );

        Set<GroupPattern> set = new TreeSet<GroupPattern>();
        set.add( one );
        set.add( two );

        Iterator<GroupPattern> it = set.iterator();
        assertSame( one, it.next() );
        assertSame( two, it.next() );
    }

    @Test
    public void longestSortsFirstWhenBasesEqual()
    {
        String shorter = "org.apache.maven";
        String longer = shorter + ".artifact";

        GroupPattern one = new GroupPattern( shorter );
        GroupPattern two = new GroupPattern( longer );

        Set<GroupPattern> set = new TreeSet<GroupPattern>();
        set.add( one );
        set.add( two );

        Iterator<GroupPattern> it = set.iterator();
        assertSame( two, it.next() );
        assertSame( one, it.next() );
    }

    @Test
    public void longestSortsFirstWhenShortestHasWildcard()
    {
        String base = "org.apache.maven";
        String wc = base + ".*";
        String longer = base + ".artifact";

        GroupPattern one = new GroupPattern( wc );
        GroupPattern two = new GroupPattern( longer );

        Set<GroupPattern> set = new TreeSet<GroupPattern>();
        set.add( one );
        set.add( two );

        Iterator<GroupPattern> it = set.iterator();
        assertSame( two, it.next() );
        assertSame( one, it.next() );
    }

    @Test
    public void unrelatedSortLexicographically()
    {
        String org = "org.apache.maven.*";
        String com = "com.mycomp.fooba.*";

        GroupPattern one = new GroupPattern( org );
        GroupPattern two = new GroupPattern( com );

        Set<GroupPattern> set = new TreeSet<GroupPattern>();
        set.add( one );
        set.add( two );

        Iterator<GroupPattern> it = set.iterator();
        assertSame( two, it.next() );
        assertSame( one, it.next() );
    }
    
    @Test
    public void globalWildcardSortsLast()
    {
        String org = "org.apache.maven.*";
        String com = "com.mycomp.fooba.*";
        String wc = "*";

        GroupPattern one = new GroupPattern( org );
        GroupPattern two = new GroupPattern( com );
        GroupPattern w = new GroupPattern( wc );

        Set<GroupPattern> set = new TreeSet<GroupPattern>();
        set.add( one );
        set.add( two );
        set.add( w );

        Iterator<GroupPattern> it = set.iterator();
        assertSame( two, it.next() );
        assertSame( one, it.next() );
        assertSame( w, it.next() );
    }
    
    @Test( expected=IllegalArgumentException.class )
    public void failWithInvalidGroupIdPattern()
    {
        new GroupPattern( "." );
    }

    @Test( expected=IllegalArgumentException.class )
    public void failWithInvalidGroupIdPattern2()
    {
        new GroupPattern( ".." );
    }

    @Test( expected=IllegalArgumentException.class )
    public void failWithInvalidGroupIdPattern3()
    {
        new GroupPattern( "/" );
    }

    @Test( expected=IllegalArgumentException.class )
    public void failWithEmptyGroupIdPattern()
    {
        new GroupPattern( "" );
    }

}
