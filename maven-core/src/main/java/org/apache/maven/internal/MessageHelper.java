package org.apache.maven.internal;

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

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to format warning messages to the console
 */
public class MessageHelper
{

    private static final int DEFAULT_MAX_SIZE = 65;
    private static final char BOX_CHAR = '*';

    public static String separatorLine()
    {
        StringBuilder sb = new StringBuilder( DEFAULT_MAX_SIZE );
        repeat( sb, '*', DEFAULT_MAX_SIZE );
        return sb.toString();
    }

    public static List<String> formatWarning( String... lines )
    {
        int size = DEFAULT_MAX_SIZE;
        int rem = size - 4;
        List<String> result = new ArrayList<>();
        StringBuilder sb = new StringBuilder( size );
        // first line
        sb.setLength( 0 );
        repeat( sb, BOX_CHAR, size );
        result.add( sb.toString() );
        // lines
        for ( String line : lines )
        {
            sb.setLength( 0 );
            String[] words = line.split( " " );
            for ( String word : words )
            {
                if ( sb.length() >= rem - word.length() - ( sb.length() > 0 ? 1 : 0 ) )
                {
                    repeat( sb, ' ', rem - sb.length() );
                    result.add( BOX_CHAR + " " + sb + " " + BOX_CHAR );
                    sb.setLength( 0 );
                }
                if ( sb.length() > 0 )
                {
                    sb.append( ' ' );
                }
                sb.append( word );
            }

            while ( sb.length() < rem )
            {
                sb.append( ' ' );
            }
            result.add( BOX_CHAR + " " + sb + " " + BOX_CHAR );
        }
        // last line
        sb.setLength( 0 );
        repeat( sb, BOX_CHAR, size );
        result.add( sb.toString() );
        return result;
    }

    private static void repeat( StringBuilder sb, char c, int nb )
    {
        for ( int i = 0; i < nb; i++ )
        {
            sb.append( c );
        }
    }
}
