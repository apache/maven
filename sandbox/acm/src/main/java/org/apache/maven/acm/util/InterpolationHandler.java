/*
 * Copyright (c) 2005 Your Corporation. All Rights Reserved.
 */
package org.apache.maven.acm.util;

import java.io.Writer;
import java.io.IOException;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @version $Id:$
 */
public interface InterpolationHandler
{
    public void interpolate( String key, Writer writer )
        throws IOException;
}
