package com.hkshenoy.jaltantraloopsb.controllers;

import com.google.gson.Gson;

import com.hkshenoy.jaltantraloopsb.helper.*;
import com.hkshenoy.jaltantraloopsb.structs.*;
import com.hkshenoy.jaltantraloopsb.optimizer.*;
import com.hkshenoy.jaltantraloopsb.optimizer.Pipe.FlowType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Enumeration;


@Controller
@RequestMapping("/branch")
@CrossOrigin("*")
public class BranchController
{		

	//version number of JalTantra

	@Value("${JALTANTRA_VERSION}")
	private String version;
	
	@Autowired
	XMLUploader xmlUploader;

	@Autowired
	ExcelUploader excelUploader;

	@Autowired
	ExcelOutputUploader excelOutputUploader;

   	@Autowired
	EPANetUploader epaNetUploader;

	@Autowired
	MapSnapshotUploader mapSnapshotUploader;

	@Autowired
	VersionComparator versionComparator;

	@Autowired
	private UserHistoryTracker userHistoryTracker;

	@Value("${server.servlet.context-path}")
	private String contextPath;


	// "/branch" endpoint returns system.html document after setting the attributes
	@RequestMapping(value="",method= RequestMethod.GET)
	public String home(Model model){
		model.addAttribute("contextPath",contextPath);
		model.addAttribute("version",version);
		return "system";
	}


