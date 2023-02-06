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
package org.apache.maven.project.path;

import java.io.File;

import junit.framework.TestCase;

@SuppressWarnings("deprecation")
public class DefaultPathTranslatorTest extends TestCase {

    public void testAlignToBasedirWhereBasedirExpressionIsTheCompleteValue() {
        File basedir = new File(System.getProperty("java.io.tmpdir"), "test").getAbsoluteFile();

        String aligned = new DefaultPathTranslator().alignToBaseDirectory("${basedir}", basedir);

        assertEquals(basedir.getAbsolutePath(), aligned);
    }

    public void testAlignToBasedirWhereBasedirExpressionIsTheValuePrefix() {
        File basedir = new File(System.getProperty("java.io.tmpdir"), "test").getAbsoluteFile();

        String aligned = new DefaultPathTranslator().alignToBaseDirectory("${basedir}/dir", basedir);

        assertEquals(new File(basedir, "dir").getAbsolutePath(), aligned);
    }

    public void testUnalignToBasedirWherePathEqualsBasedir() {
        File basedir = new File(System.getProperty("java.io.tmpdir"), "test").getAbsoluteFile();

        String unaligned = new DefaultPathTranslator().unalignFromBaseDirectory(basedir.getAbsolutePath(), basedir);

        assertEquals(".", unaligned);
    }
}
