package org.apache.maven.converter.old;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

public class Fix2
{
    public static void main( String[] args )
        throws Exception
    {
        SAXReader r = new SAXReader();
        
        Document d = r.read( new FileReader( args[0] ) );
        
        Element root = d.getRootElement();
        
        Element id = root.element( "id" );
        
        if ( id != null )
        {
            System.out.println( id.getName() );
            
            id.setName( "artifactId" );
        }
        
        File f = new File( args[0] );
        
        f.delete();
        
        OutputStream os = new FileOutputStream( args[0] );

        OutputFormat format = new OutputFormat();
        
        format.setIndentSize( 2 );
        
        format.setNewlines( true );
        
        format.setTrimText( true );

        XMLWriter writer = new XMLWriter( format );
        
        writer.setOutputStream( os );
        
        writer.write( d );
    }
}
