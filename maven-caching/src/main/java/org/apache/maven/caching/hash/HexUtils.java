package org.apache.maven.caching.hash;

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

import com.google.common.base.CharMatcher;
import com.google.common.io.BaseEncoding;

/**
 * HexUtils
 */
public class HexUtils
{

    private static final BaseEncoding ENCODING = BaseEncoding.base16().lowerCase();
    private static final CharMatcher MATCHER = CharMatcher.is( '0' );

    public static String encode( byte[] hash )
    {
        return trimLeadingZero( ENCODING.encode( hash ) );
    }

    public static byte[] decode( String hex )
    {
        return ENCODING.decode( padLeadingZero( hex ) );
    }

    private static String trimLeadingZero( String hex )
    {
        String value = MATCHER.trimLeadingFrom( hex );
        return value.isEmpty() ? "0" : value;
    }

    private static String padLeadingZero( String hex )
    {
        String value = hex.toLowerCase();
        return value.length() % 2 == 0 ? value : "0" + value;
    }

}
