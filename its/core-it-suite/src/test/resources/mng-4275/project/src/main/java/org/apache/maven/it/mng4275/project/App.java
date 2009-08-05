package org.apache.maven.it.mng4275.project;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );

        org.apache.maven.it.mng4275.relocated.App.main( args );
    }
}
