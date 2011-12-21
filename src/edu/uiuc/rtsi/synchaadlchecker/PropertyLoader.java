package edu.uiuc.rtsi.synchaadlchecker;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class PropertyLoader {
	private static final String BUNDLE_NAME = "edu.uiuc.rtsi.synchaadlchecker.synchaadlconfig"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
			.getBundle(BUNDLE_NAME);

	private PropertyLoader() {
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
