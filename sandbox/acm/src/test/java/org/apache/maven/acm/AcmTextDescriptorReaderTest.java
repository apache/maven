/*
 * Copyright (c) 2005 Your Corporation. All Rights Reserved.
 */
package org.apache.maven.acm;

import junit.framework.TestCase;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileWriter;
import java.io.Writer;
import java.io.PrintWriter;
import java.io.File;

import org.apache.maven.acm.model.Model;
import org.apache.maven.acm.model.Environment;
import org.apache.maven.acm.text.AcmTextDescriptorReader;
import org.apache.maven.acm.text.AcmTextDescriptorWriter;
import org.apache.maven.acm.convert.PropertiesConverter;
import org.apache.maven.acm.convert.PropertiesGenerator;

/**
 * Unit test for simple App.
 *
 * @author <a href="mailto:jason@zenplex.com">Jason van Zyl</a>
 */
public class AcmTextDescriptorReaderTest
    extends TestCase
{
    private String basedir;

    protected void setUp()
        throws Exception
    {
        basedir = System.getProperty( "basedir" );
    }

    public void testTextDescriptorReaderTest()
        throws Exception
    {
        Model model = getModel();

        testModel( model );

        writeModel( model, "foo.txt" );
    }

    protected void writeModel( Model model, String file )
        throws Exception
    {
        PrintWriter writer = new PrintWriter( new FileWriter( file ) );

        AcmTextDescriptorWriter w = new AcmTextDescriptorWriter();

        w.write( writer, model );
    }

    protected void testModel( Model model )
        throws Exception
    {
        assertEquals( 4, model.getEnvironments().size() );

        Environment dev = model.getEnvironment( "dev" );

        assertNotNull( dev );

        assertEquals( "devValue", dev.getProperty( "tas.editor.stamp" ) );
    }

    protected Model getModel()
        throws Exception
    {
        AcmTextDescriptorReader reader = new AcmTextDescriptorReader();

        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream( "config.txt" );

        Model model = reader.read( new InputStreamReader( is ) );

        return model;
    }

    public void testPropertiesConverter()
        throws Exception
    {
        PropertiesConverter converter = new PropertiesConverter();

        Model model = converter.convert( new File( basedir, "config").getPath() );

        writeModel( model, "toyota.config" );

        PropertiesGenerator pg = new PropertiesGenerator();

        pg.generate( model, new File( basedir, "target" ).getPath(), "t3_" );
    }
}
