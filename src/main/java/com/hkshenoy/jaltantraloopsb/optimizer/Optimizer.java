package com.hkshenoy.jaltantraloopsb.optimizer;


import com.hkshenoy.jaltantraloopsb.structs.*;
import com.hkshenoy.jaltantraloopsb.helper.CustomLogger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


// This class is responsible for optimization of the network


public class Optimizer {

	public ArrayList<PipeStruct> resultPipes;
	public ArrayList<CommercialPipeStruct> resultCost;
	public ArrayList<ResultPumpStruct> resultPumps;

	private HashMap<Integer, Node> nodes; // nodeID,node
	private HashMap<Integer, Pipe> pipes; // pipeid,pipe
	private List<PipeCost> pipeCost;
	private List<EsrCost> esrCost;

	//following three are data structures received from server request
	private GeneralStruct generalProperties;
	private EsrGeneralStruct esrGeneralProperties;
	private PumpGeneralStruct pumpGeneralProperties;

	//Datastructures to Hold the information for disconnected Nodes and duplicateLinks if found
	private HashSet<Integer> disconnectedNodes;
	private HashSet<List<Integer> > duplicatedLinks;

	private Node source;    //source node of the network


	CustomLogger customLogger;

//	private static boolean esrCosting = true;
//	private static boolean esrGen = true;
//	private static boolean removeZeroDemandNodes = false;


	//0 : only pipe optmization, used by default if no esr costing
	//1 : esr costing with only demand nodes allowed
	//2 : general esr with any node allowed
	//3 : gen2 with option to remove certain nodes as esr candidates
	//4 : use l_i_j_k instead of l_i_j
	//5 : better way to compute z_i_j
	//6 : replace how to implement constraint s_i_i = 0 => s_k_i = s_k_p
	//7 : replace above constraint with f_k=1 => sum(s_i_j)=0 and s_i_i=0=>sum(s_i_j)=0
	//8 : added pumps and valves to the optimization
	//9 : pruned some ESR cost rows, depending on the potential downstream demand
	//10: remove all sij variables and instead of node based model, use an edge based model
	private int modelNumber = 0;


	//default values are provided for following parameters, but are typically overridden by user in the server request

	// flow in secondary = factor * flow in primary
	// primary pumping hours = factor * secondary pumping hours
	private double secondaryFlowFactor = 2;

	// ratio of size of ESR to the daily demand of the nodes it serves
	private double esrCapacityFactor = 1;

	// minimum and maximum height allowed for an ESR
	private double maxEsrHeight = 25;

	// minimum and maximum power allowed for a pump
	private double minPumpPower = 1;
	private double maxPumpPower = 10000;

	// maximum pressure head that can be provided by a pump
	private int maxPumpHead = 10000;

	//container for pump and valve information
	private PumpManualStruct[] pumpManualArray;
	private ValveStruct[] valves;
	String SOLVER_ROOT_DIR;
	private double vmax;

	//create an instance of optimizer
	public Optimizer(String solverRootDir,NodeStruct[] nodeStructs, PipeStruct[] pipeStructs, CommercialPipeStruct[] commercialPipeStructs, GeneralStruct generalStruct, EsrGeneralStruct esrGeneralProperties, EsrCostStruct[] esrCostsArray, PumpGeneralStruct pumpGeneralProperties, PumpManualStruct[] pumpManualArray, ValveStruct[] valves) throws Exception {
		nodes = new HashMap<Integer, Node>();
		pipes = new HashMap<Integer, Pipe>();
		pipeCost = new ArrayList<PipeCost>();
		esrCost = new ArrayList<EsrCost>();
		generalProperties = generalStruct;
		this.SOLVER_ROOT_DIR = solverRootDir;
		this.vmax = generalProperties.max_water_speed;

		if(this.vmax==0.0){
			this.vmax=100;
		}
		int[] a = {};
		this.pumpGeneralProperties = new PumpGeneralStruct(false, 1, 100, 0, 0, 1, 0, 0, 1, a);

		Set<Integer> usedNodeIDs = new HashSet<Integer>();
		customLogger=new CustomLogger();
		// Initialize source node
		// NOTE: Pressure at each node = "Head - Elevation"
		//       However, minimum pressure required at the source is always set to 0 irrespective of the
		//       value of `generalProperties.min_node_pressure`. This is based on network file analysis.
		//       For more details, have a look at `createNetworkFile()` method where "E = Elevation of
		//       each node" is being written to the output file.
		source = new Node(
				generalProperties.source_elevation,
				0.0,
				generalProperties.source_nodeid,
				0,  // generalProperties.source_head - generalProperties.source_elevation
				generalProperties.source_nodename,
				24 / generalProperties.supply_hours,
				usedNodeIDs
		);
		source.setAllowESR(false);
		usedNodeIDs.add(source.getNodeID());
		source.setHead(generalProperties.source_head);
		nodes.put(source.getNodeID(), source);

		//initialize all the other nodes
		double totalDemand = 0.0;
		for (NodeStruct node : nodeStructs) {
			double minPressure = node.minpressure == 0 ? generalProperties.min_node_pressure : node.minpressure;
			Node n = new Node(node.elevation, node.demand, node.nodeid, minPressure, node.nodename, 24 / generalProperties.supply_hours, usedNodeIDs);
			usedNodeIDs.add(n.getNodeID());
			nodes.put(n.getNodeID(), n);
			totalDemand += n.getDemand();
		}
		// NOTE: Demand of source node = -1 * sum(demand of other nodes)
		//       This is based on network file analysis
		// nodes.get(source.getNodeID()).setDemand(-totalDemand);
		this.source.setDemand(-totalDemand);  // This will update the element pointing to the source node in `nodes` HashMap as well

		Set<Integer> usedPipeIDs = new HashSet<Integer>();

		//initialize the pipes
		for (PipeStruct pipe : pipeStructs) {
			double roughness = pipe.roughness == 0 ? generalProperties.def_pipe_roughness : pipe.roughness;

			Node startNode = nodes.get(pipe.startnode);
			if (startNode == null) {
				throw new Exception("Invalid startNode:" + pipe.startnode + " provided for pipe ID:" + pipe.pipeid);
			}

			Node endNode = nodes.get(pipe.endnode);
			if (endNode == null) {
				throw new Exception("Invalid endNode:" + pipe.endnode + " provided for pipe ID:" + pipe.pipeid);
			}

			Pipe p = new Pipe(pipe.length, startNode, endNode, pipe.diameter, roughness, pipe.pipeid, pipe.parallelallowed, usedPipeIDs);
			usedPipeIDs.add(p.getPipeID());
			pipes.put(p.getPipeID(), p);
		}

		//initialize the commercial pipe information
		for (CommercialPipeStruct commercialPipe : commercialPipeStructs) {
			double roughness = commercialPipe.roughness == 0 ? generalProperties.def_pipe_roughness : commercialPipe.roughness;
			pipeCost.add(new PipeCost(commercialPipe.diameter, commercialPipe.cost, Double.MAX_VALUE, roughness));
		}
		this.valves = valves;

		//default model number is 0 for only pipe optimization
		modelNumber = 0;

		//if ESR optimization enabled, initialize ESR properties and set modelnumber
		if (esrGeneralProperties != null && esrGeneralProperties.esr_enabled) {
			this.esrGeneralProperties = esrGeneralProperties;

			if (esrGeneralProperties.secondary_supply_hours == 0) {
				throw new Exception("ESR option is enabled, but secondary supply hours is provided as zero.");
			}

			if (esrGeneralProperties.esr_capacity_factor == 0) {
				throw new Exception("ESR option is enabled, but esr capacity factor is provided as zero.");
			}

			secondaryFlowFactor = generalProperties.supply_hours / esrGeneralProperties.secondary_supply_hours;
			esrCapacityFactor = esrGeneralProperties.esr_capacity_factor;
			maxEsrHeight = esrGeneralProperties.max_esr_height;

			modelNumber = 9;

			for (EsrCostStruct esrcost : esrCostsArray) {
				esrCost.add(new EsrCost(esrcost.mincapacity,
						esrcost.maxcapacity,
						esrcost.basecost,
						esrcost.unitcost));
			}
		}

		//if pump enabled, initialize pump properties
		if (pumpGeneralProperties != null && pumpGeneralProperties.pump_enabled) {
			this.pumpGeneralProperties = pumpGeneralProperties;
			this.pumpManualArray = pumpManualArray;
			this.minPumpPower = pumpGeneralProperties.minpumpsize;

			if (pumpGeneralProperties.efficiency == 0)
				throw new Exception("Pump option is enabled, but pump efficiency is provided as zero.");

			if (pumpGeneralProperties.design_lifetime == 0)
				throw new Exception("Pump option is enabled, but design lifetime is provided as zero.");
		}

		//set total demand required for the network
		totalDemand = getTotalCapacity();
	}

	public GeneralStruct getGeneralProperties() {
		return generalProperties;
	}

	public EsrGeneralStruct getEsrGeneralProperties() {
		return esrGeneralProperties;
	}

	public PumpGeneralStruct getPumpGeneralProperties() {
		return pumpGeneralProperties;
	}

	public String getGeneralPropertiesProjectName() {
		return generalProperties.name_project;
	}

	public String getGeneralPropertiesOrganizationName() {
		return generalProperties.name_organization;
	}


	public double getVmax(){
		return this.vmax;
	}





	//function to check the duplicate links and return true if duplicate links are found.
	private boolean checkDuplicateLinks(){
		HashSet<List<Integer>> linkSet=new HashSet<List<Integer>>();
		duplicatedLinks=new HashSet<List<Integer>>();

		for (Map.Entry<Integer, Pipe> mapElement:pipes.entrySet()){
			Pipe pipe=mapElement.getValue();
			int startNode=pipe.getStartNode().getNodeID();
			int endNode=pipe.getEndNode().getNodeID();

			if(linkSet.contains(List.of(startNode,endNode))==true){
				duplicatedLinks.add(List.of(startNode,endNode));
			}

			linkSet.add(List.of(startNode,endNode));

		}

		if(duplicatedLinks.size()>0){
			return true;
		}
		return false;
	}

	/**
	 * Check whether the network structure is valid or not
	 *
	 * @return int <br>
	 * 1 => Valid Input: Acyclic graph<br>
	 * 2 => Valid Input: Cyclic graph<br>
	 * 3 => Invalid Input: Disconnected graph (i.e. nodes disconnected from the network)<br>
	 * 4 => Invalid Input: Source Head is less than Source Elevation
	 * 5 => Invalid Input: Duplicate Links found
	 */
	//recursive dfs to check the connectivity of graph
	void dfs(Node current,HashMap<Node,ArrayList<Node> > undirectedNetwork,HashSet<Integer> visited){
		customLogger.logd("visited nodes: "+current.getNodeID());
		visited.add(current.getNodeID());

		for(Node nodes:undirectedNetwork.get(current)){
			if(visited.contains(nodes.getNodeID())==false){
				dfs(nodes,undirectedNetwork,visited);
			}
		}
	}

	//Function to check and populate the disconnected nodes in the network
	private void PopulateDisconnectedNode(HashSet<Integer> seen){

		disconnectedNodes=new HashSet<Integer>();
		for (Map.Entry<Integer, Node> mapElement : nodes.entrySet()) {
			Integer key = mapElement.getKey();
			Node node = mapElement.getValue();
			if(seen.contains(node.getNodeID()) == false){
				disconnectedNodes.add(node.getNodeID());
				customLogger.logd("diconnected nodes: "+node.getNodeID());
			}
		}
		// for( int n: disconnectedNodes){
		// 	customLogger.logd("seen:"+n);
		// }
	}

