
package edu.uiuc.aadl.maude.editors;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

import edu.cmu.sei.aadl.model.instance.SystemOperationMode;
import edu.cmu.sei.aadl.model.instance.impl.SystemInstanceImpl;
import edu.cmu.sei.aadl.model.util.SOMIterator;
import edu.uiuc.aadl.maude.RtmAadlUtil;
import edu.uiuc.aadl.maude.IO.CodegenFileManager;
import edu.uiuc.aadl.maude.IO.PropertyFileManager;
import edu.uiuc.aadl.maude.codegen.RtmAadlModelUnparser;
import edu.uiuc.aadl.maude.verification.RtmPropCodeGenerator;
import edu.uiuc.aadl.maude.verification.RtmVerificationComm;
import edu.uiuc.aadl.maude.verification.RtmVerificationSpec;
import edu.uiuc.rtsi.synchaadlchecker.ConstraintChecker;
import edu.uiuc.rtsi.synchaadlchecker.PropertyLoader;
import edu.uiuc.rtsi.synchaadlchecker.SystemSwitch;
import es.upv.dsic.issi.moment.maudesimpleGUI.MaudesimpleGUIPlugin;
import es.upv.dsic.issi.moment.maudesimpleGUI.core.Maude;


/**
 * @author Kyungmin Bae
 *
 */
public class ModelVerificationPage extends FormPage {
	
	RtmVerificationSpec spec;
	boolean constraintSatisfied = false;
	
	Text modelText = null;
	Text simulText = null;
	Table propTable = null;
	Section propSec = null;
	
	/**
	 * @param id
	 * @param title
	 */
	public ModelVerificationPage(FormEditor editor) {
		super(editor, "VerificationPage", RtmAadlUtil.getSetting("VerificationPage.label"));
	}
	
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setText(RtmAadlUtil.getSetting("VerificationPage.title"));
		toolkit.decorateFormHeading(form.getForm());
		
		Section modelSec = createModelSection(toolkit, form.getForm());
		this.propSec = createPropertySection(toolkit, form.getForm());
		
		//
		//	Layout
		//
		GridLayout top = new GridLayout();
		form.getBody().setLayout(top);
		
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		modelSec.setLayoutData(gd);
		
