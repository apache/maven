package org.apache.maven.plugin.transformer;

/* ====================================================================
 *   Copyright 2001-2005 The Apache Software Foundation.
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

import org.dom4j.Node;

/**
 *
 *
 * @author <a href="mailto:jason@zenplex.com">Jason van Zyl</a>
 *
 * @version $Id: Transformation.java 114783 2004-03-02 15:37:56Z evenisse $
 */
public class Transformation
{
    /** Pom Transformer associated with this transformation. */
    private PomTransformer pomTransformer;

    /** Node to transform. */
    private Node node;

    public Transformation( PomTransformer pomTransformer )
    {
        this.pomTransformer = pomTransformer;
    }

    // ----------------------------------------------------------------------
    // Accessors
    // ----------------------------------------------------------------------

    /**
     *
     * @return
     */
    public Node getNode()
    {
        return node;
    }

    /**
     *
     * @param node
     */
    public void setNode( Node node )
    {
        this.node = node;
    }

    /**
     *
     * @throws Exception
     */
    public void transform()
        throws Exception
    {
        pomTransformer.transformNode( node );
    }

    /**
     *
     * @return
     * @throws Exception
     */
    public String getBeforeTransformation()
        throws Exception
    {
        return getNode().asXML();
    }

    /**
     *
     * @return
     * @throws Exception
     */
    public String getAfterTransformation()
        throws Exception
    {
        return pomTransformer.getTransformedNode( getNode() ).asXML();
    }
}
