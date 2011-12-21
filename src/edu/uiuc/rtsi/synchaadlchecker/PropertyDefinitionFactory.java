package edu.uiuc.rtsi.synchaadlchecker;

import edu.cmu.sei.aadl.model.pluginsupport.OsateResourceManager;
import edu.cmu.sei.aadl.model.property.PropertyDefinition;
import edu.cmu.sei.aadl.model.property.PropertySet;

public class PropertyDefinitionFactory {
	public static PropertyDefinition lookupStandardPropertyDefinition(
			final String propertyName) {
		PropertyDefinition propertyDefinition  = OsateResourceManager.findPropertyDefinition(propertyName);
		return propertyDefinition ;
	}

	public static PropertyDefinition lookupDefinedPropertyDefinition(
			final String propertySetKey, final String propertyNameKey) {
		String propertySetName = PropertyLoader.getString(propertySetKey);
		String propertyName = PropertyLoader.getString(propertyNameKey);
		PropertySet propertySet = OsateResourceManager.findPropertySetInResourceSet(propertySetName);
		
		PropertyDefinition propertyDefinition = propertySet.findPropertyDefinition(propertyName);
		
		return propertyDefinition;
	}
}
