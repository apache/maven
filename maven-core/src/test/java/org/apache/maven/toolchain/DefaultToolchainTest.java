package org.apache.maven.toolchain;

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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;

import org.apache.maven.toolchain.java.JavaToolchainImpl;
import org.apache.maven.toolchain.model.PersistedToolchains;
import org.apache.maven.toolchain.model.ToolchainModel;
import org.apache.maven.toolchain.model.io.xpp3.MavenToolchainsXpp3Reader;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Test;

public class DefaultToolchainTest
{
    private MavenToolchainsXpp3Reader reader = new MavenToolchainsXpp3Reader();

    @Test
    public void testEquals() throws Exception
    {
        InputStream jdksIS = null;
        InputStream jdksExtraIS = null;
        try
        {
            jdksIS = ToolchainModel.class.getResourceAsStream( "toolchains-jdks.xml" );
            jdksExtraIS = ToolchainModel.class.getResourceAsStream( "toolchains-jdks-extra.xml" );

            PersistedToolchains jdks = reader.read( jdksIS );
            PersistedToolchains jdksExtra = reader.read( jdksExtraIS );

            JavaToolchainImpl tc1 = new JavaToolchainImpl( jdks.getToolchains().get( 0 ), null );
            JavaToolchainImpl tc2 = new JavaToolchainImpl( jdksExtra.getToolchains().get( 0 ), null );

            assertTrue( tc1.equals( tc1 ) );
            assertFalse( tc1.equals( tc2 ) );
            assertFalse( tc2.equals( tc1 ) );
            assertTrue( tc2.equals( tc2 ) );
        }
        finally
        {
            IOUtil.close( jdksIS );
            IOUtil.close( jdksExtraIS );
        }
    }
}
