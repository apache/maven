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
import org.codehaus.plexus.util.FileUtils;
import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.jaxen.XPath;
import org.jaxen.dom4j.Dom4jXPath;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This is the base class for any tool that attempts to transform fields
 * in the POM. Currently we are using the XML form of the POM and using Jaxen
 * but eventually we will be able to perform the same transformations on
 * POM beans. Jaxen needs to be modified and some serious cleanup needs to
 * go on in Maven internally, but this will serve as a start. An attempt is
 * made to make this tool GUI friendly.
 *
 * @author <a href="mailto:jason --at-- maven.org">Jason van Zyl</a>
 *
 * @version $Id: AbstractPomTransformer.java 115932 2004-08-06 22:43:03Z carlos $
 */
public abstract class AbstractPomTransformer
    implements PomTransformer
{
    /** POM document */
    private File project;

    /** Dom4j document. */
    private Document document;

    /** Output file. */
    private File outputFile;

    /** Properties used in transformNode */
    private Map variables;

    /** Nodes selected for transformation using xpath. */
    private List selectedNodes;

    /** Updated model obtain from MavenProject. */
    private Model updatedModel;

    private List transformations;

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public Model getUpdatedModel()
    {
        return updatedModel;
    }

    public void setUpdatedModel( Model updatedModel )
    {
        this.updatedModel = updatedModel;
    }

    public Map getVariables()
    {
        return variables;
    }

    public void setVariables( Map variables )
    {
        this.variables = variables;
    }

    public void setProject( File project )
    {
        this.project = project;
    }

    public File getProject()
    {
        return project;
    }

    public Document getDocument()
    {
        return document;
    }

    public void setDocument( Document document )
    {
        this.document = document;
    }

    public File getOutputFile()
    {
        return outputFile;
    }

    public void setOutputFile( File outputFile )
    {
        this.outputFile = outputFile;
    }

    public List getSelectedNodes()
    {
        if ( selectedNodes == null )
        {
            try
            {
                selectNodes();
            }
            catch ( Exception e )
            {
                // do nothing.
            }
        }
        return selectedNodes;
    }

    public void setSelectedNodes( List selectedNodes )
    {
        this.selectedNodes = selectedNodes;
    }

    public int getSelectedNodeCount()
    {
        return getSelectedNodes().size();
    }

    public List getTransformations()
    {
        if ( transformations == null )
        {
            createTransformations();
        }

        return transformations;
    }

    public void createTransformations()
    {
        transformations = new ArrayList();

        for ( Iterator i = getSelectedNodes().iterator(); i.hasNext(); )
        {
            Object o = i.next();

            if ( o instanceof Node )
            {
                Transformation transformation = new Transformation( this );
                transformation.setNode( (Node) o );
                transformations.add( transformation );
            }
        }
    }

    /**
     * This is the automated way of transforming the nodes if there is
     * no user interaction involved.
     *
     * @throws Exception If an error occurs while transforming the nodes.
     */
    public void transformNodes()
        throws Exception
    {
        for ( Iterator i = getSelectedNodes().iterator(); i.hasNext(); )
        {
            Object o = i.next();

            if ( o instanceof Node )
            {
                transformNode( (Node) o );
            }
        }
    }

    // ----------------------------------------------------------------------
    // Implementation
    // ----------------------------------------------------------------------

    /**
     *
     * @return
     */
    public abstract String selectProjectNodeXPathExpression();

    /**
     *
     * @return
     */
    public abstract String selectDependenciesNodesXPathExpression();

    /**
     *
     * @return
     */
    public abstract String selectPluginsNodesXPathExpression();

    /**
     *
     * @param node
     * @throws Exception
     */
    public abstract void transformNode( Node node )
        throws Exception;

    /**
     * Update the snapshot version identifiers with actual timestamp versions
     * and write out the POM in its updated form.
     *
     * @throws Exception
     */
    public void selectNodes()
        throws Exception
    {
        SAXReader reader = new SAXReader();

        setDocument( reader.read( getProject() ) );

        // The selecting nodes with the xpath expression will give us a list
        // of dependencies elements where the version element is equal to 'SNAPSHOT'.
        // So we can get any information we need, and alter anything we need to before writing
        // the dom4j document back out.

        XPath pomXpath = new Dom4jXPath( selectProjectNodeXPathExpression() );

        XPath dependenciesXpath = new Dom4jXPath( selectDependenciesNodesXPathExpression() );

        XPath pluginsXpath = new Dom4jXPath( selectPluginsNodesXPathExpression() );

        List nodes = new ArrayList();

        nodes.addAll( pomXpath.selectNodes( getDocument() ) );

        nodes.addAll( dependenciesXpath.selectNodes( getDocument() ) );

        nodes.addAll( pluginsXpath.selectNodes( getDocument() ) );

        setSelectedNodes( nodes );
    }

    /**
     *
     * @throws Exception
     */
    public void write()
        throws Exception
    {
        OutputStream os = null;

        if ( getOutputFile() != null )
        {
            // Backup the original first.
            FileUtils.copyFile( getOutputFile(), new File( getOutputFile() + ".backup" ) );

            // Now hand of the os.
            os = new FileOutputStream( getOutputFile() );
        }
        else
        {
            os = new PrintStream( System.out );
        }

        OutputFormat format = new OutputFormat();

        format.setIndentSize( 2 );

        format.setNewlines( true );

        format.setTrimText( true );

        XMLWriter writer = new XMLWriter( format );

        writer.setOutputStream( os );

        writer.write( getDocument() );

        writer.flush();

        writer.close();
    }
}
