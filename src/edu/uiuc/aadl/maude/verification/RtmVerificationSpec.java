package edu.uiuc.aadl.maude.verification;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.sei.aadl.model.instance.InstanceObject;

public class RtmVerificationSpec {
	
	private InstanceObject model;
	
	private List<RtmVerificationComm> commands;
	private List<RtmPropertyDef> definitions;
	
	public String simulBound;	// for simulation bound
	
	public RtmVerificationSpec(InstanceObject model) {
		this.model = model;
		commands = new ArrayList<RtmVerificationComm>();
		definitions = new ArrayList<RtmPropertyDef>();
		simulBound = "";
	}
	
	public InstanceObject getModel() {
		return model;
	}
	
	public List<RtmPropertyDef> getPropertyDefList() {
		return definitions;
	}
	
	public List<RtmVerificationComm> getCommandList() {
		return commands;
	}
	


}
