package edu.uiuc.aadl.maude.codegen;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.UniqueEList;
import org.eclipse.emf.ecore.EObject;

import edu.cmu.sei.aadl.model.connection.ConnectionTiming;
import edu.cmu.sei.aadl.model.connection.DataConnection;
import edu.cmu.sei.aadl.model.connection.util.ConnectionSwitch;
import edu.cmu.sei.aadl.model.core.AObject;
import edu.cmu.sei.aadl.model.core.AnnexSubclause;
import edu.cmu.sei.aadl.model.core.Connection;
import edu.cmu.sei.aadl.model.core.NamedElement;
import edu.cmu.sei.aadl.model.feature.DataPort;
import edu.cmu.sei.aadl.model.feature.EventDataPort;
import edu.cmu.sei.aadl.model.feature.EventPort;
import edu.cmu.sei.aadl.model.feature.Parameter;
import edu.cmu.sei.aadl.model.feature.Port;
import edu.cmu.sei.aadl.model.feature.util.FeatureSwitch;
import edu.cmu.sei.aadl.model.instance.ComponentInstance;
import edu.cmu.sei.aadl.model.instance.FeatureInstance;
import edu.cmu.sei.aadl.model.instance.InstanceObject;
import edu.cmu.sei.aadl.model.instance.PortConnectionInstance;
import edu.cmu.sei.aadl.model.instance.SystemInstance;
import edu.cmu.sei.aadl.model.instance.util.InstanceSwitch;
import edu.cmu.sei.aadl.model.property.BooleanAND;
import edu.cmu.sei.aadl.model.property.BooleanNOT;
import edu.cmu.sei.aadl.model.property.BooleanOR;
import edu.cmu.sei.aadl.model.property.BooleanOrPropertyReference;
import edu.cmu.sei.aadl.model.property.ComponentCategory;
import edu.cmu.sei.aadl.model.property.EnumValue;
import edu.cmu.sei.aadl.model.property.FALSE;
import edu.cmu.sei.aadl.model.property.NumberValue;
import edu.cmu.sei.aadl.model.property.PropertyAssociation;
import edu.cmu.sei.aadl.model.property.PropertyValue;
import edu.cmu.sei.aadl.model.property.ReferenceValue;
import edu.cmu.sei.aadl.model.property.StringValue;
import edu.cmu.sei.aadl.model.property.TRUE;
import edu.cmu.sei.aadl.model.property.util.PropertySwitch;
import edu.cmu.sei.aadl.model.util.AadlConstants;
import edu.cmu.sei.aadl.model.util.AadlProcessingSwitch;
import edu.cmu.sei.aadl.model.util.UnparseText;
import edu.uiuc.aadl.maude.RtmAadlUtil;

public class RtmAadlModelUnparser extends AadlProcessingSwitch {

	private UnparseText aadlText;
	private RtmOpTable opTable;
	
	private static final String NEWLINE = AadlConstants.newlineChar;
	
	public RtmAadlModelUnparser() {
		super();
		aadlText = new UnparseText();
		opTable = new RtmOpTable();
	}
	
	/**
	 * generate the RTM model. The model must be a declarative model.
	 * The specs for the subcomponent will be added to externalSpecs.
	 * 
	 * @param obj				AObject
	 * @param externalSpecs		The external specs of subcomponents will be added
	 * @return					Generated RTM Model
	 */
	public String doUnparse(AObject obj)
	{
		AObject root = obj.getAObjectRoot();
		if (root instanceof InstanceObject)
		{
			String modelName = RtmAadlUtil.escape(((InstanceObject)root).getName());
			
			// process the model
			aadlText.incrementIndent();
			self.process(root);
			String contents = aadlText.getParseOutput();
			
			// generate the output
			aadlText = new UnparseText();
			aadlText.addOutputNewline("load " + RtmAadlUtil.getSetting("Semantics.main"));
			aadlText.addOutputNewline("(tomod " + modelName + " is");
			aadlText.incrementIndent();
			aadlText.addOutputNewline("including SYNCHRONOUS-STEP .");
			aadlText.addOutputNewline(AadlConstants.emptyString);
			
			// op-declarations for ids
			aadlText.addOutputNewline("--- names, states, variables");
			opTable.printAadlText(aadlText);
			aadlText.addOutputNewline(AadlConstants.emptyString);
			
			// the initial state
			aadlText.addOutputNewline("--- the initial state");
			aadlText.addOutputNewline("op initial : -> Configuration .");
			if (root instanceof ComponentInstance) {
				aadlText.addOutputNewline(String.format("eq initial = %s : %s .",
						modelName, getComponentDeclName((ComponentInstance)root)));
				aadlText.addOutputNewline("eq MAIN = " + modelName + " .");
			}
			aadlText.addOutputNewline(AadlConstants.emptyString);
			
			// finally, add AADL contents
			aadlText.addOutputNewline("---------------------------------------------");
			aadlText.addOutputNewline("--- AADL instance");
			aadlText.addOutputNewline("---------------------------------------------");
			aadlText.addOutputNewline(AadlConstants.emptyString);
			aadlText.addOutputNewline("var COMP : ComponentId .");
			aadlText.addOutputNewline(AadlConstants.emptyString);
			aadlText.addOutput(contents);
			
			aadlText.decrementIndent();
			aadlText.addOutputNewline("endtom)");
			
			aadlText.addOutputNewline(AadlConstants.emptyString);
			aadlText.addOutputNewline("(set tick det .)");

			return aadlText.getParseOutput();
		}
		else
			throw new UnsupportedOperationException(root.eClass().getName() + " is not an instance.");
	}
	
