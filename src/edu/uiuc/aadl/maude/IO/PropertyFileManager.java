package edu.uiuc.aadl.maude.IO;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.cmu.sei.aadl.model.core.AObject;
import edu.cmu.sei.aadl.model.instance.InstanceObject;
import edu.cmu.sei.aadl.model.util.AadlUtil;
import edu.uiuc.aadl.maude.RtmAadlUtil;
import edu.uiuc.aadl.maude.verification.RtmPropertyDef;
import edu.uiuc.aadl.maude.verification.RtmVerificationComm;
import edu.uiuc.aadl.maude.verification.RtmVerificationSpec;

/**
 * 
 * @author kquine
 *
 */

public class PropertyFileManager {
	
	File file;
	
	public PropertyFileManager(File f) {
		this.file = f;
	}

	
	static public RtmVerificationSpec readProp(InputStream input) 
		throws DOMException, IOException, SAXException, ParserConfigurationException {
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(input);
		doc.getDocumentElement().normalize();
		
		// root
		Element root = doc.getDocumentElement();
		if ( !root.getNodeName().equalsIgnoreCase("verification"))
			throw new DOMException(DOMException.SYNTAX_ERR, "Invalid Root Element");
		
		
		// model
		Element modelElm = (Element) root.getElementsByTagName("model").item(0);
		String modelFile = modelElm.getChildNodes().item(0).getNodeValue().trim();
		IResource fr = ResourcesPlugin.getWorkspace().getRoot().findMember(modelFile);
		AObject model = AadlUtil.getAObject(fr);
		if ( !RtmAadlUtil.isInstanceModel(model))
			throw new DOMException(DOMException.SYNTAX_ERR, "Invalid Instance Model");
		RtmVerificationSpec res = new RtmVerificationSpec((InstanceObject)model);
		
		{
			// commands
			NodeList cList = root.getElementsByTagName("command");
			for (int ci = 0; ci < cList.getLength(); ++ci) {
				Node cNode = cList.item(ci);
				if (cNode.getNodeType() == Node.ELEMENT_NODE) {
					Element cNameElm = (Element) ((Element)cNode).getElementsByTagName("name").item(0);
					Element cValueElm = (Element) ((Element)cNode).getElementsByTagName("value").item(0);
					String name = cNameElm.getChildNodes().item(0).getNodeValue().trim();
					String value = cValueElm.getChildNodes().item(0).getNodeValue().trim();
					res.getCommandList().add(new RtmVerificationComm(name, value));
				}
			}
		}
		{
			// prop definitions
			NodeList dList = root.getElementsByTagName("definition");
			for (int di = 0; di < dList.getLength(); ++di) {
				Node dNode = dList.item(di);
				if (dNode.getNodeType() == Node.ELEMENT_NODE) {
					Element dNameElm = (Element) ((Element)dNode).getElementsByTagName("name").item(0);
					Element dValueElm = (Element) ((Element)dNode).getElementsByTagName("value").item(0);
					String name = dNameElm.getChildNodes().item(0).getNodeValue().trim();
					String value = dValueElm.getChildNodes().item(0).getNodeValue().trim();
					res.getPropertyDefList().add(new RtmPropertyDef(name, value));
				}
			}
		}
		return res;
	}
	
	static public void writeProp(RtmVerificationSpec spec, OutputStream output) 
		throws ParserConfigurationException, TransformerException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();	
		
		// root
		Document doc = db.newDocument();
		Element rootElmt = doc.createElement("verification");
		doc.appendChild(rootElmt);
		
		// model
		Element model = doc.createElement("model");
		rootElmt.appendChild(model);
		model.appendChild(doc.createTextNode(
				spec.getModel().eResource().getURI().devicePath().substring(9)));
		
		Attr modelType = doc.createAttribute("type");
		modelType.setValue("SynchAADL");
		model.setAttributeNode(modelType);
		
		// commands
		for (RtmVerificationComm vc : spec.getCommandList()) {
			Element comm = doc.createElement("command");
			rootElmt.appendChild(model);
			
			Element cname = doc.createElement("name");
			comm.appendChild(cname);
			cname.appendChild(doc.createTextNode(vc.getName()));
			
			Element cvalue = doc.createElement("value");
			comm.appendChild(cvalue);
			cvalue.appendChild(doc.createTextNode(vc.getPropertyString()));
			
			Attr propType = doc.createAttribute("type");
			propType.setValue("ltl");
			cvalue.setAttributeNode(propType);
		}
		
		// definitions
		for (RtmPropertyDef pd : spec.getPropertyDefList()) {
			Element def = doc.createElement("definition");
			rootElmt.appendChild(model);
			
			Element dname = doc.createElement("name");
			def.appendChild(dname);
			dname.appendChild(doc.createTextNode(pd.getName()));
			
			Element dvalue = doc.createElement("value");
			def.appendChild(dvalue);
			dvalue.appendChild(doc.createTextNode(pd.getDefinition()));
		}
		
		// write to the xml file
		TransformerFactory tff = TransformerFactory.newInstance();
		Transformer tf = tff.newTransformer();
		tf.setOutputProperty(OutputKeys.INDENT, "yes");
		DOMSource sor = new DOMSource(doc);
		StreamResult sres = new StreamResult(output);
		tf.transform(sor, sres);
	}

}
