package util;

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

import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileInputStream;

/**
 * Parse an XML file.
 *
 * @version $Id$
 */
public abstract class AbstractReader
    extends DefaultHandler
{
    private SAXParserFactory saxFactory;

    public boolean parse( File file )
    {
        try
        {
            saxFactory = SAXParserFactory.newInstance();

            SAXParser parser = saxFactory.newSAXParser();

            InputSource is = new InputSource( new FileInputStream( file ) );

            parser.parse( is, this );

            return true;
        }
        catch ( Exception e )
        {
            e.printStackTrace();

            return false;
        }
    }

    public void warning( SAXParseException spe )
    {
        printParseError( "Warning", spe );
    }

    public void error( SAXParseException spe )
    {
        printParseError( "Error", spe );
    }

    public void fatalError( SAXParseException spe )
    {
        printParseError( "Fatal Error", spe );
    }

    private final void printParseError( String type, SAXParseException spe )
    {
        System.err.println( type + " [line " + spe.getLineNumber() + ", row " + spe.getColumnNumber() + "]: " +
                            spe.getMessage() );
    }
}
