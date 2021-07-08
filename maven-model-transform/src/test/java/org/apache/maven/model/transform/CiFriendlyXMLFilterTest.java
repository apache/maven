package org.apache.maven.model.transform;

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

import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CiFriendlyXMLFilterTest extends AbstractXMLFilterTests
{
    @Override
    protected CiFriendlyXMLFilter getFilter(XmlPullParser parser) {

        CiFriendlyXMLFilter filter = new CiFriendlyXMLFilter( parser, true );
        filter.setChangelist( "CHANGELIST" );
        return filter;
    }

    @Test
    public void changelist() throws Exception
    {
        String input = "<project>"
            + "  <groupId>GROUPID</groupId>"
            + "  <artifactId>ARTIFACTID</artifactId>"
            +   "<version>${changelist}</version>"
            + "</project>";
        String expected = "<project>"
                        + "  <groupId>GROUPID</groupId>"
                        + "  <artifactId>ARTIFACTID</artifactId>"
                        +   "<version>CHANGELIST</version>"
                        + "</project>";

        String actual = transform( input );

        assertEquals( expected, actual );
    }
}
