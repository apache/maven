package org.apache.maven.profiles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;


public class ProfileManagerInfo
{
	private Properties interpolatorProperties;
	
	private Collection<String> activeProfileIds;
	
	private Collection<String> inactiveProfileIds;
	
	public ProfileManagerInfo(Properties interpolatorProperties, Collection<String> activeProfileIds, Collection<String> inactiveProfileIds)
	{
		this.interpolatorProperties = (interpolatorProperties != null) ? interpolatorProperties : new Properties();
		this.activeProfileIds = (activeProfileIds != null) ? activeProfileIds : new ArrayList<String>();
		this.inactiveProfileIds = (inactiveProfileIds != null) ? inactiveProfileIds : new ArrayList<String>();
	}

	public Properties getInterpolatorProperties() {
		return interpolatorProperties;
	}

	public Collection<String> getActiveProfileIds() {
		return activeProfileIds;
	}

	public Collection<String> getInactiveProfileIds() {
		return inactiveProfileIds;
	}	
}
