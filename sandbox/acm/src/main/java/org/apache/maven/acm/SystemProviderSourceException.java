/*
 * Copyright (c) 2005 Your Corporation. All Rights Reserved.
 */
package org.apache.maven.acm;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id:$
 */
public class SystemProviderSourceException
    extends Exception
{
    public SystemProviderSourceException( String message )
    {
        super( message );
    }

    public SystemProviderSourceException( Throwable cause )
    {
        super( cause );
    }

    public SystemProviderSourceException( String message, Throwable cause )
    {
        super( message, cause );
    }
}
