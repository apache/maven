package org.apache.maven.it;

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
import java.io.FileInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

/**
 * @author Benjamin Bentmann
 */
class ItUtils
{

    public static String calcHash( File file, String algo )
        throws Exception
    {
        MessageDigest digester = MessageDigest.getInstance( algo );

        FileInputStream is = new FileInputStream( file );
        DigestInputStream dis;
        try
        {
            dis = new DigestInputStream( is, digester );

            for ( byte[] buffer = new byte[1024 * 4]; dis.read( buffer ) >= 0; )
            {
                // just read it
            }
        }
        finally
        {
            is.close();
        }

        byte[] digest = digester.digest();

        StringBuffer hash = new StringBuffer( digest.length * 2 );

        for ( int i = 0; i < digest.length; i++ )
        {
            int b = digest[i] & 0xFF;

            if ( b < 0x10 )
            {
                hash.append( '0' );
            }

            hash.append( Integer.toHexString( b ) );
        }

        return hash.toString();
    }

}
