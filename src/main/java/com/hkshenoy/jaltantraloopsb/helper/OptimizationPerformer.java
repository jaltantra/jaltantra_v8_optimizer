package com.hkshenoy.jaltantraloopsb.helper;

import com.google.gson.Gson;
import com.hkshenoy.jaltantraloopsb.optimizer.Node;
import com.hkshenoy.jaltantraloopsb.optimizer.Optimizer;
import com.hkshenoy.jaltantraloopsb.optimizer.Pipe;
import com.hkshenoy.jaltantraloopsb.structs.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

@Component
@Scope("prototype")
public class OptimizationPerformer {

    @Autowired
    NonOptimizationRelatedActionPerformer nonOptimizationRelatedActionPerformer;

    @Value("${JALTANTRA_VERSION}")
    private String version;

    @Value("${solver.root.dir}")
    private String solverRootDir;

    @Autowired
    private CustomLogger customLogger;






    public void performOptimization(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // `action` was `null` - so we try to perform optimization
        PrintWriter out = response.getWriter();
        try{
            // REFER: https://www.edureka.co/community/8153/what-difference-between-getattribute-and-getparameter-java
            String place = request.getParameter("place");
            if(place!=null){
                request.setAttribute("place", place);
            }

            // Ensure that the website frontend is using the same version as the backend
            String currVersion = version;
            String clientVersion = request.getParameter("version");
            final boolean clientOld = VersionComparator.compareVersion(currVersion,clientVersion) != 0;
            if(clientOld){
                throw new Exception("Your browser is running an old JalTantra version.<br> Please save your data and press ctrl+F5 to do a hard refresh and get the latest version.<br> If still facing issues please contact the <a target='_blank' href='https://groups.google.com/forum/#!forum/jaltantra-users/join'>JalTantra Google Group</a>");
            }

            Gson gson = new Gson();
            GeneralStruct generalProperties = gson.fromJson(request.getParameter("general"), GeneralStruct.class);

            String project = generalProperties.name_project;
            if(project!=null){
                request.setAttribute("project", project);
            }

            String organization = generalProperties.name_organization;
            if(organization!=null){
                request.setAttribute("organization", organization);
            }

            String vmax = String.valueOf(generalProperties.max_water_speed);
            System.out.println("MAX WATER SPEED = "+vmax);

            NodeStruct[] nodes = gson.fromJson(request.getParameter("nodes"), NodeStruct[].class);
            if(nodes==null || nodes.length==0)
                throw new Exception("No node data provided.");

            PipeStruct[] pipes = gson.fromJson(request.getParameter("pipes"), PipeStruct[].class);
            if(pipes==null || pipes.length==0)
                throw new Exception("No pipe data provided.");

            CommercialPipeStruct[] commercialPipes = gson.fromJson(request.getParameter("commercialPipes"), CommercialPipeStruct[].class);
            if(commercialPipes==null || commercialPipes.length==0)
                throw new Exception("No commercial pipe data provided.");

            EsrGeneralStruct esrGeneralProperties = gson.fromJson(request.getParameter("esrGeneral"), EsrGeneralStruct.class);
            EsrCostStruct[] esrCostsArray = gson.fromJson(request.getParameter("esrCost"), EsrCostStruct[].class);

            PumpGeneralStruct pumpGeneralProperties = gson.fromJson(request.getParameter("pumpGeneral"), PumpGeneralStruct.class);
            PumpManualStruct[] pumpManualArray = gson.fromJson(request.getParameter("pumpManual"), PumpManualStruct[].class);

            ValveStruct[] valves = gson.fromJson(request.getParameter("valves"), ValveStruct[].class);
//


            Optimizer opt = new Optimizer(solverRootDir,nodes, pipes, commercialPipes, generalProperties, esrGeneralProperties, esrCostsArray, pumpGeneralProperties, pumpManualArray, valves);



            String runTime=request.getParameter("time");

            // Start the true optimization work
            final boolean solved = opt.Optimize(runTime, version, generalProperties.name_project);

            String message;
            if (solved) {
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

                String resultNodeString = gson.toJson(resultNodes);
                // System.out.println(resultNodeString);

                String resultPipeString = gson.toJson(opt.resultPipes);
                // System.out.println(resultPipeString);

                String resultCostString = gson.toJson(opt.resultCost);
                // System.out.println(resultCostString);

                String resultEsrCostString = gson.toJson(resultEsrCost);
                // System.out.println(resultEsrCostString);

                String resultPumpString = gson.toJson(opt.resultPumps);

                // System.out.println(generateRandomInput());
                // String coordinatesString = opt.getCoordinatesString();
                message = "{\"status\":\"success\"" +
                        ", \"data\":\"Done! " + (esrGeneralProperties.esr_enabled ? "Results may not be as expected because ESR is most probably not supported." : "") + "\"" +
                        ", \"resultnodes\":" + resultNodeString +
                        ", \"resultpipes\":" + resultPipeString +
                        ", \"resultcost\":" + resultCostString +
                        ", \"resultesrcost\":" + resultEsrCostString +
                        ", \"resultpumpcost\":" + resultPumpString + "}";
                // System.out.println(message);
            } else {

                message = "{\"status\":\"error\",\"message\":\"Failed to solve the network\"}";
            }
            out.print(message);
        }
        catch(Exception e)
        {
            customLogger.loge("Exception e = " + e.getMessage());
            customLogger.loge("Exception e = " + e.toString());
            e.printStackTrace();
            // NOTE: JSON strings do not allow new line char
            // \\\\\r\\\\\n , \\\\r\\\\n
            String error = "{\"status\":\"error\",\"message\":\"Internal server error: unknown issue\"}";
            if (e.getMessage() != null)
                error = "{\"status\":\"success\",\"message\":\"" + e.getMessage().replaceAll("(\n)+", "<br/>").replaceAll("\"", "\\\\\"") + "\"}";
            else
                customLogger.loge("FIXME: Critical error in the server");
            out.print(error);
        }
    }
}


