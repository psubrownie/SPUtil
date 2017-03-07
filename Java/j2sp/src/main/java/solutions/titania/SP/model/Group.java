package solutions.titania.SP.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * 
 * @author Scott
 * <Group ID="11" Name="Approvers" Description="Members of this group can edit and approve pages, list items, and documents." OwnerID="5" OwnerIsUser="False" />
 */
@XmlRootElement
public class Group {
	private String name;
	private String id;
	private String description;
	private String ownerID;
	private boolean ownerIsUser;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String spId) {
		this.id = spId;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getOwnerID() {
		return ownerID;
	}

	public void setOwnerID(String ownerID) {
		this.ownerID = ownerID;
	}

	public boolean isOwnerIsUser() {
		return ownerIsUser;
	}

	public void setOwnerIsUser(boolean ownerIsUser) {
		this.ownerIsUser = ownerIsUser;
	}
	

}
