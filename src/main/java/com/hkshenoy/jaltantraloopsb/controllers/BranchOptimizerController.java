package com.hkshenoy.jaltantraloopsb.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.hkshenoy.jaltantraloopsb.optimizer.*;
import com.hkshenoy.jaltantraloopsb.structs.*;
import jakarta.servlet.ServletException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/branchoptimizer")
@CrossOrigin("*")
public class BranchOptimizerController {
    ObjectMapper objectMapper = new ObjectMapper();

    private String extractEmailFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;
            String payload = new String(Base64.getDecoder().decode(parts[1]));
            // Very basic regex based json extraction for "sub" field
            return payload.replaceAll(".*\"sub\":\"([^\"]+)\".*", "$1");
        } catch (Exception e) {
            return null;
        }
    }


    @PostMapping("/optimize")
    public ResponseEntity<Map<String, Object>>  optimize(@RequestBody Map<String, Object> requestData, @RequestHeader("Authorization") String authHeader) throws ServletException, IOException {
        System.out.println("Post request Received");
        Map<String, Object> response = new HashMap<>();
        String token = authHeader.replace("Bearer ", "");
        String email = extractEmailFromToken(token);

        boolean solved=false;
        try {


            GeneralStruct generalProperties = objectMapper.convertValue(requestData.get("general"), GeneralStruct.class);
            String project = generalProperties.name_project;
            String organization = generalProperties.name_organization;

            NodeStruct[] nodes = objectMapper.convertValue(requestData.get("nodes"), NodeStruct[].class);
            if(nodes==null || nodes.length==0)
                throw new Exception("No node data provided.");

            PipeStruct[] pipes = objectMapper.convertValue(requestData.get("pipes"), PipeStruct[].class);
            if(pipes==null || pipes.length==0)
                throw new Exception("No pipe data provided.");

            CommercialPipeStruct[] commercialPipes = objectMapper.convertValue(requestData.get("commercialPipes"),CommercialPipeStruct[].class);
            if(commercialPipes==null || commercialPipes.length==0)
                throw new Exception("No commercial pipe data provided.");

            EsrGeneralStruct esrGeneralProperties = objectMapper.convertValue(requestData.get("esrGeneral"), EsrGeneralStruct.class);

            EsrCostStruct[] esrCostsArray = objectMapper.convertValue(requestData.get("esrCost"), EsrCostStruct[].class);

            PumpGeneralStruct pumpGeneralProperties = objectMapper.convertValue(requestData.get("pumpGeneral"), PumpGeneralStruct.class);

            PumpManualStruct[] pumpManualArray = objectMapper.convertValue(requestData.get("pumpManual"), PumpManualStruct[].class);

            ValveStruct[] valves = objectMapper.convertValue(requestData.get("valves"), ValveStruct[].class);
            BranchOptimizer opt = new BranchOptimizer(nodes, pipes, commercialPipes, generalProperties, esrGeneralProperties, esrCostsArray, pumpGeneralProperties, pumpManualArray, valves);

            solved = opt.Optimize();

//            Network network = new Network(requestData);

            if(solved) {
                double secondaryFlowFactor = generalProperties.supply_hours / esrGeneralProperties.secondary_supply_hours;
                double esrcapacityfactor = 1;
                if (esrGeneralProperties.esr_enabled) {
                    esrcapacityfactor = esrGeneralProperties.esr_capacity_factor;
                }
                List<NodeStruct> resultNodes = new ArrayList<NodeStruct>();
                for (Node node : opt.getNodes().values()) {
                    double demand = node.getDemand();
                    if (node.getESR() != node.getNodeID())
                        demand = demand * secondaryFlowFactor;
                    NodeStruct resultNode = new NodeStruct(
                            node.getNodeID(),
                            node.getNodeName(),
                            node.getElevation(),
                            demand,
                            node.getRequiredCapacity(esrcapacityfactor),
                            node.getResidualPressure(),
                            node.getHead(),
                            node.getPressure(),
                            node.getESR());
                    resultNodes.add(resultNode);
                }

                TreeMap<PipeCost, Double> cumulativePipeLength = new TreeMap<>();
                List<PipeStruct> resultPipes = new ArrayList<PipeStruct>();
                List<ResultPumpStruct> resultPumps = new ArrayList<ResultPumpStruct>();


                for (Pipe pipe : opt.getPipes().values()) {
                    double dia = pipe.getDiameter();
                    double dia2 = pipe.getDiameter2();
                    PipeCost chosenPipeCost = pipe.getChosenPipeCost();
                    PipeCost chosenPipeCost2 = pipe.getChosenPipeCost2();
                    double length = pipe.getLength() - pipe.getLength2();
                    if (!pipe.existingPipe()) {
                        if (!cumulativePipeLength.containsKey(chosenPipeCost))
                            cumulativePipeLength.put(chosenPipeCost, length);
                        else
                            cumulativePipeLength.put(chosenPipeCost, cumulativePipeLength.get(chosenPipeCost) + length);
                    }

                    double flow = pipe.isAllowParallel() && dia2 != 0 ?
                            pipe.getFlow() / (1 + (pipe.getRoughness2() / pipe.getRoughness()) * Math.pow(dia2 / dia, 4.87 / 1.852)) :
                            pipe.getFlow();

                    double parallelFlow = pipe.getFlow() - flow;
                    if (pipe.getFlowchoice() == Pipe.FlowType.SECONDARY) {
                        flow = flow * secondaryFlowFactor;
                    }

                    double headloss = Util.HWheadLoss(length, flow, pipe.getRoughness(), dia);
                    double headlossperkm = headloss * 1000 / length;
                    double speed = Util.waterSpeed(flow, dia);

                    double cost;
                    if (pipe.existingPipe())
                        cost = 0;
                    else
                        cost = length * chosenPipeCost.getCost();

                    Node startNode = pipe.getStartNode();
                    Node endNode = pipe.getEndNode();

                    boolean pressureExceeded = false;
                    if (generalProperties.max_pipe_pressure > 0) {
                        if (startNode.getPressure() > generalProperties.max_pipe_pressure ||
                                endNode.getPressure() > generalProperties.max_pipe_pressure) {
                            pressureExceeded = true;
                        }
                    }

                    PipeStruct resultPipe = new PipeStruct(
                            pipe.getPipeID(),
                            startNode.getNodeID(),
                            endNode.getNodeID(),
                            length,
                            dia,
                            pipe.getRoughness(),
                            flow,
                            headloss,
                            headlossperkm,
                            speed,
                            cost,
                            false,
                            pressureExceeded,
                            pipe.getFlowchoice() == Pipe.FlowType.PRIMARY,
                            pipe.getPumpHead(),
                            pipe.getPumpPower(),
                            pipe.getValveSetting()
                    );
                    resultPipes.add(resultPipe);
                    if (dia2 != 0) {
                        double length2 = pipe.isAllowParallel() ? length : pipe.getLength2();
                        if (!cumulativePipeLength.containsKey(chosenPipeCost2))
                            cumulativePipeLength.put(chosenPipeCost2, length2);
                        else
                            cumulativePipeLength.put(chosenPipeCost2, cumulativePipeLength.get(chosenPipeCost2) + length2);

                        double flow2 = pipe.isAllowParallel() ?
                                parallelFlow :
                                pipe.getFlow();

                        if (pipe.getFlowchoice() == Pipe.FlowType.SECONDARY) {
                            flow2 = flow2 * secondaryFlowFactor;
                        }

                        double headloss2 = Util.HWheadLoss(length2, flow2, pipe.getRoughness2(), dia2);
                        double headlossperkm2 = headloss2 * 1000 / length2;
                        double speed2 = Util.waterSpeed(flow2, dia2);
                        //String allowParallelString = pipe.isAllowParallel() ? "Parallel" : null;
                        double cost2 = length2 * chosenPipeCost2.getCost();


                        resultPipe = new PipeStruct(
                                pipe.getPipeID(),
                                pipe.getStartNode().getNodeID(),
                                pipe.getEndNode().getNodeID(),
                                length2,
                                dia2,
                                pipe.getRoughness2(),
                                flow2,
                                headloss2,
                                headlossperkm2,
                                speed2,
                                cost2,
                                pipe.isAllowParallel(),
                                pressureExceeded,
                                pipe.getFlowchoice() == Pipe.FlowType.PRIMARY,
                                pipe.getPumpHead(),
                                pipe.getPumpPower(),
                                pipe.getValveSetting()
                        );
                        resultPipes.add(resultPipe);
                    }

                    if (pumpGeneralProperties.pump_enabled && pipe.getPumpPower() > 0) {
                        double presentvaluefactor = Util.presentValueFactor(pumpGeneralProperties.discount_rate, pumpGeneralProperties.inflation_rate, pumpGeneralProperties.design_lifetime);
                        double primarycoeffecient = 365 * presentvaluefactor * generalProperties.supply_hours * pumpGeneralProperties.energycost_per_kwh * pumpGeneralProperties.energycost_factor;
                        double secondarycoeffecient = esrGeneralProperties.esr_enabled ? 365 * presentvaluefactor * esrGeneralProperties.secondary_supply_hours * pumpGeneralProperties.energycost_per_kwh * pumpGeneralProperties.energycost_factor : 0;
                        double power = pipe.getPumpPower();

                        double energycost = power * (pipe.getFlowchoice() == Pipe.FlowType.PRIMARY ? primarycoeffecient : secondarycoeffecient);
                        double capitalcost = power * pumpGeneralProperties.capitalcost_per_kw;


                        resultPumps.add(new ResultPumpStruct(pipe.getPipeID(),
                                pipe.getPumpHead(),
                                power,
                                energycost,
                                capitalcost,
                                energycost + capitalcost));
                    }
                }

                double cumulativeCost = 0;
                List<CommercialPipeStruct> resultCost = new ArrayList<CommercialPipeStruct>();
                for (Map.Entry<PipeCost, Double> entry : cumulativePipeLength.entrySet()) {
                    double cost = 0;
                    cost = entry.getValue() * entry.getKey().getCost();
                    cumulativeCost += cost;
                    CommercialPipeStruct resultcommercialPipe = new CommercialPipeStruct(
                            entry.getKey().getDiameter(),
                            cost,
                            entry.getValue(),
                            cumulativeCost,
                            entry.getKey().getRoughness()
                    );
                    resultCost.add(resultcommercialPipe);
                }
                List<ResultEsrStruct> resultEsrCost = new ArrayList<ResultEsrStruct>();
                if (esrGeneralProperties.esr_enabled) {
                    double cumulativeEsrCost = 0;
                    for (Node node : opt.getNodes().values()) {
                        if (node.getESR() == node.getNodeID() && node.getEsrTotalDemand() > 0) {
                            double cost = node.getEsrCost();
                            cumulativeEsrCost += cost;

                            boolean hasprimarychild = false;
                            for (Pipe pipe : node.getOutgoingPipes()) {
                                if (pipe.getFlowchoice() == Pipe.FlowType.PRIMARY) {
                                    hasprimarychild = true;
                                    break;
                                }

                            }
                            ResultEsrStruct resultEsr = new ResultEsrStruct(
                                    node.getNodeID(),
                                    node.getNodeName(),
                                    node.getElevation(),
                                    node.getEsrHeight(),
                                    node.getEsrTotalDemand(),
                                    cost,
                                    cumulativeEsrCost,
                                    hasprimarychild
                            );
                            resultEsrCost.add(resultEsr);
                        }
                    }
                }
                response.put("status", "success");
                response.put("data", "Done!");
                response.put("resultnodes", resultNodes);
                response.put("resultpipes", resultPipes);
                response.put("resultcost", resultCost);
                response.put("resultesrcost", resultEsrCost);
                response.put("resultpumpcost", resultPumps);


            } else {
                response.put("status", "Failure");
                response.put("data", "Failed to solve Network");
            }
        } catch (Exception e) {

            response.put("status", "Failure");
            response.put("data", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}
