package org.apache.maven.project.harness;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.jxpath.JXPathContext;
import org.apache.maven.model.Model;
import org.apache.maven.project.builder.PomClassicDomainModel;
import org.apache.maven.shared.model.ModelProperty;

public class PomTestWrapper {

	private PomClassicDomainModel domainModel;
	
	private JXPathContext context;
	
	public PomTestWrapper(PomClassicDomainModel domainModel) throws IOException {
		if(domainModel == null) {
			throw new IllegalArgumentException("domainModel: null");
		}
		this.domainModel = domainModel;
		context = JXPathContext.newContext(domainModel.getModel());
	}
	
	public PomTestWrapper(File file) throws IOException {
		if(file == null) {
			throw new IllegalArgumentException("file: null");
		}
		
		this.domainModel = new PomClassicDomainModel(file);
		context = JXPathContext.newContext(domainModel.getModel());
	}	
	
	public PomTestWrapper(Model model) throws IOException  {
		if(model == null) {
			throw new IllegalArgumentException("model: null");
		}
		
		this.domainModel = new PomClassicDomainModel(model);
		context = JXPathContext.newContext(domainModel.getModel());
	}		

    public PomClassicDomainModel getDomainModel()
    {
        return this.domainModel;
    }

	public String getValueOfProjectUri(String projectUri, boolean withResolvedValue) throws IOException {
		if(projectUri.contains("#collection") || projectUri.contains("#set")) {
			throw new IllegalArgumentException("projectUri: contains a collection or set");
		}
		return asMap(withResolvedValue).get(projectUri);
	}
	
	public void setValueOnModel(String expression, Object value) {
		context.setValue(expression, value);
	}
	/*
	public int containerCountForUri(String uri) throws IOException {
		if(uri == null || uri.trim().equals("")) {
			throw new IllegalArgumentException("uri: null or empty");
		}
		ModelDataSource source = new DefaultModelDataSource();
		source.init(domainModel.getModelProperties(), null);
		return source.queryFor(uri).size();
	}
	*/
	
	public Iterator<?> getIteratorForXPathExpression(String expression) {
		return context.iterate(expression);
	}
	
	public boolean containsXPathExpression(String expression) {
		return context.getValue(expression) != null;
	}

    public Object getValue(String expression) {
        return context.getValue(expression);
    }	
	
	public boolean xPathExpressionEqualsValue(String expression, String value) {
		return context.getValue(expression) != null && context.getValue(expression).equals(value);
	}	

	public Map<String, String> asMap(boolean withResolvedValues) throws IOException {
		Map<String, String> map = new HashMap<String, String>();
		for(ModelProperty mp : domainModel.getModelProperties()) {
			if(withResolvedValues) {
				map.put(mp.getUri(), mp.getResolvedValue());	
			} else {
				map.put(mp.getUri(), mp.getValue());	
			}
			
		}
		return map;
	}
	
	public boolean containsModelProperty(ModelProperty modelProperty) throws IOException {
		return domainModel.getModelProperties().contains(modelProperty);
	}
	
	public boolean containsAllModelPropertiesOf(List<ModelProperty> modelProperties) throws IOException {
		for(ModelProperty mp : modelProperties) {
			if(!containsModelProperty(mp)) {
				return false;
			}
		}
		return true;
	}	
	
	public boolean matchModelProperties(List<ModelProperty> hasProperties, List<ModelProperty> doesNotHaveProperties) throws IOException {
		return containsAllModelPropertiesOf(hasProperties) && containNoModelPropertiesOf(doesNotHaveProperties);
	}
	
	public boolean matchUris(List<String> hasAllUris, List<String> doesNotHaveUris) throws IOException {
		return hasAllUris(hasAllUris) && hasNoUris(doesNotHaveUris);
	}
	
	public boolean containNoModelPropertiesOf(List<ModelProperty> modelProperties) throws IOException {
		for(ModelProperty mp : modelProperties) {
			if(containsModelProperty(mp)) {
				return false;
			}
		}
		return true;
	}		

	public boolean hasUri(String uri) throws IOException {
		for(ModelProperty mp : domainModel.getModelProperties()) {
			if(mp.getValue().equals(uri)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean hasAllUris(List<String> uris) throws IOException {
		for(String s : uris) {
			if(!hasUri(s)) {
				return false;
			}
		}
		return true;
	}
	
	public boolean hasNoUris(List<String> uris) throws IOException {
		for(String s : uris) {
			if(hasUri(s)) {
				return false;
			}
		}
		return true;
	}	
	
}
