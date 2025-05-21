package com.hkshenoy.jaltantraloopsb.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hkshenoy.jaltantraloopsb.helper.CustomLogger;
import com.hkshenoy.jaltantraloopsb.optimizer.Node;
import com.hkshenoy.jaltantraloopsb.optimizer.Optimizer;
import com.hkshenoy.jaltantraloopsb.optimizer.Pipe;
import com.hkshenoy.jaltantraloopsb.security.JwtTokenUtil;
import com.hkshenoy.jaltantraloopsb.structs.CommercialPipeStruct;
import com.hkshenoy.jaltantraloopsb.structs.EsrCostStruct;
import com.hkshenoy.jaltantraloopsb.structs.EsrGeneralStruct;
import com.hkshenoy.jaltantraloopsb.structs.GeneralStruct;
import com.hkshenoy.jaltantraloopsb.structs.NodeStruct;
import com.hkshenoy.jaltantraloopsb.structs.PipeStruct;
import com.hkshenoy.jaltantraloopsb.structs.PumpGeneralStruct;
import com.hkshenoy.jaltantraloopsb.structs.PumpManualStruct;
import com.hkshenoy.jaltantraloopsb.structs.ResultEsrStruct;
import com.hkshenoy.jaltantraloopsb.structs.ValveStruct;

import jakarta.servlet.ServletException;

@RestController
@RequestMapping("/loopoptimizer")
@CrossOrigin("*")
public class LoopOptimizerController {
    
    ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private CustomLogger customLogger;

    @Value("${solver.root.dir}")
    private String solverRootDir;


    @PostMapping("/optimize")
    public ResponseEntity<Map<String, Object>>  optimize(@RequestBody Map<String, Object> requestData, @RequestHeader("Authorization") String authHeader) throws ServletException, IOException {
        System.out.println("Post request Received");
        Map<String, Object> response = new HashMap<>();
        String token = authHeader.replace("Bearer ", "");
        String email = "";
        try {
            email = jwtTokenUtil.getEmailFromToken(token);
        } catch (Exception e) {
            response.put("status", "Failure");
            response.put("data", "Invalid or expired token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        customLogger.logd("doPost() called (JaltantraLoopController.java)");
        try{

            // 1. Data Extraction
            String action=objectMapper.convertValue(requestData.get("action"), String.class);

            customLogger.logd("doPost() action=" + action);
            String runTime=objectMapper.convertValue(requestData.get("time"), String.class);
            GeneralStruct generalProperties=objectMapper.convertValue(requestData.get("general"), GeneralStruct.class);

            NodeStruct[] nodes=objectMapper.convertValue(requestData.get("nodes"), NodeStruct[].class);
            if(nodes==null || nodes.length==0)
                throw new Exception("No pipe data provided.");
            
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

            // 2. Optimization
            Optimizer opt= new Optimizer(solverRootDir, nodes, pipes, commercialPipes, generalProperties, esrGeneralProperties, esrCostsArray, pumpGeneralProperties, pumpManualArray, valves);

            final boolean solved= opt.Optimize(runTime, generalProperties.name_project);

            // 3. Response

            String message;
            if(solved)
            {
                if (esrGeneralProperties.esr_enabled)
                    customLogger.logw("CHECMKE: esrGeneralProperties.esr_enabled=true");
                if (pumpGeneralProperties.pump_enabled)
                    customLogger.logw("CHECMKE: pumpGeneralProperties.pump_enabled=true");

                double secondaryFlowFactor = generalProperties.supply_hours / esrGeneralProperties.secondary_supply_hours;
                double esrcapacityfactor = 1;
                if (esrGeneralProperties.esr_enabled) {
                    esrcapacityfactor = esrGeneralProperties.esr_capacity_factor;
                }

                ArrayList<NodeStruct> resultNodes = new ArrayList<>();
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
                            node.getESR()
                    );
                    resultNodes.add(resultNode);
                }

                ArrayList<ResultEsrStruct> resultEsrCost = new ArrayList<>();
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
                response.put("resultpipes", opt.resultPipes);
                response.put("resultcost", opt.resultCost);
                response.put("resultesrcost", resultEsrCost);
                response.put("resultpumpcost", opt.resultPumps);
            }
            else{
                response.put("status", "Failure");
                response.put("data", "Failed to solve Network");
            }


        }catch(Exception e)
        {
            response.put("status", "Failure");
            response.put("data", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        return ResponseEntity.status(HttpStatus.OK).body(response);
        // return response;
    }
    
}