	// "/branch/optimize" endpoint authenticates the user, accepts file uploading and optimizes the uploaded network.
	@RequestMapping(value="/optimize",method = {RequestMethod.GET, RequestMethod.POST})
    public void doPost(@AuthenticationPrincipal UserDetails user,HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{		
    	//action refers to uploading some file or optimization of network
		System.out.println("Post request Received");

		if(user != null){
			userHistoryTracker.saveUserRequest(user,request);
		}
		//--------------------------FILE UPLOAD HANDLING:START-----------------------------------
		String t = request.getParameter("action");
		if(t!=null)
		{
			if(t.equalsIgnoreCase("saveInputXml"))
			{
				xmlUploader.uploadXmlInputFile(request,response);
				return;
			}
			else if(t.equalsIgnoreCase("saveInputExcel"))
			{
				excelUploader.uploadExcelInputFile(request,response);
				return;
			}
			else if(t.equalsIgnoreCase("saveOutputExcel"))
			{
				excelOutputUploader.uploadExcelOutputFile(request,response);
				return;
			}
			else if(t.equalsIgnoreCase("saveOutputEpanet"))
			{
				epaNetUploader.uploadEpanetOutputFile(request,response);
				return;
			}
			else if(t.equalsIgnoreCase("saveMapSnapshot"))
			{
				mapSnapshotUploader.uploadMapSnapshotFile(request,response);
				return;
			}
		}
		//--------------------------FILE UPLOAD HANDLING:END----------------------------------
		//------------------Optimization Process:START----------------------------------------
		PrintWriter out = response.getWriter();
		try{
			String place = request.getParameter("place");
			if(place!=null){
				request.setAttribute("place", place);
			}
			
			String currVersion = version;
			String clientVersion = request.getParameter("version");
			boolean clientOld = versionComparator.compareVersion(currVersion,clientVersion) != 0;
			
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
			
			//System.out.println(request.getParameter("nodes"));
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
			
			BranchOptimizer opt = new BranchOptimizer(nodes, pipes, commercialPipes, generalProperties, esrGeneralProperties, esrCostsArray, pumpGeneralProperties, pumpManualArray, valves);

			boolean solved = opt.Optimize();

			Network network = new Network(request);

			String message;
			if(solved)
			{
				double secondaryFlowFactor = generalProperties.supply_hours/esrGeneralProperties.secondary_supply_hours;
				double esrcapacityfactor = 1;
				if(esrGeneralProperties.esr_enabled){
					esrcapacityfactor = esrGeneralProperties.esr_capacity_factor;
				}
				List<NodeStruct> resultNodes = new ArrayList<NodeStruct>();
				for(Node node : opt.getNodes().values())
				{
					double demand = node.getDemand();
					if(node.getESR()!=node.getNodeID())
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
				
				
				for(Pipe pipe : opt.getPipes().values()){
					double dia = pipe.getDiameter();
					double dia2 = pipe.getDiameter2();
					PipeCost chosenPipeCost = pipe.getChosenPipeCost();
					PipeCost chosenPipeCost2 = pipe.getChosenPipeCost2();
					double length = pipe.getLength() - pipe.getLength2();
					if(!pipe.existingPipe())
					{
						if(!cumulativePipeLength.containsKey(chosenPipeCost))
							cumulativePipeLength.put(chosenPipeCost, length);
						else
							cumulativePipeLength.put(chosenPipeCost, cumulativePipeLength.get(chosenPipeCost)+length);
					}

					double flow = pipe.isAllowParallel() && dia2!=0 ? 
							pipe.getFlow() / (1 + (pipe.getRoughness2()/pipe.getRoughness())*Math.pow(dia2/dia, 4.87/1.852)) :
							pipe.getFlow();
					
					double parallelFlow = pipe.getFlow() - flow;
					if(pipe.getFlowchoice()==FlowType.SECONDARY){
						flow = flow * secondaryFlowFactor;
					}
					
					double headloss = Util.HWheadLoss(length, flow, pipe.getRoughness(), dia);								
					double headlossperkm = headloss*1000/length;
					double speed = Util.waterSpeed(flow, dia); 
					
					double cost; 
					if(pipe.existingPipe())
						cost = 0;
					else
						cost = length * chosenPipeCost.getCost();
					
					Node startNode = pipe.getStartNode();
					Node endNode = pipe.getEndNode();
					
					boolean pressureExceeded = false;
					if(generalProperties.max_pipe_pressure > 0){
						if(startNode.getPressure() > generalProperties.max_pipe_pressure || 
						   endNode.getPressure() > generalProperties.max_pipe_pressure){
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
														 pipe.getFlowchoice()==FlowType.PRIMARY,
														 pipe.getPumpHead(),
														 pipe.getPumpPower(),
														 pipe.getValveSetting()
														);
					resultPipes.add(resultPipe);
					if(dia2!=0)
					{
						double length2 = pipe.isAllowParallel() ? length : pipe.getLength2();
						if(!cumulativePipeLength.containsKey(chosenPipeCost2))
							cumulativePipeLength.put(chosenPipeCost2, length2);
						else
							cumulativePipeLength.put(chosenPipeCost2, cumulativePipeLength.get(chosenPipeCost2)+length2);
						
						double flow2 = pipe.isAllowParallel() ?
								parallelFlow : 
								pipe.getFlow();
						
						if(pipe.getFlowchoice()==FlowType.SECONDARY){
							flow2 = flow2 * secondaryFlowFactor;
						}
						
						double headloss2 = Util.HWheadLoss(length2, flow2, pipe.getRoughness2(), dia2);								
						double headlossperkm2 = headloss2*1000/length2;
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
												 pipe.getFlowchoice()==FlowType.PRIMARY,
												 pipe.getPumpHead(),
												 pipe.getPumpPower(),
												 pipe.getValveSetting()
													);
						resultPipes.add(resultPipe);
					}
					
					if(pumpGeneralProperties.pump_enabled && pipe.getPumpPower()>0){
						double presentvaluefactor = Util.presentValueFactor(pumpGeneralProperties.discount_rate, pumpGeneralProperties.inflation_rate, pumpGeneralProperties.design_lifetime);		
						double primarycoeffecient = 365*presentvaluefactor*generalProperties.supply_hours*pumpGeneralProperties.energycost_per_kwh*pumpGeneralProperties.energycost_factor;
						double secondarycoeffecient = esrGeneralProperties.esr_enabled ? 365*presentvaluefactor*esrGeneralProperties.secondary_supply_hours*pumpGeneralProperties.energycost_per_kwh*pumpGeneralProperties.energycost_factor : 0;
						double power = pipe.getPumpPower();
						
						double energycost = power* (pipe.getFlowchoice()==FlowType.PRIMARY ? primarycoeffecient : secondarycoeffecient);
						double capitalcost = power*pumpGeneralProperties.capitalcost_per_kw;
						
						
						resultPumps.add(new ResultPumpStruct(pipe.getPipeID(), 
															 pipe.getPumpHead(), 
															 power, 
															 energycost, 
															 capitalcost, 
															 energycost+capitalcost));
					}
				}
				
				double cumulativeCost = 0;
				List<CommercialPipeStruct> resultCost = new ArrayList<CommercialPipeStruct>();
				for(Entry<PipeCost, Double> entry : cumulativePipeLength.entrySet())
				{		
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
				if(esrGeneralProperties.esr_enabled){
					double cumulativeEsrCost = 0;
					for(Node node : opt.getNodes().values())
					{	
						if(node.getESR()==node.getNodeID() && node.getEsrTotalDemand() > 0){
							double cost = node.getEsrCost();
							cumulativeEsrCost += cost;	
							
							boolean hasprimarychild = false;
							for(Pipe pipe : node.getOutgoingPipes()){
								if(pipe.getFlowchoice()==FlowType.PRIMARY){
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
				//System.out.println(resultNodeString);
				
				String resultPipeString = gson.toJson(resultPipes);
				//System.out.println(resultPipeString);
				
				String resultCostString = gson.toJson(resultCost);
				//System.out.println(resultCostString);
				
				String resultEsrCostString = gson.toJson(resultEsrCost);
				//System.out.println(resultEsrCostString);
				
				String resultPumpString = gson.toJson(resultPumps);
				
				//System.out.println(generateRandomInput());
				//String coordinatesString = opt.getCoordinatesString();
				message="{\"status\":\"success\", \"data\":\"Done!\", \"resultnodes\":"+resultNodeString+", \"resultpipes\":"+resultPipeString+", \"resultcost\":"+resultCostString+", \"resultesrcost\":"+resultEsrCostString+", \"resultpumpcost\":"+resultPumpString+"}";					
				//System.out.println(message);
			}
			else
			{
				message="{\"status\":\"error\",\"message\":\"Failed to solve network\"}";
			}
			out.print(message);
			//------------------Optimization Process:END----------------------------------------
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
			String error="{\"status\":\"error\",\"message\":\""+e.getMessage()+"\"}";
			out.print(error);
		}
	}

}
