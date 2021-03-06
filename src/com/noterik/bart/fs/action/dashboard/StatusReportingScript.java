/* 
* StatusReportingScript.java
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
package com.noterik.bart.fs.action.dashboard;

import java.util.List;

import org.apache.log4j.Logger;

import com.noterik.bart.fs.action.ActionAdapter;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.fs.URIParser;

/**
 * Script responsible for logging status messages to the dashboard resource
 */
public class StatusReportingScript extends ActionAdapter {
	/** version UID */
	private static final long serialVersionUID = 1L;

	/** the StatusReportingScript's log4j logger */
	private static final Logger LOG = Logger.getLogger(StatusReportingScript.class);
	
	/** Dashboard resource URI */
	private static final String DASHBOARD_URI_TEMPLATE = "/domain/{domain}/dashboard/{type}";
	
	public String run() {
		LOG.debug("Reporting status for URI "+event.getUri() + ", method: "+event.getMethod());
		
		// determine domain and resource type (of parent, since this uri is of type 'status')
		String uri = event.getUri();
		String method = event.getMethod();
		String parent = URIParser.getParentUri(uri);
		String domain = URIParser.getDomainIdFromUri(uri);
		String rType = URIParser.getResourceTypeFromUri(parent);
		
		// determine dashboard resource
		String dashboardURI = DASHBOARD_URI_TEMPLATE.replace("{domain}", domain).replace("{type}", rType);
		LOG.debug("dashboard URI "+dashboardURI);
		
		// make symlink to resource if it doesn't already exists in the dashboard
		List<String> refPars = FSXMLRequestHandler.instance().getReferParents(uri);
		boolean existsInDashboard = false;
		String reference = null;
		for(String rp : refPars) {
			if( rp.startsWith(dashboardURI) ) {
				existsInDashboard = true;
				reference = rp;
				break;
			}
		}
		if(method.equals("DELETE")) {
			if(existsInDashboard) {
				// remove from dashboard
				LOG.debug("Status exist in dashboard and method was DELETE, removing from dashboard.");
				FSXMLRequestHandler.instance().deleteNodeProperties(reference, true);
			}			
		} else if(!existsInDashboard) {
			LOG.debug("Status doesn't exist in dashboard, adding to dashboard.");
			String dashboardStatusURI = dashboardURI+"/status"; 
			StringBuffer xml = new StringBuffer("<fsxml>");
			xml.append("<attributes>");
			xml.append("<referid>"+uri+"</referid>");
			xml.append("</attributes>");
			xml.append("</fsxml>");
			FSXMLRequestHandler.instance().handlePOST(dashboardStatusURI, xml.toString());
		}
		return null;
	}

}
