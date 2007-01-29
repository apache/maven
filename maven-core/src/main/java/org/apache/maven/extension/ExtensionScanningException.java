package org.apache.maven.extension;

public class ExtensionScanningException
    extends Exception
{

    public ExtensionScanningException( String message, Throwable cause )
    {
        super( message, cause );
    }

    public ExtensionScanningException( String message )
    {
        super( message );
    }

}
