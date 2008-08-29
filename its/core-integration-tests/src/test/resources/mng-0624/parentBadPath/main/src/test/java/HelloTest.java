import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: rago2483
 * Date: Aug 6, 2008
 * Time: 10:41:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class HelloTest extends TestCase
{
    public void testHello() throws Exception
    {
        Hello hello = new Hello();
        hello.helloWorld();
    }
}
