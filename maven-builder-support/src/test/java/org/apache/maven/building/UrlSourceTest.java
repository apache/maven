package org.apache.maven.building;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

import org.junit.Test;

public class UrlSourceTest
{

    @Test
    public void testUrlSource()
    {
        try
        {
            new UrlSource( null );
            fail( "Should fail, since you must specify a url" );
        }
        catch ( IllegalArgumentException e )
        {
            assertEquals( "no url specified", e.getMessage() );
        }
    }

    @Test
    public void testGetInputStream()
        throws Exception
    {
        URL txtFile = new File( "target/test-classes/source.txt" ).toURI().toURL();
        UrlSource source = new UrlSource( txtFile );

        Scanner scanner = null;
        InputStream is = null;
        try
        {
            is = source.getInputStream();

            scanner = new Scanner( is );
            assertEquals( "Hello World!", scanner.nextLine() );
        }
        finally
        {
            if ( scanner != null )
            {
                scanner.close();
            }
            if ( is != null )
            {
                is.close();
            }
        }
    }

    @Test
    public void testGetLocation()
        throws Exception
    {
        URL txtFile = new File( "target/test-classes/source.txt" ).toURI().toURL();
        UrlSource source = new UrlSource( txtFile );
        assertEquals( txtFile.toExternalForm(), source.getLocation() );
    }

}
