package com.lookaheadcache;

import java.util.Map;

import com.sleepycat.je.cleaner.FileProcessor;
import com.sleepycat.je.cleaner.LNInfo;
import com.sleepycat.je.tree.TreeLocation;

import driver.Driver;

public privileged aspect LookAheadCaching extends LookAheadCacheAbstract {

	pointcut driver() : if(new Driver().isActivated("lookAheadCache"));
	
	pointcut processLN(Long fileNum, TreeLocation location, Long offset,
			LNInfo info, Map map, FileProcessor fp) 
		: LookAheadCacheAbstract.processLN(fileNum, location, offset,
					info, map, fp) 
					&& driver();

	Object around() : adviceexecution() && within(com.lookaheadcache.LookAheadCacheAbstract) && !driver() {
		return null;
	}
}