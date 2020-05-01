package org.apache.maven.wrapper;

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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

public class SystemPropertiesHandlerTest {

  private File tmpDir = new File("target/test-files/SystemPropertiesHandlerTest");

  @Before
  public void setupTempDir() {
    tmpDir.mkdirs();
  }

  @Test
  public void testParsePropertiesFile() throws Exception {
    File propFile = new File(tmpDir, "props");
    Properties props = new Properties();
    props.put("a", "b");
    props.put("systemProp.c", "d");
    props.put("systemProp.", "e");

    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(propFile);
      props.store(fos, "");
    } finally {
      IOUtils.closeQuietly(fos);
    }

    Map<String, String> expected = new HashMap<String, String>();
    expected.put("c", "d");

    assertThat(SystemPropertiesHandler.getSystemProperties(propFile), equalTo(expected));
  }

  @Test
  public void ifNoPropertyFileExistShouldReturnEmptyMap() {
    Map<String, String> expected = new HashMap<String, String>();
    assertThat(SystemPropertiesHandler.getSystemProperties(new File(tmpDir, "unknown")), equalTo(expected));
  }
}
