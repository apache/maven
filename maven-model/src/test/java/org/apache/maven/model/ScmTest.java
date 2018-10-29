package org.apache.maven.model;

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

import junit.framework.TestCase;

/**
 * Tests {@code Scm}.
 *
 * @author Benjamin Bentmann
 */
public class ScmTest
    extends TestCase
{

    public void testHashCodeNullSafe()
    {
        assertEquals( 64186976, new Scm().hashCode() );
        assertEquals( new Scm().hashCode(), new Scm().hashCode() );
    }

    public void testHashCodeReturnsExpectedValue()
    {
        assertEquals( -2102869462, this.createTestCase().hashCode() );
    }

    public void testEqualsNullSafe()
    {
        assertFalse( new Scm().equals( null ) );

        new Scm().equals( new Scm() );
    }

    public void testEqualsIdentity()
    {
        Scm thing = new Scm();
        assertTrue( thing.equals( thing ) );
    }

    public void testEqualsSame()
    {
        assertTrue( this.createTestCase().equals( this.createTestCase() ) );
    }

    public void testToStringNullSafe()
    {
        assertNotNull( new Scm().toString() );
    }

    private Scm createTestCase()
    {
        Scm scm = new Scm();
        scm.setId("id");
        scm.setUrl("url");
        scm.setConnection("connection");
        scm.setDeveloperConnection("developerConnection");
        scm.setChildInheritAppendPath(true);

        return scm;
    }
}
