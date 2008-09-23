package org.apache.maven.its.it0121.D;

import java.util.Date;

public class Foo
{
    public String getTimestamp()
    {
        return (new Date()).toString();
    }
}
