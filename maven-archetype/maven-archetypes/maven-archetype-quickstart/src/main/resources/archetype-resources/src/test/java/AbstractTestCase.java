package $package;

import java.io.File;

import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Abstract base class for test cases.
 *
 * @author <a href="jason@zenplex.com">Jason van Zyl</a>
 */
public abstract class AbstractTestCase
    extends TestCase 
{
    /** 
     * Basedir for all file I/O. Important when running tests from
     * the reactor.
     */
    public String basedir = System.getProperty("basedir");
    
    /**
     * Constructor.
     */
    public AbstractTestCase(String testName)
    {
        super(testName);
    }
    
    /**
     * Get test input file.
     *
     * @param path Path to test input file.
     */
    public String getTestFile(String path)
    {
        return new File(basedir,path).getAbsolutePath();
    }
}

