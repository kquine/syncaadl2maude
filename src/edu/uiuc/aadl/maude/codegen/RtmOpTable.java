package edu.uiuc.aadl.maude.codegen;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.cmu.sei.aadl.model.util.UnparseText;

public class RtmOpTable {
	
	private Map<String, Set<String>> opNameTable;
	
	public RtmOpTable() {
		opNameTable = new HashMap<String, Set<String>>();
	}
	
	/**
	 * Add required entity names into opNameTable
	 * @param sort   RTMaude sort name
	 * @param name   Constant name
	 */
	public void register(String sort, String name) {
		if ( !opNameTable.containsKey(sort) )
			opNameTable.put(sort, new HashSet<String>());
		opNameTable.get(sort).add(name);
	}
	
	public void printAadlText(UnparseText aadlText) {
		for (Map.Entry<String,Set<String>> entry : opNameTable.entrySet()) {
			aadlText.addOutput("ops ");
			for (String op : entry.getValue())
				aadlText.addOutput(op + " ");
			aadlText.addOutputNewline(": -> " + entry.getKey() + " [ctor] .");
		}
	}

}
