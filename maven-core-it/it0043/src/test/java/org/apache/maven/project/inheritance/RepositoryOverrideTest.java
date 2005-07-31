import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;

import junit.framework.TestCase;

public class RepositoryOverrideTest extends TestCase
{
    public void testPOM() throws Exception
    {
        
        BufferedInputStream in = new BufferedInputStream( new FileInputStream("target/effective-pom.xml") );

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int rd = 0;
        byte [] buffer = new byte[512];

        while ( ( rd = in.read( buffer ) ) > 0 )
        {
            out.write( buffer, 0, rd );
        }

        assertEquals( -1, out.toString().indexOf("repo1.maven.org") );
    }
}
