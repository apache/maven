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
package org.apache.maven.caching.hash;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HexUtilsTest
{

    @Test
    public void testEncodeToHex()
    {
        //array length = 8 left padded with zeroes
        assertEquals( "0000000000000000", HexUtils.encode( new byte[8] ) );
        assertEquals( "00", HexUtils.encode( new byte[1] ) );

        assertEquals( "0a", HexUtils.encode( new byte[]
        { 10
        } ) );
        assertEquals( "000000000000000a", HexUtils.encode( new byte[]
        { 0, 0, 0, 0, 0, 0, 0, 10
        } ) );

        assertEquals( "0100", HexUtils.encode( new byte[]
        { 1, 0
        } ) );
        assertEquals( "0000000000000101", HexUtils.encode( new byte[]
        { 0, 0, 0, 0, 0, 0, 1, 1
        } ) );
    }

    @Test
    public void testDecodeHex()
    {
        assertArrayEquals( new byte[]
        { 0
        }, HexUtils.decode( "00" ) );
        assertArrayEquals( new byte[]
        { 10
        }, HexUtils.decode( "0a" ) );
        assertArrayEquals( new byte[]
        { 10
        }, HexUtils.decode( "0A" ) );
        assertArrayEquals( new byte[]
        { 1, 0
        }, HexUtils.decode( "0100" ) );
    }

}