	//creates the  undirected graph and checks the connectivity by dfs
	private boolean checkConnectivity(){
		//creating undirected graph
		HashMap<Node,ArrayList<Node> > undirectedNetwork=new HashMap<Node,ArrayList<Node>>();
		for(Map.Entry<Integer, Node> mapElement:nodes.entrySet()) {
			Node node = mapElement.getValue();
			undirectedNetwork.put(node, new ArrayList<Node>());
		}

		for(Map.Entry<Integer, Node> mapElement:nodes.entrySet()) {
			Node node = mapElement.getValue();
			for (Pipe pipe : node.getOutgoingPipes()) {

				//Adding undirected edge between both the endpoint of pipe
				Node endNode=pipe.getEndNode();
				undirectedNetwork.get(node).add(endNode);
				undirectedNetwork.get(endNode).add(node);

			}
		}
		HashSet<Integer> visited=new HashSet<Integer>();
		dfs(source,undirectedNetwork,visited);
		// for( Node n: visited){
		// 	customLogger.logd("visited:"+n.getNodeID());
		// }

		PopulateDisconnectedNode(visited);

		if(disconnectedNodes.size()>0){
			return false;
		}

		return true;
	}

	private int validateNetwork() {
		if (!(this.source.getHead() >= this.source.getElevation())) {
			return 4;
		}


		if(checkConnectivity()==false){
			return 3;
		}

		if(checkDuplicateLinks()==true){
			return 5;
		}

		Node root = source;

		HashSet<Node> seen = new HashSet<>();
		Stack<Node> left = new Stack<>();
		left.add(root);

		while (!left.isEmpty()) {
			Node top = left.pop();
			if (seen.contains(top)) {
				return 2; // cycle
			}
			seen.add(top);
			for (Pipe pipe : top.getOutgoingPipes()) {
				left.push(pipe.getEndNode());
			}
		}

		// if (seen.size() != nodes.size()) {
		// 	customLogger.logd("seen="+seen.size());
		// 	customLogger.logd("nodes="+nodes.size());

		// // 	// PopulateDisconnectedNode(seen);

		// 	return 3;  // not fully connected
		// }
		return 2;
	}

	//return the total ESR capacity required in the network in litres
	private double getTotalCapacity() {
		double sum = 0;
		for (Node n : nodes.values()) {
			sum = sum + n.getRequiredCapacity(esrCapacityFactor);
		}
		return sum;
	}

	// ---

	public static int attempts=0;

	// This should point to the root of the repository: Jaltantra-Code-and-Scripts
//	@Value("${solver.root.dir}")
//	String SOLVER_ROOT_DIR;



	//static final String SOLVER_ROOT_DIR="/home/deploy/dev_v4";

	// static final String SOLVER_ROOT_DIR = "/home/sid/Jaltantra_loop/JalTantra-Code-and-Scripts";
	// This directory is for temporary use by the method `createNetworkFile()`
	static final String SOLVER_1_NEW_FILE_DIR = "./DataNetworkGraphInput";
	// If `createNetworkFile()` executes successfully, then the created network
	// file will be moved from `SOLVER_1_NEW_FILE_DIR` to this directory
	static final String SOLVER_2_HASH_FILE_DIR = "./DataNetworkGraphInput_hashed";
	// This directory is used by the Python script "CalculateNetworkCost.py"
	// REFER: `OUTPUT_DIR_LEVEL_0` in "CalculateNetworkCost.py"
	static final String SOLVER_3_AUTO_SOLVE_SCRIPT_DIR = "./NetworkResults";

	// Amount of time "CalculateNetworkCost.py" should execute the solver(s) for the network file
	static String SOLVER_EXECUTION_TIME = "00:05:00";
	static String SOLVER_EXECUTION_TIME_DISPLAY_STR = "5 minutes";

	static String list_of_times[]={"00:10:00","00:20:00"};

