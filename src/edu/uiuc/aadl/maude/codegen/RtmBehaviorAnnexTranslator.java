package edu.uiuc.aadl.maude.codegen;

import java.util.Iterator;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.topcased.aadl.behavior.BehaviorAnnexProcessingSwitch;
import org.topcased.aadl.behavior.model.Assignment;
import org.topcased.aadl.behavior.model.BehaviorAnnex;
import org.topcased.aadl.behavior.model.BehaviorArray;
import org.topcased.aadl.behavior.model.BehaviorGuard;
import org.topcased.aadl.behavior.model.BehaviorParameter;
import org.topcased.aadl.behavior.model.BehaviorState;
import org.topcased.aadl.behavior.model.BehaviorTransition;
import org.topcased.aadl.behavior.model.BehaviorVariable;
import org.topcased.aadl.behavior.model.BinaryExpression;
import org.topcased.aadl.behavior.model.BinaryOperation;
import org.topcased.aadl.behavior.model.BooleanConstant;
import org.topcased.aadl.behavior.model.Communication;
import org.topcased.aadl.behavior.model.CompositeState;
import org.topcased.aadl.behavior.model.Computation;
import org.topcased.aadl.behavior.model.Concurrent;
import org.topcased.aadl.behavior.model.ConsTerm;
import org.topcased.aadl.behavior.model.Count;
import org.topcased.aadl.behavior.model.Delay;
import org.topcased.aadl.behavior.model.DestTerm;
import org.topcased.aadl.behavior.model.FeatureRef;
import org.topcased.aadl.behavior.model.Fresh;
import org.topcased.aadl.behavior.model.IfThenElse;
import org.topcased.aadl.behavior.model.InputOutput;
import org.topcased.aadl.behavior.model.IntConstant;
import org.topcased.aadl.behavior.model.IsTerm;
import org.topcased.aadl.behavior.model.LoopVariable;
import org.topcased.aadl.behavior.model.Quantifier;
import org.topcased.aadl.behavior.model.RefConstant;
import org.topcased.aadl.behavior.model.ReferenceExpression;
import org.topcased.aadl.behavior.model.ReferencedElement;
import org.topcased.aadl.behavior.model.State;
import org.topcased.aadl.behavior.model.SubcompRef;
import org.topcased.aadl.behavior.model.SubpClassifierRef;
import org.topcased.aadl.behavior.model.TimeInterval;
import org.topcased.aadl.behavior.model.Timeout;
import org.topcased.aadl.behavior.model.UnaryExpression;
import org.topcased.aadl.behavior.model.UnaryOperation;
import org.topcased.aadl.behavior.model.UnitConstant;
import org.topcased.aadl.behavior.model.VariableExpression;
import org.topcased.aadl.behavior.util.BehaviorSwitch;

import edu.cmu.sei.aadl.model.core.AObject;
import edu.cmu.sei.aadl.model.core.ComponentImpl;
import edu.cmu.sei.aadl.model.util.AadlConstants;
import edu.cmu.sei.aadl.model.util.UnparseText;
import edu.uiuc.aadl.maude.RtmAadlUtil;

/** 
 * 
 * @author Kyungmin Bae, Artur Boronat
 *
 */
class RtmBehaviorAnnexTranslator extends BehaviorAnnexProcessingSwitch {
	
	UnparseText txt;
	private RtmOpTable opTable;
	private static final String NEWLINE = AadlConstants.newlineChar;
	
