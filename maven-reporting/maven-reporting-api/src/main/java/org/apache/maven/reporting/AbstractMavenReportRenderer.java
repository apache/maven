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

/**
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

    private int section = 0;

    public AbstractMavenReportRenderer( Sink sink )
    {
        this.sink = sink;
    }

    public void render()
    {
        sink.head();

        sink.title();

        sink.text( getTitle() );

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

        sink.text( name );

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

        sink.text( text );

        sink.tableHeaderCell_();
    }

    protected void tableCell( String text )
    {
        sink.tableCell();

        if ( text != null )
        {
            sink.text( text );
        }
        else
        {
            sink.nonBreakingSpace();
        }

        sink.tableCell_();
    }

    /**
     * Create a cell with a potential link.
     *
     * @param text the text
     * @param href the href
     */
    protected void tableCellWithLink( String text, String href )
    {
        sink.tableCell();

        if ( text != null )
        {
            if ( href != null )
            {
                String[] schemes = {"http", "https"};
                UrlValidator urlValidator = new UrlValidator( schemes );

                if ( EmailValidator.getInstance().isValid( href ) )
                {
                    link( "mailto:" + href, text );
                }
                else if ( href.toLowerCase().startsWith( "mailto:" ) )
                {
                    link( href, text );
                }
                else if ( urlValidator.isValid( href ) )
                {
                    link( href, text );
                }
                else
                {
                    sink.text( text );
                }
            }
            else
            {
                sink.text( text );
            }
        }
        else
        {
            sink.nonBreakingSpace();
        }

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

    /**
     * Create a new row : each cell could have a link.
     * <br>
     * The arrays should have the same size.
     *
     * @param texts an array of text
     * @param hrefs an array of href
     */
    protected void tableRowWithLink( String[] texts, String[] hrefs )
    {
        if ( hrefs.length != texts.length )
        {
            throw new IllegalArgumentException( "The arrays should have the same size" );
        }

        sink.tableRow();

        for ( int i = 0; i < texts.length; i++ )
        {
            tableCellWithLink( texts[i], hrefs[i] );
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
        sink.text( caption );
        sink.tableCaption_();
    }

    protected void paragraph( String paragraph )
    {
        sink.paragraph();

        sink.text( paragraph );

        sink.paragraph_();
    }

    protected void link( String href, String name )
    {
        sink.link( href );

        sink.text( name );

        sink.link_();
    }

    public abstract String getTitle();

    protected abstract void renderBody();
}