	static String run_Time = "";
	static String versionNumber = "";
	static String project_Name = "";
	/**
	 * Optimize the network. And, if done successfully, the results are stored in three ArrayLists,
	 * namely resultPipes, resultCost and resultPumps.
	 *
	 * @return whether network was solved successfully results are ready (`true`) or not (`false`)
	 * @throws Exception in case of any error/problem or to convey any status information
	 */
	public boolean Optimize(String runTime, String projectName) throws Exception {

		// Execution flow:
		//   1. Create the data files for the network
		//   2. Asynchronously launch `CalculateNetworkCost.py` for the data files

		run_Time=runTime;
		// versionNumber = version;
		project_Name = projectName;

		if(runTime.equals("1hour")){
			SOLVER_EXECUTION_TIME="01:00:00";
			SOLVER_EXECUTION_TIME_DISPLAY_STR="1 hour";
		}

		if(runTime.equals("5min")){
			SOLVER_EXECUTION_TIME="00:05:00";
			SOLVER_EXECUTION_TIME_DISPLAY_STR="5 minutes";
		}
		if(runTime.equals("1min")){
			SOLVER_EXECUTION_TIME="00:01:00";
			SOLVER_EXECUTION_TIME_DISPLAY_STR="1 minutes";
		}

		// Validate the network layout
		customLogger.logd("Network validation started...");

		final int networkValidationResult = validateNetwork();
		customLogger.logd("Network validation complete..., networkValidationResult = " + networkValidationResult);
		if (networkValidationResult == 1 || networkValidationResult == 2) {
			final String networkFileResult = createNetworkFile(); // 0-hashedfilename.R
			final String networkFileStatus = networkFileResult.substring(0, networkFileResult.indexOf("-"));
			final String networkFileName = networkFileResult.substring(2);
			String networkFileHash = networkFileName.substring(0, networkFileName.lastIndexOf("."));

			/**
			 *
			 here prompt the user to save the network file before clicking the optimize button
			 logic:
			 - create a temp file to see whether this is the first time to prompt the user,
			 if it exists then do not prompt the user otherwise prompt the user
			 *
			 */

			customLogger.logi("Solver root dir : "+ SOLVER_ROOT_DIR);
			String promptFilePAth=SOLVER_ROOT_DIR+"/"+SOLVER_2_HASH_FILE_DIR+"/"+networkFileHash+"_prompt.txt";
			boolean promptFileExist=checkFileExist(promptFilePAth);

			if(!promptFileExist){
				customLogger.logi("Please save the network file, if not, before continuing the optimization");
				throw new Exception("Please save the network file, if not, before continuing the optimization and click on optimize button again");
			}

			String previousHash=networkFileHash;

			networkFileHash=networkFileHash+runTime;

			boolean statusFileExists = (new File(SOLVER_ROOT_DIR + "/" + SOLVER_3_AUTO_SOLVE_SCRIPT_DIR + "/" + networkFileHash + "/0_status")).exists();

			// if the statusFileExists, but previously there was no output and the content of this file was false
			// then we need to re run the jaltantra for this network
			boolean resultFileContent=true;

			String resultFilePath=SOLVER_ROOT_DIR + "/" + SOLVER_3_AUTO_SOLVE_SCRIPT_DIR + "/" + networkFileHash + "/0_result.txt";
			String previousRunPath=SOLVER_ROOT_DIR + "/" + SOLVER_2_HASH_FILE_DIR + "/" + networkFileName + "_previous_run.txt";
			int cntlinesInResultFile=0;

			File resultfile = new File(resultFilePath);
			File previousRunFile=new File(previousRunPath);

			String message="";

			customLogger.logd("networkFileHash = " + networkFileHash + ", statusFileExists = " + statusFileExists);

			if (networkFileStatus.equals("0") || statusFileExists == false ) {
				customLogger.logi("Starting the solvers (CalculateNetworkCost.py) for network file '" + networkFileResult + "'");
				launchCalculateNetworkCost(SOLVER_ROOT_DIR + "/" + SOLVER_2_HASH_FILE_DIR + "/" + networkFileName);
				throw new Exception("The solver is working, refresh the page after " + SOLVER_EXECUTION_TIME_DISPLAY_STR + " to see the results");
			} else if (networkFileStatus.equals("2")) {
				// Check which of the following case is true:
				//   - solvers are running
				//   - solvers are done executing, but with an error
				//   - results already generated
				int status = checkSolverResultStatus(SOLVER_ROOT_DIR + "/" + SOLVER_3_AUTO_SOLVE_SCRIPT_DIR + "/" + networkFileHash);
				if (status == -1) {
					customLogger.loge("CHECKME: FIXME: Probably failed to start the solver launch script, or 'CalculateNetworkCost.py' failed to create the status file");

					// here we will check for the launching error of the file, if there was then delete the previous directory

					String pathToFile=SOLVER_ROOT_DIR + "/" + SOLVER_3_AUTO_SOLVE_SCRIPT_DIR + "/" + networkFileHash;
					deleteNetworkResults(pathToFile);

					throw new Exception("Internal server error: failed to start the solver launch script, click on optimize button again");
				} else if (status == 0) {
					customLogger.loge("FIXME: Failed to start the solver for network file: " + networkFileResult);
					throw new Exception("Internal server error: failed to start the solver for this network");
				} else if (status == 1) {
					String Baronm1filePath=SOLVER_ROOT_DIR+"/"+SOLVER_3_AUTO_SOLVE_SCRIPT_DIR+"/"+networkFileHash+"/baron_m1_"+previousHash+"/"+"std_out_err.txt";
					String Baronm2filePath=SOLVER_ROOT_DIR+"/"+SOLVER_3_AUTO_SOLVE_SCRIPT_DIR+"/"+networkFileHash+"/baron_m2_"+previousHash+"/"+"std_out_err.txt";

					File baronM1File = new File(Baronm1filePath);
					File baronM2File=new File(Baronm2filePath);

					String time_passed="";

					String results_for_baron_m1[];
					String cost_for_baron_m1="";

					String results_for_baron_m2[];
					String cost_for_baron_m2="";

					String optimalCost="";

					results_for_baron_m1=getIntermediateResults(baronM1File);
					results_for_baron_m2=getIntermediateResults(baronM2File);

					cost_for_baron_m1=results_for_baron_m1[0];
					cost_for_baron_m2=results_for_baron_m2[0];

					// done as baron m2 is processed later
					time_passed=results_for_baron_m2[1];
					if(time_passed.length() == 0) time_passed=results_for_baron_m1[1];
					if(cost_for_baron_m1.length() > 0){
						System.out.println("baron m1 cost : "+cost_for_baron_m1+" baron m2 cost : "+cost_for_baron_m2 +"time passed : "+time_passed);
						if(cost_for_baron_m1.compareTo(cost_for_baron_m2) >= 0){
							optimalCost=cost_for_baron_m2;
						}
						else optimalCost=cost_for_baron_m1;
					}

					String log_file=SOLVER_ROOT_DIR +"/" + SOLVER_2_HASH_FILE_DIR + "/" + previousHash + ".R" + runTime + ".log";
					File log_file_path = new File(log_file);
					String time_to_display[] = getIntermediateTime(log_file_path);

					if(time_to_display[0].length() > 0) {

						String timeFinished = time_to_display[0];
						String timeLeft = time_to_display[1];
						customLogger.logi("Solver is running for this network. Please wait..., current cost : "+optimalCost);
						throw new Exception("Solver is running for this network. Please wait...,  "+timeFinished+" passed, remaining Time : "+timeLeft);

					}
					else{
						customLogger.logi("Solver is running for this network. Please wait...");
						throw new Exception("Solver is running for this network. Please wait...");
					}

					// CustomLogger.logi("Solver is running for this network. Please wait...");
					// throw new Exception("Solver is running for this network. Please wait...");
				} else if (status == 2) {

					if(true/*!runTime.equals("5min")*/){

						//throw new Exception("Either no feasible solution found, or failed to solve the network in " + SOLVER_EXECUTION_TIME_DISPLAY_STR+" ");

						throw new Exception("Either no feasible solution found, or failed to solve the network in " + SOLVER_EXECUTION_TIME_DISPLAY_STR+" "+
								  "\n Few tips:" +
								  "\n 1.Increase the time for solver" +
								  "\n 2.Increase the Maximum velocity (if provided)"+
						          "\n 3.Add higher diameter pipes in set of commerically available pipes");
					}

					customLogger.loge("CHECKME: The solvers finished the execution, but failed to get the result. " +
							"Either some unknown error, or no feasible solution found, or failed to solve the network, click optimize again and check after " + SOLVER_EXECUTION_TIME_DISPLAY_STR+" minutes");
					//creating an attempts file and relaunching the network with the updated time
					String pathToattemptsFile=SOLVER_ROOT_DIR+"/"+SOLVER_2_HASH_FILE_DIR+"/"+networkFileName;
					File file = new File(pathToattemptsFile+"_attemptsFile.txt");
					if (!file.exists()) {
						try {
							file.createNewFile();
							// Write to a file
							String content = "0";
							attempts=0;
							try (FileWriter writer = new FileWriter(file)) {
								writer.write(content);
								System.out.println("File written successfully.");
							} catch (IOException e) {
								System.out.println("Error writing to file: " + e.getMessage());
							}
							System.out.println("File created successfully.");
						} catch (IOException e) {
							System.out.println("Error creating file: " + e.getMessage());
						}
					} else {
						// Read from a file
						try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
							String line = reader.readLine();
							attempts=Integer.parseInt(line);
							System.out.println("File content: " + line);
						} catch (IOException e) {
							System.out.println("Error reading from file: " + e.getMessage());
						}
						System.out.println("File already exists.");
					}

					for(int i=attempts;i<list_of_times.length;i++){
						SOLVER_EXECUTION_TIME=list_of_times[attempts];
						attempts++;
						try (FileWriter writer = new FileWriter(file)) {
							writer.write(""+attempts);
							System.out.println("File written successfully.");
						} catch (IOException e) {
							System.out.println("Error writing to file: " + e.getMessage());
						}
						System.out.println("system is running for "+SOLVER_EXECUTION_TIME+"minutes, attempt number "+attempts);

						//delete previously existed network results
						String pathToFile=SOLVER_ROOT_DIR + "/" + SOLVER_3_AUTO_SOLVE_SCRIPT_DIR + "/" + networkFileHash;
						deleteNetworkResults(pathToFile);
						statusFileExists = (new File(SOLVER_ROOT_DIR + "/" + SOLVER_3_AUTO_SOLVE_SCRIPT_DIR + "/" + networkFileHash + "/0_status")).exists();
						launchCalculateNetworkCost(SOLVER_ROOT_DIR + "/" + SOLVER_2_HASH_FILE_DIR + "/" + networkFileName);
						status = checkSolverResultStatus(SOLVER_ROOT_DIR + "/" + SOLVER_3_AUTO_SOLVE_SCRIPT_DIR + "/" + networkFileHash);
						// System.out.println("system is now executing for 10 minutes");
						if (status == 3) {
							customLogger.logi("Extracting the result");
							boolean ok = extractSolverResult(SOLVER_ROOT_DIR + "/" + SOLVER_3_AUTO_SOLVE_SCRIPT_DIR + "/" + networkFileHash);
							if (ok) return true;
							customLogger.loge("CHECKME: FIXME: extractSolverResult(...) return false for network file with hash =" + networkFileHash);
							String pathToFolder=SOLVER_ROOT_DIR + "/" + SOLVER_3_AUTO_SOLVE_SCRIPT_DIR + "/" + networkFileHash;
							deleteNetworkResults(pathToFolder);
							throw new Exception("Internal server error: result extraction failed for the network file with hash =" + networkFileHash+" click optimize button again or after some time to rerun the file");
						}
						SOLVER_EXECUTION_TIME_DISPLAY_STR=""+getMinutesToDisplay(SOLVER_EXECUTION_TIME);
						throw new Exception("Either no feasible solution found, or failed to solve the network, retrying for " + SOLVER_EXECUTION_TIME_DISPLAY_STR+" minutes");
					}
					if(attempts > 0){
						SOLVER_EXECUTION_TIME_DISPLAY_STR=""+getMinutesToDisplay(list_of_times[attempts-1]);
					}
					throw new Exception("Either no feasible solution found, or failed to solve the network in " + SOLVER_EXECUTION_TIME_DISPLAY_STR+" minutes");

				} else if (status == 3) {
					customLogger.logi("Extracting the result");
					boolean ok = extractSolverResult(SOLVER_ROOT_DIR + "/" + SOLVER_3_AUTO_SOLVE_SCRIPT_DIR + "/" + networkFileHash);
					if (ok) return true;
					customLogger.loge("CHECKME: FIXME: extractSolverResult(...) return false for network file with hash =" + networkFileHash);
					String pathToFolder=SOLVER_ROOT_DIR + "/" + SOLVER_3_AUTO_SOLVE_SCRIPT_DIR + "/" + networkFileHash;
					deleteNetworkResults(pathToFolder);
					throw new Exception("Internal server error: result extraction failed for the network file with hash =" + networkFileHash+" click optimize button again or after some time to get the results");
				} else {
					customLogger.loge("CHECKME: FIXME: Unexpected return value from `checkSolverResultStatus()`. deleted the past execution result for hash =" + networkFileHash+" , click the optimize button again");

					String pathToFolder=SOLVER_ROOT_DIR + "/" + SOLVER_3_AUTO_SOLVE_SCRIPT_DIR + "/" + networkFileHash;
					deleteNetworkResults(pathToFolder);

					throw new Exception("Internal server error: unknown execution status. Need to delete the past execution result for hash =" + networkFileHash+" , click the optimize button again");
				}
			} else {
				customLogger.loge("FIXME: `createNetworkFile()` method returned unexpected value ' " + networkFileResult + " '");
				throw new Exception("Internal server error: createNetworkFile() method failed, probably due to unexpected change in some method(s)");
			}
		}
		else if (networkValidationResult == 3) {


			throw new Exception("Input is not valid. Nodes unconnected in the network, Following nodes are disconnected "+disconnectedNodes.toString());
		} else if (networkValidationResult == 4) {
			throw new Exception("Source Head (" + this.source.getHead() + ") should be greater than or equal to Source Elevation (" + this.source.getElevation() + "). Please fix it in the 'General' section");
		}else if (networkValidationResult == 5) {
			throw new Exception("Duplicate Links found \n"+ "Following Links are repeated " + duplicatedLinks.toString());
		} else {
			customLogger.loge("FIXME: network validation returned unexpected value " + networkValidationResult);
			throw new Exception("Internal server error: network validation failed");
		}
		// There is NO case in which the program will reach this point
		// return true;
	}

	public static String getTimeToDisplay(String timeInSec){
		String resultTime="";
		int sec=(int)(Float.parseFloat(timeInSec));

		int hours=sec/3600;
		sec=sec%3600;

		int minutes=sec/60;
		sec=sec%60;

		if(hours > 0) resultTime=""+hours+"hours ";
		if(minutes > 0) resultTime=""+minutes+"minutes "+sec+" seconds";
		else resultTime="00 minutes"+sec+" seconds";

		return resultTime;
	}
	public static boolean checkFileExist(String filePath){

		boolean exists=false;

		File file = new File(filePath);
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				System.out.println("Error creating file: " + e.getMessage());
			}
		}
		else exists=true;

		return exists;
	}

	public static String[] getIntermediateTime(File file){
		String timeFinished="";
		String timeLeft="";
		try {
			Scanner scanner = new Scanner(file);
			String lastLine="";
			// read the contents of the file line by line
			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				Pattern pattern = Pattern.compile("Time Finished =");
				Matcher matcher = pattern.matcher(line);
				if(matcher.find()){
					lastLine = line;
				}
			}
			lastLine=lastLine.trim();
			System.out.println("last line : "+lastLine);

			Pattern pattern = Pattern.compile("Time Finished = (\\d{1,2}:\\d{2}:\\d{2}), Time Left = (\\d{1,2}:\\d{2}:\\d{2})");
			Matcher matcher = pattern.matcher(lastLine);
			if (matcher.find()) {
				timeFinished = matcher.group(1);
				timeLeft = matcher.group(2);
				System.out.println("Time Finished: " + timeFinished);
				System.out.println("Time Left: " + timeLeft);
			}
			System.out.println("Time finished : " + timeFinished + "Time left : " + timeLeft);
			scanner.close();

		} catch (FileNotFoundException e) {
			System.out.println("Either File not found or not able to extract time: " + e.getMessage());
		}
		return new String[]{timeFinished , timeLeft};
	}

	public static String[] getIntermediateResults(File file){
		String res="";
		String Remainingtime="";
		try {
			Scanner scanner = new Scanner(file);
			String lastLine="";
			// read the contents of the file line by line
			while (scanner.hasNextLine()) {
				lastLine = scanner.nextLine();
			}
			lastLine=lastLine.trim();
			System.out.println("last line : "+lastLine);
			String temp[]=lastLine.split("\\s+");
			if(temp.length > 4) {
				try {
					double number = Double.valueOf(temp[temp.length-1]);
					double time = Double.valueOf(temp[temp.length-3]);
					res=""+number;
					Remainingtime=""+time;
				}
				catch (NumberFormatException e) {
					System.out.println("There is an exception");
				}
			}
			System.out.println("number : "+res);
			scanner.close();

		} catch (FileNotFoundException e) {
			System.out.println("File not found: " + e.getMessage());
		}
		return new String[]{res,Remainingtime};
	}

	public void deleteNetworkResults(String pathToHashedNetwork){
		File directory = new File(pathToHashedNetwork);

		if (directory.isDirectory()) {
			File[] files = directory.listFiles();
			for (File file : files) {
				file.delete();
			}
			directory.delete();
			System.out.println("Directory deleted successfully.");
		} else {
			System.out.println("Error: Not a directory.");
		}
	}

	// public static String parseDate(String dateString,int multiplier) {
	// 	SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
	// 	String updatedTime="";
	// 	try {
	// 		System.out.println(dateString);
	// 		Date date = dateFormat.parse(dateString);
	//
	// 		int doubled=date.getMinutes() * multiplier;
	// 		System.out.println("minutes : "+ doubled);
	//
	// 		int hours = doubled / 60;
	// 		int remainingMinutes = doubled % 60;
	// 		int seconds = 0;
	//
	// 		updatedTime = String.format("%02d:%02d:%02d", hours, remainingMinutes, seconds);
	// 		System.out.println("Time: " + updatedTime);
	//
	// 	} catch (ParseException e) {
	// 		System.out.println("Error parsing date string: " + e.getMessage());
	// 	}
	//
	// 	return updatedTime;
	// }

	public String getMinutesToDisplay(String dateString) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
		int minutes=0;
		try {
			System.out.println(dateString);
			Date date = dateFormat.parse(dateString);

			minutes=date.getMinutes();
			System.out.println("minutes : "+ minutes);


		} catch (ParseException e) {
			System.out.println("Error parsing date string: " + e.getMessage());
		}

		return ""+minutes;
	}

	/**
	 * Create directory if it does not exist.
	 *
	 * @param dirPath path to the directory which is to be created
	 * @throws Exception if directory does not exist and its creation failed
	 */
	private void checkAndCreateDir(final String dirPath) throws Exception {
		// REFER: https://stackoverflow.com/questions/3634853/how-to-create-a-directory-in-java
		File theDir = new File(dirPath);
		if (!theDir.exists()) {
			boolean res = theDir.mkdirs();
			if (!res) {
				customLogger.loge("FIXME: Failed to create directory: '" + dirPath + "'");
				throw new Exception("Internal error at the server, the backend does not have write permission");
			}
		}
	}

	/**
	 * Generate unique filename based on the time at which the function was called and a
	 * random number, such that no file with the same name exists in `baseDirectoryPath`.
	 * <br>
	 * The filename format is: "yyyy-MM-dd_HH-mm-ss.SSS_{i}_{RandomNumber}.R"
	 * Range of i = [0, 9_99_999]
	 * Range of RandomNumber = [10_00_001, 99_99_999]
	 *
	 * @param baseDirectoryPath path to the directory for which a unique filename is to be generated
	 * @return unique filename which does not exist
	 * @throws Exception if unique filename generation fails
	 */
	private String generateUniqueFileName(final String baseDirectoryPath) throws Exception {
		checkAndCreateDir(baseDirectoryPath);

		// REFER: https://www.java-examples.com/formatting-date-custom-formats-using-simpledateformat
		// REFER: https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss.SSS_");
		String strDate = sdf.format(date);

		// REFER: https://stackoverflow.com/questions/5887709/getting-random-numbers-in-java
		// 7 digit random number with range [1000001, 9999999]
		long randomValue = (long) (1000000 + (8999999 * Math.random() + 1));

		for (int i = 0; i < 1000000; ++i) {
			if ((new File(baseDirectoryPath + "/" + strDate + i + "_" + randomValue + ".R")).exists())
				continue;
			return strDate + i + "_" + randomValue + ".R";
		}

		// REFER: https://stackoverflow.com/questions/840190/changing-the-current-working-directory-in-java
		// REFER: https://stackoverflow.com/questions/4871051/how-to-get-the-current-working-directory-in-java
		customLogger.logi("pwd: " + System.getProperty("user.dir"));
		customLogger.loge("CHECKME: FIXME: Could not generate unique filename, date time = " + strDate);
		throw new Exception(
				"Internal error at the server, could not generate a unique filename for the " +
						"request (at " + strDate + "). Probably the server is too loaded. Please try again."
		);
	}

	/**
	 * This method return the digest/hash (in hexadecimal format) of the file passed<br>
	 * REFER: https://www.geeksforgeeks.org/how-to-generate-md5-checksum-for-files-in-java/
	 *
	 * @param digest hashing algorithm to be used
	 * @param file   file object whose hash is to be found
	 * @return Hex representation of the hash
	 * @throws IOException if file read operation fails
	 */
	private static String findFileHashInHex(MessageDigest digest, File file) throws IOException {
		// Get file input stream for reading the file content
		FileInputStream fis = new FileInputStream(file);

		// Create byte array to read data in chunks
		byte[] byteArray = new byte[1024];
		int bytesCount;

		// Read the data from file and update that data in the message digest
		while ((bytesCount = fis.read(byteArray)) != -1) {
			digest.update(byteArray, 0, bytesCount);
		}

		// Close the input stream
		fis.close();

		// Store the bytes returned by the digest() method
		byte[] bytes = digest.digest();

		// This array of bytes has bytes in decimal format, so we need to convert it into hexadecimal format
		// For this we create an object of StringBuilder since it allows us to update the string i.e. its mutable
		StringBuilder sb = new StringBuilder();

		// Loop through the bytes array
		for (byte aByte : bytes) {
			// The following line converts the decimal into hexadecimal format and appends that to the
			// StringBuilder object
			sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
		}



		// Finally, we return the complete hash
		return sb.toString().toLowerCase();
	}

	/**
	 * Read a file completely and return its content.
	 *
	 * @param pathToFile path to the file which is to be read
	 * @return content of the file as String
	 * @throws IOException if readAllBytes(...) fails
	 */
	private static String readFileAsString(final String pathToFile) throws IOException {
		// REFER: https://www.geeksforgeeks.org/different-ways-reading-text-file-java/
		return new String(Files.readAllBytes(Paths.get(pathToFile)));
	}

	/**
	 * Returns the MD5 hash of the newly created network file provided everything goes properly
	 *
	 * @return String<br>
	 * "0-NetworkFileMD5Hash.R" if everything was fine and this is the first request for this network<br>
	 * "2-NetworkFileMD5Hash.R" if everything was fine and a request for this network was received in the past
	 * @throws Exception if network file creation fails due to any reason
	 */
	private String createNetworkFile() throws Exception {
		final String uniqueFileName = generateUniqueFileName(SOLVER_ROOT_DIR + "/" + SOLVER_1_NEW_FILE_DIR);
		final String pathTo1FreshFile = SOLVER_ROOT_DIR + "/" + SOLVER_1_NEW_FILE_DIR + "/" + uniqueFileName;

		// REFER: https://stackoverflow.com/questions/10667734/java-file-open-a-file-and-write-to-it
		BufferedWriter out = null;
		try {
			// Network file creation failed as a file already exists with the same name.
			// Probably, the server is loaded with too many requests.
			if ((new File(pathTo1FreshFile)).exists())
				throw new Exception("Network file creation failed because the server has too many requests running. Please retry after a few moments.");

			out = new BufferedWriter(new FileWriter(pathTo1FreshFile));

			// TODO: NOT SURE, but, this may need to be updated if we have to use `nodeName` instead of `nodeID`
			// NOTE: Set of nodes/vertexes
			out.write("set nodes :=");
			for (Node n : this.nodes.values()) {
				out.write(" " + n.getNodeID());
			}
			out.write(";");

			// NOTE: Set of commercial pipes available
			out.write("\n\nset pipes :=");
			for (int i = 0; i < this.pipeCost.size(); ++i) {
				out.write(" " + i);
			}
			out.write(";");

			// NOTE: Set of arcs/links/edges
			out.write("\n\nparam : arcs : L :=");  // NOTE: L = Total length of each arc/link
			String fixed_arcs_length = "";
			String fixed_arcs_diameter = "";
			String fixed_arcs_roughness = "";
			for (Pipe p : this.pipes.values()) {
				if(p.getDiameter() == 0.0) {
					out.write("\n" + p.getStartNode().getNodeID() + "    " + p.getEndNode().getNodeID() + "    " + p.getLength());
				}
				else{
					fixed_arcs_length = fixed_arcs_length + "\n" + Integer.toString(p.getStartNode().getNodeID()) + "    " + Integer.toString(p.getEndNode().getNodeID()) + "    " + Double.toString(p.getLength());
					fixed_arcs_diameter = fixed_arcs_diameter + "\n" +Integer.toString(p.getStartNode().getNodeID()) + "    " + Integer.toString(p.getEndNode().getNodeID()) + "    " + Double.toString(p.getDiameter());
					fixed_arcs_roughness = fixed_arcs_roughness + "\n" +Integer.toString(p.getStartNode().getNodeID()) + "    " + Integer.toString(p.getEndNode().getNodeID()) + "    " + Double.toString(p.getRoughness());
				}
			}
			out.write(";");

			out.write("\n\nparam E :=");  // NOTE: E = Elevation of each node
			for (Node n : this.nodes.values()) {
				// Selected based on network file analysis
				if (n.getNodeID() == this.source.getNodeID()) {
					// NOTE: It is necessary to use Head instead of Elevation when creating the network file
					//       so that the result of solver matches with the result of the old Jaltantra system
					//       for the input file "Sample_input_cycle_twoloop (Source Elevation changed) (acyclic).xls"
					//       which generates the network file that has md5sum "998a075a3545f6e8045a9c6538dbba2a".
					// NOTE: Constraint number 5 (i.e. "con5") of model "m1" and "m2" is the reason for this.
					//       It asks the solver to directly consider the head of the source to be equal to the
					//       elevation of the source. And, based on input file analysis, it was found that
					//       model files require the minimum pressure for source to be always 0. Hence, it is
					//       not possible to use true Elevation and "Minimum Pressure = Head - Elevation", and
					//       if we do so, then we get presolve error from the solvers.
					// NOTE: The formula "Pressure at each node = Head - Elevation" was found from:
					//       1. Note::getPressure()
					//       2. Analysis of:
					//          "Jaltantra website > Results section > Nodes tab > Elevation, Head and Pressure columns"
					out.write("\n" + n.getNodeID() + "   " + n.getHead());
					continue;
				}
				out.write("\n" + n.getNodeID() + "   " + n.getElevation());
			}
			out.write(";");

			out.write("\n\nparam P :=");  // NOTE: P = Minimum pressure required at each node
			for (Node n : this.nodes.values()) {
				// TODO: Check if we have to use getPressure() or getResidualPressure() method ?
				//       What is the difference between the two ?
				// out.write("\n" + n.getNodeID() + "   " + n.getPressure());
				out.write("\n" + n.getNodeID() + "   " + n.getResidualPressure());  // Selected based on network file analysis
			}
			out.write(";");

			out.write("\n\nparam D :=");  // NOTE: D = Demand of each node
			double totalDemand = 0.0;
			for (Node n : this.nodes.values()) {
				// NOTE: Demand of source node is always 0. Hence, we do not need to handle the source node separately.
				totalDemand += n.getDemand();
			}
			for (Node n : this.nodes.values()) {
				out.write("\n" + n.getNodeID() + "   " + n.getDemand());
			}
			out.write(";");

			out.write("\n\nparam d :=");  // NOTE: d = Diameter of each commercial pipe
			for (int i = 0; i < this.pipeCost.size(); ++i) {
				// NOTE: `this.pipeCost` is of type `ArrayList`, so `this.pipeCost.get(i)` will work in constant time
				out.write("\n" + i + "   " + this.pipeCost.get(i).getDiameter());
			}
			out.write(";");

			out.write("\n\nparam C :=");  // NOTE: C = Cost per unit length of each commercial pipe
			for (int i = 0; i < this.pipeCost.size(); ++i) {
				// NOTE: `this.pipeCost` is of type `ArrayList`, so `this.pipeCost.get(i)` will work in constant time
				out.write("\n" + i + "   " + this.pipeCost.get(i).getCost());
			}
			out.write(";");

			out.write("\n\nparam R :=");  // NOTE: R = Roughness of each commercial pipe
			for (int i = 0; i < this.pipeCost.size(); ++i) {
				// NOTE: `this.pipeCost` is of type `ArrayList`, so `this.pipeCost.get(i)` will work in constant time
				out.write("\n" + i + "   " + this.pipeCost.get(i).getRoughness());
			}
			out.write(";");

			out.write(String.format("\n\nparam Source := %d;\n", this.source.getNodeID()));  // NOTE: Source node ID

			out.write("\n\nparam : F_arcs : F_L :=");
			out.write(fixed_arcs_length);
			out.write(";");

			out.write("\n\nparam F_d :=");
			out.write(fixed_arcs_diameter);
			out.write(";");

			out.write("\n\nparam F_R :=");
			out.write(fixed_arcs_roughness);
			out.write(";");

			//adding param vmax
			out.write("\n\nparam vmax := ");
			for (Pipe p : this.pipes.values()) {
				if(p.getDiameter() == 0.0) {
					out.write("\n" + p.getStartNode().getNodeID() + "    " + p.getEndNode().getNodeID() + "    " + getVmax());
				}
			}
			out.write(";");

		} catch (IOException e) {
			customLogger.loge("Network file creation failed due to IOException:");
			customLogger.loge(e.getMessage());
			throw e;
		} finally {
			if (out != null) {
				out.close();
			}
		}
		// Network file successfully created

		// REFER: https://datacadamia.com/file/hash
		//        `Hash = Digest`
		// REFER: https://www.janbasktraining.com/community/sql-server/explain-the-differences-as-well-as-the-similarities-between-checksum-vs-hash
		//        According to me Hash and Checksum are also same in this context
		// REFER: https://www.geeksforgeeks.org/sha-256-hash-in-java/
		//        This is same as `shasum -a 256 FilePath`
		final String fileHash = findFileHashInHex(MessageDigest.getInstance("SHA-256"), new File(pathTo1FreshFile));

		final String hashedFileName = fileHash + ".R";
		final String pathTo2HashedFileName = SOLVER_ROOT_DIR + "/" + SOLVER_2_HASH_FILE_DIR + "/" + hashedFileName;
		if ((new File(pathTo2HashedFileName)).exists()) {
			customLogger.logi("Request for this network was already submitted in the past: " +
					"it may be in progress\uD83C\uDFC3 or finished\uD83C\uDFC1");
			// Delete this temporary file as this network was already given in the past
			if (!(new File(pathTo1FreshFile)).delete()) {
				customLogger.loge("FIXME: Deletion of temporary network file failed: '" + pathTo1FreshFile + "'");
			}
			return "2-" + hashedFileName;
		}

		checkAndCreateDir(SOLVER_ROOT_DIR + "/" + SOLVER_2_HASH_FILE_DIR);

		// REFER: https://stackoverflow.com/questions/4645242/how-do-i-move-a-file-from-one-location-to-another-in-java
		boolean ok = (new File(pathTo1FreshFile)).renameTo(new File(pathTo2HashedFileName));
		if (!ok) {
			customLogger.loge(String.format("FIXME: Failed to move the file: '%s' -> '%s'", pathTo1FreshFile, pathTo2HashedFileName));
			// Delete this temporary file because the rename operation failed
			if (!(new File(pathTo1FreshFile)).delete()) {
				customLogger.loge("FIXME: Deletion of temporary network file failed: '" + pathTo1FreshFile + "'");
			}
			throw new Exception("Internal server error: problem with write permission");
		}

		customLogger.logi(String.format("File renamed and moved successfully: '%s' -> '%s'", pathTo1FreshFile, pathTo2HashedFileName));

		// customLogger.logi("creating gams m1 and m2 model data file");
		// String solverToGamsm1=SOLVER_ROOT_DIR + "/" + SOLVER_2_HASH_FILE_DIR + "/"+fileHash+"m1.gms";
		// String solverToGamsm2=SOLVER_ROOT_DIR + "/" + SOLVER_2_HASH_FILE_DIR + "/"+fileHash+"m2.gms";
		// createGamsModel(pathTo2HashedFileName,solverToGamsm1,"m1");
		// createGamsModel(pathTo2HashedFileName,solverToGamsm2,"m2");
		// customLogger.logi("finished creating gams m1 and m2 model data file");
		return "0-" + hashedFileName;
	}

	// public void createGamsModel(String amplFilePath,String gamsFilePath,String modelname) throws IOException {
	// 	ArrayList<String> nodes=new ArrayList<>();
	// 	ArrayList<String> pipes=new ArrayList<>();
	// 	ArrayList<String> arcs=new ArrayList<>();
	// 	ArrayList<String> F_arcs=new ArrayList<>();
	// 	ArrayList<String> arcsLength=new ArrayList<>();
	// 	ArrayList<String> elevation=new ArrayList<>();
	// 	ArrayList<String> pressure=new ArrayList<>();
	// 	ArrayList<String> demand=new ArrayList<>();
	// 	ArrayList<String> diameter=new ArrayList<>();
	// 	ArrayList<String> pipeCost=new ArrayList<>();
	// 	ArrayList<String> pipeRoughness=new ArrayList<>();
	// 	ArrayList<String> F_L=new ArrayList<>();
	// 	ArrayList<String> F_d=new ArrayList<>();
	// 	ArrayList<String> F_R=new ArrayList<>();
	// 	Map<String, Double> arcsMap = new LinkedHashMap<>();
	// 	Map<String, Double> F_arcsMap = new LinkedHashMap<>();
	// 	Map<String, Double> F_diameter = new LinkedHashMap<>();
	// 	Map<String, Double> F_roughness = new LinkedHashMap<>();
	// 	String source="";

	// 	File file = new File(amplFilePath);
	// 	Scanner sc = new Scanner(file);

	// 	String data="";
	// 	while(sc.hasNext()){
	// 		String input=sc.nextLine();
	// 		data=data+input+"\n";
	// 	}
	// 	printSetsAndParametersAndModels(gamsFilePath,data,nodes,pipes,arcs,F_arcs,arcsLength,elevation,pressure,demand,diameter,pipeCost,pipeRoughness,F_L,F_d,F_R,arcsMap,F_arcsMap,F_diameter,F_roughness,source,modelname);
	// 	sc.close();
	// }

	// private void printSetsAndParametersAndModels(String gamsFilePath,String data, ArrayList<String> nodes, ArrayList<String> pipes, ArrayList<String> arcs, ArrayList<String> f_arcs, ArrayList<String> arcsLength, ArrayList<String> elevation, ArrayList<String> pressure, ArrayList<String> demand, ArrayList<String> diameter, ArrayList<String> pipeCost, ArrayList<String> pipeRoughness, ArrayList<String> f_l, ArrayList<String> f_d, ArrayList<String> f_r, Map<String, Double> arcsMap, Map<String, Double> f_arcsMap, Map<String, Double> f_diameter, Map<String, Double> f_roughness, String source, String modelname) throws IOException {
	// 	BufferedWriter out = null;

	// 	try {
	// 		FileWriter fstream = new FileWriter(gamsFilePath, true); //true tells to append data.
	// 		out = new BufferedWriter(fstream);

	// 		printSets(out,data,nodes,pipes,arcs,f_arcs,arcsMap,f_arcsMap,f_diameter,f_roughness,source);
	// 		printParameters(out,data,arcsMap,f_arcsMap,f_diameter,f_roughness,arcsLength,diameter,elevation,pressure,demand,pipeCost,pipeRoughness,source);
	// 		if(modelname.equals("m1")){
	// 			printModelm1(out);
	// 		}
	// 		else printModelm2(out);
	// 	}

	// 	catch (IOException e) {
	// 		System.err.println("Error: " + e.getMessage());
	// 	}

	// 	finally {
	// 		if(out != null) {
	// 			out.close();
	// 		}
	// 	}
	// }

	// private void printSets(BufferedWriter out, String data, ArrayList<String> nodes, ArrayList<String> pipes, ArrayList<String> arcs, ArrayList<String> f_arcs, Map<String, Double> arcsMap, Map<String, Double> f_arcsMap, Map<String, Double> f_diameter, Map<String, Double> f_roughness, String source) throws IOException {
	// 	out.write("Sets\n");
	// 	out.write("\t"+"nodes /");

	// 	// Pattern for matching set nodes
	// 	Pattern nodesPattern = Pattern.compile("set nodes :=\\s+(.*)\\s*;");
	// 	// Pattern for matching set pipes
	// 	Pattern pipesPattern = Pattern.compile("set pipes :=\\s+(.*)\\s*;");

	// 	// Find and extract set nodes
	// 	Matcher nodesMatcher = nodesPattern.matcher(data);
	// 	if (nodesMatcher.find()) {
	// 		String nodesStr = nodesMatcher.group(1);
	// 		String[] nodesArr = nodesStr.split("\\s+");
	// 		for (String node : nodesArr) {
	// 			nodes.add(node);
	// 		}
	// 	}

	// 	int idx=0;
	// 	for(String str:nodes){
	// 		if(idx == nodes.size()-1){
	// 			out.write(str);
	// 		}
	// 		else{
	// 			out.write(str+",\t");
	// 		}
	// 		idx++;
	// 	}
	// 	out.write(" /\n");

	// 	// Find and extract set pipes
	// 	Matcher pipesMatcher = pipesPattern.matcher(data);
	// 	if (pipesMatcher.find()) {
	// 		String pipesStr = pipesMatcher.group(1);
	// 		String[] pipesArr = pipesStr.split("\\s+");
	// 		for (String pipe : pipesArr) {
	// 			pipes.add(pipe);
	// 		}
	// 	}

	// 	idx=0;
	// 	out.write("\t"+"pipes /");
	// 	for(String str:pipes){
	// 		if(idx == pipes.size()-1){
	// 			out.write(str);
	// 		}
	// 		else out.write(str+",\t");
	// 		idx++;
	// 	}
	// 	out.write(" /\n");

	// 	Pattern patternSource = Pattern.compile("param Source :=\\s*(\\d+)\\s*;");
	// 	Matcher matcherSource = patternSource.matcher(data);
	// 	String paramSource="";
	// 	if (matcherSource.find()) {
	// 		paramSource = matcherSource.group(1);
	// 	}
	// 	out.write("\t"+"src(nodes) /"+paramSource+"/;");

	// 	out.write("\n");
	// 	out.write("alias (src,srcs);\n");
	// 	out.write("alias (nodes,j) ;\n");

	// 	out.write("Set "+"arcs(nodes,j) /");
	// 	// Pattern for matching arcs: L
	// 	Pattern arcsPattern = Pattern.compile("param : arcs : L :=\\s+([\\d\\s.]+)\\s*;");

	// 	// Find and extract arcs: L values
	// 	Matcher arcsMatcher = arcsPattern.matcher(data);
	// 	if (arcsMatcher.find()) {
	// 		String arcsData = arcsMatcher.group(1);
	// 		String[] lines = arcsData.split("\\n");

	// 		for (String line : lines) {
	// 			String[] values = line.trim().split("\\s+");
	// 			if (values.length == 3) {
	// 				String arcKey = values[0] + "." + values[1];
	// 				double arcValue = Double.parseDouble(values[2]);
	// 				arcsMap.put(arcKey, arcValue);
	// 			}
	// 		}
	// 	}

	// 	// Print the extracted values
	// 	idx=0;
	// 	for (Map.Entry<String, Double> entry : arcsMap.entrySet()) {
	// 		if(idx == arcsMap.size()-1)
	// 			out.write(entry.getKey());
	// 		else out.write(entry.getKey()+",  ");
	// 		idx++;
	// 	}
	// 	out.write("/"+";\n");

	// 	out.write("Set "+"F_arcs(nodes,j) /");

	// 	// Pattern for matching arcs: L
	// 	Pattern fixedArcsPattern = Pattern.compile("param : F_arcs : F_L :=\\s+([\\d\\s.]+)\\s*;");

	// 	// Find and extract F_arcs: F_L values
	// 	Matcher fixedArcsMatcher = fixedArcsPattern.matcher(data);
	// 	if (fixedArcsMatcher.find()) {
	// 		String fixedArcsData = fixedArcsMatcher.group(1);
	// 		String[] lines = fixedArcsData.split("\\n");

	// 		for (String line : lines) {
	// 			String[] values = line.trim().split("\\s+");
	// 			if (values.length == 3) {
	// 				String arcKey = values[0] + "." + values[1];
	// 				double arcValue = Double.parseDouble(values[2]);
	// 				f_arcsMap.put(arcKey, arcValue);
	// 			}
	// 		}
	// 	}

	// 	// Print the extracted values of F_arcs: F_L
	// 	idx = 0;
	// 	for (Map.Entry<String, Double> entry : f_arcsMap.entrySet()) {
	// 		if (idx == f_arcsMap.size() - 1)
	// 			out.write(entry.getKey());
	// 		else
	// 			out.write(entry.getKey() + ",  ");
	// 		idx++;
	// 	}
	// 	out.write("/" + ";\n");

	// 	// Pattern for matching arcs: diameter
	// 	Pattern fixed_diameterPattern = Pattern.compile("param F_d :=\\s+([\\d\\s.]+)\\s*;");

	// 	// Find and extract F_d values
	// 	Matcher fixed_diameterMatcher = fixed_diameterPattern.matcher(data);
	// 	if (fixed_diameterMatcher.find()) {
	// 		String fixed_diameterData = fixed_diameterMatcher.group(1);
	// 		String[] lines = fixed_diameterData.split("\\n");

	// 		for (String line : lines) {
	// 			String[] values = line.trim().split("\\s+");
	// 			if (values.length == 3) {
	// 				String arcKey = values[0] + "." + values[1];
	// 				double arcValue = Double.parseDouble(values[2]);
	// 				f_diameter.put(arcKey, arcValue);
	// 			}
	// 		}
	// 	}

	// 	// Pattern for matching arcs: roughness
	// 	Pattern fixed_roughnessPattern = Pattern.compile("param F_R :=\\s+([\\d\\s.]+)\\s*;");

	// 	// Find and extract F_d values
	// 	Matcher fixed_roughnessMatcher = fixed_roughnessPattern.matcher(data);
	// 	if (fixed_roughnessMatcher.find()) {
	// 		String fixed_roughnessData = fixed_roughnessMatcher.group(1);
	// 		String[] lines = fixed_roughnessData.split("\\n");

	// 		for (String line : lines) {
	// 			String[] values = line.trim().split("\\s+");
	// 			if (values.length == 3) {
	// 				String arcKey = values[0] + "." + values[1];
	// 				double arcValue = Double.parseDouble(values[2]);
	// 				f_roughness.put(arcKey, arcValue);
	// 			}
	// 		}
	// 	}

	// 	out.write("\n");
	// }

