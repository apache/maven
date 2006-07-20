package org.apache.maven.model.converter.plugins;

/*
 * Copyright 2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Utility class which features various methods for converting String-based property values.
 *
 * @author Dennis Lundberg
 * @version $Id$
 */
public class PropertyUtils
{
    static String convertOnOffToBoolean( String value )
    {
        if ( value != null )
        {
            if ( "on".equalsIgnoreCase( value ) )
            {
                return Boolean.TRUE.toString();
            }
            if ( "off".equalsIgnoreCase( value ) )
            {
                return Boolean.FALSE.toString();
            }
        }
        return null;
    }

    static String convertYesNoToBoolean( String value )
    {
        if ( value != null )
        {
            if ( "yes".equalsIgnoreCase( value ) )
            {
                return Boolean.TRUE.toString();
            }
            if ( "no".equalsIgnoreCase( value ) )
            {
                return Boolean.FALSE.toString();
            }
        }
        return null;
    }

    static String invertBoolean( String stringValue )
    {
        if ( stringValue != null )
        {
            boolean booleanValue = Boolean.valueOf( stringValue ).booleanValue();
            boolean invertedBooleanValue = !booleanValue;
            return new Boolean( invertedBooleanValue ).toString();
        }
        return null;
    }
}
