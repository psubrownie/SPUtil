package solutions.titania.SP;

import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang3.builder.EqualsBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SPLookupItem {
	@JsonProperty("lookupId")
	private Number spId;
	
	@JsonProperty("lookupValue")
	private String name;

	public Number getSpId() {
		return spId;
	}

	public void setSpId(Number spId) {
		this.spId = spId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@JsonIgnore
	@XmlTransient
	public void setName(Number name) {
		setName("" + name);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SPLookupItem && obj != null) {
			return EqualsBuilder.reflectionEquals(this, obj, false);
		} else
			return super.equals(obj);
	}
}
