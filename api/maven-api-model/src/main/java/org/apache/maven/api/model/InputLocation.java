package org.apache.maven.api.model;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class InputLocation.
 */
public class InputLocation
    implements Serializable, InputLocationTracker
{
    private final int lineNumber;
    private final int columnNumber;
    private final InputSource source;
    private final Map<Object, InputLocation> locations;

    public InputLocation( InputSource source )
    {
        this.lineNumber = -1;
        this.columnNumber = -1;
        this.source = source;
        this.locations = Collections.singletonMap( 0, this );
    }

    public InputLocation( int lineNumber, int columnNumber )
    {
        this( lineNumber, columnNumber, null, null );
    }

    public InputLocation( int lineNumber, int columnNumber, InputSource source )
    {
        this( lineNumber, columnNumber, source, null );
    }

    public InputLocation( int lineNumber, int columnNumber, InputSource source, Object selfLocationKey )
    {
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.source = source;
            this.locations = selfLocationKey != null
                ?  Collections.singletonMap( selfLocationKey, this ) : Collections.emptyMap();
    }

    public InputLocation( int lineNumber, int columnNumber, InputSource source, Map<Object, InputLocation> locations )
    {
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.source = source;
        this.locations = ImmutableCollections.copy( locations );
    }

    public int getLineNumber()
    {
        return lineNumber;
    }

    public int getColumnNumber()
    {
        return columnNumber;
    }

    public InputSource getSource()
    {
        return source;
    }

    public InputLocation getLocation( Object key )
    {
        return locations != null ? locations.get( key ) : null;
    }

    public Map<Object, InputLocation> getLocations()
    {
        return locations;
    }

    /**
     * Method merge.
     */
    public static InputLocation merge( InputLocation target, InputLocation source, boolean sourceDominant )
    {
        if ( source == null )
        {
            return target;
        }
        else if ( target == null )
        {
            return source;
        }

        Map<Object, InputLocation> locations;
        Map<Object, InputLocation> sourceLocations = source.locations;
        Map<Object, InputLocation> targetLocations = target.locations;
        if ( sourceLocations == null )
        {
            locations = targetLocations;
        }
        else if ( targetLocations == null )
        {
            locations = sourceLocations;
        }
        else
        {
            locations = new LinkedHashMap<>();
            locations.putAll( sourceDominant ? targetLocations : sourceLocations );
            locations.putAll( sourceDominant ? sourceLocations : targetLocations );
        }

        return new InputLocation( target.getLineNumber(), target.getColumnNumber(), target.getSource(), locations );
    } //-- InputLocation merge( InputLocation, InputLocation, boolean )

    /**
     * Method merge.
     */
    public static InputLocation merge( InputLocation target, InputLocation source, Collection<Integer> indices )
    {
        if ( source == null )
        {
            return target;
        }
        else if ( target == null )
        {
            return source;
        }

        Map<Object, InputLocation> locations;
        Map<Object, InputLocation> sourceLocations = source.locations;
        Map<Object, InputLocation> targetLocations = target.locations;
        if ( sourceLocations == null )
        {
            locations = targetLocations;
        }
        else if ( targetLocations == null )
        {
            locations = sourceLocations;
        }
        else
        {
            locations = new LinkedHashMap<>();
            for ( int index : indices )
            {
                InputLocation location;
                if ( index < 0 )
                {
                    location = sourceLocations.get( ~index );
                }
                else
                {
                    location = targetLocations.get( index );
                }
                locations.put( locations.size(), location );
            }
        }

        return new InputLocation( target.getLineNumber(), target.getColumnNumber(), target.getSource(), locations );
    } //-- InputLocation merge( InputLocation, InputLocation, java.util.Collection )

    /**
     * Class StringFormatter.
     *
     * @version $Revision$ $Date$
     */
    public interface StringFormatter
    {

          //-----------/
         //- Methods -/
        //-----------/

        /**
         * Method toString.
         */
        String toString( InputLocation location );

    }

}