		gd = new GridData(GridData.FILL_HORIZONTAL);
		this.propSec.setLayoutData(gd);	
	}
	
	
	private Section createModelSection(FormToolkit toolkit, Form form) {
		Section modelSec = toolkit.createSection(form.getBody(), Section.TITLE_BAR|Section.EXPANDED);
		modelSec.setText(RtmAadlUtil.getSetting("VerificationPage.modelSection.title"));
		Composite modelSecCl = toolkit.createComposite(modelSec);
		
		Label modelLabel = toolkit.createLabel(modelSecCl, "Model Location:");
		modelText = toolkit.createText(modelSecCl, "", SWT.BORDER);
		Button checkButton = toolkit.createButton(modelSecCl, "Constraints Check", SWT.PUSH);
		Button codegenButton = toolkit.createButton(modelSecCl, "Code Generation", SWT.PUSH);
		toolkit.createLabel(modelSecCl, "Simulation Bound:");
		simulText = toolkit.createText(modelSecCl, "");
		Button simulButton = toolkit.createButton(modelSecCl, "Perform Simulation", SWT.PUSH);
		modelSec.setClient(modelSecCl);
		
		//
		// Listeners
		//
		codegenButton.addSelectionListener(
				new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						try {
							RtmAadlModelUnparser compiler = new RtmAadlModelUnparser();
							final String code = compiler.doUnparse(spec.getModel());
							final InputStream input = new ByteArrayInputStream(code.getBytes());
							IFile f = ((IFileEditorInput)getEditorInput()).getFile(); 
							
							CodegenFileManager.createCodegenFile("maude",input, f);
							CodegenFileManager.copyMaudeFiles(f.getFullPath());
							
							MessageDialog.openInformation(getSite().getShell(), 
									"Codegen Result", 
									"Code generation success!");
						}
						catch (Exception ex) {
							final Writer res = new StringWriter();
							final PrintWriter pres = new PrintWriter(res);
							ex.printStackTrace(pres);
							MessageDialog.openError(getSite().getShell(), 
									"Codegen Result", 
									"Error occured: " + ex.getMessage() +
									"\n\n" + res.toString());
						}
					}
				});
		checkButton.addSelectionListener(
				new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						SystemInstanceImpl system = (SystemInstanceImpl)ModelVerificationPage.this.spec.getModel();
						ConstraintChecker checker = new ConstraintChecker();
						if (checker.isSynchronous(system)) {
							SOMIterator soms = new SOMIterator(system);
							while (soms.hasNext()) {
								SystemOperationMode som = soms.nextSOM();
								system.setCurrentSystemOperationMode(som);
								
								SystemSwitch aSystemSwitch = new SystemSwitch(system, checker);
								aSystemSwitch.defaultTraversal(system);
								}
							}
						String outputString = checker.getErrorString();
						
						if (outputString.equals("Valid Synchronous AADL model!\n"))
						{
							MessageDialog.openInformation(ModelVerificationPage.this.getSite().getShell(), 
									PropertyLoader.getString("CheckerAction.actionName"), 
									outputString);
							}
						else
						{
							MessageDialog.openError(ModelVerificationPage.this.getSite().getShell(), 
									PropertyLoader.getString("CheckerAction.actionName"), 
									outputString);
							}
						}
					});
		simulButton.addSelectionListener(
				new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						try {
							int val = Integer.parseInt(simulText.getText());
							
							Maude maude = MaudesimpleGUIPlugin.getDefault().getMaude();
							
							if (!maude.isRunning()) {
								maude.runMaude();
								maude.sendToMaude("cd " + getCurrentPath() + "\n");
								maude.sendToMaude("load " + spec.getModel().getName() + "\n");
							}
							maude.sendToMaude(RtmPropCodeGenerator.generateRtmSimul(val));
						}
						catch (NumberFormatException ne) {
							MessageDialog.openError(getSite().getShell(), 
									"Simulation Result", 
									"Not Number!");
						}
					}
				});
		
		//
		//	Layout
		//
		GridLayout layout = new GridLayout();
		modelSecCl.setLayout(layout);
		layout.numColumns = 3;
		
		GridData gd = new GridData();
		gd.verticalAlignment = SWT.TOP;
		modelLabel.setLayoutData(gd);
