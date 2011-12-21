package edu.uiuc.aadl.maude.verification;

import edu.uiuc.aadl.maude.RtmAadlUtil;

public class RtmPropCodeGenerator {
	
	static public String generateRtmModuleString(RtmVerificationSpec spec) {
		StringBuilder res = new StringBuilder();

		res.append("load "+spec.getModel().getName()+".maude\n")
		   .append("load "+RtmAadlUtil.getSetting("Semantics.analysis")+"\n")
		   .append("\n")
		   .append("(tomod " + spec.getModel().getName() + "-VERIFICATION-DEF is \n")
		   .append("  including " + RtmAadlUtil.escape(spec.getModel().getName()) + " .\n")
		   .append("  including LTL-MODEL-CHECK-AADL .\n")
		   .append("\n");
		for (RtmPropertyDef pd : spec.getPropertyDefList())
			res.append(RtmPropCodeGenerator.generateRtmEquation(pd,"  ")).append("\n");
		res.append("endtom)");
		return res.toString();
	}
	
	static public String generateRtmEquation(RtmPropertyDef pd, String indent) {
		StringBuilder res = new StringBuilder();
		res.append(indent + "op " + pd.getName() + " : -> Formula .\n")
		   .append(indent + "eq " + pd.getName() + "\n")
		   .append(indent + " = " + pd.getDefinition() + " .");
		return res.toString();
	}
	
	
	static public String generateRtmCommand(RtmVerificationComm vc) {
		return "(mc {initial} |=u " + vc.getPropertyString() + " .)    --- label: " + vc.getName();
	}
	
	static public String generateRtmSimul(int bound) {
		return "(trew {initial} in time <= " + bound + " .)";
	}

}
