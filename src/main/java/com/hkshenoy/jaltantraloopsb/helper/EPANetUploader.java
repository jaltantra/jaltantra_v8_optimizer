package com.hkshenoy.jaltantraloopsb.helper;

import com.google.gson.Gson;
import com.hkshenoy.jaltantraloopsb.structs.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


@Component
public class EPANetUploader {


    // get elevation of a point between startnode and endnode
    // point is 'length' distance from startnode
    // total distance between startnode and endnode is 'totallength'
    //generate and upload EPANET output network file
    public static double getLinearElevation(int startnode, int endnode,
                                        double length, double totallength, NodeStruct[] nodesArray) {

        double startElevation=0,endElevation =0;
        for(NodeStruct node: nodesArray){
            if(node.nodeid==startnode){
                startElevation = node.elevation;
            }
            if(node.nodeid==endnode){
                endElevation = node.elevation;
            }
        }

        if(totallength==0)
            return (startElevation+endElevation)/2;
        else
            return startElevation + (length/totallength)*(endElevation-startElevation);
    }

    public static void uploadEpanetOutputFile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try{
            OutputStream os = response.getOutputStream();
            PrintWriter pw = new PrintWriter(os);

            Gson gson = new Gson();

            GeneralStruct generalProperties = gson.fromJson(request.getParameter("general"), GeneralStruct.class);
            NodeStruct[] resultNodesArray = gson.fromJson(request.getParameter("resultnodes"), NodeStruct[].class);
            PipeStruct[] resultPipesArray = gson.fromJson(request.getParameter("resultpipes"), PipeStruct[].class);
            //CommercialPipeStruct[] resultCostsArray = gson.fromJson(request.getParameter("resultcosts"), CommercialPipeStruct[].class);
            EsrGeneralStruct esrGeneralProperties = gson.fromJson(request.getParameter("esrGeneral"), EsrGeneralStruct.class);
            ResultEsrStruct[] resultEsrsArray = gson.fromJson(request.getParameter("resultesrs"), ResultEsrStruct[].class);
            PumpGeneralStruct pumpGeneralProperties = gson.fromJson(request.getParameter("pumpGeneral"), PumpGeneralStruct.class);
            ResultPumpStruct[] resultPumpsArray = gson.fromJson(request.getParameter("resultpumps"), ResultPumpStruct[].class);
            ValveStruct[] valves = gson.fromJson(request.getParameter("valves"), ValveStruct[].class);

            String resultCoordinatesString = request.getParameter("coordinatesstring");
            String resultVerticesString = request.getParameter("verticesstring");

            if(generalProperties.name_project==null || generalProperties.name_project.isEmpty())
                generalProperties.name_project="JalTantra Project";

            String filename = generalProperties.name_project+"_output.inp";

            response.setContentType("text/plain"); // Set up mime type
            response.setHeader("Content-Disposition", "attachment; filename="+filename);

            String titleString = "[TITLE]\n"+
                    generalProperties.name_project+"\n\n";
            String endString = "[END]\n";
            String optionString = "[OPTIONS]\n"+
                    "Units\tLPS\n"+
                    "Headloss\tH-W\n\n";

            String junctionString = "[JUNCTIONS]\n"+
                    ";ID\tElev\tDemand\tPattern\n"; // id elev demand pattern
            String pipeString = "[PIPES]\n"+
                    ";ID\tStart\tEnd\tLength\tDiameter\tRoughness\tMinorLoss\tStatus\n"; //id start end length diameter roughness minorloss status
            String coordinateString = "[COORDINATES]\n"+
                    ";Node\tX-Coord\tY-Coord\n";
            String verticesString = "[VERTICES]\n"+
                    ";Pipe\tX-Coord\tY-Coord\n";
            String tanksString = "[TANKS]\n"+
                    ";ID\tElev\tInitLvl\tMinLvl\tMaxLvl\tDiam\tVolume\n";
            String patternString = "[PATTERNS]\n";
            String timesString = "[TIMES]\nDURATION\t24 HOURS\n";
            String pumpString = "[PUMPS]\n;ID\tNode1\tNode2\tProperties\n";
            String valveString = "[VALVES]\n;ID\tNode1\tNode2\tDiameter\tType\tSetting\n";
            Set<Integer> esrNodes = new HashSet<Integer>();
            Set<Integer> esrs = new HashSet<Integer>();
            Set<Integer> esrsWithPrimaryChildren = new HashSet<Integer>();
            Map<Integer,ResultPumpStruct> pumppipes = new HashMap<Integer,ResultPumpStruct>();
            Map<Integer,ValveStruct> valvepipes = new HashMap<Integer,ValveStruct>();


            for(String s: resultCoordinatesString.split(",")){
                if(s!=null &&!s.isEmpty()){
                    coordinateString += s +"\n";
                    //String[] t = s.trim().split(" ", 2);
                    //coord.put(Integer.parseInt(t[0]), t[1]);
                }
            }

            for(String s: resultVerticesString.split(",")){
                if(s!=null &&!s.isEmpty()){
                    verticesString += s +"\n";
                }
            }

            boolean esrenabled = esrGeneralProperties.esr_enabled;
            boolean pumpenabled = pumpGeneralProperties.pump_enabled;
            //NodeStruct source = null;

            if(esrenabled){
                for(ResultEsrStruct e : resultEsrsArray){
                    double maxlevel = 5;
                    double diameter = Math.sqrt(0.004*e.capacity/(Math.PI*maxlevel));

                    String nodeid = e.nodeid+"t";
                    if(e.hasprimarychild){
                        junctionString += e.nodeid+"j"+"\t"+e.elevation+"\t"+"0"+"\n";
                        esrsWithPrimaryChildren.add(e.nodeid);
                    }
                    tanksString += nodeid+"\t"+(e.elevation+e.esrheight)+"\t"+maxlevel+"\t"+"0\t"+maxlevel+"\t"+diameter+"0"+"\n";
                    esrs.add(e.nodeid);
                }

                patternString += "PriPat\t";
                int supplyhours = Math.min(24, (int)Math.ceil(generalProperties.supply_hours));
                int firstphase = (24-supplyhours)/2;
                int secondphase = supplyhours/2;
                int thirdphase = 24 - supplyhours - firstphase;
                int fourthphase = supplyhours - secondphase;

                for(int i=0;i<firstphase;i++)
                    patternString += "0 ";
                for(int i=0;i<secondphase;i++)
                    patternString += "1 ";
                for(int i=0;i<thirdphase;i++)
                    patternString += "0 ";
                for(int i=0;i<fourthphase;i++)
                    patternString += "1 ";
                patternString += "\n";

                patternString += "SecPat\t";
                supplyhours = Math.min(24, (int)Math.ceil(esrGeneralProperties.secondary_supply_hours));
                firstphase = (24-supplyhours)/2;
                secondphase = supplyhours/2;
                thirdphase = 24 - supplyhours - firstphase;
                fourthphase = supplyhours - secondphase;

                for(int i=0;i<firstphase;i++)
                    patternString += "0 ";
                for(int i=0;i<secondphase;i++)
                    patternString += "1 ";
                for(int i=0;i<thirdphase;i++)
                    patternString += "0 ";
                for(int i=0;i<fourthphase;i++)
                    patternString += "1 ";
                patternString += "\n";
            }

            if(pumpenabled){
                for(ResultPumpStruct p : resultPumpsArray){
                    pumppipes.put(p.pipeid, p);
                }
            }

            for(ValveStruct v : valves){
                valvepipes.put(v.pipeid, v);
            }

            String priPat = esrenabled?"\tPriPat":"";

            String reservoirString = "[RESERVOIRS]\n"+
                    ";ID\tHead\tPattern\n"+
                    generalProperties.source_nodeid+"\t"+generalProperties.source_head+priPat+"\n\n"; //id head pattern


            for(NodeStruct n : resultNodesArray){
                if(n.nodeid==generalProperties.source_nodeid){
                    //source = n;
                    continue;
                }
                if(esrs.contains(n.nodeid)){
                    if(n.demand>0){
                        junctionString += n.nodeid+"d"+"\t"+n.elevation+"\t"+n.demand+"\tSecPat"+"\n";
                        esrNodes.add(n.nodeid);
                    }
                    continue;
                }
                String secPat = "";
                if(n.nodeid!=n.esr)
                    secPat = "\tSecPat";
                junctionString += n.nodeid+"\t"+n.elevation+"\t"+n.demand+secPat+"\n";
            }



            Set<Integer> pipesSeen = new HashSet<Integer>();
            Set<Integer> seriesPipes = new HashSet<Integer>();
            HashMap<Integer,Double> seriesTotalLength = new HashMap<Integer,Double>();
            for(PipeStruct p : resultPipesArray){
                if(!pipesSeen.add(p.pipeid)){
                    if(!p.parallelallowed){
                        seriesPipes.add(p.pipeid);
                        seriesTotalLength.put(p.pipeid, p.length+seriesTotalLength.get(p.pipeid));
                    }
                }
                else{
                    seriesTotalLength.put(p.pipeid, p.length);
                }
            }
            pipesSeen = new HashSet<Integer>();
            for(PipeStruct p : resultPipesArray){

                String startnode = p.startnode+"";
                String endnode = p.endnode+"";
                String pipeid = p.pipeid+"";

                if(esrs.contains(p.startnode)){
                    if(p.isprimary)
                        startnode += "j";
                    else
                        startnode += "t";
                }
                if(esrs.contains(p.endnode)){
                    if(esrsWithPrimaryChildren.contains(p.endnode))
                        endnode += "j";
                    else
                        endnode += "t";
                }


                if(pipesSeen.add(p.pipeid)){
                    if(seriesPipes.contains(p.pipeid)){
                        endnode = startnode+"s"+endnode;
                        double elev = getLinearElevation(p.startnode, p.endnode, p.length, seriesTotalLength.get(p.pipeid), resultNodesArray);
                        junctionString += endnode+"\t"+elev+"\t"+"0.0"+"\n";

                    }
                    if(esrsWithPrimaryChildren.contains(p.endnode)){
                        pipeString += p.endnode+"j"+"\t"+p.endnode+"j"+"\t"+p.endnode+"t"+"\t"+
                                10+"\t"+p.diameter+"\t"+p.roughness+"\t"+
                                "0\tOpen\n";
                    }
                    if(esrNodes.contains(p.endnode)){
                        pipeString += p.endnode+"d"+"\t"+p.endnode+"t"+"\t"+p.endnode+"d"+"\t"+
                                10+"\t"+p.diameter+"\t"+p.roughness+"\t"+
                                "0\tOpen\n";
                    }

                    if(pumpenabled && pumppipes.keySet().contains(p.pipeid)){
                        String tempnode = startnode + "pump";
                        double elev = getLinearElevation(p.startnode, p.startnode, 0, 0, resultNodesArray);
                        junctionString += tempnode+"\t"+elev+"\t"+"0.0"+"\n";
                        pumpString += p.pipeid+"pump\t"+startnode+"\t"+tempnode+"\t"+"POWER "+pumppipes.get(p.pipeid).pumppower*pumpGeneralProperties.efficiency/100
                                +"\tPATTERN "+(esrenabled?(p.isprimary?"PriPat":"SecPat"):"")+"\n";
                        startnode = tempnode;
                    }
                    if(valvepipes.keySet().contains(p.pipeid)){
                        String tempnode = startnode + "valve";
                        double elev = getLinearElevation(p.startnode, p.startnode, 0, 0, resultNodesArray);
                        junctionString += tempnode+"\t"+elev+"\t"+"0.0"+"\n";
                        valveString += p.pipeid+"valve\t"+startnode+"\t"+tempnode+"\t"+p.diameter+"\tPBV\t"+valvepipes.get(p.pipeid).valvesetting+"\n";
                        startnode = tempnode;
                    }
                }
                else{
                    //pipeid already seen => parallel or series pipes
                    if(p.parallelallowed){
                        // add to vertices here
                        pipeid += "p";
                        if(pumpenabled && pumppipes.keySet().contains(p.pipeid)){
                            startnode += "pump";
                        }
                        if(valvepipes.keySet().contains(p.pipeid)){
                            startnode += "valve";
                        }
                    }
                    else{
                        startnode = startnode+"s"+endnode;
                        pipeid += "s";
                    }
                }

                pipeString += pipeid+"\t"+startnode+"\t"+endnode+"\t"+
                        p.length+"\t"+p.diameter+"\t"+p.roughness+"\t"+
                        "0\tOpen\n";
            }

            //coordinateString += generateCoordinates(resultPipesArray, resultNodesArray, source, 0, 0, -10000, 10000);
            //coordinateString += resultCoordinatesString;

            pw.println(titleString);
            pw.println(junctionString);
            if(esrenabled){
                pw.println(tanksString);
                pw.println(patternString);
                pw.println(timesString);
            }
            pw.println(reservoirString);
            if(pumpenabled){
                pw.println(pumpString);
            }
            pw.println(valveString);
            pw.println(pipeString);
            pw.println(optionString);
            pw.println(coordinateString);
            pw.println(verticesString);
            pw.println(endString);

            pw.flush();
            pw.close();

            os.flush();
            os.close();


        }
        catch(Exception e){
            System.out.println(e);
        }
    }
}
