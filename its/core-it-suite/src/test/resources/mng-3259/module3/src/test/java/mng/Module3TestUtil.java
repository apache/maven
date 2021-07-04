package mng;

import java.util.Calendar;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class Module3TestUtil
{
    public static String getCalendarAsXML(Calendar cal)
    {
        XStream xstream = new XStream(new DomDriver());
        return xstream.toXML(cal);
    }
}
