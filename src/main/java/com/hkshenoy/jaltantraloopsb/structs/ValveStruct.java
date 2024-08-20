package com.hkshenoy.jaltantraloopsb.structs;
//container for valve information

//pipeid: unique integer id of the pipe in which the valve is installed
//valvesetting: pressure reduction caused by the pressure reducing valve installed
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ValveStruct
{
	public ValveStruct() {
	}

	public ValveStruct(int pipeid, double valvesetting) {
		this.pipeid = pipeid;
		this.valvesetting = valvesetting;
	}

	public int pipeid;
	public double valvesetting;
	
	@Override
	public String toString() {
		return "ValveStruct [pipeid=" + pipeid + ", valvesetting="
				+ valvesetting + "]";
	}
}
