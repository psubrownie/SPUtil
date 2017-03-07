package solutions.titania.SP;

public class StartWorkflowRequest {
	// {listName:'Test',
	// itemID:2,
	// workflowName:'testTest',
	// parameters:[{Name:'user',Value:1073741829}]
	// }

	// <soap:Envelope xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'
	// xmlns:xsd='http://www.w3.org/2001/XMLSchema'
	// xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/'><soap:Body><StartWorkflow
	// xmlns='http://schemas.microsoft.com/sharepoint/soap/workflow/'
	// ><item>http://ksn2.faa.gov/faa/uasts/dev/Lists/test/2_.000</item>
	// <templateId>{340bcded-2d5b-40d8-af0a-4d2ff754fca1}</templateId>
	// <workflowParameters><Data><user>ksn\scott.lusebrink</user></Data></workflowParameters></StartWorkflow></soap:Body></soap:Envelope>
	public static String toSoapRequest(String template,String listUrl,Number itemNumber) {
		// TODO steal from spservices
		StringBuilder sb = new StringBuilder();
		sb.append("<soap:Envelope xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:xsd='http://www.w3.org/2001/XMLSchema' xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/'>");
		sb.append("<soap:Body><StartWorkflow xmlns='http://schemas.microsoft.com/sharepoint/soap/workflow/'>");
		sb.append("<item>"+listUrl
				+ itemNumber
				+ "_.000</item>");
		sb.append("<templateId>{"+template+"}</templateId>");
		sb.append("<workflowParameters><root /></workflowParameters></StartWorkflow></soap:Body></soap:Envelope>");
		return sb.toString();
	}
}
