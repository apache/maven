package org.apache.maven.reporting;

/*
 * Copyright 2004-2005 The Apache Software Foundation.
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

import org.apache.commons.validator.EmailValidator;
import org.apache.commons.validator.UrlValidator;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.plexus.util.StringUtils;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * An abstract class to manage report generation.
 *
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @author <a href="evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @version $Id: AbstractMavenReportRenderer.java 163373 2005-02-22 03:37:00Z brett $
 * @todo Later it may be appropriate to create something like a VelocityMavenReportRenderer that could take a velocity template and pipe that through Doxia rather than coding them up like this.
 */
public abstract class AbstractMavenReportRenderer
    implements MavenReportRenderer
{
    protected Sink sink;

    private int section;

    public AbstractMavenReportRenderer( Sink sink )
    {
        this.sink = sink;
    }

    public void render()
    {
        sink.head();

        sink.title();

        text( getTitle() );

        sink.title_();

        sink.head_();

        sink.body();

        renderBody();

        sink.body_();

        sink.flush();

        sink.close();
    }

    protected void startTable()
    {
        sink.table();
    }

    protected void endTable()
    {
        sink.table_();
    }

    protected void startSection( String name )
    {
        section = section + 1;

        switch ( section )
        {
            case 1:
                sink.section1();
                sink.sectionTitle1();
                break;
            case 2:
                sink.section2();
                sink.sectionTitle2();
                break;
            case 3:
                sink.section3();
                sink.sectionTitle3();
                break;
            case 4:
                sink.section4();
                sink.sectionTitle4();
                break;
            case 5:
                sink.section5();
                sink.sectionTitle5();
                break;

            default:
                // TODO: warning - just don't start a section
                break;
        }

        text( name );

        switch ( section )
        {
            case 1:
                sink.sectionTitle1_();
                break;
            case 2:
                sink.sectionTitle2_();
                break;
            case 3:
                sink.sectionTitle3_();
                break;
            case 4:
                sink.sectionTitle4_();
                break;
            case 5:
                sink.sectionTitle5_();
                break;

            default:
                // TODO: warning - just don't start a section
                break;
        }
    }

    protected void endSection()
    {
        switch ( section )
        {
            case 1:
                sink.section1_();
                break;
            case 2:
                sink.section2_();
                break;
            case 3:
                sink.section3_();
                break;
            case 4:
                sink.section4_();
                break;
            case 5:
                sink.section5_();
                break;

            default:
                // TODO: warning - just don't start a section
                break;
        }

        section = section - 1;

        if ( section < 0 )
        {
            throw new IllegalStateException( "Too many closing sections" );
        }
    }

    protected void tableHeaderCell( String text )
    {
        sink.tableHeaderCell();

        text( text );

        sink.tableHeaderCell_();
    }

    /**
     * Add a cell in a table.
     * <p>The text could be a link patterned text defined by <code>{text, url}</code></p>
     *
     * @param text
     * @see #linkPatternedText(String)
     */
    protected void tableCell( String text )
    {
        sink.tableCell();

        linkPatternedText( text );

        sink.tableCell_();
    }

    protected void tableRow( String[] content )
    {
        sink.tableRow();

        for ( int i = 0; i < content.length; i++ )
        {
            tableCell( content[i] );
        }

        sink.tableRow_();
    }

    protected void tableHeader( String[] content )
    {
        sink.tableRow();

        for ( int i = 0; i < content.length; i++ )
        {
            tableHeaderCell( content[i] );
        }

        sink.tableRow_();
    }

    protected void tableCaption( String caption )
    {
        sink.tableCaption();
        text( caption );
        sink.tableCaption_();
    }

    protected void paragraph( String paragraph )
    {
        sink.paragraph();

        text( paragraph );

        sink.paragraph_();
    }

    protected void link( String href, String name )
    {
        sink.link( href );

        text( name );

        sink.link_();
    }

    /**
     * Add a new text.
     * <p>If text is empty of has a null value, add the "-" charater</p>
     *
     * @param text a string
     */
    protected void text( String text )
    {
        if ( text == null || text.length() == 0 ) // Take care of spaces
        {
            sink.text( "-" );
        }
        else
        {
            sink.text( text );
        }
    }

    /**
     * Add a verbatim text.
     *
     * @param text a string
     * @see #text(String)
     */
    protected void verbatimText( String text )
    {
        sink.verbatim( true );

        text( text );

        sink.verbatim_();
    }

    /**
     * Add a verbatim text with a specific link.
     *
     * @param text a string
     * @param href an href could be null
     * @see #link(String, String)
     */
    protected void verbatimLink( String text, String href )
    {
        if ( StringUtils.isEmpty( href ) )
        {
            verbatimText( text );
        }
        else
        {
            sink.verbatim( true );

            link( href, text );

            sink.verbatim_();
        }
    }

    /**
     * Add a Javascript code.
     *
     * @param jsCode a string of Javascript
     */
    protected void javaScript( String jsCode )
    {
        sink.rawText( "<script type=\"text/javascript\">\n" + jsCode + "</script>" );
    }

    /**
     * Add a text with links inside.
     * <p>The text variable should contained this given pattern <code>{text, url}</code>
     * to handle the link creation.</p>
     *
     * @param text a text with link pattern defined.
     * @see #text(String)
     * @see #applyPattern(String)
     */
    public void linkPatternedText( String text )
    {
        if ( StringUtils.isEmpty( text ) )
        {
            text( text );
        }
        else
        {
            Map segments = applyPattern( text );

            if ( segments == null )
            {
                text( text );
            }
            else
            {
                for ( Iterator it = segments.entrySet().iterator(); it.hasNext(); )
                {
                    Map.Entry entry = (Map.Entry) it.next();

                    String name = (String) entry.getKey();
                    String href = (String) entry.getValue();

                    if ( href == null )
                    {
                        text( name );
                    }
                    else
                    {
                        if ( getValidHref( href ) != null )
                        {
                            link( getValidHref( href ), name );
                        }
                        else
                        {
                            text( text );
                        }
                    }
                }
            }
        }
    }

    /**
     * Create a link pattern text defined by <code>{text, url}</code>.
     * <p>This created pattern could be used by the method <code>linkPatternedText(String)</code> to
     * handle a text with link.</p>
     *
     * @param text
     * @param href
     * @return a link pattern
     * @see #linkPatternedText(String)
     */
    protected static String createLinkPatternedText( String text, String href )
    {
        if ( text == null )
        {
            return text;
        }

        if ( href == null )
        {
            return text;
        }

        StringBuffer sb = new StringBuffer();
        sb.append( "{" ).append( text ).append( ", " ).append( href ).append( "}" );

        return sb.toString();
    }

    /**
     * Convenience method to display a <code>Properties</code> object comma separated.
     *
     * @param props
     * @return the properties object as comma separated String
     */
    protected static String propertiesToString( Properties props )
    {
        StringBuffer sb = new StringBuffer();

        if ( props == null || props.isEmpty() )
        {
            return sb.toString();
        }

        for ( Iterator i = props.keySet().iterator(); i.hasNext(); )
        {
            String key = (String) i.next();
            sb.append( key ).append( "=" ).append( props.get( key ) );
            if ( i.hasNext() )
            {
                sb.append( ", " );
            }
        }

        return sb.toString();
    }

    /**
     * Return a valid href.
     * <p>A valid href could start by <code>mailto:</code></p>.
     *
     * @param href an href
     * @return a valid href or null if the href is not valid.
     */
    private static String getValidHref( String href )
    {
        href = href.trim();

        String[] schemes = {"http", "https"};
        UrlValidator urlValidator = new UrlValidator( schemes );

        if ( EmailValidator.getInstance().isValid( href ) )
        {
            return "mailto:" + href;
        }
        else if ( href.toLowerCase().startsWith( "mailto:" ) )
        {
            return href;
        }
        else if ( urlValidator.isValid( href ) )
        {
            return href;
        }
        else
        {
            // TODO Waiting for new release of Validator
            // http://issues.apache.org/bugzilla/show_bug.cgi?id=30686
            String hrefTmp;
            if ( !href.trim().endsWith( "/" ) )
            {
                hrefTmp = href + "/index.html";
            }
            else
            {
                hrefTmp = href + "index.html";
            }

            if ( urlValidator.isValid( hrefTmp ) )
            {
                return href;
            }

            return null;
        }
    }

    /**
     * The method parses a text an apply the given pattern <code>{text, url}</code> to create
     * a map of text/href.
     *
     * @param text a text with or without the pattern <code>{text, url}</code>
     * @return a map of text/href
     */
    private static Map applyPattern( String text )
    {
        if ( StringUtils.isEmpty( text ) )
        {
            return null;
        }

        // Map defined by key/value name/href
        // If href == null, it means 
        Map segments = new LinkedHashMap();

        // TODO Special case http://jira.codehaus.org/browse/MEV-40
        if ( text.indexOf( "${" ) != -1 )
        {
            int lastComma = text.lastIndexOf( "," );
            int lastSemi = text.lastIndexOf( "}" );
            if ( lastComma != -1 && lastSemi != -1 )
            {
                segments.put( text.substring( lastComma + 1, lastSemi ).trim(), null );
            }
            else
            {
                segments.put( text, null );
            }

            return segments;
        }

        boolean inQuote = false;
        int braceStack = 0;
        int lastOffset = 0;

        for ( int i = 0; i < text.length(); i++ )
        {
            char ch = text.charAt( i );

            if ( ch == '\'' && !inQuote )
            {
                // handle: ''
                if ( i + 1 < text.length() && text.charAt( i + 1 ) == '\'' )
                {
                    i++;
                }
                else
                {
                    inQuote = true;
                }
            }
            else
            {
                switch ( ch )
                {
                    case '{':
                        if ( !inQuote )
                        {
                            if ( braceStack == 0 )
                            {
                                if ( i != 0 ) // handle { at first character
                                {
                                    segments.put( text.substring( lastOffset, i ), null );
                                }
                                lastOffset = i + 1;
                                braceStack++;
                            }
                        }
                        break;
                    case '}':
                        if ( !inQuote )
                        {
                            braceStack--;
                            if ( braceStack == 0 )
                            {
                                String subString = text.substring( lastOffset, i );
                                lastOffset = i + 1;

                                int lastComma = subString.lastIndexOf( "," );
                                if ( lastComma != -1 )
                                {
                                    segments.put( subString.substring( 0, lastComma ).trim(),
                                                  subString.substring( lastComma + 1 ).trim() );
                                }
                                else
                                {
                                    segments.put( subString.substring( 0, lastComma ).trim(), null );
                                }
                            }
                        }
                        break;
                    case '\'':
                        inQuote = false;
                        break;
                    default:
                        break;
                }
            }
        }

        if ( !StringUtils.isEmpty( text.substring( lastOffset, text.length() ) ) )
        {
            segments.put( text.substring( lastOffset, text.length() ), null );
        }

        if ( braceStack != 0 )
        {
            throw new IllegalArgumentException( "Unmatched braces in the pattern." );
        }

        return Collections.unmodifiableMap( segments );
    }

    public abstract String getTitle();

    protected abstract void renderBody();
}
