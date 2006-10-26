import java.io.FileOutputStream;

public class NotIncludedByDefault
{
    public void testRun()
        throws Exception
    {
        FileOutputStream fout = new FileOutputStream("target/testTouchFile.txt");
        fout.write('!');
        fout.flush();
        fout.close();
    }
}
