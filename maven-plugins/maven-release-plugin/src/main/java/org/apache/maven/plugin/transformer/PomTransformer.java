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

import org.apache.maven.model.Model;
import org.dom4j.Node;

import java.io.File;

/**
 * @author <a href="mailto:jason@zenplex.com">Jason van Zyl</a>
 * @version $Id: PomTransformer.java 114783 2004-03-02 15:37:56Z evenisse $
 */
public interface PomTransformer
{
    File getProject();

    void setProject( File project );

    void transformNodes()
        throws Exception;

    void transformNode( Node node );

    Node getTransformedNode( Node node )
        throws Exception;

    void write()
        throws Exception;

    Model getUpdatedModel();

    void setUpdatedModel( Model updatedModel );

    File getOutputFile();

    void setOutputFile( File outputFile );
}
