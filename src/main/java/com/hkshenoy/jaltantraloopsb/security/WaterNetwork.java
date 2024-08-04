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

    @Column(name = "project_name", nullable = false)
    private String projectName;

    @Column(name = "organization_name")
    private String organizationName;

    @Column(name = "minimum_node_pressure", nullable = false)
    private Float minimumNodePressure;

    @Column(name = "default_pipe_roughness", nullable = false)
    private Float defaultPipeRoughness;

    @Column(name = "minimum_headloss", nullable = false)
    private Float minimumHeadloss;

    @Column(name = "maximum_headloss", nullable = false)
    private Float maximumHeadloss;

    @Column(name = "maximum_water_speed")
    private Float maximumWaterSpeed;

    @Column(name = "minimum_water_speed")
    private Float minimumWaterSpeed;

    @Column(name = "maximum_pipe_pressure")
    private Float maximumPipePressure;

    @Column(name = "number_of_supply_hours", nullable = false)
    private Float numberOfSupplyHours;

    @Column(name = "source_node_id", nullable = false)
    private Integer sourceNodeId;

    @Column(name = "source_node_name", nullable = false)
    private String sourceNodeName;

    @Column(name = "source_head", nullable = false)
    private Float sourceHead;

    @Column(name = "source_elevation", nullable = false)
    private Float sourceElevation;

}
