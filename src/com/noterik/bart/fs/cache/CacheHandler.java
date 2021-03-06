/* 
* CacheHandler.java
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
package com.noterik.bart.fs.cache;

import java.util.List;

public interface CacheHandler {
	/**
	 * Returns the name of this CacheHanler.
	 * 
	 * @return The name of this CacheHanler.
	 */
	public String getName();
	
	/**
	 * Check if Object represents a cached key
	 * @param key
	 * @return
	 */
	public boolean isKeyInCache(Object key);
	
	/**
	 * Get keys
	 * @return
	 */
	public List<Object> getKeys();
	
	/**
	 * Get Object represented in cache by key
	 * @param key
	 * @return
	 */
	public Object get(Object key);
	
	/**
	 * Get Object represented in cache by key, without updating the statistics.
	 * @param key
	 * @return
	 */
	public Object getQuiet(Object key);
	
	/**
	 * Put new Object in cache 
	 * @param key
	 * @param value
	 */
	public void put(Object key, Object value);
	
	/**
	 * Remove Object from cache
	 * @param key
	 */
	public void delete(Object key);
	
	/**
	 * Remove all Objects from cache
	 */
	public void deleteAll();
	
	/**
	 * Returns the statistics from cache.
	 * 
	 * @return The statistics from cache.
	 */
	public Statistics getStatistics();
	
	/**
	 * Clear the statistics of the cache.
	 */
	public void clearStatistics();
	
	/**
	 * Set the enabled state of the cache handler.
	 * 
	 * @param enabled
	 */
	public void setEnabled(boolean enabled);
	
	/**
	 * Returns the enabled state of the cache handler.
	 * 
	 * @return The enabled state of the cache handler.
	 */
	public boolean getEnabled();
	
	/**
	 * Destroy cache handler and clean up afterwards.
	 */
	public void destroy();
}
