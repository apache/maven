package mng;

import java.util.Calendar;

public class Module5Test extends XStreamTestCase
{
    private Calendar cal = Calendar.getInstance();

    protected Object getObject()
    {
        return cal;
    }

    protected String getXML()
    {
        return Module3TestUtil.getCalendarAsXML( cal );
    }

    public void testJMockAvailable()
    {
        assertNotNull( mock( Module4.class ) );
    }
}
