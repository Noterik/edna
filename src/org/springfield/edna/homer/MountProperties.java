/* 
* MountProperties.java
* 
* Copyright (c) 2014 Noterik B.V.
* 
* This file is part of Edna, related to the Noterik Springfield project.
*
* Edna is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Edna is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Edna.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.springfield.edna.homer;

public class MountProperties {
	
	private String hostname;
	private String path;
	private String account;
	private String password;
	private String protocol;

	
	public void setHostname(String h) {
		hostname = h;
	}
	
	public void setPath(String p) {
		path = p;
	}
	
	public void setAccount(String a) {
		account = a;
	}
	
	public void setPassword(String p) {
		password = p;
	}
	
	public void setProtocol(String p) {
		protocol = p;
	}

	public String getHostname() {
		return hostname;
	}
	
	public String getPath() {
		return path;
	}
	
	public String getAccount() {
		return account;
	}
	
	public String getPassword() {
		return password;
	}
	
	public String getProtocol() {
		return protocol;
	}
}
