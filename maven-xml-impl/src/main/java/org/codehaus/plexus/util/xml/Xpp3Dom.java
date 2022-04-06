package org.codehaus.plexus.util.xml;

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

import org.apache.maven.api.xml.Dom;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.XmlSerializer;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  NOTE: remove all the util code in here when separated, this class should be pure data.
 */
public class Xpp3Dom
        implements Serializable
{
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final Xpp3Dom[] EMPTY_DOM_ARRAY = new Xpp3Dom[0];

    public static final String CHILDREN_COMBINATION_MODE_ATTRIBUTE = "combine.children";

    public static final String CHILDREN_COMBINATION_MERGE = "merge";

    public static final String CHILDREN_COMBINATION_APPEND = "append";

    /**
     * This default mode for combining children DOMs during merge means that where element names match, the process will
     * try to merge the element data, rather than putting the dominant and recessive elements (which share the same
     * element name) as siblings in the resulting DOM.
     */
    public static final String DEFAULT_CHILDREN_COMBINATION_MODE = CHILDREN_COMBINATION_MERGE;

    public static final String SELF_COMBINATION_MODE_ATTRIBUTE = "combine.self";

    public static final String SELF_COMBINATION_OVERRIDE = "override";

    public static final String SELF_COMBINATION_MERGE = "merge";

    public static final String SELF_COMBINATION_REMOVE = "remove";

    /**
     * This default mode for combining a DOM node during merge means that where element names match, the process will
     * try to merge the element attributes and values, rather than overriding the recessive element completely with the
     * dominant one. This means that wherever the dominant element doesn't provide the value or a particular attribute,
     * that value or attribute will be set from the recessive DOM node.
     */
    public static final String DEFAULT_SELF_COMBINATION_MODE = SELF_COMBINATION_MERGE;

    private Xpp3Dom parent;
    private Dom dom;

    public Xpp3Dom( String name )
    {
        this.dom = new org.apache.maven.internal.xml.Xpp3Dom( name );
    }

    /**
     * @since 3.2.0
     * @param inputLocation The input location.
     * @param name The name of the Dom.
     */
    public Xpp3Dom( String name, Object inputLocation )
    {
        this.dom = new org.apache.maven.internal.xml.Xpp3Dom( name, null, null, null, inputLocation );
    }

    /**
     * Copy constructor.
     * @param src The source Dom.
     */
    public Xpp3Dom( Xpp3Dom src )
    {
        this( src, src.getName() );
    }

    /**
     * Copy constructor with alternative name.
     * @param src The source Dom.
     * @param name The name of the Dom.
     */
    public Xpp3Dom( Xpp3Dom src, String name )
    {
        this.dom = new org.apache.maven.internal.xml.Xpp3Dom( src.dom, name );
    }

    public Xpp3Dom( Dom dom )
    {
        this.dom = dom;
    }

    public Xpp3Dom( Dom dom, Xpp3Dom parent )
    {
        this.dom = dom;
        this.parent = parent;
    }

    public Dom getDom()
    {
        return dom;
    }

    // ----------------------------------------------------------------------
    // Name handling
    // ----------------------------------------------------------------------

    public String getName()
    {
        return dom.getName();
    }

    // ----------------------------------------------------------------------
    // Value handling
    // ----------------------------------------------------------------------

    public String getValue()
    {
        return dom.getValue();
    }

    public void setValue( String value )
    {
        update( new org.apache.maven.internal.xml.Xpp3Dom(
                dom.getName(), value, dom.getAttributes(), dom.getChildren(), dom.getInputLocation() ) );
    }

    // ----------------------------------------------------------------------
    // Attribute handling
    // ----------------------------------------------------------------------

    public String[] getAttributeNames()
    {
        return dom.getAttributes().keySet().toArray( EMPTY_STRING_ARRAY );
    }

    public String getAttribute( String name )
    {
        return dom.getAttribute( name );
    }

    /**
     *
     * @param name name of the attribute to be removed
     * @return <code>true</code> if the attribute has been removed
     * @since 3.4.0
     */
    public boolean removeAttribute( String name )
    {
        if ( ! StringUtils.isEmpty( name ) )
        {
            Map<String, String> attrs = new HashMap<>( dom.getAttributes() );
            boolean ret = attrs.remove( name ) != null;
            if ( ret )
            {
                update( new org.apache.maven.internal.xml.Xpp3Dom(
                        dom.getName(), dom.getValue(), attrs, dom.getChildren(), dom.getInputLocation() ) );
            }
            return ret;
        }
        return false;
    }

    /**
     * Set the attribute value
     *
     * @param name String not null
     * @param value String not null
     */
    public void setAttribute( String name, String value )
    {
        if ( null == value )
        {
            throw new NullPointerException( "Attribute value can not be null" );
        }
        if ( null == name )
        {
            throw new NullPointerException( "Attribute name can not be null" );
        }
        Map<String, String> attrs = new HashMap<>( dom.getAttributes() );
        attrs.put( name, value );
        update( new org.apache.maven.internal.xml.Xpp3Dom(
                dom.getName(), dom.getValue(), attrs, dom.getChildren(), dom.getInputLocation() ) );
    }

    // ----------------------------------------------------------------------
    // Child handling
    // ----------------------------------------------------------------------

    public Xpp3Dom getChild( int i )
    {
        return new Xpp3Dom( dom.getChildren().get( i ), this );
    }

    public Xpp3Dom getChild( String name )
    {
        Dom child = dom.getChild( name );
        return child != null ? new Xpp3Dom( child, this ) : null;
    }

    public void addChild( Xpp3Dom xpp3Dom )
    {
        List<Dom> children = new ArrayList<>( dom.getChildren() );
        children.add( xpp3Dom.dom );
        update( new org.apache.maven.internal.xml.Xpp3Dom(
                dom.getName(), dom.getValue(), dom.getAttributes(), children, dom.getInputLocation() ) );
    }

    public Xpp3Dom[] getChildren()
    {
        return dom.getChildren().stream()
                .map( d -> new Xpp3Dom( d, this ) ).toArray( Xpp3Dom[]::new );
    }

    public Xpp3Dom[] getChildren( String name )
    {
        return dom.getChildren().stream()
                .filter( c -> c.getName().equals( name ) )
                .map( d -> new Xpp3Dom( d, this ) ).toArray( Xpp3Dom[]::new );
    }

    public int getChildCount()
    {
        return dom.getChildren().size();
    }

    public void removeChild( int i )
    {
        List<Dom> children = new ArrayList<>( dom.getChildren() );
        children.remove( i );
        update( new org.apache.maven.internal.xml.Xpp3Dom(
                dom.getName(), dom.getValue(), dom.getAttributes(), children, dom.getInputLocation() ) );
    }

    public void removeChild( Xpp3Dom child )
    {
        List<Dom> children = new ArrayList<>( dom.getChildren() );
        children.remove( child.dom );
        update( new org.apache.maven.internal.xml.Xpp3Dom(
                dom.getName(), dom.getValue(), dom.getAttributes(), children, dom.getInputLocation() ) );
    }

    // ----------------------------------------------------------------------
    // Parent handling
    // ----------------------------------------------------------------------

    public Xpp3Dom getParent()
    {
        throw new UnsupportedOperationException();
    }

    public void setParent( Xpp3Dom parent )
    {
    }

    // ----------------------------------------------------------------------
    // Input location handling
    // ----------------------------------------------------------------------

    /**
     * @since 3.2.0
     * @return input location
     */
    public Object getInputLocation()
    {
        return dom.getInputLocation();
    }

    /**
     * @since 3.2.0
     * @param inputLocation input location to set
     */
    public void setInputLocation( Object inputLocation )
    {
        update( new org.apache.maven.internal.xml.Xpp3Dom(
                dom.getName(), dom.getValue(), dom.getAttributes(), dom.getChildren(), inputLocation ) );
    }

    // ----------------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------------

    public void writeToSerializer( String namespace, XmlSerializer serializer )
            throws IOException
    {
        // TODO: WARNING! Later versions of plexus-utils psit out an <?xml ?> header due to thinking this is a new
        // document - not the desired behaviour!
        SerializerXMLWriter xmlWriter = new SerializerXMLWriter( namespace, serializer );
        Xpp3DomWriter.write( xmlWriter, this );
        if ( xmlWriter.getExceptions().size() > 0 )
        {
            throw (IOException) xmlWriter.getExceptions().get( 0 );
        }
    }

    /**
     * Merges one DOM into another, given a specific algorithm and possible override points for that algorithm.<p>
     * The algorithm is as follows:
     * <ol>
     * <li> if the recessive DOM is null, there is nothing to do... return.</li>
     * <li> Determine whether the dominant node will suppress the recessive one (flag=mergeSelf).
     *   <ol type="A">
     *   <li> retrieve the 'combine.self' attribute on the dominant node, and try to match against 'override'...
     *        if it matches 'override', then set mergeSelf == false...the dominant node suppresses the recessive one
     *        completely.</li>
     *   <li> otherwise, use the default value for mergeSelf, which is true...this is the same as specifying
     *        'combine.self' == 'merge' as an attribute of the dominant root node.</li>
     *   </ol></li>
     * <li> If mergeSelf == true
     *   <ol type="A">
     *   <li> if the dominant root node's value is empty, set it to the recessive root node's value</li>
     *   <li> For each attribute in the recessive root node which is not set in the dominant root node, set it.</li>
     *   <li> Determine whether children from the recessive DOM will be merged or appended to the dominant DOM as
     *        siblings (flag=mergeChildren).
     *     <ol type="i">
     *     <li> if childMergeOverride is set (non-null), use that value (true/false)</li>
     *     <li> retrieve the 'combine.children' attribute on the dominant node, and try to match against
     *          'append'...</li>
     *     <li> if it matches 'append', then set mergeChildren == false...the recessive children will be appended as
     *          siblings of the dominant children.</li>
     *     <li> otherwise, use the default value for mergeChildren, which is true...this is the same as specifying
     *         'combine.children' == 'merge' as an attribute on the dominant root node.</li>
     *     </ol></li>
     *   <li> Iterate through the recessive children, and:
     *     <ol type="i">
     *     <li> if mergeChildren == true and there is a corresponding dominant child (matched by element name),
     *          merge the two.</li>
     *     <li> otherwise, add the recessive child as a new child on the dominant root node.</li>
     *     </ol></li>
     *   </ol></li>
     * </ol>
     */
    private static void mergeIntoXpp3Dom( Xpp3Dom dominant, Xpp3Dom recessive, Boolean childMergeOverride )
    {
        // TODO: share this as some sort of assembler, implement a walk interface?
        if ( recessive == null )
        {
            return;
        }
        dominant.dom = dominant.dom.merge( recessive.dom, childMergeOverride );
    }

    /**
     * Merge two DOMs, with one having dominance in the case of collision.
     *
     * @see #CHILDREN_COMBINATION_MODE_ATTRIBUTE
     * @see #SELF_COMBINATION_MODE_ATTRIBUTE
     * @param dominant The dominant DOM into which the recessive value/attributes/children will be merged
     * @param recessive The recessive DOM, which will be merged into the dominant DOM
     * @param childMergeOverride Overrides attribute flags to force merging or appending of child elements into the
     *            dominant DOM
     * @return merged DOM
     */
    public static Xpp3Dom mergeXpp3Dom( Xpp3Dom dominant, Xpp3Dom recessive, Boolean childMergeOverride )
    {
        if ( dominant != null )
        {
            mergeIntoXpp3Dom( dominant, recessive, childMergeOverride );
            return dominant;
        }
        return recessive;
    }

    /**
     * Merge two DOMs, with one having dominance in the case of collision. Merge mechanisms (vs. override for nodes, or
     * vs. append for children) is determined by attributes of the dominant root node.
     *
     * @see #CHILDREN_COMBINATION_MODE_ATTRIBUTE
     * @see #SELF_COMBINATION_MODE_ATTRIBUTE
     * @param dominant The dominant DOM into which the recessive value/attributes/children will be merged
     * @param recessive The recessive DOM, which will be merged into the dominant DOM
     * @return merged DOM
     */
    public static Xpp3Dom mergeXpp3Dom( Xpp3Dom dominant, Xpp3Dom recessive )
    {
        if ( dominant != null )
        {
            mergeIntoXpp3Dom( dominant, recessive, null );
            return dominant;
        }
        return recessive;
    }

    // ----------------------------------------------------------------------
    // Standard object handling
    // ----------------------------------------------------------------------

    @Override
    public boolean equals( Object obj )
    {
        if ( obj == this )
        {
            return true;
        }

        if ( !( obj instanceof Xpp3Dom ) )
        {
            return false;
        }

        Xpp3Dom dom = (Xpp3Dom) obj;
        return this.dom.equals( dom.dom );
    }

    @Override
    public int hashCode()
    {
        return dom.hashCode();
    }

    @Override
    public String toString()
    {
        return dom.toString();
    }

    public String toUnescapedString()
    {
        return ( ( Xpp3Dom ) dom ).toUnescapedString();
    }

    public static boolean isNotEmpty( String str )
    {
        return ( ( str != null ) && ( str.length() > 0 ) );
    }

    public static boolean isEmpty( String str )
    {
        return ( ( str == null ) || ( str.trim().length() == 0 ) );
    }

    private void update( Dom dom )
    {
        if ( parent != null )
        {
            parent.replace( this.dom, dom );
        }
        this.dom = dom;
    }

    private void replace( Dom prevChild, Dom newChild )
    {
        List<Dom> children = new ArrayList<>( dom.getChildren() );
        children.replaceAll( d -> d == prevChild ? newChild : d );
        update( new org.apache.maven.internal.xml.Xpp3Dom(
                dom.getName(), dom.getValue(), dom.getAttributes(), children, dom.getInputLocation() ) );
    }
}
