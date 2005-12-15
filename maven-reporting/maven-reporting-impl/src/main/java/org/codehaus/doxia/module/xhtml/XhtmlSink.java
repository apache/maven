package org.codehaus.doxia.module.xhtml;

import org.codehaus.doxia.sink.Sink;

/**
 * @deprecated Use org.apache.maven.doxia.module.xhtml.XhtmlSink instead.
 */
public class XhtmlSink
    implements Sink
{

    private org.apache.maven.doxia.module.xhtml.XhtmlSink sink;

    public XhtmlSink( org.apache.maven.doxia.module.xhtml.XhtmlSink sink )
    {
        this.sink = sink;
    }

    public void anchor_()
    {
        sink.anchor_();
    }

    public void anchor( String arg0 )
    {
        sink.anchor( arg0 );
    }

    public void author_()
    {
        sink.author_();
    }

    public void author()
    {
        sink.author();
    }

    public void body_()
    {
        sink.body_();
    }

    public void body()
    {
        sink.body();
    }

    public void bold_()
    {
        sink.bold_();
    }

    public void bold()
    {
        sink.bold();
    }

    public void close()
    {
        sink.close();
    }

    public void date_()
    {
        sink.date_();
    }

    public void date()
    {
        sink.date();
    }

    public void definedTerm_()
    {
        sink.definedTerm_();
    }

    public void definedTerm()
    {
        sink.definedTerm();
    }

    public void definition_()
    {
        sink.definition_();
    }

    public void definition()
    {
        sink.definition();
    }

    public void definitionList_()
    {
        sink.definitionList_();
    }

    public void definitionList()
    {
        sink.definitionList();
    }

    public void definitionListItem_()
    {
        sink.definitionListItem_();
    }

    public void definitionListItem()
    {
        sink.definitionListItem();
    }

    public boolean equals( Object arg0 )
    {
        return sink.equals( arg0 );
    }

    public void figure_()
    {
        sink.figure_();
    }

    public void figure()
    {
        sink.figure();
    }

    public void figureCaption_()
    {
        sink.figureCaption_();
    }

    public void figureCaption()
    {
        sink.figureCaption();
    }

    public void figureGraphics( String arg0 )
    {
        sink.figureGraphics( arg0 );
    }

    public void flush()
    {
        sink.flush();
    }

    public int hashCode()
    {
        return sink.hashCode();
    }

    public void head_()
    {
        sink.head_();
    }

    public void head()
    {
        sink.head();
    }

    public void horizontalRule()
    {
        sink.horizontalRule();
    }

    public void italic_()
    {
        sink.italic_();
    }

    public void italic()
    {
        sink.italic();
    }

    public void lineBreak()
    {
        sink.lineBreak();
    }

    public void link_()
    {
        sink.link_();
    }

    public void link( String arg0, String arg1 )
    {
        sink.link( arg0, arg1 );
    }

    public void link( String arg0 )
    {
        sink.link( arg0 );
    }

    public void list_()
    {
        sink.list_();
    }

    public void list()
    {
        sink.list();
    }

    public void listItem_()
    {
        sink.listItem_();
    }

    public void listItem()
    {
        sink.listItem();
    }

    public void monospaced_()
    {
        sink.monospaced_();
    }

    public void monospaced()
    {
        sink.monospaced();
    }

    public void nonBreakingSpace()
    {
        sink.nonBreakingSpace();
    }

    public void numberedList_()
    {
        sink.numberedList_();
    }

    public void numberedList( int arg0 )
    {
        sink.numberedList( arg0 );
    }

    public void numberedListItem_()
    {
        sink.numberedListItem_();
    }

    public void numberedListItem()
    {
        sink.numberedListItem();
    }

    public void pageBreak()
    {
        sink.pageBreak();
    }

    public void paragraph_()
    {
        sink.paragraph_();
    }

    public void paragraph()
    {
        sink.paragraph();
    }

    public void rawText( String arg0 )
    {
        sink.rawText( arg0 );
    }

    public void section1_()
    {
        sink.section1_();
    }

    public void section1()
    {
        sink.section1();
    }

    public void section2_()
    {
        sink.section2_();
    }

    public void section2()
    {
        sink.section2();
    }

    public void section3_()
    {
        sink.section3_();
    }

    public void section3()
    {
        sink.section3();
    }

    public void section4_()
    {
        sink.section4_();
    }

    public void section4()
    {
        sink.section4();
    }

    public void section5_()
    {
        sink.section5_();
    }

    public void section5()
    {
        sink.section5();
    }

    public void sectionTitle_()
    {
        sink.sectionTitle_();
    }

    public void sectionTitle()
    {
        sink.sectionTitle();
    }

    public void sectionTitle1_()
    {
        sink.sectionTitle1_();
    }

    public void sectionTitle1()
    {
        sink.sectionTitle1();
    }

    public void sectionTitle2_()
    {
        sink.sectionTitle2_();
    }

    public void sectionTitle2()
    {
        sink.sectionTitle2();
    }

    public void sectionTitle3_()
    {
        sink.sectionTitle3_();
    }

    public void sectionTitle3()
    {
        sink.sectionTitle3();
    }

    public void sectionTitle4_()
    {
        sink.sectionTitle4_();
    }

    public void sectionTitle4()
    {
        sink.sectionTitle4();
    }

    public void sectionTitle5_()
    {
        sink.sectionTitle5_();
    }

    public void sectionTitle5()
    {
        sink.sectionTitle5();
    }

    public void table_()
    {
        sink.table_();
    }

    public void table()
    {
        sink.table();
    }

    public void tableCaption_()
    {
        sink.tableCaption_();
    }

    public void tableCaption()
    {
        sink.tableCaption();
    }

    public void tableCell_()
    {
        sink.tableCell_();
    }

    public void tableCell_( boolean arg0 )
    {
        sink.tableCell_( arg0 );
    }

    public void tableCell()
    {
        sink.tableCell();
    }

    public void tableCell( boolean arg0, String arg1 )
    {
        sink.tableCell( arg0, arg1 );
    }

    public void tableCell( boolean arg0 )
    {
        sink.tableCell( arg0 );
    }

    public void tableCell( String arg0 )
    {
        sink.tableCell( arg0 );
    }

    public void tableHeaderCell_()
    {
        sink.tableHeaderCell_();
    }

    public void tableHeaderCell()
    {
        sink.tableHeaderCell();
    }

    public void tableHeaderCell( String arg0 )
    {
        sink.tableHeaderCell( arg0 );
    }

    public void tableRow_()
    {
        sink.tableRow_();
    }

    public void tableRow()
    {
        sink.tableRow();
    }

    public void tableRows_()
    {
        sink.tableRows_();
    }

    public void tableRows( int[] arg0, boolean arg1 )
    {
        sink.tableRows( arg0, arg1 );
    }

    public void text( String arg0 )
    {
        sink.text( arg0 );
    }

    public void title_()
    {
        sink.title_();
    }

    public void title()
    {
        sink.title();
    }

    public String toString()
    {
        return sink.toString();
    }

    public void verbatim_()
    {
        sink.verbatim_();
    }

    public void verbatim( boolean arg0 )
    {
        sink.verbatim( arg0 );
    }

}
