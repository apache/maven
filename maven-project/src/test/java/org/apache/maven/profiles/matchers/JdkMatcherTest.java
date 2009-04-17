package org.apache.maven.profiles.matchers;

import java.util.Collections;

import org.apache.maven.model.Activation;
import org.apache.maven.model.Profile;
import org.apache.maven.model.interpolator.InterpolatorProperty;

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
		assertTrue(m.isMatch(p, Collections.singletonList(new InterpolatorProperty("${java.version}", "1.5.0_16"))));
	}
}
