package edu.uiuc.aadl.maude;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.emf.common.util.EList;

import edu.cmu.sei.aadl.model.core.AObject;
import edu.cmu.sei.aadl.model.instance.SystemInstance;
import edu.cmu.sei.aadl.model.util.AadlConstants;
import edu.cmu.sei.aadl.model.util.AadlUtil;
import edu.cmu.sei.aadl.model.util.ForAllAObject;
import edu.cmu.sei.aadl.model.util.UnparseText;

public class RtmAadlUtil {
	
	private static final String NEWLINE = AadlConstants.newlineChar;
	private static final String BUNDLE_NAME = "edu.uiuc.aadl.maude.setting"; 
	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);


	/**
	 * Read the setting file
	 * @param 	the setting name
	 * @return	the setting value
	 */
	public static String getSetting(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} 
		catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
	
	/**
	 * Does processing of list with separators
	 * 
	 * @param list
	 * @param separator	
	 * @param empty			shown when the list is empty.
	 * @param newline		add new line for each item when true.
	 */
	@SuppressWarnings("unchecked")
	static public void processEList(ForAllAObject self, UnparseText aadlText, EList list, String separator, String empty) {
		boolean first = true;
		String[] sep = separator.split(NEWLINE, -1);
		
		if (list == null || list.isEmpty())
			aadlText.addOutput(empty);
		else {
			for (Object o : list)
			{
				if (first)
					first = false;
				else {
					for (int i = 0; i < sep.length; ++i) {
						if (i > 0)
							aadlText.addOutputNewline(AadlConstants.emptyString);
						aadlText.addOutput(sep[i]);
					}
				}
				
				if (o instanceof AObject)
					self.processObject((AObject) o);
//				else if (o instanceof AbstractEnumerator)
//					aadlText.addOutput(((AbstractEnumerator) o).getName().toLowerCase());
				else
					throw new IllegalStateException("processEList: unexpected behavior!");
			}
		}
	}
	
	/**
	 * Escape all "_" in the name. 
	 * @param name  target name
	 * @return      escaped name
	 */
	static public String escape(String name) {
		return name.replaceAll("_", "").replaceAll("\\.", " . ");
	}
	
	static public String translateUnit(String num, String unit) {
		if (unit == null)						return num;
		if (unit.toLowerCase().equals("ms"))	return num;
		if (unit.toLowerCase().equals("sec"))	return num;
		return num + " " + unit;
	}
	
	static public boolean isInstanceModel(Object obj) {
		AObject model = AadlUtil.getAObject(obj);
		return (model != null) && (model instanceof SystemInstance);
	}
	
	

}
