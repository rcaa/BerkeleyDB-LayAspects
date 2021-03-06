/*-
 * See the file LICENSE for redistribution information.
 *
 * Copyright (c) 2006
 *      Sleepycat Software.  All rights reserved.
 *
 * $Id: VerifyUtils.java,v 1.1.6.1.2.2 2006/10/23 17:22:06 ckaestne Exp $
 */

package com.sleepycat.je.cleaner;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DbInternal;
import com.sleepycat.je.dbi.DatabaseImpl;
import com.sleepycat.je.dbi.SortedLSNTreeWalker;
import com.sleepycat.je.dbi.SortedLSNTreeWalker.TreeNodeProcessor;
import com.sleepycat.je.log.LogEntryType;
import com.sleepycat.je.utilint.DbLsn;

/**
 * Verify cleaner data structures
 */
public class VerifyUtils {

	private static final boolean DEBUG = false;

	/**
	 * Compare the lsns referenced by a given Database to the lsns held in the
	 * utilization profile. Assumes that the database and environment is
	 * quiescent, and that there is no current cleaner activity.
	 */
	public static void checkLsns(Database db) throws DatabaseException {

		DatabaseImpl dbImpl = DbInternal.dbGetDatabaseImpl(db);

		/* Get all the lsns in the database. */
		GatherLSNs gatherLsns = new GatherLSNs();
		long rootLsn = dbImpl.getTree().getRootLsn();
		SortedLSNTreeWalker walker = new SortedLSNTreeWalker(dbImpl, false, // don't
																			// remove
																			// from
																			// INList
				rootLsn, gatherLsns);
		walker.walk();
		Set lsnsInTree = gatherLsns.getLsns();
		lsnsInTree.add(new Long(rootLsn));

		/* Get all the files used by this database. */
		Iterator iter = lsnsInTree.iterator();
		Set fileNums = new HashSet();

		while (iter.hasNext()) {
			long lsn = ((Long) iter.next()).longValue();
			fileNums.add(new Long(DbLsn.getFileNumber(lsn)));
		}

		/* Gather up the obsolete lsns in these file summary lns */
		iter = fileNums.iterator();
		Set obsoleteLsns = new HashSet();
		UtilizationProfile profile = dbImpl.getDbEnvironment()
				.getUtilizationProfile();

		while (iter.hasNext()) {
			Long fileNum = (Long) iter.next();

			PackedOffsets obsoleteOffsets = new PackedOffsets();
			TrackedFileSummary tfs = profile.getObsoleteDetail(fileNum,
					obsoleteOffsets, false /* logUpdate */);
			PackedOffsets.Iterator obsoleteIter = obsoleteOffsets.iterator();
			while (obsoleteIter.hasNext()) {
				long offset = obsoleteIter.next();
				Long oneLsn = new Long(DbLsn.makeLsn(fileNum.longValue(),
						offset));
				obsoleteLsns.add(oneLsn);
				if (DEBUG) {
					System.out.println("Adding 0x"
							+ Long.toHexString(oneLsn.longValue()));
				}
			}
		}

		/* Check than none the lsns in the tree is in the UP. */
		boolean error = false;
		iter = lsnsInTree.iterator();
		while (iter.hasNext()) {
			Long lsn = (Long) iter.next();
			if (obsoleteLsns.contains(lsn)) {
				System.err.println("Obsolete lsns contains valid lsn "
						+ DbLsn.getNoFormatString(lsn.longValue()));
				error = true;
			}
		}

		/*
		 * Check that none of the lsns in the file summary ln is in the tree.
		 */
		iter = obsoleteLsns.iterator();
		while (iter.hasNext()) {
			Long lsn = (Long) iter.next();
			if (lsnsInTree.contains(lsn)) {
				System.err.println("Tree contains obsolete lsn "
						+ DbLsn.getNoFormatString(lsn.longValue()));
				error = true;
			}
		}

		if (error) {
			throw new DatabaseException("Lsn mismatch");
		}
	}

	private static class GatherLSNs implements TreeNodeProcessor {
		private Set lsns = new HashSet();

		public void processLSN(long childLSN, LogEntryType childType) {
			lsns.add(new Long(childLSN));
		}

		public Set getLsns() {
			return lsns;
		}
	}
}
