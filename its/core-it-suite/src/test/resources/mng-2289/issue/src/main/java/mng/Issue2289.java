package mng;

import junit.framework.TestCase;

public class Issue2289
{
    public static void main(final String[] args)
    {
        TestCase tc = new TestCase("Dummy") {};
        System.exit(tc == null ? -1 : 0);
    }
}
