package org.eclipse.update.core;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

public class VersionedIdentifier {
	private String id;
	private Version version;
	private static final String	SEPARATOR = "_";
	
	public VersionedIdentifier(String idWithVersion) {
		
		if (idWithVersion==null || (idWithVersion=idWithVersion.trim()).equals("")) {
				this.id = "";
				this.version = new Version();
			}
		
		int loc = idWithVersion.lastIndexOf(SEPARATOR);
		if (loc != -1) {
			id = idWithVersion.substring(0, loc);
			String versionName = idWithVersion.substring(loc+1);
			version = new Version(versionName);
		} else {
			this.id = "";			
			version = new Version();
		}
	}
	
	public VersionedIdentifier(String id, String versionName) {
		if (id==null || (id=id.trim()).equals("") || versionName==null)
				throw new IllegalArgumentException();
		this.id = id;
		this.version = new Version(versionName);
	}
	
	public String getIdentifier() {
		return id;
	}
	
	public Version getVersion() {
		return version;
	}
	
	public String toString(){
		return id.equals("")?"":id+SEPARATOR+version.toString();
	}
	

	public boolean equals(VersionedIdentifier vid) {
		if (!this.id.equals(vid.id)) return false;
		return this.version.equals(vid.version);
	}
}

