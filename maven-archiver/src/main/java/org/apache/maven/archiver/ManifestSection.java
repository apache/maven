package org.apache.maven.archiver;

import java.util.HashMap;
import java.util.Map;

public class ManifestSection {

	private String name = null;
	
	private Map manifestEntries = new HashMap();
	
	public void setName( String name ) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
    public void addManifestEntry( Object key, Object value )
    {
        manifestEntries.put( key, value );
    }

    public void addManifestEntries( Map map )
    {
        manifestEntries.putAll( map );
    }

    public boolean isManifestEntriesEmpty()
    {
        return manifestEntries.isEmpty();
    }

    public Map getManifestEntries()
    {
        return manifestEntries;
    }

}
