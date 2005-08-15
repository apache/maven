import java.io.FileOutputStream;

public class DefaultTest
{
    public void testRun()
        throws Exception
    {
        FileOutputStream fout = new FileOutputStream("target/defaultTestTouchFile.txt");
        fout.write('!');
        fout.flush();
        fout.close();
    }
}
