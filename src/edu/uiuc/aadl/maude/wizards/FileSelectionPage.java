package edu.uiuc.aadl.maude.wizards;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

import edu.cmu.sei.aadl.model.core.AObject;
import edu.cmu.sei.aadl.model.core.util.AadlModelPlugin;
import edu.cmu.sei.aadl.model.instance.InstanceObject;
import edu.cmu.sei.aadl.model.util.AadlUtil;
import edu.uiuc.aadl.maude.RtmAadlUtil;

/**
 * The "New" wizard page allows setting the container for the new file as well
 * as the file name. The page will only accept file name without the extension
 * OR with the extension that matches the expected one (prop).
 */

public class FileSelectionPage extends WizardPage {
	private ISelection selection;
	boolean modelChosen = false;
	
	private TreeViewer modelChooser = null;
	private Button destDefaultCheck;
	private Text destPathText = null;
	private Text destContainerText = null;
	private Button browseLocation = null;
	
	static private String defalutLoc = "verification";
	static private String propExt = "prop";

	/**
	 * Constructor for SampleNewWizardPage.
	 * 
	 * @param pageName
	 */
	public FileSelectionPage(ISelection selection) {
		super("wizardPage");
		setTitle("AADL2Maude Property Verification");
		setDescription("This wizard creates a new property file (*.prop)" +
				" that can be used to verify an AADL instance model using Real-Time Maude");
		this.selection = selection;
	}
	
	public InstanceObject getSelectedInstanceModel() {
		AObject model = AadlUtil.getAObject(getSelectedResource());
		return RtmAadlUtil.isInstanceModel(model) ? (InstanceObject)model : null;
	}
	
