/*
 *  Copyright (C) 2011 John Casey.
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *  
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.apache.maven.repository.automirror;

import junit.framework.TestCase;

public class MirrorRoutingTableTest
    extends TestCase
{
    
    public void testFindMirrorMatch()
    {
        String canonical = "http://repo1.maven.org/maven2";
        
        MirrorRoutingTable table = new MirrorRoutingTable();
        MirrorRoute route = new MirrorRoute( "test", "http://nowhere.com/mirror", 10, true, canonical );
        table.addMirror( route );
        
        MirrorRoute result = table.getMirror( canonical );
        
        assertEquals( route, result );
    }

    public void testMirrorMatchNotFound()
    {
        String canonical = "http://repo1.maven.org/maven3";
        
        MirrorRoutingTable table = new MirrorRoutingTable();
        MirrorRoute route = new MirrorRoute( "test", "http://nowhere.com/mirror", 10, true, "http://repo1.maven.org/maven2" );
        table.addMirror( route );
        
        MirrorRoute result = table.getMirror( canonical );
        
        assertNull( result );
    }

}
