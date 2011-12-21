package edu.uiuc.aadl.maude.verification;

/**
 * Property specification container.
 * 
 * TODO: Currently,  i)there is no syntax check for properties,
 *    and ii) Only un-timed commands are supported. 
 * 
 * @author kquine
 *
 */
public class RtmVerificationComm {
	
	//
	//The basic syntax checking will be useful.
	//TODO: reachability and invariant should be added
	
	private String name;
	private String prop;
	
	public RtmVerificationComm(String name, String property) {
		this.name = name;
		this.prop = property;
	}

	public String getName() {
		return name;
	}
	
	public String getPropertyString() {
		return prop;
	}

	
}
