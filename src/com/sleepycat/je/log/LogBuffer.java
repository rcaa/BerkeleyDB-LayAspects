/*-
 * See the file LICENSE for redistribution information.
 *
 * Copyright (c) 2002-2006
 *      Sleepycat Software.  All rights reserved.
 *
 * $Id: LogBuffer.java,v 1.1.6.1.2.4 2006/10/25 21:29:06 ckaestne Exp $
 */

package com.sleepycat.je.log;

import java.nio.ByteBuffer;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.dbi.EnvironmentImpl;
import com.sleepycat.je.utilint.DbLsn;

/**
 * DbLogBuffers hold outgoing, newly written log entries.
 */
class LogBuffer implements LogSource {

	private static final String DEBUG_NAME = LogBuffer.class.getName();

	/* Storage */
	private ByteBuffer buffer;

	/* Information about what log entries are held here. */
	private long firstLsn;

	private long lastLsn;

	/* The read latch serializes access to and modification of the LSN info. */


	LogBuffer(int capacity, EnvironmentImpl env) throws DatabaseException {

			buffer = ByteBuffer.allocate(capacity);
		reinit();
	}

	/*
	 * Used by LogManager for the case when we have a temporary buffer in hand
	 * and no LogBuffers in the LogBufferPool are large enough to hold the
	 * current entry being written. We just wrap the temporary ByteBuffer in a
	 * LogBuffer and pass it to FileManager. [#12674].
	 */
	LogBuffer(ByteBuffer buffer, long firstLsn) throws DatabaseException {

		this.buffer = buffer;
		this.firstLsn = firstLsn;
		this.lastLsn = firstLsn;
	}

	void reinit() throws DatabaseException {

		buffer.clear();
		firstLsn = DbLsn.NULL_LSN;
		lastLsn = DbLsn.NULL_LSN;
	}

	/*
	 * Write support
	 */

	/**
	 * Return first LSN held in this buffer. Assumes the log write latch is
	 * held.
	 */
	long getFirstLsn() {
		return firstLsn;
	}

	/**
	 * This LSN has been written to the log.
	 */
	void registerLsn(long lsn) throws DatabaseException {

			if (lastLsn != DbLsn.NULL_LSN) {
				assert (DbLsn.compareTo(lsn, lastLsn) > 0);
			}
			lastLsn = lsn;
			if (firstLsn == DbLsn.NULL_LSN) {
				firstLsn = lsn;
			}
	}

	/**
	 * Check capacity of buffer. Assumes that the log write latch is held.
	 * 
	 * @return true if this buffer can hold this many more bytes.
	 */
	boolean hasRoom(int numBytes) {
		return (numBytes <= (buffer.capacity() - buffer.position()));
	}

	/**
	 * @return the actual data buffer.
	 */
	ByteBuffer getDataBuffer() {
		return buffer;
	}

	/**
	 * @return capacity in bytes
	 */
	int getCapacity() {
		return buffer.capacity();
	}

	/*
	 * Read support
	 */

	/**
	 * Support for reading a log entry out of a still-in-memory log
	 * 
	 * @return true if this buffer holds the entry at this LSN. The buffer will
	 *         be latched for read. Returns false if LSN is not here, and
	 *         releases the read latch.
	 */
	boolean containsLsn(long lsn) throws DatabaseException {

		/* Latch before we look at the LSNs. */
		boolean found = false;
		if ((firstLsn != DbLsn.NULL_LSN)
				&& ((DbLsn.compareTo(firstLsn, lsn) <= 0) && (DbLsn.compareTo(
						lastLsn, lsn) >= 0))) {
			found = true;
		}

		if (found) {
			return true;
		} else {
			return false;
		}
	}


	/*
	 * LogSource support
	 */

	/**
	 * @see LogSource#release
	 */
	public void release() throws DatabaseException {
	}



	/**
	 * @see LogSource#getBytes
	 */
	public ByteBuffer getBytes(long fileOffset) {

		/*
		 * Make a copy of this buffer (doesn't copy data, only buffer state) and
		 * position it to read the requested data.
		 * 
		 * Note that we catch Exception here because it is possible that another
		 * thread is modifying the state of buffer simultaneously. Specifically,
		 * this can happen if another thread is writing this log buffer out and
		 * it does (e.g.) a flip operation on it. The actual mark/pos of the
		 * buffer may be caught in an unpredictable state. We could add another
		 * latch to protect this buffer, but that's heavier weight than we need.
		 * So the easiest thing to do is to just retry the duplicate operation.
		 * See [#9822].
		 */
		ByteBuffer copy = null;
		while (true) {
			try {
				copy = buffer.duplicate();
				copy
						.position((int) (fileOffset - DbLsn
								.getFileOffset(firstLsn)));
				break;
			} catch (IllegalArgumentException IAE) {
				continue;
			}
		}
		return copy;
	}

	/**
	 * @see LogSource#getBytes
	 */
	public ByteBuffer getBytes(long fileOffset, int numBytes) {
		ByteBuffer copy = getBytes(fileOffset);
		/* Log Buffer should always hold a whole entry. */
		assert (copy.remaining() >= numBytes) : "copy.remaining="
				+ copy.remaining() + " numBytes=" + numBytes;
		return copy;
	}
}
