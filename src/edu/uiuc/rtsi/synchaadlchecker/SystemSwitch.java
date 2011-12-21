package edu.uiuc.rtsi.synchaadlchecker;

import edu.cmu.sei.aadl.model.instance.ComponentInstance;
import edu.cmu.sei.aadl.model.instance.ConnectionInstance;
import edu.cmu.sei.aadl.model.instance.PortConnectionInstance;
import edu.cmu.sei.aadl.model.instance.SystemInstance;
import edu.cmu.sei.aadl.model.instance.util.InstanceSwitch;
import edu.cmu.sei.aadl.model.property.ComponentCategory;
import edu.cmu.sei.aadl.model.util.AadlProcessingSwitch;

public class SystemSwitch extends AadlProcessingSwitch {

	SystemInstance systemInstance;
	String outputString;
	ConstraintChecker checker;

	public SystemSwitch(SystemInstance systemInstance, ConstraintChecker checker) {
		this.systemInstance = systemInstance;
		this.outputString = "";
		this.checker = checker;
	}

	@Override
	protected void initSwitches() {
		instanceSwitch = new InstanceSwitch() {
			@Override
			public Object caseComponentInstance(ComponentInstance obj) {
				switch (obj.getCategory().getValue()) {
				case ComponentCategory.THREAD:
					checker.checkThreadProperties(obj);
					return DONE;
				}
				return DONE;
			}

			@Override
			public Object caseConnectionInstance(ConnectionInstance ci) {
				// System.out.println("ConnectionInstance: " + ci);
				if (ci instanceof PortConnectionInstance) {
					PortConnectionInstance pci = (PortConnectionInstance) ci;
					checker.checkConnectionRules(pci);
				}
				return DONE;
			}
		};

	}
}
