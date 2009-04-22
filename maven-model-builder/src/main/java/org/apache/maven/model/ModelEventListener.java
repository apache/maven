package org.apache.maven.model;

import java.util.List;

import org.apache.maven.model.Model;

public interface ModelEventListener {

	void fire(Model model);
	
    List<String> getUris();
    
}

