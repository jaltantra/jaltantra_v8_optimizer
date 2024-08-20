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

    @Column(name = "user_id")
    private Long userid;

}