// 	private void printParameters(BufferedWriter out, String data, Map<String, Double> arcsMap, Map<String, Double> f_arcsMap, Map<String, Double> f_diameter, Map<String, Double> f_roughness, ArrayList<String> arcsLength, ArrayList<String> diameter, ArrayList<String> elevation, ArrayList<String> pressure, ArrayList<String> demand, ArrayList<String> pipeCost, ArrayList<String> pipeRoughness, String source) throws IOException {

// 		out.write("Parameters\n");
// 		out.write("\t"+"Len(nodes,j) /");
// 		int idx=0;
// 		for (Map.Entry<String, Double> entry : arcsMap.entrySet()) {
// 			String str=entry.getKey();
// 			String len[]=str.split("\\.");
// 			if(idx == arcsMap.size()-1)
// 				out.write(len[0]+"\t."+len[1]+"\t"+entry.getValue());
// 			else out.write(len[0]+"\t."+len[1]+"\t"+entry.getValue()+",  ");
// 			idx++;
// 		}
// 		out.write("/"+"\n");

// 		out.write("\t"+"F_L(nodes,j) /");
// 		idx=0;
// 		for (Map.Entry<String, Double> entry : f_arcsMap.entrySet()) {
// 			String str=entry.getKey();
// 			String len[]=str.split("\\.");
// 			if(idx == f_arcsMap.size()-1)
// 				out.write(len[0]+"\t."+len[1]+"\t"+entry.getValue());
// 			else out.write(len[0]+"\t."+len[1]+"\t"+entry.getValue()+",  ");
// 			idx++;
// 		}
// 		out.write("/"+"\n");

