package org.apache.maven.util;

import org.codehaus.plexus.util.xml.Xpp3Dom;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
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
 * TODO: describe
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
public class Xpp3DomUtils
{
    private static void mergeIntoXpp3Dom( Xpp3Dom dominant, Xpp3Dom recessive )
    {
        // TODO: how to mergeXpp3Dom lists rather than override?
        // TODO: share this as some sort of assembler, implement a walk interface?
        Xpp3Dom[] children = recessive.getChildren();
        for ( int i = 0; i < children.length; i++ )
        {
            Xpp3Dom child = children[i];
            Xpp3Dom childDom = dominant.getChild( child.getName() );
            if ( childDom != null )
            {
                mergeIntoXpp3Dom( childDom, child );
            }
            else
            {
                dominant.addChild( copyXpp3Dom( child ) );
            }
        }
    }

    public static Xpp3Dom copyXpp3Dom( Xpp3Dom src )
    {
        // TODO: into Xpp3Dom as a copy constructor
        Xpp3Dom dom = new Xpp3Dom( src.getName() );
        dom.setValue( src.getValue() );

        String[] attributeNames = src.getAttributeNames();
        for ( int i = 0; i < attributeNames.length; i++ )
        {
            String attributeName = attributeNames[i];
            dom.setAttribute( attributeName, src.getAttribute( attributeName ) );
        }

        Xpp3Dom[] children = src.getChildren();
        for ( int i = 0; i < children.length; i++ )
        {
            dom.addChild( copyXpp3Dom( children[i] ) );
        }

        return dom;
    }

    public static Xpp3Dom mergeXpp3Dom( Xpp3Dom dominant, Xpp3Dom recessive )
    {
        if ( dominant != null )
        {
            mergeIntoXpp3Dom( dominant, recessive );
            return dominant;
        }
        return recessive;
    }
}
