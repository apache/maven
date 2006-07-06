package org.apache.maven.plugin.coreit.sub;

import org.apache.maven.plugin.coreit.Bla;

/**
 */
public class MyBla
    implements Bla
{
    private String field;

    public String getField()
    {
        return field; 
    }

    public void setField( String field )
    {
        this.field = field; 
    }

    public String toString()
    {
        return getClass() + "-" + field;
    }
}
