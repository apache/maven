
import java.net.URLClassLoader;
import java.net.URL;

public class IsolatedClassLoader
    extends URLClassLoader
{
    private ClassLoader parent = ClassLoader.getSystemClassLoader();

    public IsolatedClassLoader()
    {
        super( new URL[0], null );
    }

    public void addURL( URL url )
    {
        super.addURL( url );
    }

    public synchronized Class loadClass( String className )
        throws ClassNotFoundException
    {
        Class c = findLoadedClass( className );

        ClassNotFoundException ex = null;

        if ( c == null )
        {
            try
            {
                c = findClass( className );
            }
            catch ( ClassNotFoundException e )
            {
                ex = e;

                if ( parent != null )
                {
                    c = parent.loadClass( className );
                }
            }
        }

        if ( c == null )
        {
            throw ex;
        }

        return c;
    }
}