// 		out.write("\t"+"E(nodes) /");
// 		// Extract values of param E
// 		String regexE = "param E :=\\s*((?:\\d+\\s+\\d+\\.\\d+\\s*)+);";
// 		Pattern patternE = Pattern.compile(regexE);
// 		Matcher matcherE = patternE.matcher(data);

// 		if (matcherE.find()) {
// 			String paramEValues = matcherE.group(1);
// 			String[] eValues = paramEValues.split("\\s+");
// 			for (int i = 0; i < eValues.length; i += 2) {
// 				String node = eValues[i];
// 				String value = eValues[i + 1];
// 				out.write(node + "  " + value);
// 				if(i != eValues.length-2) out.write(", ");
// 			}
// 		}
// 		out.write("/\n");

// 		out.write("\t"+"P(nodes) /");
// 		String regexP = "param P :=\\s*([\\s\\S]*?);";
// 		Pattern patternP = Pattern.compile(regexP);
// 		Matcher matcherP = patternP.matcher(data);

// 		if (matcherP.find()) {
// 			String paramPValues = matcherP.group(1);
// 			String[] pValues = paramPValues.split("\\s+");
// 			for (int i = 0; i < pValues.length; i += 2) {
// 				String node = pValues[i];
// 				String value = pValues[i + 1];
// 				out.write(node + "  "+value);
// 				if(i != pValues.length-2) out.write(", ");
// 			}
// 		}
// 		out.write("/\n");

