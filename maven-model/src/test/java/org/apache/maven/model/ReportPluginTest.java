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
 * Tests {@code ReportPlugin}.
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
public class ReportPluginTest
    extends TestCase
{

    public void testHashCodeNullSafe()
    {
        new ReportPlugin().hashCode();
    }

    public void testEqualsNullSafe()
    {
        assertFalse( new ReportPlugin().equals( null ) );

        new ReportPlugin().equals( new ReportPlugin() );
    }

    public void testEqualsIsKey()
    {
        ReportPlugin thing = new ReportPlugin();
        thing.setGroupId( "groupId" );
        thing.setArtifactId( "artifactId" );
        thing.setVersion( "1.0" );
        ReportPlugin thing2 = new ReportPlugin();
        thing2.setGroupId( "groupId" );
        thing2.setArtifactId( "artifactId" );
        thing2.setVersion( "2.0" );
        assertEquals( thing2, thing );

        ReportPlugin thing3 = new ReportPlugin();
        thing3.setGroupId( "otherGroupId" );
        thing3.setArtifactId( "artifactId" );
        assertFalse( thing3.equals( thing ) );
    }

    public void testHashcodeIsId()
    {
        ReportPlugin thing = new ReportPlugin();
        thing.setGroupId( "groupId" );
        thing.setArtifactId( "artifactId" );
        thing.setVersion( "1.0" );
        ReportPlugin thing2 = new ReportPlugin();
        thing2.setGroupId( "groupId" );
        thing2.setArtifactId( "artifactId" );
        thing2.setVersion( "2.0" );
        assertEquals( thing2.hashCode(), thing.hashCode() );

        ReportPlugin thing3 = new ReportPlugin();
        thing3.setGroupId( "otherGroupId" );
        thing3.setArtifactId( "artifactId" );
        assertFalse( thing3.hashCode() == thing.hashCode() );
    }

    public void testEqualsIdentity()
    {
        ReportPlugin thing = new ReportPlugin();
        assertTrue( thing.equals( thing ) );
    }

    public void testToStringNullSafe()
    {
        assertNotNull( new ReportPlugin().toString() );
    }

}