	@Override
	protected void initSwitches() {

		//NOTE: caseAObject, caseNamedElement, casePropertyHolder, and caseMode call the "coreSwitch".
		//NOTE: casePropertyValue and caseReferenceValue call the "propertySwitch"
		
		instanceSwitch = new InstanceSwitch() {
			
			private Map<ComponentInstance,EList<Connection>> connContextMap =
				new HashMap<ComponentInstance,EList<Connection>>();
			private Stack<Set<String>> subcompStack = new Stack<Set<String>>();
			private Set<String> compDeclSet = new HashSet<String>();

			/**
			 * component instances
			 */
			public Object caseComponentInstance(ComponentInstance object) {
				String compId = RtmAadlUtil.escape(object.getName());
				String compCate = RtmAadlUtil.escape(object.getCategory().getFileName());
				String compDecl = getComponentDeclName(object);
				
				// register component id name, and fill subcomponent information
				// only if this component is a subcomponent.
				opTable.register("ComponentId", compId); 
				if ( !subcompStack.isEmpty() )	
					subcompStack.peek().add(compId + " : " + compDecl);
				
				// generate each declaration once.
				if ( !compDeclSet.contains(compDecl)) {
					compDeclSet.add(compDecl);
					boolean isThread = (object.getCategory().getValue() == ComponentCategory.THREAD);
					
					// handle subcomponents first.
					subcompStack.add(new HashSet<String>());
					self.processEList(object.getComponentInstance());
					
					// // process behavior annex subclause for threads
					if ( isThread && object.getXComponentImpl() != null) {
						for (Object o : object.getXComponentImpl().getAnnexSubclause()) {
							AnnexSubclause as = (AnnexSubclause)o;
							if (as.getName().equals("behavior_specification")) 
								(new RtmBehaviorAnnexTranslator(aadlText, opTable)).process(as);
						}
					}
					
					aadlText.addOutputNewline(String.format("eq COMP : %s =", compDecl));
					aadlText.incrementIndent();
					aadlText.addOutputNewline(String.format("< COMP : %s |", compCate));
					aadlText.incrementIndent();
					
					aadlText.addOutputNewline("features : ");
					aadlText.incrementIndent();
					RtmAadlUtil.processEList(self, aadlText, object.getFeatureInstance(), NEWLINE, "none");
					aadlText.addOutputNewline(",");
					aadlText.decrementIndent();
					
					if ( isThread ) {
						aadlText.addOutputNewline("behaviorRef : (" + compDecl + "),");
						aadlText.addOutputNewline("variables : stateVariables(" + compDecl +"),");
						aadlText.addOutputNewline("currState : initialState(" + compDecl +"),");
						aadlText.addOutputNewline("completeStates : completeStates(" + compDecl +"),");
					}
					
					aadlText.addOutputNewline("properties : ");
					aadlText.incrementIndent();
					RtmAadlUtil.processEList(self, aadlText, object.getPropertyAssociation(), " ;" + NEWLINE, "noProperty");
					aadlText.addOutputNewline(",");
					aadlText.decrementIndent();
					
					aadlText.addOutputNewline("connections : ");
					aadlText.incrementIndent();
					RtmAadlUtil.processEList(self,aadlText, connContextMap.get(object), " ;" + NEWLINE, "none");
					aadlText.addOutputNewline(",");
					aadlText.decrementIndent();
					
					aadlText.addOutputNewline("subcomponents : ");
					aadlText.incrementIndent();
					if (subcompStack.peek().isEmpty())
						aadlText.addOutputNewline("none");
					for (String s : subcompStack.pop())
						aadlText.addOutputNewline("(" + s + ")");
					aadlText.decrementIndent();
					
					aadlText.decrementIndent();
					aadlText.addOutputNewline("> .");
					aadlText.decrementIndent();
					aadlText.addOutputNewline(AadlConstants.emptyString);
				}
				return DONE;
			}
			

			/**
			 * system instances. Check connection instances.
			 */
			public Object caseSystemInstance(SystemInstance object) {
				self.processEList(object.getConnectionInstance());
				return NOT_DONE;
			}
			
			/**
			 * pass through to featureSwitch.
			 */
			public Object caseFeatureInstance(FeatureInstance object) {
				return featureSwitch.doSwitch(object.getFeature());
			}

			/**
			 * For a connection instance, construct corresponding declarative connection
			 * table for the later use. 
			 */
			@SuppressWarnings("unchecked")
			public Object casePortConnectionInstance(PortConnectionInstance object) {
				Iterator conns = object.getConnection().iterator();
				Iterator ctxts = object.getConnectionContext().iterator();
				while (conns.hasNext()) {
					Connection conn = (Connection) conns.next();
					ComponentInstance ctxt = (ComponentInstance) ctxts.next();
					if ( !connContextMap.containsKey(ctxt))
						connContextMap.put(ctxt, new UniqueEList());
					connContextMap.get(ctxt).add(conn);
				}
				return DONE;
			}
			
			/**
			 * TODO: NOT_DEFINED = caseAccessConnectionInstance, caseModeTransitionConnectionInstance
			 *         caseMode, caseSystemOperationMode, caseModeInstance, caseModeTransitionInstance
			 *         caseFlowSpecInstance, caseEndToEndFlowInstance, caseFlowElementInstance
			 *         SystemInstanceConfiguration
			 */
			public Object defaultCase(EObject object) {
				aadlText.addOutputNewline(object.getClass().getName() + " : " + object.toString());
				return object;
			}
		};
		
		featureSwitch = new FeatureSwitch() {
			private String portCate = "";		// for port
			
			/**
			 * Does the common part of port
			 */
			public Object casePort(Port object) {
				String portId = RtmAadlUtil.escape(object.getName());
				String dir = (object.getDirection().incoming() ? "In" : "")
			               + (object.getDirection().outgoing() ? "Out" : "");
				String fmt = 
					(object.getContainingComponentType().getCategory().getName().equals("thread")
						&& dir.equals("In") && portCate.equals("Data"))
				          ? "< %s : %s%sThreadPort | content : noMsg, fresh : false >"
				          : "< %s : %s%sPort | content : noMsg >";
				aadlText.addOutput(String.format(fmt, portId, dir, portCate));
				//TODO: properties and refined ports are omitted here!
				opTable.register("PortId", portId);					// register port id
				portCate = "";
				return DONE;
			}
			
			/**
			 * Does the parameter
			 */
			public Object caseParameter(Parameter object) {
				String paramId = RtmAadlUtil.escape(object.getName());
				String dir = (object.getDirection().incoming() ? "In" : "")
					       + (object.getDirection().outgoing() ? "Out" : "");
				aadlText.addOutput(String.format("< %s : %sParam | value : noValue >", paramId, dir));
				//TODO: properties and refined ports are omitted here!
				opTable.register("ParamId", paramId);				// register param id
				return DONE;
			}
			
			public Object caseDataPort(DataPort object) { portCate="Data"; return NOT_DONE;}
			public Object caseEventDataPort(EventDataPort object) { portCate="EventData"; return NOT_DONE;}
			public Object caseEventPort(EventPort object) { portCate="Event"; return NOT_DONE;}
			
			/**
			 * TODO: NOT_DEFINED = caseFeatures, caseSubprogram, casePortGroup, casePortGroupType
			 *                     caseBusAccess, caseServerSubprogram, caseDataAccess
			 */
			public Object defaultCase(EObject object) {
				throw new UnsupportedOperationException(object.eClass().getName() + ": not supported yet.");
			}
		};
		
		connectionSwitch = new ConnectionSwitch() {
			
			private String connectionType = "";	// for connection
			
			/**
			 * Translate a connection.  This includes all specific cases.
			 */
			public Object caseConnection(Connection object) {
				// TODO: refined connections, properties, and modeMembers are not considered here!
				aadlText.addOutput(String.format("(%s %s %s)", 
						RtmAadlUtil.escape(object.getSrcQualifiedName()),
						connectionType.isEmpty() ? "-->" : connectionType,
						RtmAadlUtil.escape(object.getDstQualifiedName())));
				connectionType = "";
				return DONE;
			}
			
			/**
			 *  Set a connection type for data connections. The default case is "-->".
			 */
			public Object caseDataConnection(DataConnection object) {
				connectionType = object.getTiming() == ConnectionTiming.IMMEDIATE_LITERAL ? "-->" : "-->>";
				return NOT_DONE;
			}
			
			/**
			 * If translation is not defined, throw an exception.
			 * NOT_DEFINED = caseConnections
			 */
			public Object defaultCase(EObject object) {
				throw new UnsupportedOperationException(object.eClass().getName() + ": not supported yet.");
			}

		};
		
		propertySwitch = new PropertySwitch() {
			
			// The following property set cases are ignored:
			//   casePropertySet, casePropertyDefinition, caseClassifierType, ,caseUnitsType, 
			//   caseEnumLiteral, caseUnitLiteral, caseReferenceType,
			//   casePropertyConstant, caseAadlboolean, caseAadlstring, caseEnumType, caseRangeType
			//   caseAadlinteger, caseAadlreal
			
			/**
			 * if not defined for a value, throw an exception.
			 * TODO: NOT_DEFINED = caseProperties,
			 *                     caseClassifierValue, caseRangeValue, 
			 *                     casePropertyReference, casePropertyConstant
			 */
			public Object casePropertyValue(PropertyValue object) {
				throw new UnsupportedOperationException(
						"The value " + object.getValueAsString() + " is  not supported yet.");
			}

			public Object caseReferenceValue(ReferenceValue object) {
				boolean first = true;
				for (Object o : object.getReferenceElement()) {
					if (first)
						first = false;
					else
						aadlText.addOutput(" . ");
					aadlText.addOutput(RtmAadlUtil.escape(((NamedElement) o).getName()));
				}
				return DONE;
			}

			@SuppressWarnings("unchecked")
			public Object casePropertyAssociation(PropertyAssociation object) {
				//TODO: "+=>", "access", "applies to", "binding", modeMembers are ignored.
				//FIXME: use object.getQualifiedName() to avoid conflicts.
				aadlText.addOutput(RtmAadlUtil.escape(object.getPropertyDefinition().getName()) + "(");
				final EList<EObject> pl = object.getPropertyValue();
				if (pl != null) {
					if (pl.size() == 1)
						self.processEList(pl);
					else {
						aadlText.addOutput("(");
						RtmAadlUtil.processEList(self, aadlText, pl, ", ", "");
						aadlText.addOutput(")");
					}
				}
				aadlText.addOutput(")");
				return DONE;
			}
			

			@SuppressWarnings("unchecked")
			public Object caseBooleanAND(BooleanAND object) {
				EList<BooleanOrPropertyReference> el = object.getBooleanValue();
				self.process(el.get(0));
				aadlText.addOutput(" and ");
				self.process(el.get(1));
				return DONE;
			}
			
			@SuppressWarnings("unchecked")
			public Object caseBooleanOR(BooleanOR object) {
				EList<BooleanOrPropertyReference> el = object.getBooleanValue();
				self.process(el.get(0));
				aadlText.addOutput(" or ");
				self.process(el.get(1));
				return DONE;
			}

			public Object caseBooleanNOT(BooleanNOT object) {
				aadlText.addOutput("not ");
				self.process(object.getBooleanValue());
				return DONE;
			}

			public Object caseTRUE(TRUE object) { aadlText.addOutput("true"); return DONE;}
			public Object caseFALSE(FALSE object) { aadlText.addOutput("false"); return DONE;}

			public Object caseStringValue(StringValue object) {
				String s = object.getValue();
				if (s.startsWith("\"") && s.endsWith("\"")) {
					aadlText.addOutput(s);
				} else {
					//FIXME: an ad-hoc method to handle InputConstraints.
					// InputConstrains had better to be moved into property description.
					// aadlText.addOutput("\"" + s + "\"");
					aadlText.addOutput(s);
				}
				return DONE;
			}

			public Object caseNumberValue(NumberValue object) {
				String valstr = object.getValueString();
				String us = object.getUnitLiteralName();
				aadlText.addOutput(RtmAadlUtil.translateUnit(valstr, us));
				return DONE;
			}
			
			public Object caseEnumValue(EnumValue object) {
				if (object.getEnumLiteral() != null)
					aadlText.addOutput(object.getEnumLiteral().getName());
				else
					aadlText.addOutput(object.getValueAsString());
				return DONE;
			}
		};
	}
	
	public String getComponentDeclName(ComponentInstance object) {
		String compCate = RtmAadlUtil.escape(object.getCategory().getUnparseName());
		if (object.getXComponentImpl() != null) {
			String typeName = RtmAadlUtil.escape(object.getXComponentImpl().getQualifiedTypeName());
			String implName = RtmAadlUtil.escape(object.getXComponentImpl().getImplName());
			opTable.register("TypeName", typeName);
			opTable.register("ImplName", implName);
			return compCate + " " + typeName + " . " + implName;
		}
		else if (object.getXComponentType() != null) {
			String typeName = RtmAadlUtil.escape(object.getXComponentType().getQualifiedName());
			opTable.register("TypeName", typeName);
			return compCate + " " + typeName;
		}
		else
			return compCate + " " + "$NODECL$";
	}

}
