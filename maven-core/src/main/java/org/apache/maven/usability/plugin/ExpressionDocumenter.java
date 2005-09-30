package org.apache.maven.usability.plugin;

import org.codehaus.plexus.util.IOUtil;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class ExpressionDocumenter
{

    private static final String[] EXPRESSION_ROOTS = { "project", "settings", "session", "plugin" };

    private static final String EXPRESSION_DOCO_ROOTPATH = "META-INF/maven/plugin-expressions/";

    private static Map expressionDocumentation;

    public static Map load()
        throws ExpressionDocumentationException
    {
        if ( expressionDocumentation == null )
        {
            expressionDocumentation = new HashMap();
            
            ClassLoader docLoader = initializeDocLoader();

            for ( int i = 0; i < EXPRESSION_ROOTS.length; i++ )
            {
                InputStream docStream = null;
                try
                {
                    docStream = docLoader
                        .getResourceAsStream( EXPRESSION_DOCO_ROOTPATH + EXPRESSION_ROOTS[i] + ".paramdoc.xml" );

                    if ( docStream != null )
                    {
                        Map doco = parseExpressionDocumentation( docStream );

                        expressionDocumentation.putAll( doco );
                    }
                }
                catch ( IOException e )
                {
                    throw new ExpressionDocumentationException( "Failed to read documentation for expression root: " + EXPRESSION_ROOTS[i], e );
                }
                catch ( ParserConfigurationException e )
                {
                    throw new ExpressionDocumentationException( "Failed to parse documentation for expression root: " + EXPRESSION_ROOTS[i], e );
                }
                catch ( SAXException e )
                {
                    throw new ExpressionDocumentationException( "Failed to parse documentation for expression root: " + EXPRESSION_ROOTS[i], e.getException() );
                }
                finally
                {
                    IOUtil.close( docStream );
                }
            }
        }
        
        return expressionDocumentation;
    }

    /**
     * <expressions>
     *   <expression>
     *     <syntax>project.distributionManagementArtifactRepository</syntax>
     *     <origin><![CDATA[
     *   <distributionManagement>
     *     <repository>
     *       <id>some-repo</id>
     *       <url>scp://host/path</url>
     *     </repository>
     *     <snapshotRepository>
     *       <id>some-snap-repo</id>
     *       <url>scp://host/snapshot-path</url>
     *     </snapshotRepository>
     *   </distributionManagement>
     *   ]]></origin>
     *     <usage><![CDATA[
     *   The repositories onto which artifacts should be deployed.
     *   One is for releases, the other for snapshots.
     *   ]]></usage>
     *   </expression>
     * <expressions>
     * @throws IOException 
     * @throws ParserConfigurationException 
     * @throws SAXException 
     */
    private static Map parseExpressionDocumentation( InputStream docStream )
        throws IOException, ParserConfigurationException, SAXException
    {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();

        ExpressionInfoParser eiParser = new ExpressionInfoParser();
        
        InputSource is = new InputSource( docStream );

        parser.parse( new InputSource( docStream ), eiParser );

        return eiParser.getExpressionInfoMappings();
    }

    private static ClassLoader initializeDocLoader()
        throws ExpressionDocumentationException
    {
        String myResourcePath = ExpressionDocumenter.class.getName().replace( '.', '/' ) + ".class";
        
        URL myResource = ExpressionDocumenter.class.getClassLoader().getResource( myResourcePath );

        String myClasspathEntry = myResource.getPath();
        
        myClasspathEntry = myClasspathEntry.substring( 0, myClasspathEntry.length() - ( myResourcePath.length() + 2 ) );
        
        if ( myClasspathEntry.startsWith( "file:" ) )
        {
            myClasspathEntry = myClasspathEntry.substring( "file:".length() );
        }

        URL docResource;
        try
        {
            docResource = new File( myClasspathEntry ).toURL();
        }
        catch ( MalformedURLException e )
        {
            throw new ExpressionDocumentationException(
                                                        "Cannot construct expression documentation classpath resource base.",
                                                        e );
        }

        return new URLClassLoader( new URL[] { docResource } );
    }

    private static final class ExpressionInfoParser
        extends DefaultHandler
    {

        private static final String EXPRESSION = "expression";
        
        private static final String SYNTAX = "syntax";

        private static final String ORIGIN = "origin";

        private static final String USAGE = "usage";

        private static final String BAN = "ban";

        private static final String DEPRECATION = "deprecation";
        
        private static final String ADDENDUM = "addendum";

        private Map expressionInfos = new HashMap();

        private StringBuffer currentBuffer;

        private StringBuffer currentExpressionName;

        private StringBuffer currentUsage;

        private StringBuffer currentOrigin;
        
        private StringBuffer currentAddendum;

        private StringBuffer currentBan;

        private StringBuffer currentDeprecation;

        Map getExpressionInfoMappings()
        {
            return expressionInfos;
        }

        public void characters( char[] ch, int start, int length )
            throws SAXException
        {
            if ( currentBuffer != null )
            {
                currentBuffer.append( ch, start, length );
            }
        }

        public void endElement( String uri, String localName, String qName )
            throws SAXException
        {
            if ( EXPRESSION.equals( qName ) )
            {
                String expression = currentExpressionName.toString().trim();

                ExpressionDocumentation ei = new ExpressionDocumentation();
                ei.setExpression( expression );

                if ( currentUsage != null )
                {
                    ei.setUsage( currentUsage.toString().trim() );
                }

                if ( currentOrigin != null )
                {
                    ei.setOrigin( currentOrigin.toString().trim() );
                }

                if ( currentBan != null )
                {
                    ei.setBanMessage( currentBan.toString().trim() );
                }

                if ( currentDeprecation != null )
                {
                    ei.setDeprecationMessage( currentDeprecation.toString().trim() );
                }
                
                if ( currentAddendum != null )
                {
                    ei.setAddendum( currentAddendum.toString().trim() );
                }

                expressionInfos.put( expression, ei );

                reset();
            }
        }

        private void reset()
        {
            currentExpressionName = null;
            currentUsage = null;
            currentOrigin = null;
            currentBan = null;
            currentDeprecation = null;
            currentAddendum = null;
            currentBuffer = null;
        }

        public void startElement( String uri, String localName, String qName, Attributes attributes )
            throws SAXException
        {
            if ( SYNTAX.equals( qName ) )
            {
                currentExpressionName = new StringBuffer();
                currentBuffer = currentExpressionName;
            }
            else if ( ORIGIN.equals( qName ) )
            {
                currentOrigin = new StringBuffer();
                currentBuffer = currentOrigin;
            }
            else if ( USAGE.equals( qName ) )
            {
                currentUsage = new StringBuffer();
                currentBuffer = currentUsage;
            }
            else if ( BAN.equals( qName ) )
            {
                currentBan = new StringBuffer();
                currentBuffer = currentBan;
            }
            else if ( DEPRECATION.equals( qName ) )
            {
                currentDeprecation = new StringBuffer();
                currentBuffer = currentDeprecation;
            }
            else if ( ADDENDUM.equals( qName ) )
            {
                currentAddendum = new StringBuffer();
                currentBuffer = currentAddendum;
            }
        }
    }

}
