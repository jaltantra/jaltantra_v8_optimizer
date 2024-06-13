package com.hkshenoy.jaltantraloopsb.helper;

import com.google.gson.Gson;
import com.hkshenoy.jaltantraloopsb.structs.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;

@Component
public class XMLUploader {

    @Value("${JALTANTRA_VERSION}")
    private String version;
    //generate and upload XML input network file
    public static void uploadXmlInputFile(HttpServletRequest request, HttpServletResponse response)
    {
        try
        {
            OutputStream os = response.getOutputStream();

            Gson gson = new Gson();
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("root");
            doc.appendChild(rootElement);

            GeneralStruct generalProperties = gson.fromJson(request.getParameter("general"), GeneralStruct.class);

            if(generalProperties.name_project==null || generalProperties.name_project.isEmpty())
                generalProperties.name_project="JalTantra Project";

            Element nameElement = doc.createElement("name");
            nameElement.appendChild(doc.createTextNode(generalProperties.name_project));
            rootElement.appendChild(nameElement);

            Element organizationElement = doc.createElement("organization");
            organizationElement.appendChild(doc.createTextNode(generalProperties.name_organization));
            rootElement.appendChild(organizationElement);

            String version="";



            Element versionElement = doc.createElement("version");
            versionElement.appendChild(doc.createTextNode(version));
            rootElement.appendChild(versionElement);

            response.setContentType("application/xml"); // Set up mime type
            response.setHeader("Content-Disposition", "attachment; filename="+generalProperties.name_project+".xml");

            // node elements
            Element nodes = doc.createElement("nodes");
            rootElement.appendChild(nodes);

            Element defaultMinPressure = doc.createElement("defaultMinPressure");
            defaultMinPressure.appendChild(doc.createTextNode(Double.toString(generalProperties.min_node_pressure)));
            nodes.appendChild(defaultMinPressure);

            //Element peakFactor = doc.createElement("peakFactor");
            //peakFactor.appendChild(doc.createTextNode(textFieldPeakFactor.getText()));
            //nodes.appendChild(peakFactor);

            Element supplyHours = doc.createElement("supplyHours");
            supplyHours.appendChild(doc.createTextNode(Double.toString(generalProperties.supply_hours)));
            nodes.appendChild(supplyHours);

            Element sourceNode = doc.createElement("source");
            sourceNode.setAttribute("nodeID", Integer.toString(generalProperties.source_nodeid));
            sourceNode.setAttribute("nodeName", generalProperties.source_nodename);
            sourceNode.setAttribute("elevation", Double.toString(generalProperties.source_elevation));
            sourceNode.setAttribute("head", Double.toString(generalProperties.source_head));
            nodes.appendChild(sourceNode);

            NodeStruct[] nodesArray = gson.fromJson(request.getParameter("nodes"), NodeStruct[].class);
            if(nodesArray!=null && nodesArray.length!=0)
            {
                for(NodeStruct node : nodesArray)
                {
                    Element nodeElement = doc.createElement("node");

                    nodeElement.setAttribute("nodeID", Integer.toString(node.nodeid));
                    nodeElement.setAttribute("nodeName", node.nodename);
                    nodeElement.setAttribute("elevation", Double.toString(node.elevation));
                    if(node.demand!=0)
                        nodeElement.setAttribute("demand", Double.toString(node.demand));
                    if(node.minpressure!=0)
                        nodeElement.setAttribute("minPressure", Double.toString(node.minpressure));
                    nodes.appendChild(nodeElement);
                }
            }

            Element pipes = doc.createElement("pipes");
            rootElement.appendChild(pipes);

            Element defaultRoughness = doc.createElement("defaultRoughness");
            defaultRoughness.appendChild(doc.createTextNode(Double.toString(generalProperties.def_pipe_roughness)));
            pipes.appendChild(defaultRoughness);

            Element minHeadLossPerKM = doc.createElement("minHeadLossPerKM");
            minHeadLossPerKM.appendChild(doc.createTextNode(Double.toString(generalProperties.min_hl_perkm)));
            pipes.appendChild(minHeadLossPerKM);

            Element maxHeadLossPerKM = doc.createElement("maxHeadLossPerKM");
            maxHeadLossPerKM.appendChild(doc.createTextNode(Double.toString(generalProperties.max_hl_perkm)));
            pipes.appendChild(maxHeadLossPerKM);

            if(generalProperties.max_water_speed > 0){
                Element maxWaterSpeed = doc.createElement("maxWaterSpeed");
                maxWaterSpeed.appendChild(doc.createTextNode(Double.toString(generalProperties.max_water_speed)));
                pipes.appendChild(maxWaterSpeed);
            }

            if(generalProperties.max_pipe_pressure > 0){
                Element maxPipePressure = doc.createElement("maxPipePressure");
                maxPipePressure.appendChild(doc.createTextNode(Double.toString(generalProperties.max_pipe_pressure)));
                pipes.appendChild(maxPipePressure);
            }

            PipeStruct[] pipesArray = gson.fromJson(request.getParameter("pipes"), PipeStruct[].class);
            if(pipesArray!=null && pipesArray.length!=0)
            {
                for(PipeStruct pipe : pipesArray)
                {
                    Element pipeElement = doc.createElement("pipe");

                    pipeElement.setAttribute("pipeID", Integer.toString(pipe.pipeid));
                    pipeElement.setAttribute("length", Double.toString(pipe.length));
                    pipeElement.setAttribute("startNode", Integer.toString(pipe.startnode));
                    pipeElement.setAttribute("endNode", Integer.toString(pipe.endnode));
                    if(pipe.diameter!=0)
                        pipeElement.setAttribute("diameter", Double.toString(pipe.diameter));
                    if(pipe.roughness!=0)
                        pipeElement.setAttribute("roughness", Double.toString(pipe.roughness));
                    if(pipe.parallelallowed)
                        pipeElement.setAttribute("parallelAllowed", Boolean.toString(pipe.parallelallowed));

                    pipes.appendChild(pipeElement);
                }
            }

            Element pipeCosts = doc.createElement("pipeCosts");
            rootElement.appendChild(pipeCosts);

            CommercialPipeStruct[] commercialPipesArray = gson.fromJson(request.getParameter("commercialpipes"), CommercialPipeStruct[].class);
            if(commercialPipesArray!=null && commercialPipesArray.length!=0)
            {
                for(CommercialPipeStruct commercialPipe : commercialPipesArray)
                {
                    Element pipeCostElement = doc.createElement("pipeCost");
                    pipeCostElement.setAttribute("diameter", Double.toString(commercialPipe.diameter));
                    if(commercialPipe.roughness!=0)
                        pipeCostElement.setAttribute("roughness", Double.toString(commercialPipe.roughness));
                    pipeCostElement.setAttribute("cost", Double.toString(commercialPipe.cost));

                    pipeCosts.appendChild(pipeCostElement);
                }
            }

            EsrGeneralStruct esrGeneralProperties = gson.fromJson(request.getParameter("esrGeneral"), EsrGeneralStruct.class);
            EsrCostStruct[] esrCostsArray = gson.fromJson(request.getParameter("esrCost"), EsrCostStruct[].class);

            if(esrGeneralProperties!=null){
                Element esr = doc.createElement("esr");
                rootElement.appendChild(esr);

                Element esrEnabled = doc.createElement("esr_enabled");
                esrEnabled.appendChild(doc.createTextNode(Boolean.toString(esrGeneralProperties.esr_enabled)));
                esr.appendChild(esrEnabled);

                Element secondarySupplyHours = doc.createElement("secondary_supply_hours");
                secondarySupplyHours.appendChild(doc.createTextNode(Double.toString(esrGeneralProperties.secondary_supply_hours)));
                esr.appendChild(secondarySupplyHours);

                Element esrCapacityFactor = doc.createElement("esr_capacity_factor");
                esrCapacityFactor.appendChild(doc.createTextNode(Double.toString(esrGeneralProperties.esr_capacity_factor)));
                esr.appendChild(esrCapacityFactor);

                Element maxEsrHeight = doc.createElement("max_esr_height");
                maxEsrHeight.appendChild(doc.createTextNode(Double.toString(esrGeneralProperties.max_esr_height)));
                esr.appendChild(maxEsrHeight);

                Element allowDummy = doc.createElement("allow_dummy");
                allowDummy.appendChild(doc.createTextNode(Boolean.toString(esrGeneralProperties.allow_dummy)));
                esr.appendChild(allowDummy);

                Element mustEsrs = doc.createElement("must_esrs");
                if(esrGeneralProperties.must_esr!=null){
                    for(int i: esrGeneralProperties.must_esr){
                        Element mustEsr = doc.createElement("must_esr");
                        mustEsr.appendChild(doc.createTextNode(Integer.toString(i)));
                        mustEsrs.appendChild(mustEsr);
                    }
                }
                esr.appendChild(mustEsrs);

                Element mustNotEsrs = doc.createElement("must_not_esrs");
                if(esrGeneralProperties.must_not_esr!=null){
                    for(int i: esrGeneralProperties.must_not_esr){
                        Element mustNotEsr = doc.createElement("must_not_esr");
                        mustNotEsr.appendChild(doc.createTextNode(Integer.toString(i)));
                        mustNotEsrs.appendChild(mustNotEsr);
                    }
                }
                esr.appendChild(mustNotEsrs);

                Element esrCosts = doc.createElement("esr_costs");
                if(esrCostsArray!=null && esrCostsArray.length!=0){
                    for(EsrCostStruct esrCost : esrCostsArray){
                        Element esrCostElement = doc.createElement("esr_cost");
                        esrCostElement.setAttribute("mincapacity", Double.toString(esrCost.mincapacity));
                        esrCostElement.setAttribute("maxcapacity", Double.toString(esrCost.maxcapacity));
                        esrCostElement.setAttribute("basecost", Double.toString(esrCost.basecost));
                        esrCostElement.setAttribute("unitcost", Double.toString(esrCost.unitcost));

                        esrCosts.appendChild(esrCostElement);
                    }
                }
                esr.appendChild(esrCosts);
            }

            PumpGeneralStruct pumpGeneralProperties = gson.fromJson(request.getParameter("pumpGeneral"), PumpGeneralStruct.class);
            PumpManualStruct[] pumpManualArray = gson.fromJson(request.getParameter("pumpManual"), PumpManualStruct[].class);

            if(pumpGeneralProperties!=null){
                Element pump = doc.createElement("pump");
                rootElement.appendChild(pump);

                Element pumpEnabled = doc.createElement("pump_enabled");
                pumpEnabled.appendChild(doc.createTextNode(Boolean.toString(pumpGeneralProperties.pump_enabled)));
                pump.appendChild(pumpEnabled);

                Element minimumPumpSize = doc.createElement("minimum_pump_size");
                minimumPumpSize.appendChild(doc.createTextNode(Double.toString(pumpGeneralProperties.minpumpsize)));
                pump.appendChild(minimumPumpSize);

                Element pumpEfficiency = doc.createElement("pump_efficiency");
                pumpEfficiency.appendChild(doc.createTextNode(Double.toString(pumpGeneralProperties.efficiency)));
                pump.appendChild(pumpEfficiency);

                Element capitalCost = doc.createElement("capital_cost_per_kw");
                capitalCost.appendChild(doc.createTextNode(Double.toString(pumpGeneralProperties.capitalcost_per_kw)));
                pump.appendChild(capitalCost);

                Element energyCost = doc.createElement("energy_cost_per_kwh");
                energyCost.appendChild(doc.createTextNode(Double.toString(pumpGeneralProperties.energycost_per_kwh)));
                pump.appendChild(energyCost);

                Element designLifetime = doc.createElement("design_lifetime");
                designLifetime.appendChild(doc.createTextNode(Integer.toString(pumpGeneralProperties.design_lifetime)));
                pump.appendChild(designLifetime);

                Element discountRate = doc.createElement("discount_rate");
                discountRate.appendChild(doc.createTextNode(Double.toString(pumpGeneralProperties.discount_rate)));
                pump.appendChild(discountRate);

                Element inflationRate = doc.createElement("inflation_rate");
                inflationRate.appendChild(doc.createTextNode(Double.toString(pumpGeneralProperties.inflation_rate)));
                pump.appendChild(inflationRate);

                Element mustNotPumps = doc.createElement("pipes_without_pumps");
                if(pumpGeneralProperties.must_not_pump!=null){
                    for(int i: pumpGeneralProperties.must_not_pump){
                        Element mustNotPump = doc.createElement("pipe_without_pump");
                        mustNotPump.appendChild(doc.createTextNode(Integer.toString(i)));
                        mustNotPumps.appendChild(mustNotPump);
                    }
                }
                pump.appendChild(mustNotPumps);

                Element pumpManual = doc.createElement("manual_pumps");
                if(pumpManualArray!=null && pumpManualArray.length!=0){
                    for(PumpManualStruct p : pumpManualArray){
                        Element pumpManualElement = doc.createElement("manual_pump");
                        pumpManualElement.setAttribute("pipeid", Integer.toString(p.pipeid));
                        pumpManualElement.setAttribute("pumppower", Double.toString(p.pumppower));

                        pumpManual.appendChild(pumpManualElement);
                    }
                }
                pump.appendChild(pumpManual);
            }

            ValveStruct[] valves = gson.fromJson(request.getParameter("valves"), ValveStruct[].class);
            if(valves!=null && valves.length>0){
                Element valvesElement = doc.createElement("valves");
                rootElement.appendChild(valvesElement);

                for(ValveStruct v : valves){
                    Element valveElement = doc.createElement("valve");
                    valveElement.setAttribute("pipeid", Integer.toString(v.pipeid));
                    valveElement.setAttribute("valvesetting", Double.toString(v.valvesetting));

                    valvesElement.appendChild(valveElement);
                }
            }

            Element map = doc.createElement("map");
            rootElement.appendChild(map);

            String mapSourceNode = request.getParameter("mapsource");
            Element mapSourceNodeElement = doc.createElement("map_source_node");
            mapSourceNodeElement.setAttribute("nodeid", mapSourceNode);
            map.appendChild(mapSourceNodeElement);

            Element mapNodes = doc.createElement("map_nodes");
            map.appendChild(mapNodes);

            MapNodeStruct[] mapNodesArray = gson.fromJson(request.getParameter("mapnodes"), MapNodeStruct[].class);

            if(mapNodesArray!=null && mapNodesArray.length!=0)
            {
                for(MapNodeStruct mapNode : mapNodesArray)
                {
                    Element mapNodeElement = doc.createElement("map_node");
                    mapNodeElement.setAttribute("nodeid", Integer.toString(mapNode.nodeid));
                    mapNodeElement.setAttribute("nodename", mapNode.nodename);
                    mapNodeElement.setAttribute("latitude", Double.toString(mapNode.latitude));
                    mapNodeElement.setAttribute("longitude", Double.toString(mapNode.longitude));
                    mapNodeElement.setAttribute("isesr", Boolean.toString(mapNode.isesr));

                    mapNodes.appendChild(mapNodeElement);
                }
            }

            Element mapPipes = doc.createElement("map_pipes");
            map.appendChild(mapPipes);

            MapPipeStruct[] mapPipesArray = gson.fromJson(request.getParameter("mappipes"), MapPipeStruct[].class);
            if(mapPipesArray!=null && mapPipesArray.length!=0)
            {
                for(MapPipeStruct mapPipe : mapPipesArray)
                {
                    Element mapPipeElement = doc.createElement("map_pipe");
                    mapPipeElement.setAttribute("encodedpath", mapPipe.encodedpath);
                    mapPipeElement.setAttribute("originid", Integer.toString(mapPipe.originid));
                    mapPipeElement.setAttribute("destinationid", Integer.toString(mapPipe.destinationid));
                    mapPipeElement.setAttribute("length", Double.toString(mapPipe.length));

                    mapPipes.appendChild(mapPipeElement);
                }
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            DOMSource source = new DOMSource(doc);
            StreamResult sr = new StreamResult(os);

            transformer.transform(source, sr);

            os.flush();
            os.close();
        }
        catch (Exception e)
        {
            System.out.println(e);
        }
    }
}
