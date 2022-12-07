package antIntegration;

public class EclipseArtifactsStandalone {

	public static void main(String[] args) {
		System.setProperty("javax.xml.parsers.DocumentBuilderFactory","org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");
		System.setProperty("javax.xml.parsers.SAXParserFactory","org.apache.xerces.jaxp.SAXParserFactoryImpl");
		System.setProperty("javax.xml.transform.TransformerFactory","org.apache.xalan.processor.TransformerFactoryImpl");
		EclipseArtifacts artifact = new EclipseArtifacts();
		artifact.execute();
	}

}
