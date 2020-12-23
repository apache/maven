package mng;

import junit.framework.TestCase;
import com.thoughtworks.xstream.XStream;
import org.jmock.MockObjectTestCase;

public abstract class XStreamTestCase extends MockObjectTestCase
{
	private XStream xstream;

	public void setUp()
	{
		xstream = new XStream();
	}

	public void testToXml() {
		String xml = xstream.toXML(getObject());
		assertEquals(getXML(), xml);
	}

	protected abstract Object getObject();
	protected abstract String getXML();
}
