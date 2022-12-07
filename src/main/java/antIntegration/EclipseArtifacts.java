package antIntegration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class EclipseArtifacts extends Task{

	public void execute() throws BuildException {
		Properties buildProperties = new Properties();
		String currentDirectory = System.getProperty("user.dir");
		log("got current directory:"+currentDirectory,Project.MSG_INFO);
		StringBuffer antBuildFilePath = new StringBuffer(currentDirectory);
		antBuildFilePath.append(File.separator);
		antBuildFilePath.append(Constants.BUILD_FILE);
		log("created build file path:"+antBuildFilePath,Project.MSG_INFO);
		File buildFile = new File(antBuildFilePath.toString());
		log("got build file:"+buildFile.getAbsolutePath(),Project.MSG_INFO);
		Document document = null;
		try {
			document = DocumentBuilderFactoryImpl.newInstance().newDocumentBuilder().parse(buildFile);
			document.getDocumentElement().normalize();
			AntBuildDocumentWrapper buildDocument = new AntBuildDocumentWrapper(document);
			buildDocument.buildProperties(buildProperties);
			Node javacNodeElement =  buildDocument.findChildNodeByTargetNameAttribute(Constants.XML_ELEMENT_COMPILE, Constants.COMPILE_JAVAC_ELEMENT);
			String javcSrcAttribute = javacNodeElement.getAttributes().getNamedItem(Constants.JAVAC_SRC_ATTRIBUTE).getTextContent();
			String javacDestAttribute = javacNodeElement.getAttributes().getNamedItem(Constants.JAVAC_DEST_ATTRIBUTE).getTextContent();
			String[] javacDestFragments = javacDestAttribute.split("/");
			String buildDir = StringUtils.strip(javacDestFragments[0], "${}");
			buildDir = (String) buildProperties.get(buildDir);
			javacDestAttribute = StringUtils.replace(javacDestAttribute,javacDestFragments[0], buildDir);
			Node javacClasspathElement =  buildDocument.getChildNodeByTag(javacNodeElement,Constants.JAVAC_CLASSPATH_SUB_ELEMENT);
			String classPathRef = javacClasspathElement.getAttributes().getNamedItem(Constants.JAVAC_CLASSPATH_REF_ID).getTextContent();
			List<String> classpathElements = buildDocument.getClassPathElements(classPathRef, buildProperties);
			VelocityEngine velocityEngine = new VelocityEngine();
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			InputStream velocityProperties = loader.getResourceAsStream(Constants.VELOCITY_PROPERTIES);
			log("got velocityProperties file:"+velocityProperties,Project.MSG_INFO);
			Properties props = new Properties();
			props.load(velocityProperties);
			log("loaded velocityProperties file",Project.MSG_INFO);
			props.entrySet().forEach(entry -> {
				log("VelocityProperties Entry:"+entry.getKey(),Project.MSG_INFO);
				log("VelocityProperties Entry:"+entry.getValue(),Project.MSG_INFO);
			});
			velocityEngine.init(props);
			File eclipseProjectFile = new File(Constants.ECLIPSE_PROJECT_FILE);
			if(eclipseProjectFile.createNewFile()) {
				Template projectTemplate = velocityEngine.getTemplate("project.vm");
				log("Got project template:"+projectTemplate,Project.MSG_INFO);
				VelocityContext context = new VelocityContext();
				context.put("project", "AntEclipseArtifacts");
				FileWriter fw = new FileWriter(eclipseProjectFile);
				projectTemplate.merge(context,fw);
				fw.flush();
			}
			File eclipseClasspathFile = new File(Constants.ECLIPSE_CLASSPATH_FILE);
			if(eclipseClasspathFile.createNewFile()) {
				Template classPathTemplate = velocityEngine.getTemplate("classpath.vm");
				log("Got classpath template:"+classPathTemplate,Project.MSG_INFO);
				VelocityContext context = new VelocityContext();
				context.put("javaSourceDirectory",javcSrcAttribute);
				context.put("outputDirectory",javacDestAttribute);
				context.put("classpath",classpathElements);
				FileWriter fw = new FileWriter(eclipseClasspathFile);
				classPathTemplate.merge(context,fw);
				fw.flush();
			}
		} catch (SAXException ex) {
			log("Unable to parse build file",Project.MSG_ERR);
			log(ex,Project.MSG_ERR);
		} catch (IOException ex) {
			ex.printStackTrace();
			log("Unable to find build file in the current working directory",Project.MSG_ERR);
			log(ex,Project.MSG_ERR);
		} catch (ParserConfigurationException ex) {
			log("Unable to parse build file",Project.MSG_ERR);
			log(ex,Project.MSG_ERR);
		}
		
		
    }
}
