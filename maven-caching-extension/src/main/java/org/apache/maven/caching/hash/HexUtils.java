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

import java.nio.charset.StandardCharsets;

/**
 * HexUtils
 */
@SuppressWarnings( "checkstyle:MagicNumber" )
public class HexUtils
{

    private static final byte[] ENC_ARRAY;
    private static final byte[] DEC_ARRAY;

    static
    {
        ENC_ARRAY = new byte[16];
        DEC_ARRAY = new byte[256];
        for ( byte i = 0; i < 10; i++ )
        {
            ENC_ARRAY[i] = ( byte ) ( '0' + i );
            DEC_ARRAY['0' + i] = i;
        }
        for ( byte i = 10; i < 16; i++ )
        {
            ENC_ARRAY[i] = ( byte ) ( 'a' + i - 10 );
            DEC_ARRAY['a' + i - 10] = i;
            DEC_ARRAY['A' + i - 10] = i;
        }
    }

    public static String encode( byte[] hash )
    {
        byte[] hexChars = new byte[hash.length * 2];
        for ( int j = 0; j < hash.length; j++ )
        {
            int v = hash[j] & 0xFF;
            hexChars[j * 2] = ENC_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = ENC_ARRAY[v & 0x0F];
        }
        return new String( hexChars, StandardCharsets.US_ASCII );
    }

    public static byte[] decode( String hex )
    {
        int size = hex.length();
        if ( size % 2 != 0 )
        {
            throw new IllegalArgumentException( "String length should be even" );
        }
        byte[] bytes = new byte[size / 2];
        int idx = 0;
        for ( int i = 0; i < size; i += 2 )
        {
            bytes[idx++] = ( byte ) ( DEC_ARRAY[hex.charAt( i )] << 4 | DEC_ARRAY[hex.charAt( i + 1 )] );
        }
        return bytes;
    }

    public static byte[] toByteArray( long value )
    {
        byte[] result = new byte[8];
        for ( int i = 7; i >= 0; i-- )
        {
            result[i] = ( byte ) ( value & 0xFF );
            value >>= 8;
        }
        return result;
    }
}