	RtmBehaviorAnnexTranslator(UnparseText txt, RtmOpTable opTable) { 
		this.txt = txt; 
		this.opTable = opTable;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void initSwitches() {
		behaviorAnnexSwitch = new BehaviorSwitch() {
			
			boolean inAction = false;
			
			/**
			 * If translation is not defined, throw an exception.
			 * TODO: NOT_IMPLEMENTED = caseConcurrent, caseForall, caseExists, caseIteration, caseLoop
			 *                         caseIntRange, caseEnumRange, caseComposite
			 */
			public Object defaultCase(EObject object) {
				throw new UnsupportedOperationException(object.eClass().getName() + ": not supported yet.");
			}

			public Object caseBehaviorAnnex(BehaviorAnnex ba) {
				//TODO: composite or concurrent states are not processed!
				
				// behavior annex can be defined for component implementations!
				ComponentImpl impl = ba.getContainingComponentImpl();
				String qid = RtmAadlUtil.escape(impl.getCategory().getUnparseName()) + " " 
					       + RtmAadlUtil.escape(impl.getQualifiedTypeName())
						   + " . " + RtmAadlUtil.escape(impl.getImplName());;				
				
				//FIXME: if there is no initial, variables are not generated.
				txt.addOutputNewline("eq stateVariables(" + qid + ") =");
				txt.incrementIndent();
				processEList(ba.getInitial(), NEWLINE, "emptyValuation");
				txt.addOutputNewline(" .");
				txt.decrementIndent();
				txt.addOutputNewline(AadlConstants.emptyString);
				
				// register variable ids
				self.processEList(ba.getVariable());
				
				txt.addOutputNewline("eq states(" + qid + ") =");
				txt.incrementIndent();
				processEList(ba.getState(), "", "noLocDecl");
				txt.decrementIndent();
				txt.addOutputNewline(" .");
				txt.addOutputNewline(AadlConstants.emptyString);

				txt.addOutputNewline("eq transitions(" + qid + ") =");
				txt.incrementIndent();
				processEList(ba.getTransition(), ";" + NEWLINE, "emptyTransitionSet");
				txt.addOutputNewline(" .");
				txt.decrementIndent();
				txt.addOutputNewline(AadlConstants.emptyString);
				return DONE;
			}

			public Object caseBehaviorVariable(BehaviorVariable bv) {
				opTable.register(translateType(bv.getType().getQualifiedName()) + "VarId",
						RtmAadlUtil.escape(bv.getName()));
				return DONE;
			}
			
			public Object caseBehaviorState(BehaviorState st) {
				String stateId = RtmAadlUtil.escape(st.getName());
				opTable.register("Location", stateId);
				if (st.isInitial()) 
					txt.addOutputNewline("initial: " + stateId + " ");
				if (st.isComplete()) 
					txt.addOutputNewline("complete: " + stateId + " ");
				if (st.isReturn()) 
					txt.addOutputNewline("return: " + stateId + " ");
				if (st.isUrgent()) 
					txt.addOutputNewline("urgent: " + stateId + " ");
				if (st.isEntry()) 
					txt.addOutputNewline("entry: " + stateId + " ");
				if (st.isExit()) 
					txt.addOutputNewline("exit: " + stateId + " ");
				if (st instanceof CompositeState) 
					txt.addOutputNewline("composite: " + stateId + " ");
				if (st instanceof Concurrent) 
					txt.addOutputNewline("concurrent: " + stateId + " ");
				if (!(st.isInitial() || st.isComplete() || st.isReturn() || st.isUrgent() ||
						st.isEntry() || st.isExit() || 
						(st instanceof CompositeState) || (st instanceof Concurrent))) { 
					txt.addOutputNewline("other: " + stateId);
				}
				return DONE;
			}
			
			public Object caseBehaviorTransition(BehaviorTransition tr) {
				txt.addOutput("(");
				//TODO: label, iter and priority are ignored. 
				for (Iterator<State> it = tr.getSrc().listIterator(); it.hasNext(); ) {
					State st = it.next();
					txt.addOutput(RtmAadlUtil.escape(st.getName()));
					if (it.hasNext()) txt.addOutput(" , ");
				}
				txt.addOutput(" -[");
				self.process(tr.getGuard());
				txt.addOutput("]-> ");
				txt.addOutput(RtmAadlUtil.escape(tr.getDst().getName()));
				
				txt.addOutputNewline(" {");
				txt.incrementIndent();
				inAction = true;
				processEList(tr.getAction(), ";" + NEWLINE, "nil");
				inAction = false;
				txt.decrementIndent();
				txt.addOutput("}");
				
				if (tr.getDuration() != null) 
					self.process(tr.getDuration());
				txt.addOutput(")");
				return DONE;
			}
			
			public Object caseBehaviorGuard(BehaviorGuard g) {
				if (g.getOn() != null)
					self.process(g.getOn());
				if (g.getEvent() != null) 
					processEList(g.getEvent(), " & ", "");
				if (g.getWhen() != null) {
					txt.addOutput(" when ");
					self.process(g.getWhen());
				}
				return DONE;
			}
			
			public Object caseReferenceExpression(ReferenceExpression r) {
				self.process(r.getRef());
				return  DONE;
			}
			
			public Object caseReferencedElement(ReferencedElement r) {
				if (r.getOwner() != null) {
					self.process(r.getOwner());
					txt.addOutput(" . ");
				}
				return DONE;
			}

			public Object caseFeatureRef(FeatureRef r) {
				caseReferencedElement(r);
				txt.addOutput(RtmAadlUtil.escape(r.getFeature().getName()));
				return DONE;
			}

			public Object caseSubcompRef(SubcompRef r) {
				caseReferencedElement(r);
				txt.addOutput(RtmAadlUtil.escape(r.getSubc().getName()));
				return DONE;
			}
			
			public Object caseSubpClassifierRef(SubpClassifierRef r) {
				caseReferencedElement(r);
				txt.addOutput(RtmAadlUtil.escape(r.getSubp().getName()));
				return DONE;
			}
			
			public Object caseBinaryOperation(BinaryOperation o) {
				String s = "<binop>";
				switch (o) {
				case AND: s = " and "; break;
				case OR: s = " or "; break;
				case LESS: s = " < "; break;
				case MORE: s = " > "; break;
				case LESS_EQUAL: s = " <= "; break;
				case MORE_EQUAL: s = " >= "; break;
				case EQUAL: s = " = "; break;
				case NOT_EQUAL: s = " != "; break;
				case PLUS: s = " + "; break;
				case MINUS: s = " - "; break;
				case STAR: s = " * "; break;
				case DIV: s = " / "; break;
				}
				txt.addOutput(s);
				return DONE;
			}

			public Object caseBinaryExpression(BinaryExpression b) {
				boolean withParen = false;
				int lp = 6;
				txt.addOutput("(");
				if (b.getArg1() instanceof BinaryExpression)
						lp = priority(((BinaryExpression) b.getArg1()).getOperation());
				if (lp < priority(b.getOperation())) 
					withParen = true;
				if (withParen) 
					txt.addOutput("(");
				self.process(b.getArg1());
				if (withParen) 
					txt.addOutput(")");
				withParen = false;
				int rp = 6;
				if (b.getArg2() instanceof BinaryExpression)
						rp = priority(((BinaryExpression) b.getArg2()).getOperation());
				if (rp < priority(b.getOperation()) ||
						rp == priority(b.getOperation()) && 
						(b.getOperation()==BinaryOperation.MINUS || b.getOperation()==BinaryOperation.DIV))
					withParen = true;
				caseBinaryOperation(b.getOperation());
				if (withParen) 
					txt.addOutput("(");
				self.process(b.getArg2());
				if (withParen) 
					txt.addOutput(")");
				txt.addOutput(")");
				return DONE;
			}
			
			public Object caseUnaryOperation(UnaryOperation o) {
				String s = "<unary>";
				switch(o) {
				case NOT: s = "not "; break;
				case NEGATIVE: s = "-"; break;
				case POSITIVE: s = "+"; break;
				}
				txt.addOutput(s);
				return DONE;
			}
			
			public Object caseUnaryExpression(UnaryExpression u) {
				caseUnaryOperation(u.getOperation());
				boolean withParen = false;
				if (u.getAadlSpec() instanceof BinaryExpression) 
					withParen = true;
				if (withParen) 
					txt.addOutput("(");
				self.process(u.getArg());
				if (withParen) 
					txt.addOutput(")");
				return DONE;
			}

			public Object caseQuantifier(Quantifier l) {
				caseIteration(l);
				txt.addOutputNewline(": (");
				self.process(l.getBody());
				txt.addOutput(")");
				return DONE;
			}
			
			public Object caseVariableExpression(VariableExpression v) {
				txt.addOutput(RtmAadlUtil.escape(v.getDecl().getName()));
				return DONE;
			}
			
			public Object caseIntConstant(IntConstant c) {
				txt.addOutput(""+c.getValue());
				return DONE;
			}
			
			public Object caseBooleanConstant(BooleanConstant c) {
				txt.addOutput(c.isValue()?"true":"false");
				return DONE;
			}

			public Object caseTimeout(Timeout t) {
				txt.addOutput("timeout ");
				self.process(t.getTimeout());
				return DONE;
			}
			
			public Object caseLoopVariable(LoopVariable v) {
				txt.addOutput(v.getDecl().getVar());
				return DONE;
			}
			
			public Object caseUnitConstant(UnitConstant u) {
				txt.addOutput(RtmAadlUtil.translateUnit(u.getValue() + "", u.getUnit()));
				return DONE;
			}
			
			public Object caseRefConstant(RefConstant c) {
				try {
					//txt.addOutput("value("+c.getValue().getParsedPropertyReference().getQualifiedName()+")");
					txt.addOutput("value("+c.getValue().getReferencedProperty().eContainer().toString()+"::"+c.getValue().getValueAsString()+")");
				}catch(Exception e) {System.out.println("unparse ERROR:"+c.getValue().getValueAsString());}
				return DONE;
			}
			
			public Object caseCount(Count c) {  
				txt.addOutput(RtmAadlUtil.escape(c.getValue().getName())+"'count");
				return DONE;
			}
			
			public Object caseFresh(Fresh c) { // fresh(port)
				txt.addOutput("fresh("+RtmAadlUtil.escape(c.getValue().getName())+")");
				return DONE;
			}

			public Object caseBehaviorArray(BehaviorArray a) {
				self.process(a.getTab());
				txt.addOutput("[");
				self.process(a.getIndex());
				txt.addOutput("]");
				return DONE;
			}
			
			public Object caseDestTerm(DestTerm t) {
				txt.addOutput(t.getFunc().getQualifiedClassifierName()+"'"+t.getDestr()+"(");
				self.process((AObject) t.getParameter().get(0));
				txt.addOutput(")");
				return DONE;
			}
			
			public Object caseIsTerm(IsTerm t) {
				try {
					txt.addOutput(t.getFunc().getQualifiedClassifierName()+" ? (");
					self.process((AObject) t.getParameter().get(0));
					txt.addOutput(")");
				} catch (Exception err) { System.out.println("unparse ERROR in isTerm:"+t.getFctref());}
				return DONE;
			}

			public Object caseConsTerm(ConsTerm t) {
				txt.addOutput(t.getFunc().getQualifiedClassifierName());
				if (!t.getParameter().isEmpty()) {
					txt.addOutput("(");
					processEList(t.getParameter(), ",", "");
					txt.addOutput(")");
				}
				return DONE;
			}
			
			public Object caseAssignment(Assignment a) {
				txt.addOutput("(");
				self.process(a.getLeftExpression());
				txt.addOutput( inAction ? " := ":" |-> " );
				self.process(a.getRightExpression());
				txt.addOutput(")");
				return DONE;
			}
			
			public Object caseCommunication(Communication c) {
				txt.addOutput("(");
				self.process(c.getPort());
				if (c.isSending()) 
					txt.addOutput(" ! "); 
				else 
					txt.addOutput(" ? ");
				if (!c.getParameter().isEmpty()) {
					txt.addOutput("(");
					processEList(c.getParameter(), ",", "");
					txt.addOutput(")");
				}
				txt.addOutput(")");
				return DONE;
			}
			
			public Object caseInputOutput(InputOutput o) {
				processEList(o.getEvents(), "&", "");
				return DONE;
			}
			
			public Object caseBehaviorParameter(BehaviorParameter p) {
				self.process(p.getValue());
				return DONE;
			}
			
			public Object caseIfThenElse(IfThenElse ite, boolean elsif) {
				txt.addOutput(elsif? "(elsif " : "(if ");
				
				// condition
				txt.addOutput("(");
				self.process(ite.getCnd());
				txt.addOutputNewline(")");
				
				// true case
				txt.incrementIndent();
				txt.addOutputNewline("(");
				processEList(ite.getIft(), ";" + NEWLINE, "");
				txt.addOutput(")");
				txt.addOutput(elsif? ")" : "");
				txt.addOutputNewline(AadlConstants.emptyString);
				txt.decrementIndent();
				
				// false case
				if (!ite.getIff().isEmpty()) {
					if (ite.getIff().size() == 1 && 
							ite.getIff().get(0) instanceof IfThenElse) {	// elsif
						txt.addOutput(elsif? "" : "(");	// add "(" when starting elsif
						caseIfThenElse((IfThenElse) ite.getIff().get(0), true);
					} 
					else {	// else
						txt.addOutput(elsif? ")" : "");	// add ")" when finishing elsif (with else)
						
						txt.addOutputNewline("else ");
						txt.incrementIndent();
						txt.addOutputNewline("(");
						processEList(ite.getIff(), ";" + NEWLINE, "");
						txt.addOutput(")");
						txt.decrementIndent();
					}
				}
				else {
					txt.addOutput(elsif? ")" : "");	// add ")" when finishing elsif (without else)
				}
				txt.addOutput(elsif? "" : "end if)");
				return DONE;
			}
			
			public Object caseIfThenElse(IfThenElse ite) {
				return caseIfThenElse(ite, false); 
			}
			
			public Object caseDelay(Delay d) {
				txt.addOutput("delay");
				self.process(d.getDuration());
				return DONE;
			}

			public Object caseComputation(Computation c) {
				txt.addOutput("computation");
				self.process(c.getDuration());
				return DONE;
			}
			
			public Object caseTimeInterval(TimeInterval tm) {
				txt.addOutput("(");
				self.process(tm.getMin());
				txt.addOutput(",");
				self.process(tm.getMax());
				txt.addOutput(")");
				return DONE;
			}
		};
	}
	
	private String translateType(String bt)
	{
		if (bt.equals("behavior::boolean"))
			return "Bool";
		else if (bt.equals("behavior::integer"))
			return "Int";
		else
			throw new UnsupportedOperationException("Type " + bt + " is not supported yet.");
	}
	
	private int priority(BinaryOperation o) {
		switch (o) {
		case OR: return 1;
		case AND: return 2;
		case LESS: return 3;
		case MORE: return 3;
		case LESS_EQUAL: return 3;
		case MORE_EQUAL: return 3;
		case EQUAL: return 3;
		case NOT_EQUAL: return 3;
		case PLUS: return 4;
		case MINUS: return 4;
		case STAR: return 5;
		case DIV: return 5;
		}
		return 0;
	}
	
	@SuppressWarnings("unchecked")
	private void processEList(EList list, String separator, String empty) {
		RtmAadlUtil.processEList(self, txt, list, separator, empty);
	}
	

}