// 		out.write("\t"+"D(nodes) /");
// 		// Extract values of param D
// 		String regexD = "param D :=\\s*((?:\\d+\\s+-?\\d+\\.\\d+\\s*)+);";
// 		Pattern patternD = Pattern.compile(regexD);
// 		Matcher matcherD = patternD.matcher(data);

// 		if (matcherD.find()) {
// 			String paramDValues = matcherD.group(1);
// 			String[] dValues = paramDValues.split("\\s+");
// 			for (int i = 0; i < dValues.length; i += 2) {
// 				String node = dValues[i];
// 				String value = dValues[i + 1];
// 				out.write(node + "  "+value);
// 				if(i != dValues.length-2) out.write(", ");
// 			}
// 		}
// 		out.write("/\n");

// 		out.write("\t"+"dia(pipes) /");
// 		// Extract values of param d
// 		String regexd = "param d :=\\s*((?:\\d+\\s+\\d+\\.\\d+\\s*)+);";
// 		Pattern patternd = Pattern.compile(regexd);
// 		Matcher matcherd = patternd.matcher(data);

// 		if (matcherd.find()) {
// 			String paramDValues = matcherd.group(1);
// 			String[] dValues = paramDValues.split("\\s+");
// 			for (int i = 0; i < dValues.length; i += 2) {
// 				String node = dValues[i];
// 				String value = dValues[i + 1];
// 				out.write(node + "  "+value);
// 				if(i != dValues.length-2) out.write(", ");
// 			}
// 		}
// 		out.write("/\n");

// 		out.write("\t"+"C(pipes) /");
// 		String regexC = "param C :=\\s*((?:\\d+\\s+\\d+\\.\\d+\\s*)+);";
// 		Pattern patternC = Pattern.compile(regexC);
// 		Matcher matcherC = patternC.matcher(data);

// 		if (matcherC.find()) {
// 			String paramCValues = matcherC.group(1);
// 			String[] cValues = paramCValues.split("\\s+");
// 			for (int i = 0; i < cValues.length; i += 2) {
// 				String node = cValues[i];
// 				String value = cValues[i + 1];
// 				out.write(node + "  "+value);
// 				if(i != cValues.length-2) out.write(", ");
// 			}
// 		}
// 		out.write("/\n");

// 		out.write("\t"+"R(pipes) /");
// 		String regexR = "param R :=\\s*([\\s\\S]*?);";
// 		Pattern patternR = Pattern.compile(regexR);
// 		Matcher matcherR = patternR.matcher(data);

// 		if (matcherR.find()) {
// 			String paramRValues = matcherR.group(1);
// 			String[] rValues = paramRValues.split("\\s+");
// 			for (int i = 0; i < rValues.length; i += 2) {
// 				String node = rValues[i];
// 				String value = rValues[i + 1];
// 				out.write(node + "  "+value);
// 				if(i != rValues.length-2) out.write(", ");
// 			}
// 		}

// 		out.write("/\n");

// 		idx=0;
// 		out.write("\t"+"F_d(nodes,j) /");
// 		for (Map.Entry<String, Double> entry : f_diameter.entrySet()) {
// 			String str=entry.getKey();
// 			String len[]=str.split("\\.");
// 			if(idx == f_diameter.size()-1)
// 				out.write(len[0]+"\t."+len[1]+"\t"+entry.getValue());
// 			else out.write(len[0]+"\t."+len[1]+"\t"+entry.getValue()+",  ");
// 			idx++;
// 		}
// 		out.write("/"+"\n");

// 		// System.out.println("/");

// 		idx=0;
// 		out.write("\t"+"F_R(nodes,j) /");
// 		for (Map.Entry<String, Double> entry : f_roughness.entrySet()) {
// 			String str=entry.getKey();
// 			String len[]=str.split("\\.");
// 			if(idx == f_roughness.size()-1)
// 				out.write(len[0]+"\t."+len[1]+"\t"+entry.getValue());
// 			else out.write(len[0]+"\t."+len[1]+"\t"+entry.getValue()+",  ");
// 			idx++;
// 		}


// 		out.write("/;\n");

// 		out.write("\n");

// 	}

// 	void printModelm2(BufferedWriter out) throws IOException {
// 		out.write("Scalar omega  /10.68/;\n");
// 		out.write("Scalar bnd ;\n");
// 		out.write("Scalar qm;\n");
// 		out.write("Scalar q_M;\n");

// 		out.write("\n");

// 		out.write("bnd = sum(src,D(src));\n");
// 		out.write("q_M=-bnd;\n");
// 		out.write("qm=0;\n");

// 		out.write("\n");

// 		out.write("Variable l(nodes,j,pipes); \n");
// 		out.write("l.lo(nodes,j,pipes)= 0;\n");

// 		out.write("\n");

// 		out.write("Variable q1(nodes,j);\n");
// 		out.write("q1.lo(nodes,j)=qm;\n");
// 		out.write("q1.up(nodes,j)=q_M;\n");

// 		out.write("\n");

// 		out.write("Variable q2(nodes,j);\n");
// 		out.write("q2.lo(nodes,j)=qm;\n");
// 		out.write("q2.up(nodes,j)=q_M;\n");

// 		out.write("\n");

// 		/**
// 		 * Variable F_q1(nodes,j);
// 		 * F_q1.lo(nodes,j)=qm;
// 		 * F_q1.up(nodes,j)=q_M;
// 		 *
// 		 *
// 		 * Variable F_q2(nodes,j);
// 		 * F_q2.lo(nodes,j)=qm;
// 		 * F_q2.up(nodes,j)=q_M;
// 		 */
// 		out.write("Variable F_q1(nodes,j);\n");
// 		out.write("F_q1.lo(nodes,j)=qm;\n");
// 		out.write("F_q1.lo(nodes,j)=qm;\n");

// 		out.write("\n");

