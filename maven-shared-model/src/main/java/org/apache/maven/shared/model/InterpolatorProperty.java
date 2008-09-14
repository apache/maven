package org.apache.maven.shared.model;

import java.util.*;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * Provides interpolator property information.
 */
public final class InterpolatorProperty
{
    /**
     * The key (or name) of the property
     */
    private final String key;

    /**
     * The value of the property
     */
    private final String value;

    /**
     * Metadata tag (general use)
     */
    private String tag;


    /**
     * Constructor
     *
     * @param key   the key (or name) of the property. May not be null
     * @param value the value of the property. May not be null.
     */
    public InterpolatorProperty( String key, String value )
    {
        this(key, value, null);
    }

    public InterpolatorProperty( String key, String value, String tag )
    {
        if ( key == null )
        {
            throw new IllegalArgumentException( "key: null" );
        }

        if ( value == null )
        {
            throw new IllegalArgumentException( "value: null" );
        }
        this.key = key;
        this.value = value;
        this.tag = tag;

    }

    /**
     * Returns key (or name) of property.
     *
     * @return key (or name) of property
     */
    public String getKey()
    {
        return key;
    }

    /**
     * Returns value of property.
     *
     * @return value of property
     */
    public String getValue()
    {
        return value;
    }

    public String getTag()
    {
        return tag;
    }

    public void setTag(String tag)
    {
        this.tag = tag;
    }

    /**
     * Returns true if key values match, otherwise returns false.
     *
     * @param o interpolator property to compare
     * @return true if key values match, otherwise returns false
     */
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        InterpolatorProperty that = (InterpolatorProperty) o;

        if ( !key.equals( that.key ) )
        {
            return false;
        }

        return true;
    }

    /**
     * Returns hash code of interpolator property key.
     *
     * @return hash code of interpolator property key
     */
    public int hashCode()
    {
        return key.hashCode();
    }

    public String toString()
    {
        return "Key = " + key + ", Value = " + value +  ", Hash = " +
            this.hashCode();
    }

    public static List<InterpolatorProperty> toInterpolatorProperties( Properties properties, String tag )
    {
        if( properties == null )
        {
            throw new IllegalArgumentException( "properties: null" );
        }

        List<InterpolatorProperty> interpolatorProperties = new ArrayList<InterpolatorProperty>();
        for ( Map.Entry<Object, Object> e : properties.entrySet() )
        {
            interpolatorProperties.add( new InterpolatorProperty( "${" + e.getKey() +"}", (String) e.getValue(), tag) );
        }
        return interpolatorProperties;
    }
}
