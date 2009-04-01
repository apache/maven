package org.apache.maven.profiles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.shared.model.InterpolatorProperty;

public class ProfileContextInfo
{
	private List<InterpolatorProperty> interpolatorProperties;
	
	private Collection<String> activeProfileIds;
	
	private Collection<String> inactiveProfileIds;
	
	public ProfileContextInfo(List<InterpolatorProperty> interpolatorProperties, Collection<String> activeProfileIds, Collection<String> inactiveProfileIds)
	{
		this.interpolatorProperties = (interpolatorProperties != null) ? interpolatorProperties : new ArrayList<InterpolatorProperty>();
		this.activeProfileIds = (activeProfileIds != null) ? activeProfileIds : new ArrayList<String>();
		this.inactiveProfileIds = (inactiveProfileIds != null) ? inactiveProfileIds : new ArrayList<String>();
	}

	public List<InterpolatorProperty> getInterpolatorProperties() {
		return interpolatorProperties;
	}

	public Collection<String> getActiveProfileIds() {
		return activeProfileIds;
	}

	public Collection<String> getInactiveProfileIds() {
		return inactiveProfileIds;
	}	
}
