package solutions.titania.SP;

public class GetListRequest {
	public static String toSoapRequest(String listName) {
		return "<soap:Envelope xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:xsd='http://www.w3.org/2001/XMLSchema' xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/'><soap:Body><GetList xmlns='http://schemas.microsoft.com/sharepoint/soap/'><listName>"
				+ listName
				+ "</listName></GetList></soap:Body></soap:Envelope>";
	}
}
