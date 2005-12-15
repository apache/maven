package org.codehaus.doxia.site.renderer.sink;

import java.util.List;

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

/**
 * @author <a href="mailto:evenisse@codehaus.org>Emmanuel Venisse</a>
 * @version $Id$
 */
public class SiteRendererSink
    extends org.apache.maven.doxia.siterenderer.sink.SiteRendererSink
{

    private final org.apache.maven.doxia.siterenderer.sink.SiteRendererSink sinkDelegate;

    public SiteRendererSink( org.apache.maven.doxia.siterenderer.sink.SiteRendererSink sinkDelegate )
    {
        super( null, null );
        this.sinkDelegate = sinkDelegate;
    }
    
    public org.apache.maven.doxia.siterenderer.sink.SiteRendererSink getSinkDelegate()
    {
        return sinkDelegate;
    }

    public void anchor_()
    {
        sinkDelegate.anchor_();
    }

    public void anchor( String arg0 )
    {
        sinkDelegate.anchor( arg0 );
    }

    public void author_()
    {
        sinkDelegate.author_();
    }

    public void author()
    {
        sinkDelegate.author();
    }

    public void body_()
    {
        sinkDelegate.body_();
    }

    public void body()
    {
        sinkDelegate.body();
    }

    public void bold_()
    {
        sinkDelegate.bold_();
    }

    public void bold()
    {
        sinkDelegate.bold();
    }

    public void close()
    {
        sinkDelegate.close();
    }

    public void date_()
    {
        sinkDelegate.date_();
    }

    public void date()
    {
        sinkDelegate.date();
    }

    public void definedTerm_()
    {
        sinkDelegate.definedTerm_();
    }

    public void definedTerm()
    {
        sinkDelegate.definedTerm();
    }

    public void definition_()
    {
        sinkDelegate.definition_();
    }

    public void definition()
    {
        sinkDelegate.definition();
    }

    public void definitionList_()
    {
        sinkDelegate.definitionList_();
    }

    public void definitionList()
    {
        sinkDelegate.definitionList();
    }

    public void definitionListItem_()
    {
        sinkDelegate.definitionListItem_();
    }

    public void definitionListItem()
    {
        sinkDelegate.definitionListItem();
    }

    public boolean equals( Object arg0 )
    {
        return sinkDelegate.equals( arg0 );
    }

    public void figure_()
    {
        sinkDelegate.figure_();
    }

    public void figure()
    {
        sinkDelegate.figure();
    }

    public void figureCaption_()
    {
        sinkDelegate.figureCaption_();
    }

    public void figureCaption()
    {
        sinkDelegate.figureCaption();
    }

    public void figureGraphics( String arg0 )
    {
        sinkDelegate.figureGraphics( arg0 );
    }

    public void flush()
    {
        sinkDelegate.flush();
    }

    public List getAuthors()
    {
        return sinkDelegate.getAuthors();
    }

    public String getBody()
    {
        return sinkDelegate.getBody();
    }

    public String getDate()
    {
        return sinkDelegate.getDate();
    }

    public String getTitle()
    {
        return sinkDelegate.getTitle();
    }

    public int hashCode()
    {
        return sinkDelegate.hashCode();
    }

    public void head_()
    {
        sinkDelegate.head_();
    }

    public void head()
    {
        sinkDelegate.head();
    }

    public void horizontalRule()
    {
        sinkDelegate.horizontalRule();
    }

    public void italic_()
    {
        sinkDelegate.italic_();
    }

    public void italic()
    {
        sinkDelegate.italic();
    }

    public void lineBreak()
    {
        sinkDelegate.lineBreak();
    }

    public void link_()
    {
        sinkDelegate.link_();
    }

    public void link( String arg0, String arg1 )
    {
        sinkDelegate.link( arg0, arg1 );
    }

    public void link( String arg0 )
    {
        sinkDelegate.link( arg0 );
    }

    public void list_()
    {
        sinkDelegate.list_();
    }

    public void list()
    {
        sinkDelegate.list();
    }

    public void listItem_()
    {
        sinkDelegate.listItem_();
    }

    public void listItem()
    {
        sinkDelegate.listItem();
    }

    public void monospaced_()
    {
        sinkDelegate.monospaced_();
    }

    public void monospaced()
    {
        sinkDelegate.monospaced();
    }

    public void nonBreakingSpace()
    {
        sinkDelegate.nonBreakingSpace();
    }

    public void numberedList_()
    {
        sinkDelegate.numberedList_();
    }

    public void numberedList( int arg0 )
    {
        sinkDelegate.numberedList( arg0 );
    }

    public void numberedListItem_()
    {
        sinkDelegate.numberedListItem_();
    }

    public void numberedListItem()
    {
        sinkDelegate.numberedListItem();
    }

    public void pageBreak()
    {
        sinkDelegate.pageBreak();
    }

    public void paragraph_()
    {
        sinkDelegate.paragraph_();
    }

    public void paragraph()
    {
        sinkDelegate.paragraph();
    }

    public void rawText( String arg0 )
    {
        sinkDelegate.rawText( arg0 );
    }

    public void section1_()
    {
        sinkDelegate.section1_();
    }

    public void section1()
    {
        sinkDelegate.section1();
    }

    public void section2_()
    {
        sinkDelegate.section2_();
    }

    public void section2()
    {
        sinkDelegate.section2();
    }

    public void section3_()
    {
        sinkDelegate.section3_();
    }

    public void section3()
    {
        sinkDelegate.section3();
    }

    public void section4_()
    {
        sinkDelegate.section4_();
    }

    public void section4()
    {
        sinkDelegate.section4();
    }

    public void section5_()
    {
        sinkDelegate.section5_();
    }

    public void section5()
    {
        sinkDelegate.section5();
    }

    public void sectionTitle_()
    {
        sinkDelegate.sectionTitle_();
    }

    public void sectionTitle()
    {
        sinkDelegate.sectionTitle();
    }

    public void sectionTitle1_()
    {
        sinkDelegate.sectionTitle1_();
    }

    public void sectionTitle1()
    {
        sinkDelegate.sectionTitle1();
    }

    public void sectionTitle2_()
    {
        sinkDelegate.sectionTitle2_();
    }

    public void sectionTitle2()
    {
        sinkDelegate.sectionTitle2();
    }

    public void sectionTitle3_()
    {
        sinkDelegate.sectionTitle3_();
    }

    public void sectionTitle3()
    {
        sinkDelegate.sectionTitle3();
    }

    public void sectionTitle4_()
    {
        sinkDelegate.sectionTitle4_();
    }

    public void sectionTitle4()
    {
        sinkDelegate.sectionTitle4();
    }

    public void sectionTitle5_()
    {
        sinkDelegate.sectionTitle5_();
    }

    public void sectionTitle5()
    {
        sinkDelegate.sectionTitle5();
    }

    public void table_()
    {
        sinkDelegate.table_();
    }

    public void table()
    {
        sinkDelegate.table();
    }

    public void tableCaption_()
    {
        sinkDelegate.tableCaption_();
    }

    public void tableCaption()
    {
        sinkDelegate.tableCaption();
    }

    public void tableCell_()
    {
        sinkDelegate.tableCell_();
    }

    public void tableCell_( boolean arg0 )
    {
        sinkDelegate.tableCell_( arg0 );
    }

    public void tableCell()
    {
        sinkDelegate.tableCell();
    }

    public void tableCell( boolean arg0, String arg1 )
    {
        sinkDelegate.tableCell( arg0, arg1 );
    }

    public void tableCell( boolean arg0 )
    {
        sinkDelegate.tableCell( arg0 );
    }

    public void tableCell( String arg0 )
    {
        sinkDelegate.tableCell( arg0 );
    }

    public void tableHeaderCell_()
    {
        sinkDelegate.tableHeaderCell_();
    }

    public void tableHeaderCell()
    {
        sinkDelegate.tableHeaderCell();
    }

    public void tableHeaderCell( String arg0 )
    {
        sinkDelegate.tableHeaderCell( arg0 );
    }

    public void tableRow_()
    {
        sinkDelegate.tableRow_();
    }

    public void tableRow()
    {
        sinkDelegate.tableRow();
    }

    public void tableRows_()
    {
        sinkDelegate.tableRows_();
    }

    public void tableRows( int[] arg0, boolean arg1 )
    {
        sinkDelegate.tableRows( arg0, arg1 );
    }

    public void text( String arg0 )
    {
        sinkDelegate.text( arg0 );
    }

    public void title_()
    {
        sinkDelegate.title_();
    }

    public void title()
    {
        sinkDelegate.title();
    }

    public String toString()
    {
        return sinkDelegate.toString();
    }

    public void verbatim_()
    {
        sinkDelegate.verbatim_();
    }

    public void verbatim( boolean arg0 )
    {
        sinkDelegate.verbatim( arg0 );
    }
}
