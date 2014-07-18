/* 
* OpenTagging.java
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
package com.noterik.bart.fs.fscommand.dynamic.playlist.generators;

import java.util.*;
import org.apache.log4j.Logger;
import org.dom4j.Element;

import com.noterik.bart.fs.fscommand.dynamic.config.flash;
import com.noterik.bart.fs.fscommand.dynamic.playlist.GeneratorInterface;
import com.noterik.bart.fs.fscommand.dynamic.playlist.PlaylistGenerator.generators;
import com.noterik.bart.fs.fscommand.dynamic.playlist.util.TimeLine;

import org.dom4j.*;

/**
 * 
 * @author Daniel Ockeloen
 * 
 * Create a videoplaylist based on the layer name and filter , It generates video blocks
 * based on found blocks.
 * 
 * all events are shifted based on that (not implemented yet).
 *
 */
public class OpenTagging implements GeneratorInterface {

	/** Logger */
	private static Logger logger = Logger.getLogger(flash.class);
	

	
	/*
	 * the incoming generate call from either quickpresentation or direct restlet
	 * 
	 * @see com.noterik.bart.fs.fscommand.dynamic.playlist.GeneratorInterface#generate(org.dom4j.Node, java.lang.String)
	 */
	public Element generate(Element pr,String wantedplaylist, Element params,Element domainvpconfig,Element fsxml) {
		// for now this does nothing its a trick to allow for a full playout so
		// we can tagging.
		//System.out.println("open tagging called");
		
		TimeLine timeline = new TimeLine(pr);
		
		// lets map old events  into the playlist
		timeline.remapOldEvents();
		
		// lets map all the video based events into the playlist
		timeline.remapVideoEvents(fsxml);
		
		// ok we are all done so put all the events into the presentation
		timeline.insertNodesInPresentation();
		
		// always return the presentation even if no hits found (empty video list now)
		return pr;
	}
	
}