// 		out.write("Variable F_q2(nodes,j);\n");
// 		out.write("F_q2.lo(nodes,j)=qm;\n");
// 		out.write("F_q2.up(nodes,j)=q_M;\n");

// 		out.write("Variables z;\n");

// 		out.write("\n");

// 		out.write("Variable h(nodes);\n");

// 		out.write("\n");

// 		out.write("Equations cost \"objective function\",bound1(nodes,j,pipes),cons1(nodes),cons2(nodes),cons3(nodes,j),cons5(src), cons4(nodes,j), cons6(nodes,j), cons7(nodes,j) ;\n");

// 		out.write("cost..  z=e=sum(arcs(nodes,j),sum(pipes,l(arcs,pipes)*c(pipes)));\n");

// 		out.write("bound1(nodes,j,pipes)$arcs(nodes,j).. l(nodes,j,pipes) =l= Len(nodes,j);\n");
// 		out.write("cons1(nodes).. sum(arcs(j,nodes),(q1(arcs)-q2(arcs))) + sum(F_arcs(j,nodes),(F_q1(F_arcs)-F_q2(F_arcs))) =e= sum(arcs(nodes,j),(q1(arcs)-q2(arcs))) + sum(F_arcs(nodes,j),(F_q1(F_arcs)-F_q2(F_arcs))) + D(nodes);\n");
// 		out.write("cons2(nodes).. h(nodes) =g= E(nodes) + P(nodes);\n");
// 		out.write("cons3(arcs(nodes,j)).. h(nodes)-h(j)=e=sum(pipes,(((q1(arcs)*0.001)**1.852 - (q2(arcs)*0.001)**1.852)*omega*l(arcs,pipes))/((R(pipes)**1.852)*(dia(pipes)/1000)**4.87));\n");
// 		out.write("cons7(F_arcs(nodes,j)).. h(nodes)-h(j)=e=(((F_q1(F_arcs)*0.001)**1.852 - (F_q2(F_arcs)*0.001)**1.852)*omega*F_L(F_arcs))/((F_R(F_arcs)**1.852)*(F_d(F_arcs)/1000)**4.87);\n");
// 		out.write("cons4(arcs(nodes,j)).. sum(pipes,l(arcs,pipes)) =e=Len(arcs);\n");
// 		out.write("cons5(src)..  h(src)=e= sum(srcs,E(srcs));\n");
// 		out.write("cons6(arcs(nodes,j)).. q1(arcs)*q2(arcs) =l= q_M*qm;\n");

// 		out.write("\n");

// 		out.write("model m2  /all/  ;\n");
// 		// System.out.println("Option threads=4;\n");
// 		// System.out.println("m2.optfile =1;\n");
// 		out.write("solve m2 using minlp minimizing z ;\n");
// 	}

// 	private void printModelm1(BufferedWriter out) throws IOException {

// 		out.write("Scalar omega  /10.68/;\n");
// 		out.write("Scalar bnd ;\n");
// 		out.write("Scalar qm;\n");
// 		out.write("Scalar q_M;\n");

// 		out.write("\n");

// 		out.write("bnd = sum(src,D(src));\n");
// 		out.write("q_M=-bnd;\n");
// 		out.write("qm=bnd;\n");

// 		out.write("\n");

// 		out.write("Variable l(nodes,j,pipes);\n");
// 		out.write("l.lo(nodes,j,pipes)= 0;\n");

// 		out.write("\n");

// 		out.write("Variable q(nodes,j);\n");
// 		out.write("q.lo(nodes,j)=qm;\n");
// 		out.write("q.up(nodes,j)=q_M;\n");

// 		out.write("\n");

// 		out.write("Variable F_q(nodes,j);\n");
// 		out.write("F_q.lo(nodes,j)=qm;\n");
// 		out.write("F_q.up(nodes,j)=q_M;\n");

// 		out.write("\n");

// 		out.write("Variables z;\n");

// 		out.write("\n");

// 		out.write("Variable h(nodes);\n");

// 		out.write("\n");

// 		out.write("Equations cost \"objective function\",bound1(nodes,j,pipes),cons1(nodes),cons2(nodes),cons3(nodes,j),cons5(src), cons4(nodes,j), cons6(nodes,j);\n");

// 		out.write("cost..  z=e=sum(arcs(nodes,j),sum(pipes,l(arcs,pipes)*c(pipes)));\n");

// 		out.write("bound1(nodes,j,pipes)$arcs(nodes,j).. l(nodes,j,pipes) =l= Len(nodes,j);\n");
// 		out.write("cons1(nodes).. sum(arcs(j,nodes),q(arcs)) + sum(F_arcs(j,nodes),F_q(F_arcs)) =e= sum(arcs(nodes,j),q(arcs)) + sum(F_arcs(nodes,j),F_q(F_arcs))  + D(nodes);\n");
// 		out.write("cons2(nodes).. h(nodes) =g= E(nodes) + P(nodes);\n");
// 		out.write("cons3(arcs(nodes,j)).. h(nodes)-h(j)=e=sum(pipes,(signpower(q(arcs),1.852))*(0.001**1.852)*omega*l(arcs,pipes)/((R(pipes)**1.852)*(dia(pipes)/1000)**4.87));\n");
// 		out.write("cons6(F_arcs(nodes,j)).. h(nodes)-h(j)=e=signpower(F_q(F_arcs),1.852)*(0.001**1.852)*omega*F_L(F_arcs)/((F_R(F_arcs)**1.852)*(F_d(F_arcs)/1000)**4.87);\n");
// 		// out.write("cons6(F_arcs(nodes,j)).. h(nodes)-h(j)=e=(F_q(F_arcs)*(abs(F_q(F_arcs))**0.852))*(0.001**1.852)*(omega*F_L(F_arcs)/((F_R(F_arcs)**1.852)*(F_d(F_arcs)/1000)**4.87));\n");
// 		out.write("cons4(arcs(nodes,j)).. sum(pipes,l(arcs,pipes)) =e=Len(arcs);\n");
// 		out.write("cons5(src)..  h(src)=e= sum(srcs,E(srcs));\n");

// 		out.write("\n");

// 		out.write("model m1  /all/  ;\n");
// //			System.out.println("Option threads=4;");
// // 		out.write("m1.optfile =1;\n");
// 		out.write("solve m1 using minlp minimizing z ;\n");

