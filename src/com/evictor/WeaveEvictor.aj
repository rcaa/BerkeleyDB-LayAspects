package com.evictor;

import com.sleepycat.je.recovery.Checkpointer;
import driver.Driver;

public privileged aspect WeaveEvictor extends WeaveEvictorAbstract {

	pointcut driver() : if(new Driver().isActivated("evictor"));
	
	pointcut hook_checkpointStart(Checkpointer cp) 
	 : WeaveEvictorAbstract.hook_checkpointStart(cp) 
	 	&& driver();
	
	Object around() : adviceexecution() && within(com.evictor.WeaveEvictorAbstract) && !driver() {
		return null;
	}
}