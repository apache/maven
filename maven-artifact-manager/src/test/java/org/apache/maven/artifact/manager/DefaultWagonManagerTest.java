package org.apache.maven.artifact.manager;

/* ====================================================================
 *   Copyright 2001-2004 The Apache Software Foundation.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ====================================================================
 */

import org.apache.maven.wagon.UnsupportedProtocolException;
import org.apache.maven.wagon.Wagon;
import org.codehaus.plexus.PlexusTestCase;

/**
 * @author <a href="michal.maczka@dimatics.com">Michal Maczka</a>
 * @version $Id$
 */
public class DefaultWagonManagerTest
    extends PlexusTestCase
{
    public void testDefaultWagonManager()
        throws Exception
    {
        WagonManager wagonManager = (WagonManager) lookup( WagonManager.ROLE );

        Wagon wagon = null;

        wagon = (Wagon) wagonManager.getWagon( "a" );

        assertNotNull( wagon );

        wagon = (Wagon) wagonManager.getWagon( "b1" );

        assertNotNull( wagon );

        wagon = (Wagon) wagonManager.getWagon( "b2" );

        assertNotNull( wagon );

        wagon = (Wagon) wagonManager.getWagon( "c" );

        assertNotNull( wagon );

        try
        {
            wagon = (Wagon) wagonManager.getWagon( "d" );

            fail( "Expected :" + UnsupportedProtocolException.class.getName() );
        }
        catch ( UnsupportedProtocolException e )
        {
            //ok
            assertTrue( true );
        }
    }
}
