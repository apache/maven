package util;

import java.net.URL;
import java.net.URLClassLoader;

public class IsolatedClassLoader
    extends URLClassLoader
{
    private ClassLoader parent;

    public IsolatedClassLoader()
    {
        this( ClassLoader.getSystemClassLoader() );
    }

    public IsolatedClassLoader( ClassLoader parent )
    {
        super( new URL[0] );
        this.parent = parent;
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
