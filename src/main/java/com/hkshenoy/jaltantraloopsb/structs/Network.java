package com.hkshenoy.jaltantraloopsb.structs;

import jakarta.servlet.http.HttpServletRequest;

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

    public Network(HttpServletRequest request) {
        this.general = request.getParameter("general");
        this.nodes = request.getParameter("nodes");
        this.pipes = request.getParameter("pipes");
        this.commercialPipes = request.getParameter("commercialPipes");
        this.esrGeneral = request.getParameter("esrGeneral");
        this.esrCost = request.getParameter("esrCost");
        this.pumpGeneral = request.getParameter("pumpGeneral");
        this.pumpManual = request.getParameter("pumpManual");
        this.valves = request.getParameter("valves");
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
