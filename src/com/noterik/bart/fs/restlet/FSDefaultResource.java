/* 
* FSDefaultResource.java
* 
* Copyright (c) 2012 Noterik B.V.
* 
* This file is part of smithers, related to the Noterik Springfield project.
*
* Smithers is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Smithers is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Smithers.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.noterik.bart.fs.restlet;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.restlet.representation.Representation;
import org.restlet.resource.ServerResource;


public abstract class FSDefaultResource extends ServerResource {

	/** The FSDefaultResource's log4j Logger */
	private static Logger logger = Logger.getLogger(FSDefaultResource.class);

	protected final String getResourceUri() {
		String path = getRequest().getResourceRef().getPath();
		String uri2 = path.substring(2);
		String uri = uri2.substring(uri2.indexOf("/"));
		if (uri.lastIndexOf("/") == uri.length() - 1) {
			uri = uri.substring(0, uri.lastIndexOf("/"));
		}
		return uri;
	}

	protected String getRequestBodyData(Representation representation) {
		String data = null;
		try {
			if (representation != null) {
				data = representation.getText();
			}
		} catch (IOException e2) {
			logger.error("Could not get request body data",e2);
			return null;
		}
		return data;
	}
}