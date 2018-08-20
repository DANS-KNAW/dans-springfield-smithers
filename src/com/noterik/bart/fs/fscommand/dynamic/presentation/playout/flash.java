/* 
* flash.java
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
package com.noterik.bart.fs.fscommand.dynamic.presentation.playout;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.fscommand.DynamicCommand;
import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;
import com.noterik.springfield.tools.fs.FSXMLBuilder;
import com.noterik.springfield.tools.fs.URIParser;

public class flash implements DynamicCommand {
	/** Logger */
	private static Logger log = Logger.getLogger(flash.class);

	public synchronized String run(String uri,String xml) {
		log.debug("start dynamic/presentation/playout/flash");
		log.debug("qpr url="+uri);
		long timer_start = new Date().getTime();
		Document returnXml = DocumentHelper.createDocument();
		Element fsxml = returnXml.addElement("fsxml");
		fsxml.addElement("properties");
			
		String domain = URIParser.getDomainIdFromUri(uri);			
		String user = URIParser.getUserIdFromUri(uri);
		String selectedplaylist = "";
		Element handlerparams = null;
			
		int pos = xml.indexOf("<virtualpath>");
		if (pos!=-1) {
			selectedplaylist = xml.substring(pos+13+14);
			pos = selectedplaylist.indexOf("</virtualpath>");
			if (pos!=-1) {
				selectedplaylist = selectedplaylist.substring(0,pos);
				try {
					handlerparams = (Element) DocumentHelper.parseText(xml).getRootElement();
				} catch(Exception docerror) {
					log.error("invalid parameters in xml");
				}
			} else {
				log.error("invalid virtual path");
			}
		}
					
		if (uri.indexOf("/collection/") == -1 || uri.indexOf("/presentation") == -1) {
			return FSXMLBuilder.getErrorMessage("403", "No collection presentation found",
					"You have to supply a valid collection presentation", "http://teamelements.noterik.nl/team");
		}
			
		config conf = new config(domain, user, selectedplaylist, handlerparams);
			
		Document presentationProperties = cache.get(uri);
		if (presentationProperties==null) {
			presentationProperties = FSXMLRequestHandler.instance().getNodeProperties(uri, false);
			if (presentationProperties == null) {
				return FSXMLBuilder.getErrorMessage("404", "Presentation not found",
					"You have to supply an existing presentation", "http://teamelements.noterik.nl/team");
			}
			cache.put(uri, presentationProperties);
		}
	
		String collection = uri.substring(uri.indexOf("/collection/")+12, uri.indexOf("/presentation/"));
		conf.setCollection(collection);
			
		log.debug("presentation "+uri+" domain "+domain+" user "+user+" collection "+collection);
			
		Node presentationXml = getPresentation(uri);
		if (presentationXml != null) {
			fsxml.add(presentationXml);
			log.debug("past presentation xml");
			List<Element> videos = addVideos(presentationXml);
			for (int i = 0; i < videos.size(); i++) {
				fsxml.add(videos.get(i));
			}
			log.debug("past adding video(s)");
		}
		Node presentationConfig = getPresentationConfig(uri, presentationXml, conf);

		if (presentationConfig != null) {
			fsxml.add(presentationConfig);
			log.debug("past presentation config");
			
			List<Element> players = addPlayer(presentationConfig);			
			for (int j = 0; j < players.size(); j++) {
				fsxml.add(players.get(j));
			}
		}			
		log.debug("past adding player(s)");
		
		// moved the remapping of the presentation so we already have the video nodes.
		// Warning: This relies on the corrected presentation config (sponsor/user/collection level
		// don't move before the presentationconfig it's added to the document (dom4j issue)	!!
		presentationXml = dynamicTransform((Element)presentationXml,conf,fsxml);

		Node collectionConfig = getCollectionConfig(uri, conf);			
		if (collectionConfig != null) {
			fsxml.add(collectionConfig);
		}
			
		log.debug("past collection config");
		
		log.debug("end dynamic/presentation/playout/flash");
			
		long timer_end = new Date().getTime();
		log.debug("GENTIME="+(timer_end-timer_start)+" CACHE AT "+cache.getPerformance()+"% req="+cache.getTotalRequest()+" size="+cache.getCacheSize()+" URI="+uri);
		return fsxml.asXML();
	}

	private static Node getPresentationConfig(String presentation, Node presentationXml, config c) {		
		Document tmpConf = null;		

		// domain conf
		log.debug("before domain conf");
		String url = "/domain/"+c.getDomain()+"/config/presentation/filesystem/1";
		Document conf = cache.get(url);
		if (conf==null) {
			conf = FSXMLRequestHandler.instance().getNodeProperties(url, false);
			cache.put(url, conf);
		}
		log.debug("after domain conf");
		Boolean allowReplace = conf.selectSingleNode("/fsxml/filesystem[@id='1']/properties/allow_replace") == null ? false : Boolean.valueOf(conf.selectSingleNode("/fsxml/filesystem[@id='1']/properties/allow_replace").getText());
		
		//check if presentation has a sponsor, take his config
		String sponsor = presentationXml.selectSingleNode("//presentation/properties/sponsor") == null ? null : presentationXml.selectSingleNode("//presentation/properties/sponsor").getText();
		if (sponsor != null) {
			log.debug("found sponsor in presentation = "+sponsor);
			// sponsor conf
			log.debug("before sponsor conf");
			tmpConf = cache.get(url);
			if (tmpConf==null && !cache.isEmpty(sponsor+"/config/presentation/filesystem/1")) {
				tmpConf = FSXMLRequestHandler.instance().getNodeProperties(sponsor+"/config/presentation/filesystem/1", false);	
				cache.put(sponsor+"/config/presentation/filesystem/1",tmpConf);
			}
			log.debug("after sponsor conf");
			
			if (tmpConf != null && (conf == null || !allowReplace)) {
				conf = tmpConf;
				allowReplace = conf.selectSingleNode("/fsxml/filesystem[@id='1']/properties/allow_replace") == null ? false : Boolean.valueOf(conf.selectSingleNode("/fsxml/filesystem[@id='1']/properties/allow_replace").getText());
			} else if (tmpConf != null && allowReplace) {
				handleIncludeExcludeNodes(conf, tmpConf);
				allowReplace = tmpConf.selectSingleNode("/fsxml/filesystem[@id='1']/properties/allow_replace") == null ? false : Boolean.valueOf(tmpConf.selectSingleNode("/fsxml/filesystem[@id='1']/properties/allow_replace").getText());
				tmpConf = null;
			}			
		}
		
		// user conf
		log.debug("before user conf");
		tmpConf = cache.get("/domain/"+c.getDomain()+"/user/"+c.getUser()+"/config/presentation/filesystem/1");
		if (tmpConf==null && !cache.isEmpty("/domain/"+c.getDomain()+"/user/"+c.getUser()+"/config/presentation/filesystem/1")) {
			tmpConf = FSXMLRequestHandler.instance().getNodeProperties("/domain/"+c.getDomain()+"/user/"+c.getUser()+"/config/presentation/filesystem/1", false);	
			cache.put("/domain/"+c.getDomain()+"/user/"+c.getUser()+"/config/presentation/filesystem/1", tmpConf);
		}	
		log.debug("after sponsor conf");
		
		if (tmpConf != null && (conf == null || !allowReplace)) {
			conf = tmpConf;
			allowReplace = conf.selectSingleNode("/fsxml/filesystem[@id='1']/properties/allow_replace") == null ? false : Boolean.valueOf(conf.selectSingleNode("/fsxml/filesystem[@id='1']/properties/allow_replace").getText());
		} else if (tmpConf != null && allowReplace) {
			handleIncludeExcludeNodes(conf, tmpConf);
			allowReplace = tmpConf.selectSingleNode("/fsxml/filesystem[@id='1']/properties/allow_replace") == null ? false : Boolean.valueOf(tmpConf.selectSingleNode("/fsxml/filesystem[@id='1']/properties/allow_replace").getText());
			tmpConf = null;
		}

		// user collection conf
		log.debug("before collection conf");
		tmpConf = cache.get("/domain/"+c.getDomain()+"/user/"+c.getUser()+"/collection/"+c.getCollection()+"/config/presentation/filesystem/1");
		if (tmpConf==null && !cache.isEmpty("/domain/"+c.getDomain()+"/user/"+c.getUser()+"/collection/"+c.getCollection()+"/config/presentation/filesystem/1")) {
			tmpConf = FSXMLRequestHandler.instance().getNodeProperties("/domain/"+c.getDomain()+"/user/"+c.getUser()+"/collection/"+c.getCollection()+"/config/presentation/filesystem/1", false);
			cache.put("/domain/"+c.getDomain()+"/user/"+c.getUser()+"/collection/"+c.getCollection()+"/config/presentation/filesystem/1", tmpConf);
		}
		log.debug("after collection conf");
		
		if (tmpConf != null) {
			String refer = tmpConf.selectSingleNode("/fsxml/filesystem/@referid") == null ? "" : tmpConf.selectSingleNode("/fsxml/filesystem/@referid").getText();
			if (!refer.equals("")) {
				tmpConf = FSXMLRequestHandler.instance().getNodeProperties(refer, false);				
			}
		}
		
		if (tmpConf != null && (conf == null || !allowReplace)) {
			conf = tmpConf;
		} else if (tmpConf != null && allowReplace) {
			handleIncludeExcludeNodes(conf, tmpConf);
		}
		
		if (conf != null) {
			return conf.selectSingleNode("fsxml/filesystem[@id='1']").detach();
		}	
		return null;
	}
	
	private static void handleIncludeExcludeNodes(Document conf, Document tmpConf) {
		List<Node> includeNodes = tmpConf.selectNodes("/fsxml/filesystem[@id='1']/*[@id and not(ends-with(@id,'_exclude'))]");
		List<Node> excludeNodes = tmpConf.selectNodes("/fsxml/filesystem[@id='1']/*[ends-with(@id,'_exclude')]");
		
		log.debug("number of includeNodes = "+includeNodes.size());
		for (int j = 0; j < includeNodes.size(); j++) {
			log.debug(j+" = "+includeNodes.get(j).toString());
		}
		log.debug("number of excludeNodes = "+excludeNodes.size());
		for (int j = 0; j < excludeNodes.size(); j++) {
			log.debug(j+" = "+excludeNodes.get(j).toString());
		}
		
		Element base = (Element) conf.selectSingleNode("/fsxml/filesystem[@id='1']");
		
		if (includeNodes != null) {
			for (int i = 0; i < includeNodes.size(); i++) {
				String nodename = includeNodes.get(i).getName();					
				String nodeid = includeNodes.get(i).valueOf("@id");
				
				log.debug("check if node exists "+nodename+" id "+nodeid);
				
				Node existingNode = base.selectSingleNode(nodename+"[@id='"+nodeid+"']");
				if (existingNode != null) {
					log.debug("node exists, replace");
					List contentOfBase = base.content();
					int index = contentOfBase.indexOf(existingNode);
					contentOfBase.set(index, includeNodes.get(i).detach());
				} else {
					base.add(includeNodes.get(i).detach());
				}
			}
		}
		
		if (excludeNodes != null) {
			log.debug("handling exclude nodes for user");
			for (int i = 0; i < excludeNodes.size(); i++) {
				log.debug("handling exclude node nr "+i);
				String nodename = excludeNodes.get(i).getName();					
				String nodeid = excludeNodes.get(i).valueOf("@id");					
				nodeid = nodeid.substring(0, nodeid.lastIndexOf("_exclude"));
				
				log.debug("about to exclude "+nodename+" with id "+nodeid);
				
				Node remove = base.selectSingleNode(nodename+"[@id='"+nodeid+"']");
				if (remove != null) {
					log.debug("node to exclude found, detach");
					remove.detach();
				}
			}
		}
	}
	
	private static List<Element> addPlayer(Node configNode) {
		List<Node> players = configNode.selectNodes("//player");
		List<Element> result = new ArrayList<Element>();
		
		for(Iterator<Node> iter = players.iterator(); iter.hasNext(); ) {
			Element player = (Element) iter.next();
		
			String refer = player.selectSingleNode("@referid") == null ? "" : player.selectSingleNode("@referid").getText();
			if (!refer.equals("")) {
				Document playerXml = cache.get(refer);
				if (playerXml==null && !cache.isEmpty(refer)) {
					playerXml = FSXMLRequestHandler.instance().getNodeProperties(refer, false);
// daniel
					cache.put(refer,playerXml);
				}
				if (playerXml != null) {
					Element p = (Element) playerXml.selectSingleNode("fsxml/player").detach();
					p.addAttribute("fullid", refer);
					result.add(p);
				}				
			}
		}
		return result;
	}
	
	private static Node getCollectionConfig(String presentation, config c) {
		String url = "/domain/"+c.getDomain()+"/user/"+c.getUser()+"/collection/"+c.getCollection()+"/config/collection";
		Document conf = cache.get(url);
		if (conf==null && !cache.isEmpty(url)) {
			conf = FSXMLRequestHandler.instance().getNodeProperties(url, false);
			//log.debug("CONF (Needs fix daniel)="+conf+" R="+cache.isEmpty(url));
			cache.put(url, conf);
		}
		
		if (conf != null) {
			return conf.selectSingleNode("fsxml/config[@id='collection']").detach();
		}
		return null;
	}
	
	private static Node getPresentation(String presentation) {
		Document pres = cache.get(presentation);
		if (pres==null) {
			pres = FSXMLRequestHandler.instance().getNodeProperties(presentation, false);
			cache.put(presentation, pres);
		}
		
		String refer = "";
		
		if (pres != null) {
			refer = pres.selectSingleNode("fsxml/presentation/@referid") == null ? "" : pres.selectSingleNode("fsxml/presentation/@referid").getText();
			if (refer != "") {
				pres = cache.get(refer);
				if (pres==null) {
					pres = FSXMLRequestHandler.instance().getNodeProperties(refer, false);
					cache.put(refer, pres);
				}
			}
		}
		if (pres != null) {
			Element pr = (Element) pres.selectSingleNode("fsxml/presentation").detach();
			if (!refer.equals("")) {
				pr.addAttribute("fullid", refer);
				return pr;
			}
			return pr;
		}
		return null;
	}
	
	private static Node dynamicTransform(Element pr, config conf, Element fsxml) {
		Element domainvpconfig = (Element) fsxml.selectSingleNode("//filesystem[@id='1']/videoplaylist[@id='1']");
		String selectedplaylist = conf.getSelectedPlaylist();
		Element handlerparams = conf.getHandlerParams();
		if (domainvpconfig!=null) {
			log.debug("vp config = "+domainvpconfig.asXML());
			// so lets see if we have a forward
			String forward = domainvpconfig.selectSingleNode("properties/forward") == null ? "" : domainvpconfig.selectSingleNode("properties/forward").getText();
			log.debug("forward = "+forward);
			if (selectedplaylist=="" && forward!=null && forward!="") {
				selectedplaylist = forward;
				log.debug("selected playlist = "+selectedplaylist);
			}
		}
		

		return pr;
	}
	
	private static List<Element> addVideos(Node presentationNode) {	
		List<Node> videos = presentationNode.selectNodes("//videoplaylist/video");
		ArrayList<String> refers = new ArrayList<String>(videos.size());
		List<Element> vids = new ArrayList<Element>();
		
		for(Iterator<Node> iter = videos.iterator(); iter.hasNext(); ) {
			Element video = (Element) iter.next();
			
			String refer = video.selectSingleNode("@referid") == null ? "" : video.selectSingleNode("@referid").getText();
			log.debug("getting video "+refer);
			if (!refer.equals("") && !refers.contains(refer)) {
				refers.add(refer);
				Document videoXml = cache.get(refer);
				if (videoXml==null) {
					videoXml = FSXMLRequestHandler.instance().getNodeProperties(refer, false);
					cache.put(refer, videoXml);
				}
				if (videoXml != null) {
					Element vid = (Element) videoXml.selectSingleNode("fsxml/video").detach();
					vid.addAttribute("fullid", refer);
					//log.debug("U="+vid.asXML());
					vids.add(vid);
				}
			}
		}
		return vids;
	}
	
	public static class config {
		private String domain;
		private String user;
		private String collection;
		private String selectedplaylist;
		private Element handlerparams;
		
		public config(String domain, String user, String selectedplaylist, Element handlerparams) {
			this.domain = domain;
			this.user = user;
			this.selectedplaylist = selectedplaylist;
			this.handlerparams = handlerparams;
		}
		
		public String getDomain() {
			return domain;
		}
		
		public String getUser() {
			return user;
		}
		
		public String getSelectedPlaylist() {
			return selectedplaylist;
		}
		
		public String getCollection() {
			return collection;
		}
		
		public Element getHandlerParams() {
			return handlerparams;
		}
		
		public void setCollection(String collection) {
			this.collection = collection;
		}
	}
}
