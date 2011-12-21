package edu.uiuc.rtsi.synchaadlchecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import edu.cmu.sei.aadl.model.connection.DataConnection;
import edu.cmu.sei.aadl.model.connection.EventConnection;
import edu.cmu.sei.aadl.model.connection.EventDataConnection;
import edu.cmu.sei.aadl.model.connection.FeatureContext;
import edu.cmu.sei.aadl.model.connection.impl.DataConnectionImpl;
import edu.cmu.sei.aadl.model.instance.ComponentInstance;
import edu.cmu.sei.aadl.model.instance.PortConnectionInstance;
import edu.cmu.sei.aadl.model.instance.SystemInstance;
import edu.cmu.sei.aadl.model.property.BooleanValue;
import edu.cmu.sei.aadl.model.property.EnumValue;
import edu.cmu.sei.aadl.model.property.IntegerValue;
import edu.cmu.sei.aadl.model.property.PropertyDefinition;
import edu.cmu.sei.osate.workspace.names.standard.AadlProperties;

public class ConstraintChecker {
	enum ConnectionDataType {DATA, EVENT, EVENT_DATA, NONE};
	private HashMap<String, ArrayList<String>> checkedImpls; 

	public ConstraintChecker(){
		checkedImpls = new HashMap<String, ArrayList<String>>();
	}

	public boolean checkThreadProperties(ComponentInstance thread) {
		try {
			if(!isPeriodic(thread)) {
				String exception = thread.getComponentInstancePath() + " is not periodic\n";
				addExceptions(thread.getComponentInstancePath(), exception);
				return false;
			}
			IntegerValue periodValue = isPeriodDefined(thread);
			if(periodValue == null) {
				String exception = thread.getComponentInstancePath() + " does not hava valid period\n";
				addExceptions(thread.getComponentInstancePath(), exception);
				return false;
			}

			IntegerValue synchPeriodValue = getSynchrounousPeriod(thread);
			if(synchPeriodValue == null) {
				String exception = thread.getComponentInstancePath() + " is not associated with a valid Synchronous Period\n";
				addExceptions(thread.getComponentInstancePath(), exception);
				return false;
			}

			if(!compare(periodValue, synchPeriodValue)) {
				String exception = thread.getComponentInstancePath() + "'s period is not equal to the specified Synchronous Period\n";
				addExceptions(thread.getComponentInstancePath(), exception);
				return false;
			}
			return true;
		} catch (Exception e) {

		}
		return false;	
	}

	@SuppressWarnings("unchecked")
	public boolean isPeriodic(ComponentInstance thread) {
		try {
			PropertyDefinition dispatchProtocolProperty = PropertyDefinitionFactory.lookupStandardPropertyDefinition(AadlProperties.DISPATCH_PROTOCOL);
			List list = thread.getPropertyValueList(dispatchProtocolProperty);
			if(list.size() > 0) {
				for(int i = 0; i < list.size(); i++) {
					EnumValue val = (EnumValue)list.get(i);
					if(!val.getEnumLiteral().getName().equalsIgnoreCase("Periodic")) {
						return false;
					}	
				}				
				return true;
			}
		} catch(Exception e) {}

		return false;
	}

	@SuppressWarnings("unchecked")
	public IntegerValue isPeriodDefined(ComponentInstance thread) {
		IntegerValue periodVal = null;
		try {
			PropertyDefinition threadPeriodProperty = PropertyDefinitionFactory.lookupStandardPropertyDefinition(AadlProperties.PERIOD);

			List list = thread.getPropertyValueList(threadPeriodProperty);
			if(list.size() > 0) {
				periodVal = (IntegerValue) list.get(0); 

				for(int i = 1; i < list.size(); i++) {
					IntegerValue temp = (IntegerValue) list.get(i);
					String tempVal = temp.getValueAsString();
					if (!tempVal.equalsIgnoreCase(periodVal.getValueAsString())){
						return null;
					}
				}				
				return periodVal;
			}			
		} catch(Exception e) {}

		return null;
	}

	@SuppressWarnings("unchecked")
	public IntegerValue getSynchrounousPeriod(ComponentInstance thread) {
		IntegerValue synchPeriodVal = null;
		try {
			PropertyDefinition synchPeriodProperty = PropertyDefinitionFactory.lookupDefinedPropertyDefinition("Synchronous.propertyset", "Synchronous.periodProperty");

			List list = thread.getPropertyValueList(synchPeriodProperty);
			if(list.size() > 0) {
				synchPeriodVal = (IntegerValue) list.get(0); 

				for(int i = 1; i < list.size(); i++) {
					IntegerValue temp = (IntegerValue) list.get(i);
					String tempVal = temp.getValueAsString();
					if (!tempVal.equalsIgnoreCase(synchPeriodVal.getValueAsString())){
						return null;
					}
				}
				return synchPeriodVal;
			}

		} catch(Exception e) {}

		return null;
	}

