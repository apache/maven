
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class JavacCompiler
    extends AbstractCompiler
{
    static final int OUTPUT_BUFFER_SIZE = 1024;

    public JavacCompiler()
    {
    }

    public List compile( String[] classpathElements, String[] sourceDirectories, String destinationDirectory )
        throws Exception
    {
        /*
        for ( int i = 0; i < classpathElements.length; i++ )
        {
            System.out.println( "classpathElement = " + classpathElements[i] );
        }
        */

        File destinationDir = new File( destinationDirectory );

        if ( !destinationDir.exists() )
        {
            destinationDir.mkdirs();
        }

        String[] sources = getSourceFiles( sourceDirectories );

        int j = 5;

        String[] args = new String[sources.length + j];

        args[0] = "-d";

        args[1] = destinationDir.getAbsolutePath();

        args[2] = "-nowarn";

        args[3] = "-classpath";

        args[4] = getClasspathString( classpathElements );

        for ( int i = 0; i < sources.length; i++ )
        {
            args[i + j] = sources[i];
        }

        IsolatedClassLoader cl = new IsolatedClassLoader();

        File toolsJar = new File( System.getProperty( "java.home" ), "../lib/tools.jar" );

        cl.addURL( toolsJar.toURL() );

        Class c = cl.loadClass( "sun.tools.javac.Main" );

        Constructor cons = c.getConstructor( new Class[]{OutputStream.class, String.class} );

        ByteArrayOutputStream err = new ByteArrayOutputStream();

        Object compiler = cons.newInstance( new Object[]{err, "javac"} );

        Method compile = c.getMethod( "compile", new Class[]{String[].class} );

        Boolean ok = (Boolean) compile.invoke( compiler, new Object[]{args} );

        List messages = parseModernStream( new BufferedReader( new InputStreamReader( new ByteArrayInputStream( err.toByteArray() ) ) ) );

        return messages;
    }

    protected List parseModernStream( BufferedReader input )
        throws IOException
    {
        List errors = new ArrayList();

        String line = null;

        StringBuffer buffer = null;

        while ( true )
        {
            // cleanup the buffer
            buffer = new StringBuffer(); // this is quicker than clearing it

            // most errors terminate with the '^' char
            do
            {
                if ( ( line = input.readLine() ) == null )
                {
                    return errors;
                }

                buffer.append( line );

                buffer.append( '\n' );
            }
            while ( !line.endsWith( "^" ) );

            // add the error bean
            errors.add( parseModernError( buffer.toString() ) );
        }
    }

    private CompilerError parseModernError( String error )
    {
        StringTokenizer tokens = new StringTokenizer( error, ":" );

        try
        {
            String file = tokens.nextToken();

            if ( file.length() == 1 )
            {
                file = new StringBuffer( file ).append( ":" ).append( tokens.nextToken() ).toString();
            }

            int line = Integer.parseInt( tokens.nextToken() );

            String message = tokens.nextToken( "\n" ).substring( 1 );

            String context = tokens.nextToken( "\n" );

            String pointer = tokens.nextToken( "\n" );

            int startcolumn = pointer.indexOf( "^" );

            int endcolumn = context.indexOf( " ", startcolumn );

            if ( endcolumn == -1 )
            {
                endcolumn = context.length();
            }

            return new CompilerError( file, false, line, startcolumn, line, endcolumn, message );
        }
        catch ( NoSuchElementException nse )
        {
            return new CompilerError( "no more tokens - could not parse error message: " + error );
        }
        catch ( Exception nse )
        {
            return new CompilerError( "could not parse error message: " + error );
        }
    }

    public String toString()
    {
        return "Sun Javac Compiler";
    }
}
