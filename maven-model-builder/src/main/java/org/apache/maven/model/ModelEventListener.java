package org.apache.maven.model;

import org.apache.maven.model.Model;

public interface ModelEventListener {

	void fire(Model model);
    
}

