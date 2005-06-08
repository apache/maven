package org.apache.maven.it0022;

import java.io.*;
import java.net.URL;

import junit.framework.TestCase;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class SomeAddTest
    extends TestCase
{
    
    public void testAdd() throws Exception
    {
        String className = Person.class.getName().replace( '.', '/' ) + ".class";
        
        URL resource = Person.class.getClassLoader().getResource( className );
        
        File personFile = new File( resource.getPath() ).getAbsoluteFile();
        File dir = personFile.getParentFile();
        
        File testFile = new File( dir, "test.txt" );
        
        FileWriter writer = new FileWriter( testFile );
        writer.write("this is a test");
        writer.flush();
        writer.close();
    }

}