	public IPath getPropFullPath() {
		IResource modelRes = getSelectedResource();
		if (modelRes != null) {
			String fileName = 
				modelRes.getFullPath().removeFileExtension().addFileExtension(propExt).lastSegment();
			return (new Path(destPathText.getText().trim())).
					append(destContainerText.getText().trim()).append(fileName);
		}
		return null;
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		//
		//	widgets
		//
		Composite container = new Composite(parent, SWT.NONE);
		Label browseLabel = new Label(container, SWT.NONE);
		
		// for choosing an aaxl instance model
		browseLabel.setText("&Choose an instance model to verify:");
		modelChooser = new TreeViewer(container, SWT.BORDER);
		modelChooser.setContentProvider(new WorkbenchContentProvider());
		modelChooser.setLabelProvider(new WorkbenchLabelProvider());
		modelChooser.setComparator(new ResourceComparator(ResourceComparator.TYPE));
		modelChooser.setInput(ResourcesPlugin.getWorkspace().getRoot());
		modelChooser.addFilter(	
				new ViewerFilter() {	//filters for instance models
					public boolean select(Viewer viewer, Object parentElement, Object element) {
						if (element instanceof IResource) {
							return ((IResource)element).getType() == IResource.FILE ?
									 RtmAadlUtil.isInstanceModel(element) 
									:containerHasInstanceModel((IContainer)element);
						}
						return false;
					}
				});
		
		// for a destination
		destDefaultCheck = new Button(container, SWT.CHECK);
		destDefaultCheck.setText("&Use default location");
		Label desDirLabel = new Label(container, SWT.NULL);
		desDirLabel.setText("&Location:");
		destPathText = new Text(container, SWT.BORDER | SWT.SINGLE);
		browseLocation = new Button(container, SWT.PUSH);
		browseLocation.setText("&Browse...");
		Label desFileLabel = new Label(container, SWT.NULL);
		desFileLabel.setText("&Container:");
		destContainerText = new Text(container, SWT.BORDER | SWT.SINGLE);
		
		//
		// Listeners
		//
		modelChooser.addSelectionChangedListener(
				new ISelectionChangedListener() {
					public void selectionChanged(SelectionChangedEvent event) {
						updateDefaultSelection();
						dialogChanged();
					}
				});
		destDefaultCheck.addSelectionListener(
				new SelectionAdapter() {
					public void widgetSelected(SelectionEvent event) {
						updateDefaultSelection();
						dialogChanged();
					} 
				});
		destPathText.addModifyListener(
				new ModifyListener() { public void modifyText(ModifyEvent e) { dialogChanged();} });
		destContainerText.addModifyListener(
				new ModifyListener() { public void modifyText(ModifyEvent e) { dialogChanged();} });
		browseLocation.addSelectionListener(
				new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						ContainerSelectionDialog dialog = new ContainerSelectionDialog(
								getShell(), ResourcesPlugin.getWorkspace().getRoot(), true,
								"Select a location");
						if (dialog.open() == ContainerSelectionDialog.OK) {
							Object[] result = dialog.getResult();
							if (result.length == 1) {
								destPathText.setText(((Path) result[0]).toString());
							}
						}
					}
				});
		
		//
		// Layout
		//
		GridData grid;
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		
		grid = new GridData(GridData.FILL_HORIZONTAL);
		grid.horizontalSpan = 3;
		browseLabel.setLayoutData(grid);
		
		grid = new GridData(GridData.FILL_BOTH);
		grid.heightHint = 50;
		grid.horizontalSpan = 3;
		modelChooser.getControl().setLayoutData(grid);
		
		grid = new GridData(GridData.BEGINNING);
		grid.horizontalSpan = 3;
		destDefaultCheck.setLayoutData(grid);
		
		grid = new GridData(GridData.FILL_HORIZONTAL);
		destPathText.setLayoutData(grid);
		destContainerText.setLayoutData(grid);
		
		grid = new GridData(GridData.FILL_HORIZONTAL);
		destContainerText.setLayoutData(grid);
		
		initialize();
		dialogChanged();
		setControl(container);
	}
	
	private IResource getSelectedResource() {
		ISelection selection = modelChooser.getSelection();
		if (selection != null && selection.isEmpty() == false &&
				selection instanceof IStructuredSelection) {
			Object selected = ((IStructuredSelection)selection).getFirstElement();
			if (selected instanceof IResource)
				return  (IResource)selected;
		}
		return null;
	}

	/**
	 * Tests if the current workbench selection is a suitable container to use.
	 */
	private void initialize() {
		if (selection != null && selection.isEmpty() == false
				&& selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.size() > 1)
				return;
			Object obj = ssel.getFirstElement();
			if (obj instanceof IResource) {
				if (((IResource)obj).getType() == IResource.FILE) {
					if (RtmAadlUtil.isInstanceModel(obj))
						modelChooser.setSelection(new StructuredSelection((IResource)obj), true);
				}
			}
		}
		destDefaultCheck.setSelection(true);
		updateDefaultSelection();
	}
	
	private void updateDefaultSelection() {
		IResource modelRes = getSelectedResource();
		modelChosen = modelRes != null &&  RtmAadlUtil.isInstanceModel(modelRes);
		
		// use default folder
		if (destDefaultCheck.getSelection() == true) {
			destPathText.setEnabled(false);
			destContainerText.setEnabled(false);
			browseLocation.setEnabled(false);
			
			if (modelChosen) {
				destPathText.setText(modelRes.getProject().getFullPath().toString());
				destContainerText.setText(defalutLoc);
			}
			else {
				destPathText.setText("");
				destContainerText.setText("");
			}
		}
		else {
			destPathText.setEnabled(true);
			destContainerText.setEnabled(true);
			browseLocation.setEnabled(true);
		}
	}


	/**
	 * Ensures that both text fields are set.
	 */

	private void dialogChanged() {
		if (modelChosen == false) {
			updateStatus("The AADL Instance model (*.aaxl) must be selected");
			return;
		}
		
		IResource path = 
			ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(destPathText.getText()));
		if (path == null || (path.getType() & (IResource.PROJECT | IResource.FOLDER)) == 0) {
			updateStatus("Destination location must exist");
			return;
		}
		if (!path.isAccessible()) {
			updateStatus("Project must be writable");
			return;
		}

		String fileName = destContainerText.getText();
		if (fileName.length() == 0) {
			updateStatus("Container name must be specified");
			return;
		}
		if (fileName.replace('\\', '/').indexOf('/', 1) > 0) {
			updateStatus("Container name must be valid");
			return;
		}
		
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}
	
	private boolean containerHasInstanceModel(IContainer element) {
		IResource[] members = null;
		try {
			members = element.members();
		}
		catch (CoreException e) {
			AadlModelPlugin.logThrowable(e);
			return false;
		}
		for (IResource mem : members) {
			if (mem.getType() == IResource.FILE) {
				if (RtmAadlUtil.isInstanceModel(mem))
					return true;
			}
			else if (containerHasInstanceModel((IContainer)mem))
				return true;
		}
		return false;
	}
}