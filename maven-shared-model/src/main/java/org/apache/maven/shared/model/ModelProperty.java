package org.apache.maven.shared.model;

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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps a URI to a string value, which may be null. This class is immutable.
 */
public final class ModelProperty
{

    /**
     * A pattern used for finding pom, project and env properties
     */
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile( "\\$\\{(pom\\.|project\\.|env\\.)?([^}]+)\\}" );

    /**
     * URI of the resource
     */
    private final String uri;

    /**
     * Value associated with the uri
     */
    private final String value;

    /**
     * The count of '/' within this model property's uri, which is the depth of its XML nodes.
     */
    private final int depth;

    /**
     * Value of this model property after interpolation
     */
    private String resolvedValue;

    /**
     * List of unresolved expressions within this model property's value
     */
    private final List<String> unresolvedExpressions;

    /**
     * Constructor
     *
     * @param uri   URI of the resource. May not be null
     * @param value Value associated with specified uri. Value may be null if uri does not map to primitive type.
     */
    public ModelProperty( String uri, String value )
    {
        if ( uri == null )
        {
            throw new IllegalArgumentException( "uri" );
        }
        this.uri = uri;
        this.value = value;
        resolvedValue = value;

        unresolvedExpressions = new ArrayList<String>();
        if ( value != null )
        {
            Matcher matcher = EXPRESSION_PATTERN.matcher( value );
            while ( matcher.find() )
            {
                unresolvedExpressions.add( matcher.group( 0 ) );
            }
        }

        String uriWithoutProperty;
        int index =  uri.lastIndexOf( "/" );
        if(index > -1) {
            uriWithoutProperty = uri.substring( 0, uri.lastIndexOf( "/" ) );
            if(uriWithoutProperty.endsWith("#property") || uriWithoutProperty.endsWith("combine.children") )
            {
                uriWithoutProperty = uriWithoutProperty.substring( 0, uriWithoutProperty.lastIndexOf( "/" ) );
            }
        }
        else
        {
            uriWithoutProperty = uri;
        }

        depth = uriWithoutProperty.split( "/" ).length;
    }

    /**
     * Returns URI key
     *
     * @return URI key
     */
    public String getUri()
    {
        return uri;
    }

    /**
     * Returns value for the URI key. Value may be null.
     *
     * @return value for the URI key. Value may be null
     */
    public String getValue()
    {
        return value;
    }

    /**
     * Value of this model property after interpolation. CDATA section will be added if needed.
     *
     * @return value of this model property after interpolation
     */
    public String getResolvedValue()
    {
        if( !uri.contains("#property") && resolvedValue != null && !resolvedValue.startsWith ("<![CDATA[")
                && (resolvedValue.contains( "=" ) || resolvedValue.contains( "<" )))
        {
            resolvedValue =  "<![CDATA[" + resolvedValue + "]]>";
        }
        return resolvedValue;
    }

    /**
     * Returns true if model property is completely interpolated, otherwise returns false.
     *
     * @return true if model property is completely interpolated, otherwise returns false
     */
    public boolean isResolved()
    {
        return unresolvedExpressions.isEmpty();
    }

    /**
     * Returns copy of the uninterpolated model property
     *
     * @return copy of the uninterpolated model property
     */
    public ModelProperty createCopyOfOriginal()
    {
        return new ModelProperty( uri, value );
    }

    /**
     * Returns the count of '/' within this model property's uri, which is the depth of its XML nodes.
     *
     * @return the count of '/' within this model property's uri, which is the depth of its XML nodes
     */
    public int getDepth()
    {
        return depth;
    }

    /**
     * Returns true if this model property is a direct parent of the specified model property, otherwise returns false.
     *
     * @param modelProperty the model property
     * @return true if this model property is a direct parent of the specified model property, otherwise returns false
     */
    public boolean isParentOf( ModelProperty modelProperty )
    {
        if ( Math.abs( depth - modelProperty.getDepth() ) > 1 )
        {
            return false;
        }
        if ( uri.equals( modelProperty.getUri() ) || uri.startsWith( modelProperty.getUri() ) )
        {
            return false;
        }
        return ( modelProperty.getUri().startsWith( uri ) );
    }

    /**
     * Returns this model property as an interpolator property, allowing the interpolation of model elements within
     * other model elements.
     *
     * @param baseUri the base uri of the model property
     * @return this model property as an interpolator property, allowing the interpolation of model elements within
     *         other model elements
     */
    public InterpolatorProperty asInterpolatorProperty( String baseUri )
    {
        if ( uri.contains( "#collection" ) || uri.contains("#set") || value == null )
        {
            return null;
        }
        String key = "${" + uri.replace( baseUri + "/", "" ).replace( "/", "." ) + "}";
        return new InterpolatorProperty( key, value );
    }

    /**
     * Resolves any unresolved model property expressions using the specified interpolator property
     *
     * @param property the interpolator property used to resolve
     */
    public void resolveWith( InterpolatorProperty property )
    {
        if ( property == null )
        {
            throw new IllegalArgumentException( "property: null" );
        }
        if ( isResolved() )
        {
            return;
        }
        for ( String expression : unresolvedExpressions )
        {
            if ( property.getKey().equals( expression ) )
            {
                resolvedValue = resolvedValue.replace( property.getKey(), property.getValue() );
                unresolvedExpressions.clear();
                Matcher matcher = EXPRESSION_PATTERN.matcher( resolvedValue );
                while ( matcher.find() )
                {
                    unresolvedExpressions.add( matcher.group( 0 ) );
                }
                break;
            }
        }
    }

    public String toString()
    {
        return "Uri = " + uri + ", Value = " + value + ", Resolved Value = " + resolvedValue + ", Hash = " +
            this.hashCode();
    }
}
