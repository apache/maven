package org.apache.maven.profiles.matchers;

import java.util.Properties;

import org.apache.maven.model.Activation;
import org.apache.maven.model.Profile;

import junit.framework.TestCase;

public class JdkMatcherTest extends TestCase
{
	public void testJdkMatch()
		throws Exception
	{ 
		Profile p = new Profile();
		Activation a = new Activation();
		a.setJdk("(1.3,100)");
		p.setActivation(a);
		
		JdkMatcher m = new JdkMatcher();
		Properties props = new Properties();
		props.setProperty("${java.version}", "1.5.0_16");
		
		assertTrue(m.isMatch(p, props ));
	}
}
