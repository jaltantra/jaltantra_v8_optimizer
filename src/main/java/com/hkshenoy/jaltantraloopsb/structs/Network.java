package com.hkshenoy.jaltantraloopsb.structs;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public class Network {
    public String general;
    public String nodes;
    public String pipes;
    public String commercialPipes;
    public String esrGeneral;
    public String esrCost;
    public String pumpGeneral;
    public String pumpManual;
    public String valves;

    public Network(Map<String, Object> requestData) {
        this.general = requestData.get("general").toString();
        this.nodes = requestData.get("nodes").toString();
        this.pipes = requestData.get("pipes").toString();
        this.commercialPipes = requestData.get("commercialPipes").toString();
        this.esrGeneral = requestData.get("esrGeneral").toString();
        this.esrCost = requestData.get("esrCost").toString();
        this.pumpGeneral = requestData.get("pumpGeneral").toString();
        this.pumpManual = requestData.get("pumpManual").toString();
        this.valves = requestData.get("valves").toString();
    }



    @Override
    public String toString() {
        return "Network{" +
                "general='" + general + '\'' +
                ", nodes='" + nodes + '\'' +
                ", pipes='" + pipes + '\'' +
                ", commercialPipes='" + commercialPipes + '\'' +
                ", esrGeneral='" + esrGeneral + '\'' +
                ", esrCost='" + esrCost + '\'' +
                ", pumpGeneral='" + pumpGeneral + '\'' +
                ", pumpManual='" + pumpManual + '\'' +
                ", valves='" + valves + '\'' +
                '}';
    }
}
