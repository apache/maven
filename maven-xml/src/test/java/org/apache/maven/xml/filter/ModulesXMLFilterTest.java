package org.apache.maven.xml.filter;

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

import static org.xmlunit.assertj.XmlAssert.assertThat;

import org.junit.Before;
import org.junit.Test;

public class ModulesXMLFilterTest extends AbstractXMLFilterTests {

	private ModulesXMLFilter filter;
	
	@Before
	public void setup() {
		filter = new ModulesXMLFilter();
	}
	
	@Test
	public void testEmptyModules() throws Exception {
		String input = "<project><modules/></project>";
        String expected = "<project/>";
        String actual = transform( input, filter );
        assertThat( actual ).and( expected ).areIdentical();
	}

	@Test
	public void testSetOfModules() throws Exception {
		String input = "<project><modules>"
				+ "<module>ab</module>"
				+ "<module>../cd</module>"
				+ "</modules></project>";
		String expected = "<project/>";
		String actual = transform( input, filter );
		assertThat( actual ).and( expected ).areIdentical();
	}
	
	@Test
    public void testNoModules() throws Exception {
        String input = "<project><name>NAME</name></project>";
        String expected = input;
        String actual = transform( input, filter );
        assertThat( actual ).and( expected ).areIdentical();
    }
}
