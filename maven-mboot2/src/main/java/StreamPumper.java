
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id$
 */
public class StreamPumper
    extends Thread
{
    private static final int BUFFER_SIZE = 512;

    private BufferedInputStream stream;
    private boolean endOfStream = false;
    private int SLEEP_TIME = 5;
    private OutputStream out;

    public StreamPumper( BufferedInputStream is, OutputStream out )
    {
        this.stream = is;
        this.out = out;
    }

    public void pumpStream() throws IOException
    {
        byte[] buf = new byte[BUFFER_SIZE];
        if ( !endOfStream )
        {
            int bytesRead = stream.read( buf, 0, BUFFER_SIZE );

            if ( bytesRead > 0 )
            {
                out.write( buf, 0, bytesRead );
            }
            else if ( bytesRead == -1 )
            {
                endOfStream = true;
            }
        }
    }

    public void run()
    {
        try
        {
            while ( !endOfStream )
            {
                pumpStream();
                sleep( SLEEP_TIME );
            }
        }
        catch ( Exception e )
        {
            // getLogger().warn("Jikes.run()", e);
        }
    }
}
