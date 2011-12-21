package edu.uiuc.aadl.maude;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.osgi.framework.Bundle;

import edu.cmu.sei.aadl.model.core.AObject;
import edu.cmu.sei.aadl.model.util.AadlUtil;
import edu.cmu.sei.aadl.unparser.provider.AadlUnparsePlugin;
import edu.cmu.sei.osate.ui.actions.AaxlReadOnlyActionAsJob;
import edu.uiuc.aadl.maude.codegen.RtmAadlModelUnparser;

/** 
 * 
 * @author Kyungmin Bae
 *
 */
public final class GenerateRTMAction extends AaxlReadOnlyActionAsJob {
	
	@Override
	protected Bundle getBundle() {
		return Activator.getDefault().getBundle();
	}
	
	@Override
	protected String getMarkerType() {
		return "edu.uiuc.aadl.maude.RTMaudeTranslationMarker";
	}
	
	@Override
	protected String getActionName() {
		return "RTMaude Model Generator";
	}

	@Override
	protected void doAaxlAction(IProgressMonitor monitor, AObject obj) {
		monitor.beginTask("Generating a RTMaude Instance Model", IProgressMonitor.UNKNOWN);
		RtmAadlModelUnparser compiler = new RtmAadlModelUnparser();
		writeStrToFile(compiler.doUnparse(obj), obj.eResource().getURI());
		monitor.done();
	}
	
	public static void writeStrToFile(String code, URI fileUri)
	{
		URI maudeFileUri = fileUri.trimFileExtension().appendFileExtension("maude");
		Path path = new Path(maudeFileUri.devicePath().substring(9));
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
		
		if (file != null) {
			final InputStream input = new ByteArrayInputStream(code.getBytes());
			try {
				if (file.exists()) {
					file.setContents(input, true, true, null);
				} else {
					AadlUtil.makeSureFoldersExist(path);
					file.create(input, true, null);
				}
				file.setDerived(true);
				file.deleteMarkers(null, true, IResource.DEPTH_INFINITE);
			} catch (final CoreException e) {
				AadlUnparsePlugin.INSTANCE.log(e);
			}
		}
	}

}