	@SuppressWarnings("unchecked")
	public boolean isEnvironment(ComponentInstance thread) {
		try {
			PropertyDefinition pd = PropertyDefinitionFactory.lookupDefinedPropertyDefinition("Synchronous.propertyset", "Synchronous.environmentProperty");
			List list = thread.getPropertyValueList(pd);
			//System.err.println(thread);
			//System.err.println(list);
			if(list.size() == 1) {
				BooleanValue bv = (BooleanValue) list.get(0);
				if(bv.isValue()) {
					return true;
				}
			}
		} catch(Exception e) {
			//System.err.println(thread);
			
			//e.printStackTrace();
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	public boolean isEnvironment(FeatureContext component) {
		try {
			PropertyDefinition pd = PropertyDefinitionFactory.lookupDefinedPropertyDefinition("Synchronous.propertyset", "Synchronous.environmentProperty");
			List list = component.getPropertyValueList(pd);
			//System.err.println(thread);
			//System.err.println(list);
			if(list.size() == 1) {
				BooleanValue bv = (BooleanValue) list.get(0);
				if(bv.isValue()) {
					return true;
				}
			}
		} catch(Exception e) {
			//System.err.println(thread);
			
			//e.printStackTrace();
		}
		return false;
	}
	public boolean compare(IntegerValue v1, IntegerValue v2) {
		String v1Val = v1.getValueAsString();
		String v2Val = v2.getValueAsString();

		if(v1Val.equalsIgnoreCase(v2Val)) {
			return true;
		}
		return false;
	}

	private void addExceptions(String key, String val) {
		ArrayList<String> logValues;
		logValues = checkedImpls.get(key);
		if(logValues == null) {
			logValues = new ArrayList<String>();
		}
		logValues.add(val);
		checkedImpls.put(key, logValues);
	}

	public boolean checkConnectionRules(PortConnectionInstance pci) {
		ConnectionDataType connDataType = getConnectionDataType(pci);

		if(connDataType == ConnectionDataType.DATA) {
			int connContextSize = pci.getConnectionContext().size();						
			ComponentInstance srcContainingComponent = (ComponentInstance)pci.getConnectionContext().get(0);
			ComponentInstance destContainingComponent = (ComponentInstance)pci.getConnectionContext().get(connContextSize-1);
						
			DataConnectionImpl dataSrcConn = (DataConnectionImpl)pci.getConnection().get(0);
			DataConnectionImpl dataDestConn = (DataConnectionImpl)pci.getConnection().get(connContextSize-1);

			FeatureContext srcContext = dataSrcConn.getSrcContext();
			FeatureContext destContext = dataDestConn.getDstContext();
			
			String srcPortName = "(" + srcContainingComponent.getXComponentImpl().getName() + ")" +
				srcContext.getName() + "." + 
				dataSrcConn.getSrc().getName(); 

			String destPortName = "(" + destContainingComponent.getXComponentImpl().getName() + ")" +
				destContext.getName() + "." + 
				dataDestConn.getDst().getName(); 

			if(isDelayedConnection(pci)) {
				if(!isEnvironment(srcContext) && !isEnvironment(destContext)) {
					return true;
				}
				addExceptions(srcPortName + "->> " + destPortName, "Invalid delayed communication");
				return false;
			} else {
				if(isEnvironment(srcContext) && !isEnvironment(destContext)) {
					return true;
				}
				addExceptions(srcPortName + "-> " + destPortName, "Invalid immediate communication");
				return false;
			}						
		} else {
			addExceptions(pci.getComponentInstancePath(), "Invalid port communication");
			return false;
		}
		
	}

	public ConnectionDataType getConnectionDataType(PortConnectionInstance pci) {
		if(pci.getConnection().get(0) instanceof EventConnection) {
			return ConnectionDataType.EVENT;
		}
		if(pci.getConnection().get(0) instanceof EventDataConnection) {
			return ConnectionDataType.EVENT_DATA;
		}
		if(pci.getConnection().get(0) instanceof DataConnection) {
			return ConnectionDataType.DATA;
		}

		else {
			return ConnectionDataType.NONE;
		}
	} 

	public boolean isDelayedConnection(PortConnectionInstance pci) {
		return pci.isDelayed();
	}
	@SuppressWarnings("unchecked")
	public boolean isSynchronous(SystemInstance system) {
		try {
			PropertyDefinition pd = PropertyDefinitionFactory.lookupDefinedPropertyDefinition("Synchronous.propertyset", "Synchronous.systemProperty");
			List list = system.getPropertyValueList(pd);
			if(list.size() == 1) {
				BooleanValue bv = (BooleanValue) list.get(0);
				if(bv.isValue()) {
					return true;
				}
			}
		} catch(Exception e) {
			//e.printStackTrace();
		}
		addExceptions(system.getName(), "\"Synchronous\" property not satisfied");
		return false;
	}



	public String getErrorString(){
		String output = "";
		ArrayList<String> ketSet = new ArrayList<String>(this.checkedImpls.keySet());

		if(ketSet.size() == 0) {
			output += "No exception found!\n";
		}
		else {
			for(String key: ketSet) {
				output += key + ":" + "\n";
				ArrayList<String> values = this.checkedImpls.get(key);

				for(String value: values) {
					output += "\t" + value + "\n";
				}
				output += "\n";
			}
		}
		return output;
	}
}
