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

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.jupiter.api.Test;
import org.apache.maven.model.transform.sax.AbstractSAXFilter;
import org.junit.jupiter.api.BeforeEach;

import org.xml.sax.SAXException;

public class CiFriendlyXMLFilterTest extends AbstractXMLFilterTests
{
    private CiFriendlyXMLFilter filter;

    @BeforeEach
    public void setUp()
    {
        filter = new CiFriendlyXMLFilter( true );
        filter.setChangelist( "CHANGELIST" );
    }

    @Override
    protected AbstractSAXFilter getFilter()
        throws TransformerException, SAXException, ParserConfigurationException
    {
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
