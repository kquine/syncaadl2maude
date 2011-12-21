package edu.uiuc.aadl.maude.verification;

/**
 * Property definition container of the form (name = definition)
 *
 *TODO: Make the definition can be parameterized with variables (how can we detect sorts??)
 * 
 * @author kquine
 *
 */
public class RtmPropertyDef {
	
	//FIXME: The whole defs cannot contain the character "_"!
	
	private String name;
	private String definition;
	
	public RtmPropertyDef(String name, String def) {
		this.name = name;
		this.definition = def;
	}

	public String getName() {
		return name;
	}

	public String getDefinition() {
		return definition;
	}

}
