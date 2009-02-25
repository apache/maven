package org.apache.maven.settings;

import java.io.File;

import junit.framework.TestCase;

public class SettingsTest extends TestCase {

	private Settings settingsNoProxies;
	private Settings settingsOneInactiveProxy;
	private Settings settingsOneActiveProxy;

	
	protected void setUp() throws Exception {
		
		super.setUp();

		// Read different settings files for proxy testing.
		DefaultMavenSettingsBuilder settingsBuilder = new DefaultMavenSettingsBuilder();
		settingsNoProxies = settingsBuilder.buildSettings(new File("src/test/resources/org/apache/maven/settings/settings-no-proxies.xml"), false);
		settingsOneInactiveProxy = settingsBuilder.buildSettings(new File("src/test/resources/org/apache/maven/settings/settings-one-inactive-proxy.xml"), false);
		settingsOneActiveProxy = settingsBuilder.buildSettings(new File("src/test/resources/org/apache/maven/settings/settings-one-active-proxy.xml"), false);
		
	}
	
	
	public void testProxySettings() {
		
		assertNull(settingsNoProxies.getActiveProxy());
		assertNull(settingsOneInactiveProxy.getActiveProxy());
		assertNotNull(settingsOneActiveProxy.getActiveProxy());
	}
}
