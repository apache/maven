package org.apache.maven.cli;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class MavenShowVersionTest  {

	@Test
	public void testVersion(){
		String out = CLIReportingUtils.showVersion();
		assertTrue(out.indexOf("JRE used") >= 0);
		System.out.println(out);
	}
	
}
