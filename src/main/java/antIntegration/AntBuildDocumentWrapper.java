package antIntegration;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AntBuildDocumentWrapper {
	private Document buildDocument = null;
	public AntBuildDocumentWrapper(Document doc) {
		this.buildDocument = doc;
	}
	
	public String getTextAttribute(String nodeName,String attributeName) {
		NodeList searchNode = buildDocument.getElementsByTagName(nodeName);
		int numberOfChildren = searchNode.getLength();
		for(int i=0; i < numberOfChildren-1; i++) {
			Node node = searchNode.item(i);
			if(node.hasAttributes()){
				return node.getAttributes().getNamedItem(attributeName).getTextContent();
			}
		}
		return null;
	}
	public void buildProperties(Properties buildProps) {
		NodeList searchNode = buildDocument.getElementsByTagName(Constants.XML_ELEMENT_PROPERTY);
		int numberOfChildren = searchNode.getLength();
		for(int i=0; i < numberOfChildren-1; i++) {
			Node node = searchNode.item(i);
			if(node.hasAttributes()){
				String propKey = node.getAttributes().getNamedItem(Constants.PROPERTY_NAME_ATTRIBUTE).getTextContent();
				String propValue = node.getAttributes().getNamedItem(Constants.PROPERTY_VALUE_ATTRIBUTE).getTextContent();
				buildProps.putIfAbsent(propKey, propValue);
			}
		}
	}
	public Node findChildNodeByTargetNameAttribute(String targetNameAttribute,String childNodeName) {
		NodeList searchNode = buildDocument.getElementsByTagName(Constants.BUILD_TARGET_ELEMENT);
		int numberOfChildren = searchNode.getLength();
		Node matchedNode = null;
		for(int i=0; i < numberOfChildren-1; i++) {
			Node node = searchNode.item(i);
			if(node.hasAttributes()){
				String compileTarget = node.getAttributes().getNamedItem(Constants.PROPERTY_NAME_ATTRIBUTE).getTextContent();
				if(StringUtils.isNotEmpty(compileTarget) && StringUtils.equalsIgnoreCase(compileTarget,targetNameAttribute)) {
					matchedNode = traverseNodeTree(node,childNodeName); 
					break;
				}
			}
		}
		return matchedNode;
	}
	public Node getChildNodeByTag(Node searchNode,String childNodeName) {
		Node matchedNode = null;
		if(searchNode.hasChildNodes()) {
			matchedNode = traverseNodeTree(searchNode,childNodeName); 
		}
		return matchedNode;
	}
	public List<String> getClassPathElements(String classpathRefId,Properties buildProperties){
		NodeList pathNode = buildDocument.getElementsByTagName(Constants.XML_ELEMENT_PATH);
		List<String> classpathElements = new ArrayList<String>();
		int numberOfPaths = pathNode.getLength();
		for(int i=0; i < numberOfPaths; i++) {
			Node node = pathNode.item(i);
			if(node.getNodeType() == Node.ELEMENT_NODE)
			{
				Element pathElementNode = (Element)node;
				if(pathElementNode.hasAttributes()){
					String thisClasspathRef = pathElementNode.getAttributes().getNamedItem(Constants.PATH_ELEMENT_ID_ATTRIBUTE).getTextContent();
					if(thisClasspathRef.equalsIgnoreCase(classpathRefId)) {
						NodeList childNodes = pathElementNode.getChildNodes();
						int childNodesLength = childNodes!= null?childNodes.getLength():0;
						if(childNodesLength > 0) {
							for(int j=0; j < childNodesLength-1; j++) {
								Node thisNode = childNodes.item(j);
								Element fileSetNode = thisNode.getNodeType() == Node.ELEMENT_NODE?(Element)thisNode:null;
								if(fileSetNode != null && Constants.PATH_SUB_ELEMENT_FILESET.equalsIgnoreCase(fileSetNode.getNodeName())) {								
									String fileSetDirectory = fileSetNode.getAttributes().getNamedItem(Constants.FILESET_DIR_ATTRIBUTE).getTextContent();
									NodeList jarInclude = fileSetNode.getChildNodes();
									int jarIncludeLength = jarInclude!= null?jarInclude.getLength():0;
									fileSetDirectory = StringUtils.strip(fileSetDirectory, "${}");
									fileSetDirectory = (String) buildProperties.get(fileSetDirectory);
									for(int k=0; k < jarIncludeLength-1; k++) {
										Node thisJarNode = jarInclude.item(k);
										Element jarIncludeNode =  thisJarNode.getNodeType() == Node.ELEMENT_NODE?(Element)thisJarNode:null;
										if(jarIncludeNode != null) {
											String jarRef = jarIncludeNode.getAttributes().getNamedItem(Constants.PROPERTY_NAME_ATTRIBUTE).getTextContent();
											StringBuffer jarAbsolutePath = new StringBuffer(fileSetDirectory);
											jarAbsolutePath.append("/");
											jarAbsolutePath.append(jarRef);
											classpathElements.add(jarAbsolutePath.toString());
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return classpathElements;
	}
	private Node traverseNodeTree(Node currentNode,String childNodeName) {
		NodeList childNodes = currentNode.getChildNodes();
		int childNodesLength = childNodes!= null?childNodes.getLength():0;
		if(childNodesLength > 0) {
			for(int j=0; j < childNodesLength; j++) {
				Node childNode = childNodes.item(j);
				if(childNodeName.equalsIgnoreCase(childNode.getNodeName())) {
					return childNode;
				}
				else {
					 traverseNodeTree(childNode,childNodeName);
				}
			}
		}
		else {
			return null;
		}
		return null;
	}
}
