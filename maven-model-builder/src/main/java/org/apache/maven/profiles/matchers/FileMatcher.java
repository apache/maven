package org.apache.maven.profiles.matchers;

import java.io.File;
import java.util.Properties;

import org.apache.maven.model.ActivationFile;
import org.apache.maven.model.Profile;

public class FileMatcher implements ProfileMatcher {

	public boolean isMatch(Profile profile, Properties properties) {
		if (profile == null) {
			throw new IllegalArgumentException("profile: null");
		}

		if(profile.getActivation() == null || profile.getActivation().getFile() == null)
		{
			return false;
		}
		
		ActivationFile f = profile.getActivation().getFile();
		
		if (f.getExists() != null && !new File(f.getExists()).exists()) {
			return false;
		}
		
		if (f.getMissing() != null && new File(f.getMissing()).exists()) {
			return false;
		}
		
		return true;
	}
}
