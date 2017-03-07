package solutions.titania.SP;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SoapUtil {
    public static final String UPDATE_LIST_ITEM_SOAP_ACTION = "http://schemas.microsoft.com/sharepoint/soap/UpdateListItems";
    public static final String START_WORKFLOW_SOAP_ACTION = "http://schemas.microsoft.com/sharepoint/soap/workflow/StartWorkflow";

    static Logger log = Logger.getLogger(SoapUtil.class.getName());

    public static final String SP_LIST_SERVICE = "/_vti_bin/Lists.asmx";
    public static final String SP_WORKFLOW_SERVICE = "/_vti_bin/Workflow.asmx";
    public static final String SP_USER_GROUP_SERVICE = "/_vti_bin/usergroup.asmx";
    public static final String SP_PERMISSION_SERVICE = "/_vti_bin/Permissions.asmx";
    public static final String SP_USER_GROUP_REST_SERVICE = "/_api/Web/SiteGroups";

    

    public static final SimpleDateFormat spDateFormater = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private boolean findProblemRecord = false;

    private SPEnvironment env;

    public SoapUtil(SPEnvironment env) {
	this.env = env;
    }

    public void setDebug(boolean b) {
	findProblemRecord = b;
    }

    public String buildGetListItemsRequest(String listName, String viewName, String query, String viewFields,
	    String rowLimit, String queryOptions) {
	StringBuilder sb = new StringBuilder();
	sb.append(
		"<soap:Envelope xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:xsd='http://www.w3.org/2001/XMLSchema' xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/'><soap:Body><GetListItems xmlns='http://schemas.microsoft.com/sharepoint/soap/'>");
	sb.append("<listName>" + listName + "</listName>");
	sb.append("<viewName>" + "</viewName>");
	sb.append("<query>" + query + "</query>");
	sb.append("<viewFields><ViewFields>" + viewFields + "</ViewFields></viewFields>");
	sb.append("<rowLimit>9999</rowLimit>");
	sb.append(
		"<queryOptions><QueryOptions></QueryOptions></queryOptions></GetListItems></soap:Body></soap:Envelope>");
	return sb.toString();

    }

    public Boolean doesGroupExist(String groupName) throws Exception {
	String resp = getUserCollectionFromGroup(groupName);
	if (resp.contains("Group cannot be found."))
	    return false;
	return true;
    }

    public String getUserCollectionFromGroup(String groupName) throws Exception {
	String soapRequest = "<soap:Envelope xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:xsd='http://www.w3.org/2001/XMLSchema' xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/'>"
		+ "<soap:Body><GetUserCollectionFromGroup xmlns='http://schemas.microsoft.com/sharepoint/soap/directory/'><groupName>"
		+ groupName + "</groupName></GetUserCollectionFromGroup></soap:Body></soap:Envelope>";
	return makeSOAPCall(soapRequest, null, env.getUrl() + SP_USER_GROUP_SERVICE, 0);
    }

    public String getGroupCollectionFromSite() throws Exception {
	String soapRequest = "<soap:Envelope xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:xsd='http://www.w3.org/2001/XMLSchema' xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/'>"
		+ "<soap:Body><GetGroupCollectionFromSite xmlns='http://schemas.microsoft.com/sharepoint/soap/directory/' ></GetGroupCollectionFromSite></soap:Body></soap:Envelope>";
	return makeSOAPCall(soapRequest, null, env.getUrl() + SP_USER_GROUP_SERVICE, 0);
    }
    
    public enum PermissionType{
	List,Web;
    }
    
    public String getPermissionCollection(String name, PermissionType type) throws Exception {
   	String soapRequest = "<soap:Envelope xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:xsd='http://www.w3.org/2001/XMLSchema' xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/'>"
   		+ "<soap:Body><GetPermissionCollection  xmlns='http://schemas.microsoft.com/sharepoint/soap/directory/' ><objectName>"+name+"</objectName><objectType>"+type.name()+"</objectType></GetPermissionCollection></soap:Body></soap:Envelope>";
   	return makeSOAPCall(soapRequest, "http://schemas.microsoft.com/sharepoint/soap/directory/GetPermissionCollection", env.getUrl() + SP_PERMISSION_SERVICE, 0);
       }

    public String createGroup(String groupName, String owner, String description, String user) throws Exception {
	String soapRequest = "<soap:Envelope xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:xsd='http://www.w3.org/2001/XMLSchema' xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/'><soap:Body><AddGroup xmlns='http://schemas.microsoft.com/sharepoint/soap/directory/' >"
		+ "<groupName>" + groupName + "</groupName>"
		// +
		// "<ownerIdentifier>"+owner+"</ownerIdentifier><ownerType>group</ownerType>"
		+ "<ownerIdentifier>333Admins</ownerIdentifier><ownerType>group</ownerType>" + "<defaultUserLoginName>"
		+ user + "</defaultUserLoginName>" + "<description>" + description
		+ "</description></AddGroup></soap:Body></soap:Envelope>";
	System.out.println(soapRequest);
	return makeSOAPCall(soapRequest, "http://schemas.microsoft.com/sharepoint/soap/directory/AddGroup",
		env.getUrl() + SP_USER_GROUP_SERVICE, 0);
    }

    public void insertItem(SPListItem item) {
	insertItems(Collections.singletonList(item));
    }

    public void insertItem(SPListItem item, Boolean continueOnError) {
	insertItems(Collections.singletonList(item), continueOnError);
    }

    public void insertItems(Collection<? extends SPListItem> list) {
	insertItems(list, false);
    }

    public void insertItems(Collection<? extends SPListItem> list, Boolean continueOnError) {
	String resp = null;
	if (findProblemRecord && list.size() != 1) {
	    for (SPListItem c : list) {
		insertItems(Collections.singletonList(c), continueOnError);
	    }
	} else if (list.size() > 0) {
	    String listName = SPUtil.getListName(list.iterator().next().getClass());
	    List<String> soapCalls = new ArrayList<>();
	    StringBuilder sb = new StringBuilder();
	    sb.append(
		    "<soap:Envelope xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:xsd='http://www.w3.org/2001/XMLSchema' xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/'>");
	    sb.append("<soap:Body><UpdateListItems xmlns='http://schemas.microsoft.com/sharepoint/soap/'><listName>"
		    + listName + "</listName>");
	    sb.append("<updates><Batch OnError='Continue'>");
	    int method = 1;

	    for (SPListItem c : list) {

		log.debug("inserting: " + c.toString());
		StringBuilder row = new StringBuilder();
		row.append("<Method ID='" + method++ + "' Cmd='New'>");
		try {
		    row.append(c.getFieldList());
		} catch (Exception e) {
		    e.printStackTrace();
		    log.error("Failed to make FieldList: " + c.toString());
		}
		row.append("</Method>");

		if (sb.length() + row.length() < 45000) {// leaving safety room
							 // max is actually
							 // 49000ish
		    sb.append(row.toString());
		} else {
		    sb.append("</Batch></updates></UpdateListItems></soap:Body></soap:Envelope>");
		    soapCalls.add(sb.toString());

		    sb = new StringBuilder();
		    sb.append(
			    "<soap:Envelope xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:xsd='http://www.w3.org/2001/XMLSchema' xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/'>");
		    sb.append(
			    "<soap:Body><UpdateListItems xmlns='http://schemas.microsoft.com/sharepoint/soap/'><listName>"
				    + listName + "</listName>");
		    sb.append("<updates><Batch OnError='Continue'>");
		    sb.append(row.toString());
		    method = 1;
		}

	    }
	    sb.append("</Batch></updates></UpdateListItems></soap:Body></soap:Envelope>");
	    soapCalls.add(sb.toString());

	    for (String soapCall : soapCalls) {
		try {

		    resp = makeUpdateListItemsSOAPCall(soapCall);
		    if (resp.startsWith("<!DOCTYPE HTML PUBLIC")) {
			resp = makeUpdateListItemsSOAPCall(sb.toString());
			if (resp.startsWith("<!DOCTYPE HTML PUBLIC")) {
			    log.error("Req:" + sb.toString());
			    log.error("Resp:" + resp);
			    log.error("Trouble");
			    if (!continueOnError)
				System.exit(1);
			}
		    }
		} catch (Exception e) {
		    e.printStackTrace();
		    log.error("Request(" + sb.length() + "):" + sb.toString());
		    log.error("Response: " + resp);
		    log.error("Trouble", e);
		    if (!continueOnError)
			System.exit(1);

		}
		verifyResponseForErrorCode(sb.toString(), resp);
		updateListWithSpIds(list, resp);
	    }

	}
	// return resp;
    }

    public void updateItem(SPListItem spItem, SPListItem newItem) throws Exception {
	String listName = SPUtil.getListName(newItem.getClass());
	// updateItem(Collections.singletonList(item), listName);
	List<String> changedFields = BeanUtil.compareObjects(spItem, newItem);
	if (!changedFields.isEmpty()) {
	    StringBuilder sb = new StringBuilder();
	    sb.append(
		    "<soap:Envelope xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:xsd='http://www.w3.org/2001/XMLSchema' xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/'>");
	    sb.append("<soap:Body><UpdateListItems xmlns='http://schemas.microsoft.com/sharepoint/soap/'><listName>"
		    + listName + "</listName>");
	    sb.append("<updates><Batch OnError='Continue'>");

	    log.info("Updating: " + newItem.toString());
	    int method = 1;
	    StringBuilder row = new StringBuilder();
	    row.append("<Method ID='" + method++ + "' Cmd='Update'>");
	    row.append(newItem.getFieldList(changedFields));
	    row.append("<Field Name='ID'>" + newItem.getSpId() + "</Field>");
	    row.append("</Method>");
	    sb.append(row.toString());
	    sb.append("</Batch></updates></UpdateListItems></soap:Body></soap:Envelope>");

	    String request = sb.toString();
	    String resp = makeUpdateListItemsSOAPCall(request);
	    verifyResponseForErrorCode(request, resp);
	}
    }

    public void updateItem(SPListItem item) throws Exception {
	updateItem(Collections.singletonList(item));
    }

    public void updateItem(List<? extends SPListItem> items) throws Exception {
	if (findProblemRecord && items.size() != 1) {

	    for (SPListItem c : items) {
		updateItem(Collections.singletonList(c));
	    }

	    // for (Entry<Integer, ? extends SPListItem> e : existingCOA
	    // .entrySet()) {
	    // Map<Integer, SPListItem> singleMap = new HashMap<>();
	    // singleMap.put(e.getKey(), e.getValue());
	    //
	    // updateItem(singleMap, listName);
	    // }

	} else if (items.size() > 0) {
	    String listName = SPUtil.getListName(items.get(0).getClass());
	    List<String> soapCalls = new ArrayList<>();

	    StringBuilder sb = new StringBuilder();
	    sb.append(
		    "<soap:Envelope xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:xsd='http://www.w3.org/2001/XMLSchema' xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/'>");
	    sb.append("<soap:Body><UpdateListItems xmlns='http://schemas.microsoft.com/sharepoint/soap/'><listName>"
		    + listName + "</listName>");
	    sb.append("<updates><Batch OnError='Continue'>");
	    int method = 1;
	    for (SPListItem c : items) {
		// SPListItem c = e.getValue();
		log.debug("Updating: " + c.toString());

		StringBuilder row = new StringBuilder();
		row.append("<Method ID='" + method++ + "' Cmd='Update'>");
		row.append(c.getFieldList());
		row.append("<Field Name='ID'>" + c.getSpId() + "</Field>");
		row.append("</Method>");

		// leaving safety room max is actually 49000ish
		if (sb.length() + row.length() < 45000) {
		    sb.append(row.toString());
		} else {
		    sb.append("</Batch></updates></UpdateListItems></soap:Body></soap:Envelope>");
		    soapCalls.add(sb.toString());

		    sb = new StringBuilder();
		    sb.append(
			    "<soap:Envelope xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:xsd='http://www.w3.org/2001/XMLSchema' xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/'>");
		    sb.append(
			    "<soap:Body><UpdateListItems xmlns='http://schemas.microsoft.com/sharepoint/soap/'><listName>"
				    + listName + "</listName>");
		    sb.append("<updates><Batch OnError='Continue'>");
		    sb.append(row.toString());
		    method = 1;

		}

	    }
	    sb.append("</Batch></updates></UpdateListItems></soap:Body></soap:Envelope>");
	    soapCalls.add(sb.toString());

	    for (String request : soapCalls) {
		String resp = makeUpdateListItemsSOAPCall(request);
		verifyResponseForErrorCode(request, resp);
	    }
	}
    }

    public void verifyResponseForErrorCode(String req, String resp) {
	if (resp.contains("<ErrorCode>") && !resp.contains("<ErrorCode>0x00000000</ErrorCode>")) {
	    log.error("Req: " + req);
	    log.error("Resp: " + resp);
	    throw new RuntimeException("Webservice response error");
	}

    }

    public String makeStartWorkflowSOAPCall(String soapRequest) throws Exception {
	return makeSOAPCall(soapRequest, START_WORKFLOW_SOAP_ACTION, env.getUrl() + SP_WORKFLOW_SERVICE, 0);
    }

    public String makeGetListItemsSOAPCall(String soapRequest) throws Exception {
	return makeSOAPCall(soapRequest, null, env.getUrl() + SP_LIST_SERVICE, 0);
    }

    public String makeGetListSOAPCall(String listName) throws Exception {
	return makeSOAPCall(GetListRequest.toSoapRequest(listName), null, env.getUrl() + SP_LIST_SERVICE, 0);
    }

    public String makeUpdateListItemsSOAPCall(String soapRequest) throws Exception {
	return makeSOAPCall(soapRequest, UPDATE_LIST_ITEM_SOAP_ACTION, env.getUrl() + SP_LIST_SERVICE, 0);
    }

    public String makeSOAPCall(String soapRequest, String soapAction, String serviceEndpoint, int attempt)
	    throws Exception {
	log.debug("SoapRequest: " + soapRequest);
	String cookies = env.getCookieString();
	URL url = new URL(serviceEndpoint);
	HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	connection.setRequestProperty("Cookie", cookies);
	connection.setDoOutput(true);
	connection.setDoInput(true);
	connection.setInstanceFollowRedirects(false);
	connection.setRequestMethod("POST");
	connection.setRequestProperty("Content-Type", "text/xml;charset='UTF-8'");
	connection.setRequestProperty("charset", "utf-8");
	connection.setRequestProperty("Content-Length", "" + Integer.toString(soapRequest.getBytes().length));
	connection.setUseCaches(false);
	if (soapAction != null)
	    connection.setRequestProperty("SOAPAction", soapAction);

	DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
	// wr.writeBytes(soapRequest);
	wr.write(soapRequest.getBytes(Charset.forName("UTF-8")));
	// wr.writeUTF(soapRequest);
	wr.flush();
	String line;
	StringBuilder sb = new StringBuilder();
	int code = connection.getResponseCode();
	if (code == 200) {
	    BufferedReader reader = new BufferedReader(
		    new InputStreamReader(connection.getInputStream(), Charset.forName("UTF-8")));

	    while ((line = reader.readLine()) != null) {
		sb.append(line);
	    }
	} else if (code == 500) {
	    log.error("ERROR-500");
	    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));

	    while ((line = reader.readLine()) != null) {
		sb.append(line);
	    }
	    log.error(soapRequest);
	    log.error(sb.toString());
	} else if (code == 400) {
	    log.error("ERROR-400");
	    log.error(soapRequest);
	    throw new RuntimeException("Bad Request");
	} else {
	    log.error("Unhandled Code: " + code);
	    log.error("Request: " + soapRequest);
	    InputStream is = connection.getInputStream();
	    if (is == null)
		is = connection.getErrorStream();

	    BufferedReader reader = new BufferedReader(new InputStreamReader(is));

	    while ((line = reader.readLine()) != null) {
		sb.append(line);
	    }
	    log.error("Response: " + sb.toString());
	    if (attempt == 0) {
		env.expireCookies();
		return makeSOAPCall(soapRequest, soapAction, serviceEndpoint, attempt + 1);
	    }
	}

	wr.close();
	connection.disconnect();
	String response = sb.toString();
	log.debug("SoapResponse: " + response);
	return response;
    }

    private void updateListWithSpIds(Collection<? extends SPListItem> listC, String resp) {
	try {
	    List<? extends SPListItem> list = new ArrayList<>(listC);
	    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	    DocumentBuilder db = dbf.newDocumentBuilder();
	    Document doc = db.parse(new ByteArrayInputStream(resp.getBytes()));

	    doc.getDocumentElement().normalize();
	    Element docEl = doc.getDocumentElement();

	    NodeList rows = docEl.getElementsByTagName("Result");
	    for (int i = 0; i < rows.getLength(); i++) {
		Element result = (Element) rows.item(i);
		String methodId = result.getAttribute("ID");// 2,New
		Integer methodIdInt = Integer.parseInt(methodId.split(",")[0]);
		Node row = result.getElementsByTagName("z:row").item(0);
		NamedNodeMap attrs = row.getAttributes();
		Integer spId = Integer.parseInt(attrs.getNamedItem("ows_ID").getTextContent());
		list.get(methodIdInt - 1).setSpId(spId);
	    }
	} catch (Exception e) {
	    log.error("Resp:" + resp);
	    throw new RuntimeException(resp, e);
	}
    }

    // TODO Doesn't URLEncoder do this ?
    public static String urlEncode(String s) {
	// return URLEncoder.encode(s, "UTF-8");
	if (s == null)
	    s = "";
	s = s.replaceAll("&", "&amp;");
	s = s.replaceAll("<", "&lt;");
	s = s.replaceAll(">", "&gt;");
	s = s.replaceAll("\"", "&quot;");
	s = s.replaceAll("'", "&apos;");
	s = s.replaceAll("⁰", "&deg;");
	s = s.replaceAll(Character.toString((char) 8230), "...");
	s = s.replaceAll(Character.toString((char) 65535), "");// No idea what
							       // this is
							       // suppose to be.
	// s = s.replaceAll("…", "...");// Yes this happened :)
	return s;
    }

    public static String urlDecode(String s) {
	if (s == null)
	    s = "";
	s = s.replaceAll("&amp;amp;", "&");// It comes in like this from SP
	s = s.replaceAll("&amp;", "&");
	s = s.replaceAll("&lt;", "<");
	s = s.replaceAll("&gt;", ">");
	s = s.replaceAll("&quot;", "\"");
	s = s.replaceAll("&apos;", "'");
	s = s.replaceAll("&#39;", "'");
	s = s.replaceAll("&#10;", "\n");
	s = s.replaceAll("&deg;", "⁰");
	// s = s.replaceAll("...","…");// Yes this happened :)
	return s;
    }

    public void deleteItem(SPListItem item) throws Exception {
	String listName = SPUtil.getListName(item.getClass());
	StringBuilder sb = new StringBuilder();
	sb.append(
		"<soap:Envelope xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:xsd='http://www.w3.org/2001/XMLSchema' xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/'><soap:Body><UpdateListItems xmlns='http://schemas.microsoft.com/sharepoint/soap/'><listName>");
	sb.append(listName);
	sb.append("</listName><updates><Batch OnError='Continue'>");
	int method = 0;

	sb.append("<Method ID='");
	sb.append(method++);
	sb.append("' Cmd='Delete'><Field Name='ID'>");
	sb.append(item.getSpId());
	sb.append("</Field></Method>");

	sb.append("</Batch></updates></UpdateListItems></soap:Body></soap:Envelope>");
	String resp = makeUpdateListItemsSOAPCall(sb.toString());
	log.debug(resp);

    }

    // public void getListItemsRest() throws Exception {
    // String cookies = getCookieString();
    // // String = "param1=a&param2=b&param3=c";
    // String request =
    // "https://ksn2.faa.gov/faa/uasts/dev/_vti_bin/listdata.svc/Test(1)";
    // URL url = new URL(request);
    // HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    // connection.setRequestProperty("Cookie", cookies);
    // // connection.setDoOutput(true);
    // connection.setDoInput(true);
    // connection.setInstanceFollowRedirects(false);
    // connection.setRequestMethod("GET");
    // connection.setRequestProperty("Accept",
    // "application/json;charset='UTF-8'");
    // connection.setRequestProperty("charset", "utf-8");
    // connection.setUseCaches(false);
    //
    // String line;
    // BufferedReader reader = new BufferedReader(new InputStreamReader(
    // connection.getInputStream()));
    //
    // while ((line = reader.readLine()) != null) {
    // System.out.println(line);
    // }
    // connection.disconnect();
    // }
}
