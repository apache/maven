package org.apache.maven.acm;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id:$
 */
public interface ParameterProvider
{
    String getParameter( String system, String key )
        throws SystemProviderSourceException;
}
