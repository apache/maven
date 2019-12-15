package org.apache.maven.model.building;

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

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.condition.OS.WINDOWS;

/**
 * Test that validate the solution of MNG-6261 issue
 */
class FileModelSourceTest
{

    /**
     * Test of equals method, of class FileModelSource.
     */
    @Test
    void testEquals() throws Exception
    {
        File tempFile = createTempFile( "pomTest" );
        FileModelSource instance = new FileModelSource( tempFile );

        assertThat(instance.equals( null )).isFalse();
        assertThat( instance.equals( new Object() ) ).isFalse();
        assertThat( instance.equals( instance ) ).isTrue();
        assertThat( instance.equals( new FileModelSource( tempFile ) ) ).isTrue();
    }

    @Test
    @EnabledOnOs( WINDOWS )
    void testWindowsPaths() throws Exception
    {
        File upperCaseFile = createTempFile( "TESTE" );
        String absolutePath = upperCaseFile.getAbsolutePath();
        File lowerCaseFile = new File( absolutePath.toLowerCase() );

        FileModelSource upperCaseFileSouce = new FileModelSource( upperCaseFile );
        FileModelSource lowerCaseFileSouce = new FileModelSource( lowerCaseFile );

        assertThat( upperCaseFileSouce.equals( lowerCaseFileSouce ) ).isTrue();
    }

    private File createTempFile( String name ) throws IOException
    {
        File tempFile = File.createTempFile( name, ".xml" );
        tempFile.deleteOnExit();
        return tempFile;
    }

}
