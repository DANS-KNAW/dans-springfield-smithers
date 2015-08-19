/* 
* GenericDAO.java
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
package com.noterik.bart.fs.fscommand.dao;

import java.io.Serializable;

/**
 * Generic Data Access Object interface, for basic CRUD functionality.
 * 
 * @author Derk Crezee <d.crezee@noterik.nl>
 * @copyright Copyright: Noterik B.V. 2009
 * @package com.noterik.bart.fs.dao
 * @access private
 *
 */
public interface GenericDAO <T,K extends Serializable> {
	
	/**
	 * Create object in persistent data source.
	 *  
	 * @param transferObject	transfer object
	 * @return					success
	 */
	public boolean create(T transferObject) throws DAOException;
	
	/**
	 * Loads object from data source and returns a transfer object.
	 * 
	 * @param key	key defining object in data source.
	 * @return		transfer object, null upon failure
	 */
	public T read(K key) throws DAOException;
	
	/**
	 * Update object in persistent data source.
	 * 
	 * @param transferObject	transfer object
	 * @return 					success
	 */
	public boolean update(T transferObject) throws DAOException;
	
	/**
	 * Delete object from data source, defined by key.
	 * 
	 * @param key				key defining object in data source.
	 * @return 					success
	 */
	public boolean delete(K key) throws DAOException;
}