//		
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		modelText.setLayoutData(gd);
		
		gd = new GridData();
		gd.horizontalSpan = 2;
		codegenButton.setLayoutData(gd);
		
		gd = new GridData(GridData.FILL_HORIZONTAL);
		simulText.setLayoutData(gd);
		
		return modelSec;
	}
	
	
	private Section createPropertySection(FormToolkit toolkit, Form form) {
		Section propSec = toolkit.createSection(form.getBody(), Section.TITLE_BAR|Section.EXPANDED);
		propSec.setText(RtmAadlUtil.getSetting("VerificationPage.propSection.title"));
		Composite propSecCl = toolkit.createComposite(propSec);
		
		//TODO: add checkbox for the table: SWT.CHECK
		propTable = toolkit.createTable(propSecCl, SWT.BORDER | SWT.MULTI);
		String[] titles = {"Name", "Property", "Category", "Result"};
		
		TableColumn[] column = new TableColumn[titles.length];
		for (int i=0; i< titles.length; i++) {
			column[i] = new TableColumn(propTable, SWT.NONE);
			column[i].setText(titles[i]);
		}
		propTable.setLinesVisible (true);
		propTable.setHeaderVisible (true);
		
		column[0].setWidth(70); column[0].setResizable(false);
		column[1].setResizable(false);
		column[2].setWidth(60); column[2].setResizable(false);
		column[2].setAlignment(SWT.CENTER);
		
		
		Button verifyButton = toolkit.createButton(propSecCl, "Perform Verification", SWT.PUSH);
		propSec.setClient(propSecCl);
		
		//
		//  Listener
		//
		verifyButton.addSelectionListener(
				new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						try {
							StringBuilder result = new StringBuilder();
							result.append(RtmPropCodeGenerator.generateRtmModuleString(spec))
							      .append("\n\n");
							for (RtmVerificationComm rc : spec.getCommandList())
								result.append(RtmPropCodeGenerator.generateRtmCommand(rc) + "\n");
							
							final InputStream input = new ByteArrayInputStream(result.toString().getBytes());
							IFile f = ((IFileEditorInput)getEditorInput()).getFile();
							
							String cgFileName = 
								CodegenFileManager.createCodegenFile("verification.maude",input, f);
							
							Maude maude = MaudesimpleGUIPlugin.getDefault().getMaude(); 
							if (!maude.isRunning()) {
								maude.runMaude();
							}
							maude.sendToMaude("cd " + getCurrentPath() + "\n");
							maude.sendToMaude("load " + cgFileName + "\n");
							
						}
						catch (Exception ex) {
							ex.printStackTrace();
							MessageDialog.openError(getSite().getShell(), 
									"Run Result", 
									"Error occured: " + ex.getMessage());
						}
						
					}
				});
		propTable.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				int newWidth = propTable.getSize().x
				             - propTable.getColumn(0).getWidth()
				             - propTable.getColumn(2).getWidth()
				             - propTable.getVerticalBar().getSize().x
				             - propTable.getBorderWidth() * 2 - 10;
				if (newWidth > 60) 
					propTable.getColumn(1).setWidth(newWidth);
			}
			
		});
		
		//
		//	Layout
		//
		GridLayout layout = new GridLayout();
		propSecCl.setLayout(layout);
		
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
	    gd.minimumHeight = 60;
	    gd.grabExcessVerticalSpace = true;
		propTable.setLayoutData(gd);
		
		gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
		verifyButton.setLayoutData(gd);
		
		return propSec;
	}
	
	private String getCurrentPath() {
		return ((IFileEditorInput)getEditorInput()).
			getFile().getLocation().removeLastSegments(1).toOSString();
	}
	
	protected void updateModel(String content) {
		InputStream input = new ByteArrayInputStream(content.getBytes());
		try {
			this.spec = PropertyFileManager.readProp(input);
			if (this.spec != null)
				this.modelText.setText(this.spec.getModel().eResource().getURI().devicePath().substring(9));
		}
		catch (IOException e) {
			MessageDialog.openError(getSite().getShell(), "IO Error!", "Error: " + e.getMessage());
			e.printStackTrace();
		} 
		catch (DOMException e) {
			MessageDialog.openError(getSite().getShell(), "XML Error!", "Error: " + e.getMessage());
		} 
		catch (SAXException e) {
			MessageDialog.openError(getSite().getShell(), "XML Parsing Error!", "Error: " + e.getMessage());
			e.printStackTrace();
		} 
		catch (ParserConfigurationException e) {
			MessageDialog.openError(getSite().getShell(), "XML Parser Error!", "Error: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	protected void updateContent(String content) {
		updateModel(content);
		if (this.spec != null)
		{
			this.simulText.setText(this.spec.simulBound);
			
			int numItems = this.propTable.getItemCount();
			int index = 0;
			
			for (RtmVerificationComm pc : this.spec.getCommandList())
			{
				TableItem item;
				if (index < numItems) {
					item = this.propTable.getItem(index++);
				}
				else {
					item = new TableItem(this.propTable, 0);
				}
				item.setText(0, pc.getName());
				item.setText(1, pc.getPropertyString());
				item.setText(2, "LTL");
	        }
			if (index < numItems) {
				this.propTable.remove(index, numItems - 1);
			}
			this.propSec.pack();
		}
	}
		
}