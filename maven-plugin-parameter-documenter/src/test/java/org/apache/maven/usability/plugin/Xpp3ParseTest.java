package org.apache.maven.usability.plugin;

import junit.framework.TestCase;

import org.apache.maven.usability.plugin.io.xpp3.ParamdocXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Properties;

public class Xpp3ParseTest
    extends TestCase
{
    
    public void testParse() throws IOException, XmlPullParserException
    {
        InputStream testDocStream = getClass().getClassLoader().getResourceAsStream( "test.paramdoc.xml" );
        ParamdocXpp3Reader reader = new ParamdocXpp3Reader();
        
        ExpressionDocumentation documentation = reader.read(new InputStreamReader( testDocStream ) );
        
        Map exprs = documentation.getExpressionsBySyntax();
        
        Expression expr = (Expression) exprs.get( "localRepository" );
        
        assertNotNull( expr );
        
        Properties p = expr.getCliOptions();
        
        assertNotNull( p );
        
        assertEquals( 1, p.size() );
        
        assertEquals( "Override the local repository location on a per-build basis.", p.getProperty( "-Dmaven.repo.local=/path/to/local/repo" ) );
        
    }

}
