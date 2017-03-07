package solutions.titania.SP.model;

/**
 * 
 * @author Scott <User ID="8323"
 *         Sid="S-1-5-21-3215564045-1863808890-1157122868-2027720"
 *         Name="Jason CTR Weber/ACT/CNTR/FAA" LoginName="FAA\afs080jw"
 *         Email="Jason.CTR.Weber@faa.gov" Notes="" IsSiteAdmin="True"
 *         IsDomainGroup="False" Flags="0" />
 */
public class User {
	private String id;
	private String sid;
	private String name;
	private String loginName;
	private String email;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSid() {
		return sid;
	}

	public void setSid(String sid) {
		this.sid = sid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLoginName() {
		return loginName;
	}

	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof User) {
			User o = (User) obj;
			return getSid().equals(o.getSid());
		} else
			return super.equals(obj);
	}

//	@Override
//	public int hashCode() {
//		if (sid != null)
//			return sid.hashCode();
//		else
//			return super.hashCode();
//	}
}
