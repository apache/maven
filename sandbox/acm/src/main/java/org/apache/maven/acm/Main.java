/*
 * Copyright (c) 2005 Your Corporation. All Rights Reserved.
 */
package org.apache.maven.acm;

import org.apache.maven.acm.convert.PropertiesConverter;
import org.apache.maven.acm.model.Model;
import org.apache.maven.acm.text.AcmTextDescriptorWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * Unit test for simple App.
 *
 * @author <a href="mailto:jason@zenplex.com">Jason van Zyl</a>
 */
public class Main
{
    static protected void writeModel( Model model, String file )
        throws Exception
    {
        PrintWriter writer = new PrintWriter( new FileWriter( file ) );

        AcmTextDescriptorWriter w = new AcmTextDescriptorWriter();

        w.write( writer, model );
    }

    static public void main( String[] args )
        throws Exception
    {
        String sourceDirectory = args[0];

        String modelFile = args[1];

        PropertiesConverter converter = new PropertiesConverter();

        Model model = converter.convert( sourceDirectory );

        writeModel( model, modelFile );
    }
}
