package util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

public final class IOUtil
{
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    public static void copy( final InputStream input, final OutputStream output )
        throws IOException
    {
        copy( input, output, DEFAULT_BUFFER_SIZE );
    }

    public static void copy( final InputStream input,
                             final OutputStream output,
                             final int bufferSize )
        throws IOException
    {
        final byte[] buffer = new byte[bufferSize];
        int n = 0;
        while ( -1 != ( n = input.read( buffer ) ) )
        {
            output.write( buffer, 0, n );
        }
    }

    public static void copy( final Reader input, final Writer output )
        throws IOException
    {
        copy( input, output, DEFAULT_BUFFER_SIZE );
    }

    /**
     * Copy chars from a <code>Reader</code> to a <code>Writer</code>.
     *
     * @param bufferSize Size of internal buffer to use.
     */
    public static void copy( final Reader input, final Writer output, final int bufferSize )
        throws IOException
    {
        final char[] buffer = new char[bufferSize];
        int n = 0;
        while ( -1 != ( n = input.read( buffer ) ) )
        {
            output.write( buffer, 0, n );
        }
        output.flush();
    }

    public static void copy( final InputStream input, final Writer output )
        throws IOException
    {
        copy( input, output, DEFAULT_BUFFER_SIZE );
    }

    public static void copy( final InputStream input, final Writer output, final int bufferSize )
        throws IOException
    {
        final InputStreamReader in = new InputStreamReader( input );
        copy( in, output, bufferSize );
    }
}
