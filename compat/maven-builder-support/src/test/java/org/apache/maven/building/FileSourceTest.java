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
package org.apache.maven.building;

import java.io.File;
import java.io.InputStream;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

class FileSourceTest {

    @Test
    void fileSource() {
        NullPointerException e = assertThatExceptionOfType(NullPointerException.class).as("Should fail, since you must specify a file").isThrownBy(() -> new FileSource((File) null)).actual();
        assertThat(e.getMessage()).isEqualTo("file cannot be null");
    }

    @Test
    void getInputStream() throws Exception {
        File txtFile = new File("target/test-classes/source.txt");
        FileSource source = new FileSource(txtFile);

        try (InputStream is = source.getInputStream();
                Scanner scanner = new Scanner(is)) {

            assertThat(scanner.nextLine()).isEqualTo("Hello World!");
        }
    }

    @Test
    void getLocation() {
        File txtFile = new File("target/test-classes/source.txt");
        FileSource source = new FileSource(txtFile);
        assertThat(source.getLocation()).isEqualTo(txtFile.getAbsolutePath());
    }

    @Test
    void getFile() {
        File txtFile = new File("target/test-classes/source.txt");
        FileSource source = new FileSource(txtFile);
        assertThat(source.getFile()).isEqualTo(txtFile.getAbsoluteFile());
    }
}
