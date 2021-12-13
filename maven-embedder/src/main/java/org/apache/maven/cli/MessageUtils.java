package org.apache.maven.cli;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class
 * TODO: merge into maven-shared-utils MessageUtils class
 */
public class MessageUtils
{

    private static final Pattern LAST_ANSI_SEQUENCE = Pattern.compile( "(\u001B\\[[;\\d]*[ -/]*[@-~])[^\u001B]*$" );

    private static final String ANSI_RESET = "\u001B\u005Bm";

    public static List<String> splitLines( String msg )
    {
        List<String> coloredLines = new ArrayList<>();
        String[] lines = msg.split( "(\r\n)|(\r)|(\n)" );
        String currentColor = "";

        for ( String line : lines )
        {
            // look for last ANSI escape sequence to check if nextColor
            String nextColor = currentColor;
            Matcher matcher = LAST_ANSI_SEQUENCE.matcher( line );
            if ( matcher.find() )
            {
                nextColor = matcher.group( 1 );
                if ( ANSI_RESET.equals( nextColor ) )
                {
                    // last ANSI escape code is reset: no next color
                    nextColor = "";
                }
            }

            // effective line, with reset if end is colored
            coloredLines.add( currentColor + line + ( "".equals( nextColor ) ? "" : ANSI_RESET ) );

            currentColor = nextColor;
        }
        return coloredLines;
    }
}
