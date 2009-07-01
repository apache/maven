package org.apache.maven.model.building;

import org.apache.maven.model.Model;


public interface ModelEventListener {

	void fire(Model model);
    
}

