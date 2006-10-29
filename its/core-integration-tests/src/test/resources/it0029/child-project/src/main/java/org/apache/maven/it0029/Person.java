package org.apache.maven.it0029;

public class Person
{
    private String name;

    public void setName( String newName )
    {
        assert true;

        this.name = newName;
    }

    public String getName()
    {
        return name;
    }
}
