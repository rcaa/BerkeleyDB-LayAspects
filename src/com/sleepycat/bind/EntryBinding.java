/*-
 * See the file LICENSE for redistribution information.
 *
 * Copyright (c) 2000-2006
 *      Sleepycat Software.  All rights reserved.
 *
 * $Id: EntryBinding.java,v 1.1.6.1 2006/07/28 09:02:41 ckaestne Exp $
 */

package com.sleepycat.bind;

import com.sleepycat.je.DatabaseEntry;

/**
 * A binding between a key or data entry and a key or data object.
 * 
 * @author Mark Hayes
 */
public interface EntryBinding {

	/**
	 * Converts a entry buffer into an Object.
	 * 
	 * @param entry
	 *            is the source entry buffer.
	 * 
	 * @return the resulting Object.
	 */
	Object entryToObject(DatabaseEntry entry);

	/**
	 * Converts an Object into a entry buffer.
	 * 
	 * @param object
	 *            is the source Object.
	 * 
	 * @param entry
	 *            is the destination entry buffer.
	 */
	void objectToEntry(Object object, DatabaseEntry entry);
}
