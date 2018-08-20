/* 
* CacheTableWriter.java
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

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.dom4j.Node;

import com.noterik.bart.fs.fsxml.FSXMLRequestHandler;

public class CacheTableWriter extends Thread {
	private static final Logger log = Logger.getLogger(CacheTableWriter.class);
	
	private CacheTableReader reader = null;
	private int mb = 1024*1024;
	private volatile boolean running = true;
	
	public CacheTableWriter(String name,CacheTableReader reader) {
		super(name);
		this.reader = reader;
	}
	
	public void run() {
		while (running) {
			try {
				sleep(10*1000);
				//log.debug("FR="+reader.fullyLoaded()+" C="+cache.getCachedUrls());
				if (reader.fullyLoaded() && cache.getCachedUrls()!=null) {
					
					String body = "<fsxml><properties><list>";
				
					for(Iterator<String> iter = cache.getCachedUrls(); iter.hasNext(); ) {
						String url = (String)iter.next();
						url = url.replace(",", ";");
						//log.debug("URL="+url);
						body+=url;
						if (iter.hasNext()) body+=",";
					}
				
					body+="</list></properties></fsxml>";
					String hostname = InetAddress.getLocalHost().toString();
					int pos = hostname.indexOf("/");
					if (pos!=-1) hostname=hostname.substring(pos+1);
					//log.debug("HOSTNAME="+hostname+" "+body);
					FSXMLRequestHandler.instance().handlePUT("/domain/webtv/tmp/cache/dataset/"+hostname+"/properties",body);;
				}
				Runtime runtime = Runtime.getRuntime();

				long totalmem = runtime.totalMemory() / mb;
				long freemem = runtime.freeMemory() / mb;
				long usedmem = (runtime.totalMemory() - runtime.freeMemory()) / mb;
				long maxmem = runtime.maxMemory() / mb;
				//log.debug("totalmem = "+totalmem+"MB");
				//log.debug("freemem = "+freemem+"MB");
				//log.debug("usedmem = "+usedmem+"MB");
				//log.debug("maxmem = "+maxmem+"MB");
				sleep(50*1000);
			} catch (InterruptedException ex) {
				// break out of the loop without a error
			} catch(Exception e) {
				log.debug("Can't sleep in CacheTableWriter."+e);
			//	e.printStackTrace();
			}
		}
		log.debug("shutting down CacheTableWriter");
	}
	
	public void destroy() {
		running = false;
		this.interrupt();
	}
	

}
