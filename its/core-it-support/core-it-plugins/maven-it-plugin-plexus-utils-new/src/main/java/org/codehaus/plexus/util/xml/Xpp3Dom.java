package org.codehaus.plexus.util.xml;

import java.io.IOException;

import org.codehaus.plexus.util.xml.pull.XmlSerializer;

public class Xpp3Dom
{

    private String root;

    public Xpp3Dom( String root )
    {
        this.root = root;
    }

    public void writeToSerializer( String namespace, XmlSerializer s )
        throws IOException
    {
        s.startDocument( "UTF-8", Boolean.FALSE );
        s.startTag( namespace, root );
        s.endTag( namespace, root );
        s.endDocument();
    }

}
