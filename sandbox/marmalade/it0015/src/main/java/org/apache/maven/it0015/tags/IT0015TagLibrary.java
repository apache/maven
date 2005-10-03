package org.apache.maven.it0015.tags;

import org.codehaus.marmalade.metamodel.AbstractMarmaladeTagLibrary;

/**
 * @author jdcasey
 */
public class IT0015TagLibrary
    extends AbstractMarmaladeTagLibrary
{
    
    public IT0015TagLibrary()
    {
        registerTag("writeFile", WriteFileTag.class);
    }

}
