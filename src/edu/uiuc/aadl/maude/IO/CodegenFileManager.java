package edu.uiuc.aadl.maude.IO;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import edu.cmu.sei.aadl.model.util.AadlUtil;
import edu.uiuc.aadl.maude.Activator;
import edu.uiuc.aadl.maude.RtmAadlUtil;

public class CodegenFileManager {
	
	
	public static String createCodegenFile(String extension, InputStream code, IFile targetUri) {
		IPath path = targetUri.getFullPath().removeFileExtension().addFileExtension(extension);
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
		
//		URI maudeFileUri = targetUri.trimFileExtension().appendFileExtension(extension);
//		Path path = new Path(maudeFileUri.devicePath().substring(9));
//		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
		
		if (file != null)
			setFileContent(code, file);
		return file.getName();
	}
	
	@SuppressWarnings("unchecked")
	public static void copyMaudeFiles(IPath target) throws IOException {
		IPath tpath = target.toFile().isDirectory()? target : target.removeLastSegments(1);
		Enumeration urls = Activator.getDefault().getBundle().findEntries(
				RtmAadlUtil.getSetting("Semantics.path"), "*", true);
		while (urls.hasMoreElements()) {
			URL su = (URL)urls.nextElement();
			IFile nfile = ResourcesPlugin.getWorkspace().getRoot().getFile(tpath.append(su.getFile()));
			if (nfile != null)
				setFileContent(su.openStream(), nfile);
		}
	}
	
	private static void setFileContent(InputStream content, IFile file) {
		try {
			if (file.exists())
				file.setContents(content, true, true, null);
			else {
				AadlUtil.makeSureFoldersExist(file.getFullPath());
				file.create(content, true, null);
			}
			file.setDerived(true);
			file.deleteMarkers(null, true, IResource.DEPTH_INFINITE);
		}
		catch (final CoreException e) {
			e.printStackTrace();
		}
	}

}
