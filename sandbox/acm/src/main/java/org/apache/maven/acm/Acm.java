package org.apache.maven.acm;

// Have a single parameter for a system
//
// system = {dev|test|qa|production}
//
// Need to be able to change these values at runtime and persist the changes
// so that they are preserved for future builds I need to keep this information
// outside of the build environment

// For toyota where can I store the values ...

public class Acm
    implements ParameterProvider
{
    public static final String ACM_SYSTEM_ID = "acm.system.id";

    private String systemId;

    private ParameterProvider provider;

    public Acm( ParameterProvider parameterProvider )
    {
        this( System.getProperty( ACM_SYSTEM_ID ), parameterProvider );
    }

    public Acm( String systemId, ParameterProvider parameterProvider )
    {
        this.systemId = systemId;

        this.provider = parameterProvider;
    }

    public String getParameter( String key )
        throws SystemProviderSourceException
    {
        return getParameter( getSystemId(), key );
    }

    public String getParameter( String system, String key )
        throws SystemProviderSourceException
    {
        if ( system == null )
        {
            return null;
        }

        return provider.getParameter( system, key );
    }

    public String getSystemId()
    {
        return systemId;
    }

}
