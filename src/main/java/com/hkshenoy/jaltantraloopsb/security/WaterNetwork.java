package com.hkshenoy.jaltantraloopsb.security;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "water_networks")
public class WaterNetwork implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "network_id", nullable = false, unique = true)
    private Integer networkId;

    @Column(name = "general", columnDefinition = "LONGTEXT", nullable = false)
    private String general;

    @Column(name = "nodes", columnDefinition = "LONGTEXT", nullable = false)
    private String nodes;

    @Column(name = "pipes", columnDefinition = "LONGTEXT", nullable = false)
    private String pipes;

    @Column(name = "commercialPipes", columnDefinition = "LONGTEXT", nullable = false)
    private String commercialPipes;

    @Column(name = "esrGeneral", columnDefinition = "LONGTEXT", nullable = false)
    private String esrGeneral;

    @Column(name = "esrCost", columnDefinition = "LONGTEXT", nullable = false)
    private String esrCost;

    @Column(name = "pumpGeneral", columnDefinition = "LONGTEXT", nullable = false)
    private String pumpGeneral;

    @Column(name = "pumpManual", columnDefinition = "LONGTEXT", nullable = false)
    private String pumpManual;

    @Column(name = "valves", columnDefinition = "LONGTEXT", nullable = false)
    private String valves;

    @Column(name="solved", nullable = false)
    private boolean solved;

    @Column(name="type")
    private String type;

    public Integer getNetworkId() {
        return networkId;
    }

    public void setNetworkId(Integer networkId) {
        this.networkId = networkId;
    }

    public String getGeneral() {
        return general;
    }

    public void setGeneral(String general) {
        this.general = general;
    }

    public String getNodes() {
        return nodes;
    }

    public void setNodes(String nodes) {
        this.nodes = nodes;
    }

    public String getPipes() {
        return pipes;
    }

    public void setPipes(String pipes) {
        this.pipes = pipes;
    }

    public String getCommercialPipes() {
        return commercialPipes;
    }

    public void setCommercialPipes(String commercialPipes) {
        this.commercialPipes = commercialPipes;
    }

    public String getEsrGeneral() {
        return esrGeneral;
    }

    public void setEsrGeneral(String esrGeneral) {
        this.esrGeneral = esrGeneral;
    }

    public String getEsrCost() {
        return esrCost;
    }

    public void setEsrCost(String esrCost) {
        this.esrCost = esrCost;
    }

    public String getPumpGeneral() {
        return pumpGeneral;
    }

    public void setPumpGeneral(String pumpGeneral) {
        this.pumpGeneral = pumpGeneral;
    }

    public String getPumpManual() {
        return pumpManual;
    }

    public void setPumpManual(String pumpManual) {
        this.pumpManual = pumpManual;
    }

    public String getValves() {
        return valves;
    }

    public void setValves(String valves) {
        this.valves = valves;
    }

    public boolean isSolved() {
        return solved;
    }

    public void setSolved(boolean solved) {
        this.solved = solved;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
