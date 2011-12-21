package edu.uiuc.rtsi.synchaadlchecker;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

//import edu.cmu.sei.aadl.model.core.*;
import edu.cmu.sei.aadl.model.core.AObject;
import edu.cmu.sei.aadl.model.instance.*;
import edu.cmu.sei.aadl.model.instance.impl.SystemInstanceImpl;
//import edu.cmu.sei.aadl.model.instance.impl.*;
//import edu.cmu.sei.aadl.model.instance.impl.SystemInstanceImpl;
import edu.cmu.sei.aadl.model.util.SOMIterator;
import edu.cmu.sei.osate.ui.actions.AaxlReadOnlyActionAsJob;

public class CheckerAction extends AaxlReadOnlyActionAsJob {

	@Override
	protected String getActionName() {
		return PropertyLoader.getString("CheckerAction.actionName"); //$NON-NLS-1$
	}

	@Override
	protected String getMarkerType() {
		return PropertyLoader.getString("CheckerAction.safetyObjectMarker"); //$NON-NLS-1$
	}

	@Override
	protected void doAaxlAction(IProgressMonitor monitor, AObject root) {
	
		if (root instanceof SystemInstance) {
			final SystemInstanceImpl system = (SystemInstanceImpl) root;
		
			monitor.beginTask("Analyzing " + system.getName(), IProgressMonitor.UNKNOWN); //$NON-NLS-1$
			
			System.out.println("Synch AADL Constraint Checker begins\n\n");
			String outputString = "";
			
			ConstraintChecker checker = new ConstraintChecker();
			if(checker.isSynchronous(system)) {
				final SOMIterator soms = new SOMIterator(system);
				while (soms.hasNext()) {
					final SystemOperationMode som = soms.nextSOM();
					system.setCurrentSystemOperationMode(som);
					
					SystemSwitch aSystemSwitch = new SystemSwitch(system, checker);
					aSystemSwitch.defaultTraversal(system);	
				}	
			} 
			outputString += checker.getErrorString();			
			
			DisplayMsg dm = new DisplayMsg();
			dm.setTitle(PropertyLoader.getString("CheckerAction.actionName"));
			dm.setMsg(outputString);
			Display.getDefault().asyncExec(dm);

			System.out.println("Results: \n" + outputString);
			monitor.done();
		}
	}
	
	// thread class for displaying messages in the job
	public class DisplayMsg implements Runnable {
		String title;
		String msg;

		public void setMsg(String s) {
			msg = s;
		}

		public void setTitle(String s) {
			title = s;
		}

		public void run() {
			MessageDialog.openInformation(getShell(), title, msg);
		}
	}
}

	