// 	}



	// printSetsAndParametersAndModels();



	/**
	 * Launch "CalculateNetworkCost_JaltantraLauncher.sh" and wait for 2 seconds. If the process
	 * exits within 2 seconds, then log it (saying that we need to check into this matter) and return.
	 *
	 * @param networkFilePath path to the network file which is the be solved
	 * @throws Exception if `SOLVER_ROOT_DIR` directory does not exist,
	 *                   or function is unable to start the launcher process,
	 *                   or exception is thrown by `process.wairFor(...)`
	 */
	private void launchCalculateNetworkCost(final String networkFilePath) throws Exception {
		if (!(new File(SOLVER_ROOT_DIR)).exists()) {
			customLogger.loge("FIXME: SOLVER_ROOT_DIR directory does not exist: ' " + SOLVER_ROOT_DIR + "'");
			throw new Exception("Internal server error: SOLVER_ROOT_DIR directory does not exist");
		}

		// REFER: https://mkyong.com/java/how-to-execute-shell-command-from-java/
		// REFER: https://www.geeksforgeeks.org/how-to-execute-native-shell-commands-from-java-program/
		try {
			customLogger.logd(System.getProperty("os.name"));

			// NOTE: This process cannot be run on Windows
			ProcessBuilder pb = new ProcessBuilder(
					"bash",
					SOLVER_ROOT_DIR + "/CalculateNetworkCost_JaltantraLauncher.sh",
					networkFilePath,
					SOLVER_EXECUTION_TIME,
					run_Time,
					versionNumber,
					project_Name
			);
			pb.directory(new File(System.getProperty("user.home")));
			Process process = pb.start();

			boolean exited = process.waitFor(2, TimeUnit.SECONDS);
			if (exited)
				customLogger.logi("CHECKME: CalculateNetworkCost.py exited within 2 seconds, probably due to some issue");
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Check the execution status of "CalculateNetworkCost.py"
	 *
	 * @return int
	 * <li>-1 if "pathToNetworkSpecificDirectory/0_status" file does not exist</li>
	 * <li>-2 if solver status has unknown/unhandled value (refer code and
	 * documentation of "CalculateNetworkCost.py", and update this method)</li>
	 * <li>1 if solver is "running"</li>
	 * <li>2 if solver has "finished"</li>
	 * <li>3 if solver has "successfully finished"</li>
	 * @throws Exception if "launch_error" occurred
	 */
	private int checkSolverResultStatus(final String pathToNetworkSpecificDirectory) throws Exception {
		// REFER: CalculateNetworkCost.py docs for this
		if (!(new File(pathToNetworkSpecificDirectory + "/0_status")).exists())
			return -1;
		// NOTE: We use strip() as a safety measure to remove useless spaces
		// NOTE: We have to use startsWith() method because of trailing new line character(s) and other extra
		//       things which "CalculateNetworkCost.py" may be writing to the "0_status" file
		final String status = readFileAsString(pathToNetworkSpecificDirectory + "/0_status").strip();
		if (status.startsWith("launch_error")) {
			String directoryPath = pathToNetworkSpecificDirectory;

			// Create a File object representing the directory
			File directory = new File(directoryPath);

			// Check if the directory exists
			if (directory.exists()) {
				// Use the deleteDirectory method to recursively delete the directory
				boolean success = deleteDirectory(directory);

				if (success) {
					customLogger.logi("Network Specific Directory deleted successfully.");
				} else {
					System.out.println("Failed to delete network specific directory.");
				}
			} else {
				System.out.println("Network Specific Directory does not exist.");
			}
			customLogger.loge("FIXME: Failed to start the solver for network file: " + pathToNetworkSpecificDirectory.substring(pathToNetworkSpecificDirectory.lastIndexOf("/") + 1));
			throw new Exception("Internal server error: failed to start the solver for this network =" + pathToNetworkSpecificDirectory.substring(pathToNetworkSpecificDirectory.lastIndexOf("/") + 1) + "\n" + status.substring(status.indexOf("\n") + 1));
		}
		/*if (status.startsWith("running"))
			return 1;*/
		if (status.startsWith("finished") || status.startsWith("running"))
			return 2;
		if (status.startsWith("success"))
			return 3;
		return -2;
	}

	// Recursive method to delete a directory and its contents
	private static boolean deleteDirectory(File directory) {
		if (directory.isDirectory()) {
			File[] files = directory.listFiles();
			if (files != null) {
				for (File file : files) {
					// Recursive call to delete subdirectories and files
					deleteDirectory(file);
				}
			}
		}
		// Delete the directory once all its contents are deleted
		return directory.delete();
	}

	/**
	 * Extract solution for a given network and store it in:
	 * <br>&emsp;• resultPipes
	 * <br>&emsp;• resultCost
	 * <br>&emsp;• resultPumps
	 *
	 * @param pathToNetworkSpecificDirectory path to the directory which was created by CalculateNetworkCost.py
	 *                                       to store all the data related to a network file given to it to solve
	 * @return true if solver result was successfully extracted
	 * @throws Exception if any one of the below condition occurs:
	 *                   <br>&emsp;• result file does not exist
	 *                   <br>&emsp;• failure to read the result file
	 *                   <br>&emsp;• solver failed to find the solution due to some reason
	 */
	private boolean extractSolverResult(final String pathToNetworkSpecificDirectory) throws Exception {
		final String pathToResultFile = pathToNetworkSpecificDirectory + "/0_result.txt";
		if (!(new File(pathToResultFile)).exists())
			throw new Exception("Internal server error: result file does not exist");

		final String result = readFileAsString(pathToResultFile);
		final String[] resultLines = result.split("\n");
		customLogger.logi("0_result.txt first line = " + resultLines[0]);
		if (resultLines[0].equals("False")) {
			customLogger.loge("CHECKME: Solvers failed to find the solution for the network with hash = " + pathToNetworkSpecificDirectory.substring(pathToNetworkSpecificDirectory.lastIndexOf("/") + 1));
			throw new Exception("Internal server error: solvers failed to find the solution");
		}
		customLogger.logi("Best result found for network with hash = " + pathToNetworkSpecificDirectory.substring(pathToNetworkSpecificDirectory.lastIndexOf("/") + 1));
		customLogger.logi("Solver = " + resultLines[1]);
		customLogger.logi("Model = " + resultLines[2]);
		customLogger.logi("Best result = " + resultLines[4]);

		// REFER: https://www.geeksforgeeks.org/how-to-execute-native-shell-commands-from-java-program/
		ArrayList<String> extractedSolutionLines = new ArrayList<>();
		TreeMap<PipeCost, Double> cumulativePipeLength = new TreeMap<>();
		this.resultPipes = new ArrayList<>();
		this.resultCost = new ArrayList<>();
		this.resultPumps = new ArrayList<>();

		try {
			// This process cannot be run on Windows
			ProcessBuilder pb = new ProcessBuilder(
					"python3",
					SOLVER_ROOT_DIR + "/CalculateNetworkCost_ExtractResultFromAmplOutput.py",
					resultLines[3],
					pathToNetworkSpecificDirectory + "/0_graph_network_data_testcase.R",
					"1"
			);
			pb.directory(new File(System.getProperty("user.home")));
			Process process = pb.start();

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			customLogger.logd("*** The solution is ***");
			String line;
			while ((line = reader.readLine()) != null) {
				customLogger.logd(line);
				extractedSolutionLines.add(line);
			}

			int exitVal = process.waitFor();
			if (exitVal != 0) {
				customLogger.logi("extractSolverResult(...): exitVal = 0");
				customLogger.loge("********************");
				customLogger.loge("Log of CalculateNetworkCost_ExtractResultFromAmplOutput.py");
				customLogger.loge("********************");
				BufferedReader readerErr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				while ((line = readerErr.readLine()) != null) {
					customLogger.loge(line);
				}
				customLogger.loge("********************");
				return false;
			}

			int solutionLineIdx = 0;
			int numberOfHeads = Integer.parseInt(extractedSolutionLines.get(solutionLineIdx));
			solutionLineIdx += 1;
			for (int iHead = 0; iHead < numberOfHeads; ++iHead) {
				String[] arr = extractedSolutionLines.get(solutionLineIdx).split(" ");
				solutionLineIdx += 1;

				int node = Integer.parseInt(arr[0]);
				double head = Double.parseDouble(arr[1]);

				this.nodes.get(node).setHead(head);
			}

			int numberOfFlows = Integer.parseInt(extractedSolutionLines.get(solutionLineIdx));
			solutionLineIdx += 1;
			for (int iFlow = 0; iFlow < numberOfFlows; ++iFlow) {
				String[] arr = extractedSolutionLines.get(solutionLineIdx).split(" ");
				solutionLineIdx += 1;

				int arcSourceVertex = Integer.parseInt(arr[0]);
				int arcDestinationVertex = Integer.parseInt(arr[1]);
				double flow = Double.parseDouble(arr[2]);
				if (flow < 0.0) {
					// CustomLogger.logi("IMPORTANT: -ve flow indicates that water flows from destination to source vertex");
					// CustomLogger.logi("IMPORTANT: Swapping source and destination vertex as flow is -ve");
					// int temp = arcDestinationVertex;
					// arcDestinationVertex = arcSourceVertex;
					// arcSourceVertex = temp;
				}

				Pipe lCurrArc = null;
				for (Pipe p : this.getPipes().values()) {
					if (p.getStartNode().getNodeID() != arcSourceVertex || p.getEndNode().getNodeID() != arcDestinationVertex)
						continue;
					lCurrArc = p;
					break;
				}
				if (lCurrArc == null) {
					customLogger.loge("arcSourceVertex = " + arcSourceVertex + ", arcDestinationVertex = " + arcDestinationVertex);
					throw new Exception("Internal server error: Failed to extract the solution");
				}
				lCurrArc.setFlow(flow);
			}

			int numberOfArcs = Integer.parseInt(extractedSolutionLines.get(solutionLineIdx));
			solutionLineIdx += 1;
			for (int arcNum = 0; arcNum < numberOfArcs; ++arcNum) {
				String[] arr = extractedSolutionLines.get(solutionLineIdx).split(" ");
				solutionLineIdx += 1;
				int arcSourceVertex = Integer.parseInt(arr[0]);
				int arcDestinationVertex = Integer.parseInt(arr[1]);
				int arcOptimalPipeCount = Integer.parseInt(arr[2]);
				Pipe lCurrArc = null;
				for (Pipe p : this.getPipes().values()) {
					if (p.getStartNode().getNodeID() != arcSourceVertex || p.getEndNode().getNodeID() != arcDestinationVertex)
						continue;
					lCurrArc = p;
					break;
				}
				if (lCurrArc == null) {
					throw new Exception("Internal server error: Failed to extract the solution");
				}
				if(arcOptimalPipeCount == 0){ // This means this arcs corresponds to a fixed arc
					double lPipeLength = lCurrArc.getLength();
					double lPipeDiameter = lCurrArc.getDiameter();
					double lPipeFlow = lCurrArc.getFlow();
					double lPipeRoughness = lCurrArc.getRoughness();
					double lPipeHeadLoss = Util.HWheadLoss(lPipeLength, lPipeFlow, lPipeRoughness, lPipeDiameter);
					double lCurrPipeCost = 0.0; // It is assumed fixed arc pipes have 0 cost
					boolean lPipePressureExceeded = false;
					if (generalProperties.max_pipe_pressure > 0.0)
						if (lCurrArc.getStartNode().getPressure() > generalProperties.max_pipe_pressure || lCurrArc.getEndNode().getPressure() > generalProperties.max_pipe_pressure)
							lPipePressureExceeded = true;
					this.resultPipes.add(new PipeStruct(
							lCurrArc.getPipeID(), arcSourceVertex, arcDestinationVertex, lPipeLength, lPipeDiameter,
							lPipeRoughness, lPipeFlow, lPipeHeadLoss, lPipeHeadLoss * 1000 / lPipeLength,
							Util.waterSpeed(lPipeFlow, lPipeDiameter), lPipeLength * lCurrPipeCost,
							false, lPipePressureExceeded, lCurrArc.getFlowchoice() == Pipe.FlowType.PRIMARY,
							lCurrArc.getPumpHead(), lCurrArc.getPumpPower(), lCurrArc.getValveSetting()
					));
				}

				for (int pipeNum = 0; pipeNum < arcOptimalPipeCount; ++pipeNum) { // This is a non fixed arc
					// Prefix "l" denotes local variable
					String[] lArr2 = extractedSolutionLines.get(solutionLineIdx).split(" ");
					solutionLineIdx += 1;
					int lPipeIdx = Integer.parseInt(lArr2[0]);
					double lPipeLength = Double.parseDouble(lArr2[1]);

					PipeCost lCurrPipeCost = this.getPipeCost().get(lPipeIdx);
					double lPipeDiameter = lCurrPipeCost.getDiameter();
					double lPipeFlow = lCurrArc.getFlow();
					double lPipeHeadLoss = Util.HWheadLoss(lPipeLength, lPipeFlow, lCurrArc.getRoughness(), lPipeDiameter);
					boolean lPipePressureExceeded = false;
					if (generalProperties.max_pipe_pressure > 0.0)
						if (lCurrArc.getStartNode().getPressure() > generalProperties.max_pipe_pressure || lCurrArc.getEndNode().getPressure() > generalProperties.max_pipe_pressure)
							lPipePressureExceeded = true;

					int linkId = -1;
					for (Pipe p : this.pipes.values()) {
						if (arcSourceVertex == p.getStartNode().getNodeID()
								&& arcDestinationVertex == p.getEndNode().getNodeID()) {
							linkId = p.getPipeID();
							break;
						}
					}
					if (linkId == -1) {
						customLogger.loge(String.format(
								"Link ID could not be found for " +
										"arcSourceVertex=%d, arcDestinationVertex=%d, lPipeLength=%f, lPipeDiameter=%f",
								arcSourceVertex,
								arcDestinationVertex,
								lPipeLength,
								lPipeDiameter
						));
					}
					this.resultPipes.add(new PipeStruct(
							linkId, arcSourceVertex, arcDestinationVertex, lPipeLength, lPipeDiameter,
							lCurrPipeCost.getRoughness(), lPipeFlow, lPipeHeadLoss, lPipeHeadLoss * 1000 / lPipeLength,
							Util.waterSpeed(lPipeFlow, lPipeDiameter), lPipeLength * lCurrPipeCost.getCost(),
							false, lPipePressureExceeded, lCurrArc.getFlowchoice() == Pipe.FlowType.PRIMARY,
							lCurrArc.getPumpHead(), lCurrArc.getPumpPower(), lCurrArc.getValveSetting()
					));
					if (pumpGeneralProperties.pump_enabled && lCurrArc.getPumpPower() > 0) {
						double presentvaluefactor = Util.presentValueFactor(pumpGeneralProperties.discount_rate, pumpGeneralProperties.inflation_rate, pumpGeneralProperties.design_lifetime);
						double primarycoeffecient = 365 * presentvaluefactor * generalProperties.supply_hours * pumpGeneralProperties.energycost_per_kwh * pumpGeneralProperties.energycost_factor;
						double secondarycoeffecient = esrGeneralProperties.esr_enabled ? 365 * presentvaluefactor * esrGeneralProperties.secondary_supply_hours * pumpGeneralProperties.energycost_per_kwh * pumpGeneralProperties.energycost_factor : 0;
						double power = lCurrArc.getPumpPower();

						double energycost = power * (lCurrArc.getFlowchoice() == Pipe.FlowType.PRIMARY ? primarycoeffecient : secondarycoeffecient);
						double capitalcost = power * pumpGeneralProperties.capitalcost_per_kw;


						resultPumps.add(new ResultPumpStruct(lCurrArc.getPipeID(),
								lCurrArc.getPumpHead(),
								power,
								energycost,
								capitalcost,
								energycost + capitalcost));
					}
					if (!cumulativePipeLength.containsKey(lCurrPipeCost))
						cumulativePipeLength.put(lCurrPipeCost, lPipeLength);
					else
						cumulativePipeLength.put(lCurrPipeCost, cumulativePipeLength.get(lCurrPipeCost) + lPipeLength);
				}
			}

			double cumulativeCost = 0;
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
			return true;
		} catch (IOException | InterruptedException e) {
			customLogger.loge(e.toString());
			e.printStackTrace();
		}

		return false;
	}

	// ---

	public HashMap<Integer, Node> getNodes() {
		return nodes;
	}

	public HashMap<Integer, Pipe> getPipes() {
		return pipes;
	}

	public List<PipeCost> getPipeCost() {
		return pipeCost;
	}

}