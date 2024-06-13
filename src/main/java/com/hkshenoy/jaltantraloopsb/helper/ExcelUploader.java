package com.hkshenoy.jaltantraloopsb.helper;

import com.google.gson.Gson;
import com.hkshenoy.jaltantraloopsb.structs.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


@Component
public class ExcelUploader {

    @Value("${JALTANTRA_VERSION}")
    private static String version;


    //generate and upload Excel input network file
    public void uploadExcelInputFile(HttpServletRequest request, HttpServletResponse response)
    {
        try
        {
            OutputStream os = response.getOutputStream();

            Gson gson = new Gson();

            GeneralStruct generalProperties = gson.fromJson(request.getParameter("general"), GeneralStruct.class);
            NodeStruct[] nodesArray = gson.fromJson(request.getParameter("nodes"), NodeStruct[].class);
            PipeStruct[] pipesArray = gson.fromJson(request.getParameter("pipes"), PipeStruct[].class);
            CommercialPipeStruct[] costsArray = gson.fromJson(request.getParameter("commercialpipes"), CommercialPipeStruct[].class);
            MapNodeStruct[] mapnodesArray = gson.fromJson(request.getParameter("mapnodes"), MapNodeStruct[].class);
            MapPipeStruct[] mappipesArray = gson.fromJson(request.getParameter("mappipes"), MapPipeStruct[].class);

            if(generalProperties.name_project==null || generalProperties.name_project.isEmpty())
                generalProperties.name_project="JalTantra Project";

            String filename = generalProperties.name_project+"_input.xls";

            response.setContentType("application/vnd.ms-excel"); // Set up mime type
            response.setHeader("Content-Disposition", "attachment; filename="+filename);

            Workbook wb = new HSSFWorkbook();

            Font font = wb.createFont();
            font.setBold(true);
            CellStyle cellStyle = wb.createCellStyle();
            cellStyle.setFont(font);




            cellStyle.setAlignment(HorizontalAlignment.CENTER);
            cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            cellStyle.setBorderBottom(BorderStyle.THIN);
            cellStyle.setBorderRight(BorderStyle.THIN);
            cellStyle.setBorderLeft(BorderStyle.THIN);
            cellStyle.setBorderTop(BorderStyle.THIN);
            cellStyle.setWrapText(true);

            CellStyle largeBoldCellStyle = wb.createCellStyle();
            largeBoldCellStyle.setAlignment(HorizontalAlignment.CENTER);
            largeBoldCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            font = wb.createFont();
            font.setBold(true);
            font.setFontHeightInPoints((short) 16);
            largeBoldCellStyle.setFont(font);

            CellStyle smallBoldCellStyle = wb.createCellStyle();
            smallBoldCellStyle.setAlignment(HorizontalAlignment.CENTER);
            smallBoldCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            font = wb.createFont();
            font.setBold(true);
            font.setFontHeightInPoints((short) 12);
            smallBoldCellStyle.setFont(font);

            CellStyle integerStyle = wb.createCellStyle();
            DataFormat integerFormat = wb.createDataFormat();
            integerStyle.setDataFormat(integerFormat.getFormat("#,##0"));
            integerStyle.setAlignment(HorizontalAlignment.CENTER);
            integerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            integerStyle.setBorderBottom(BorderStyle.THIN);
            integerStyle.setBorderRight(BorderStyle.THIN);
            integerStyle.setBorderLeft(BorderStyle.THIN);
            integerStyle.setBorderTop(BorderStyle.THIN);

            CellStyle doubleStyle = wb.createCellStyle();
            DataFormat doubleFormat = wb.createDataFormat();
            doubleStyle.setDataFormat(doubleFormat.getFormat("#,##0.00"));
            doubleStyle.setAlignment(HorizontalAlignment.CENTER);
            doubleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            doubleStyle.setBorderBottom(BorderStyle.THIN);
            doubleStyle.setBorderRight(BorderStyle.THIN);
            doubleStyle.setBorderLeft(BorderStyle.THIN);
            doubleStyle.setBorderTop(BorderStyle.THIN);

            Sheet generalSheet = wb.createSheet("General");

            PrintSetup ps = generalSheet.getPrintSetup();

            ps.setScale((short)80);

            int rowindex = 0;
            int noderowindex , piperowindex, costrowindex, esrgeneralrowindex, esrcostrowindex, pumpgeneralrowindex, pumpmanualrowindex, valverowindex, mapnoderowindex, mappiperowindex;
            int generalstart, nodestart , pipestart, coststart, esrgeneralstart, esrcoststart, pumpgeneralstart, pumpmanualstart, valvestart, mapnodestart, mappipestart;

            Row row = generalSheet.createRow(rowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,10));
            rowindex++;
            Cell generalCell = row.createCell(0);
            generalCell.setCellValue("JalTantra: System For Optimization of Piped Water Networks, version:"+version);
            generalCell.setCellStyle(largeBoldCellStyle);
            row.setHeightInPoints((float) 21.75);

            row = generalSheet.createRow(rowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,10));
            rowindex++;
            generalCell = row.createCell(0);
            generalCell.setCellValue("developed by CSE and CTARA departments of IIT Bombay");
            generalCell.setCellStyle(largeBoldCellStyle);
            row.setHeightInPoints((float) 21.75);


            Date date = new Date();
            DateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm a");

            row = generalSheet.createRow(rowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,10));
            rowindex++;

            generalCell = row.createCell(0);
            generalCell.setCellValue(dateFormat.format(date));
            generalCell.setCellStyle(smallBoldCellStyle);

            rowindex = rowindex + 2;
            if(rowindex%2!=0)
                rowindex++;
            generalstart = rowindex;
            row = generalSheet.createRow(rowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
            generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,3,5));
            rowindex++;
            generalCell = row.createCell(0);
            generalCell.setCellValue("Network Name");
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(1);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(2);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(3);
            generalCell.setCellValue(generalProperties.name_project);
            CellStyle tempCellStyle = wb.createCellStyle();
            tempCellStyle.cloneStyleFrom(cellStyle);
            font = wb.createFont();
            font.setBold(true);
            font.setFontHeightInPoints((short) 12);
            tempCellStyle.setFont(font);
            generalCell.setCellStyle(tempCellStyle);
            generalCell = row.createCell(4);
            generalCell.setCellStyle(tempCellStyle);
            generalCell = row.createCell(5);
            generalCell.setCellStyle(tempCellStyle);

            row = generalSheet.createRow(rowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
            rowindex++;
            generalCell = row.createCell(0);
            generalCell.setCellValue("Organization Name");
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(1);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(2);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(3);
            generalCell.setCellValue(generalProperties.name_organization);
            generalCell.setCellStyle(integerStyle);

            row = generalSheet.createRow(rowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
            rowindex++;
            generalCell = row.createCell(0);
            generalCell.setCellValue("Minimum Node Pressure");
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(1);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(2);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(3);
            generalCell.setCellValue(generalProperties.min_node_pressure);
            generalCell.setCellStyle(integerStyle);

            row = generalSheet.createRow(rowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
            rowindex++;
            generalCell = row.createCell(0);
            generalCell.setCellValue("Default Pipe Roughness 'C'");
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(1);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(2);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(3);
            generalCell.setCellValue(generalProperties.def_pipe_roughness);
            generalCell.setCellStyle(integerStyle);

            row = generalSheet.createRow(rowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
            rowindex++;
            generalCell = row.createCell(0);
            generalCell.setCellValue("Minimum Headloss per KM");
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(1);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(2);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(3);
            generalCell.setCellValue(generalProperties.min_hl_perkm);
            generalCell.setCellStyle(doubleStyle);

            row = generalSheet.createRow(rowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
            rowindex++;
            generalCell = row.createCell(0);
            generalCell.setCellValue("Maximum Headloss per KM");
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(1);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(2);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(3);
            generalCell.setCellValue(generalProperties.max_hl_perkm);
            generalCell.setCellStyle(doubleStyle);

            row = generalSheet.createRow(rowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
            rowindex++;
            generalCell = row.createCell(0);
            generalCell.setCellValue("Maximum Water Speed");
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(1);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(2);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(3);
            if(generalProperties.max_water_speed > 0){
                generalCell.setCellValue(generalProperties.max_water_speed);
            }
            generalCell.setCellStyle(doubleStyle);

            row = generalSheet.createRow(rowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
            rowindex++;
            generalCell = row.createCell(0);
            generalCell.setCellValue("Maximum Pipe Pressure");
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(1);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(2);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(3);
            if(generalProperties.max_pipe_pressure > 0){
                generalCell.setCellValue(generalProperties.max_pipe_pressure);
            }
            generalCell.setCellStyle(doubleStyle);

            row = generalSheet.createRow(rowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
            rowindex++;
            generalCell = row.createCell(0);
            generalCell.setCellValue("Number of Supply Hours");
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(1);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(2);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(3);
            generalCell.setCellValue(generalProperties.supply_hours);
            generalCell.setCellStyle(integerStyle);

            row = generalSheet.createRow(rowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
            rowindex++;
            generalCell = row.createCell(0);
            generalCell.setCellValue("Source Node ID");
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(1);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(2);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(3);
            generalCell.setCellValue(generalProperties.source_nodeid);
            generalCell.setCellStyle(integerStyle);

            row = generalSheet.createRow(rowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
            rowindex++;
            generalCell = row.createCell(0);
            generalCell.setCellValue("Source Node Name");
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(1);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(2);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(3);
            generalCell.setCellValue(generalProperties.source_nodename);
            generalCell.setCellStyle(integerStyle);

            row = generalSheet.createRow(rowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
            rowindex++;
            generalCell = row.createCell(0);
            generalCell.setCellValue("Source Elevation");
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(1);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(2);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(3);
            generalCell.setCellValue(generalProperties.source_elevation);
            generalCell.setCellStyle(integerStyle);

            row = generalSheet.createRow(rowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
            rowindex++;
            generalCell = row.createCell(0);
            generalCell.setCellValue("Source Head");
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(1);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(2);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(3);
            generalCell.setCellValue(generalProperties.source_head);
            generalCell.setCellStyle(integerStyle);


            Sheet nodeSheet = generalSheet;//wb.createSheet("Nodes");
            noderowindex = rowindex + 2;
            if(noderowindex%2==0)
                noderowindex++;

            row = nodeSheet.createRow(noderowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(noderowindex,noderowindex,0,4));
            noderowindex++;
            Cell nodeCell = row.createCell(0);
            nodeCell.setCellValue("NODE DATA");
            nodeCell.setCellStyle(cellStyle);
            for(int i=1;i<5;i++)
            {
                nodeCell = row.createCell(i);
                nodeCell.setCellStyle(cellStyle);
            }
            nodestart = noderowindex;
            Row nodeTitleRow = nodeSheet.createRow(noderowindex);
            noderowindex++;
            nodeCell = nodeTitleRow.createCell(0);
            nodeCell.setCellValue("Node ID");
            nodeCell.setCellStyle(cellStyle);
            nodeCell = nodeTitleRow.createCell(1);
            nodeCell.setCellValue("Node Name");
            nodeCell.setCellStyle(cellStyle);
            nodeCell = nodeTitleRow.createCell(2);
            nodeCell.setCellValue("Elevation");
            nodeCell.setCellStyle(cellStyle);
            nodeCell = nodeTitleRow.createCell(3);
            nodeCell.setCellValue("Demand");
            nodeCell.setCellStyle(cellStyle);
            nodeCell = nodeTitleRow.createCell(4);
            nodeCell.setCellValue("Min. Pressure");
            nodeCell.setCellStyle(cellStyle);

            for(NodeStruct node : nodesArray)
            {
                if(node.nodeid==generalProperties.source_nodeid)
                    continue;

                Row nodeRow = nodeSheet.createRow(noderowindex);
                noderowindex++;

                nodeCell = nodeRow.createCell(0);
                nodeCell.setCellValue(node.nodeid);
                nodeCell.setCellStyle(integerStyle);

                nodeCell = nodeRow.createCell(1);
                nodeCell.setCellValue(node.nodename);
                nodeCell.setCellStyle(integerStyle);

                nodeCell = nodeRow.createCell(2);
                nodeCell.setCellValue(node.elevation);
                nodeCell.setCellStyle(integerStyle);

                nodeCell = nodeRow.createCell(3);
                if(node.demand!=0)
                    nodeCell.setCellValue(node.demand);
                nodeCell.setCellStyle(doubleStyle);

                nodeCell = nodeRow.createCell(4);
                if(node.minpressure!=0)
                    nodeCell.setCellValue(node.minpressure);
                nodeCell.setCellStyle(integerStyle);
            }

            Sheet pipeSheet = generalSheet;// wb.createSheet("Pipes");

            piperowindex = noderowindex + 2;
            if(piperowindex%2==0)
                piperowindex++;
            row = generalSheet.createRow(piperowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(piperowindex,piperowindex,0,6));
            piperowindex++;
            Cell pipeCell = row.createCell(0);
            pipeCell.setCellValue("PIPE DATA");
            pipeCell.setCellStyle(cellStyle);
            for(int i=1;i<7;i++)
            {
                pipeCell = row.createCell(i);
                pipeCell.setCellStyle(cellStyle);
            }
            pipestart = piperowindex;
            Row pipeTitleRow = pipeSheet.createRow(piperowindex);
            piperowindex++;
            pipeCell = pipeTitleRow.createCell(0);
            pipeCell.setCellValue("Pipe ID");
            pipeCell.setCellStyle(cellStyle);
            pipeCell = pipeTitleRow.createCell(1);
            pipeCell.setCellValue("Start Node");
            pipeCell.setCellStyle(cellStyle);
            pipeCell = pipeTitleRow.createCell(2);
            pipeCell.setCellValue("End Node");
            pipeCell.setCellStyle(cellStyle);
            pipeCell = pipeTitleRow.createCell(3);
            pipeCell.setCellValue("Length");
            pipeCell.setCellStyle(cellStyle);
            pipeCell = pipeTitleRow.createCell(4);
            pipeCell.setCellValue("Diameter");
            pipeCell.setCellStyle(cellStyle);
            pipeCell = pipeTitleRow.createCell(5);
            pipeCell.setCellValue("Roughness 'C'");
            pipeCell.setCellStyle(cellStyle);
            pipeCell = pipeTitleRow.createCell(6);
            pipeCell.setCellValue("Parallel Allowed");
            pipeCell.setCellStyle(cellStyle);

            for(PipeStruct pipe : pipesArray)
            {
                Row pipeRow = pipeSheet.createRow(piperowindex);
                piperowindex++;

                pipeCell = pipeRow.createCell(0);
                pipeCell.setCellValue(pipe.pipeid);
                pipeCell.setCellStyle(integerStyle);

                pipeCell = pipeRow.createCell(1);
                pipeCell.setCellValue(pipe.startnode);
                pipeCell.setCellStyle(integerStyle);

                pipeCell = pipeRow.createCell(2);
                pipeCell.setCellValue(pipe.endnode);
                pipeCell.setCellStyle(integerStyle);

                pipeCell = pipeRow.createCell(3);
                pipeCell.setCellValue(pipe.length);
                pipeCell.setCellStyle(integerStyle);

                pipeCell = pipeRow.createCell(4);
                if(pipe.diameter!=0)
                    pipeCell.setCellValue(pipe.diameter);
                pipeCell.setCellStyle(integerStyle);

                pipeCell = pipeRow.createCell(5);
                if(pipe.roughness!=0)
                    pipeCell.setCellValue(pipe.roughness);
                pipeCell.setCellStyle(integerStyle);

                pipeCell = pipeRow.createCell(6);
                pipeCell.setCellStyle(integerStyle);
                if(pipe.parallelallowed)
                    pipeCell.setCellValue(pipe.parallelallowed);
            }

            Sheet costSheet = generalSheet; //wb.createSheet("Cost");

            costrowindex = piperowindex + 2;
            if(costrowindex%2==0)
                costrowindex++;
            row = generalSheet.createRow(costrowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(costrowindex,costrowindex,0,2));
            costrowindex++;
            Cell costCell = row.createCell(0);
            costCell.setCellValue("COMMERCIAL PIPE DATA");
            costCell.setCellStyle(cellStyle);
            for(int i=1;i<3;i++)
            {
                costCell = row.createCell(i);
                costCell.setCellStyle(cellStyle);
            }
            coststart = costrowindex;
            Row costTitleRow = costSheet.createRow(costrowindex);
            costrowindex++;
            costCell = costTitleRow.createCell(0);
            costCell.setCellValue("Diameter");
            costCell.setCellStyle(cellStyle);
            costCell = costTitleRow.createCell(1);
            costCell.setCellValue("Roughness");
            costCell.setCellStyle(cellStyle);
            costCell = costTitleRow.createCell(2);
            costCell.setCellValue("Cost");
            costCell.setCellStyle(cellStyle);

            for(CommercialPipeStruct commercialpipe : costsArray)
            {
                Row costRow = costSheet.createRow(costrowindex);
                costrowindex++;

                costCell = costRow.createCell(0);
                costCell.setCellValue(commercialpipe.diameter);
                costCell.setCellStyle(integerStyle);

                costCell = costRow.createCell(1);
                if(commercialpipe.roughness!=0)
                    costCell.setCellValue(commercialpipe.roughness);
                costCell.setCellStyle(integerStyle);

                costCell = costRow.createCell(2);
                costCell.setCellValue(commercialpipe.cost);
                costCell.setCellStyle(integerStyle);
            }

            EsrGeneralStruct esrGeneralProperties = gson.fromJson(request.getParameter("esrGeneral"), EsrGeneralStruct.class);
            EsrCostStruct[] esrCostsArray = gson.fromJson(request.getParameter("esrCost"), EsrCostStruct[].class);

            Sheet esrGeneralSheet = generalSheet;

            esrgeneralrowindex = costrowindex + 2;
            if(esrgeneralrowindex%2==0)
                esrgeneralrowindex++;
            row = esrGeneralSheet.createRow(esrgeneralrowindex);
            esrGeneralSheet.addMergedRegion(new CellRangeAddress(esrgeneralrowindex,esrgeneralrowindex,0,3));
            esrgeneralrowindex++;
            Cell esrGeneralCell = row.createCell(0);
            esrGeneralCell.setCellValue("ESR GENERAL DATA");
            esrGeneralCell.setCellStyle(cellStyle);
            for(int i=1;i<4;i++)
            {
                esrGeneralCell = row.createCell(i);
                esrGeneralCell.setCellStyle(cellStyle);
            }
            esrgeneralstart = esrgeneralrowindex;

            row = esrGeneralSheet.createRow(esrgeneralrowindex);
            esrGeneralSheet.addMergedRegion(new CellRangeAddress(esrgeneralrowindex,esrgeneralrowindex,0,2));
            esrgeneralrowindex++;
            esrGeneralCell = row.createCell(0);
            esrGeneralCell.setCellValue("ESR Enabled");
            esrGeneralCell.setCellStyle(cellStyle);
            esrGeneralCell = row.createCell(1);
            esrGeneralCell.setCellStyle(cellStyle);
            esrGeneralCell = row.createCell(2);
            esrGeneralCell.setCellStyle(cellStyle);
            esrGeneralCell = row.createCell(3);
            esrGeneralCell.setCellValue(esrGeneralProperties.esr_enabled);
            esrGeneralCell.setCellStyle(integerStyle);

            row = esrGeneralSheet.createRow(esrgeneralrowindex);
            esrGeneralSheet.addMergedRegion(new CellRangeAddress(esrgeneralrowindex,esrgeneralrowindex,0,2));
            esrgeneralrowindex++;
            esrGeneralCell = row.createCell(0);
            esrGeneralCell.setCellValue("Secondary Supply Hours");
            esrGeneralCell.setCellStyle(cellStyle);
            esrGeneralCell = row.createCell(1);
            esrGeneralCell.setCellStyle(cellStyle);
            esrGeneralCell = row.createCell(2);
            esrGeneralCell.setCellStyle(cellStyle);
            esrGeneralCell = row.createCell(3);
            esrGeneralCell.setCellValue(esrGeneralProperties.secondary_supply_hours);
            esrGeneralCell.setCellStyle(integerStyle);

            row = esrGeneralSheet.createRow(esrgeneralrowindex);
            esrGeneralSheet.addMergedRegion(new CellRangeAddress(esrgeneralrowindex,esrgeneralrowindex,0,2));
            esrgeneralrowindex++;
            esrGeneralCell = row.createCell(0);
            esrGeneralCell.setCellValue("ESR Capacity Factor");
            esrGeneralCell.setCellStyle(cellStyle);
            esrGeneralCell = row.createCell(1);
            esrGeneralCell.setCellStyle(cellStyle);
            esrGeneralCell = row.createCell(2);
            esrGeneralCell.setCellStyle(cellStyle);
            esrGeneralCell = row.createCell(3);
            esrGeneralCell.setCellValue(esrGeneralProperties.esr_capacity_factor);
            esrGeneralCell.setCellStyle(doubleStyle);

            row = esrGeneralSheet.createRow(esrgeneralrowindex);
            esrGeneralSheet.addMergedRegion(new CellRangeAddress(esrgeneralrowindex,esrgeneralrowindex,0,2));
            esrgeneralrowindex++;
            esrGeneralCell = row.createCell(0);
            esrGeneralCell.setCellValue("Maximum ESR Height");
            esrGeneralCell.setCellStyle(cellStyle);
            esrGeneralCell = row.createCell(1);
            esrGeneralCell.setCellStyle(cellStyle);
            esrGeneralCell = row.createCell(2);
            esrGeneralCell.setCellStyle(cellStyle);
            esrGeneralCell = row.createCell(3);
            esrGeneralCell.setCellValue(esrGeneralProperties.max_esr_height);
            esrGeneralCell.setCellStyle(integerStyle);

            row = esrGeneralSheet.createRow(esrgeneralrowindex);
            esrGeneralSheet.addMergedRegion(new CellRangeAddress(esrgeneralrowindex,esrgeneralrowindex,0,2));
            esrgeneralrowindex++;
            esrGeneralCell = row.createCell(0);
            esrGeneralCell.setCellValue("Allow ESRs at zero demand nodes");
            esrGeneralCell.setCellStyle(cellStyle);
            esrGeneralCell = row.createCell(1);
            esrGeneralCell.setCellStyle(cellStyle);
            esrGeneralCell = row.createCell(2);
            esrGeneralCell.setCellStyle(cellStyle);
            esrGeneralCell = row.createCell(3);
            esrGeneralCell.setCellValue(esrGeneralProperties.allow_dummy);
            esrGeneralCell.setCellStyle(integerStyle);


            String mustHaveEsrs = "";
            if(esrGeneralProperties.must_esr!=null){
                for (int nodeid : esrGeneralProperties.must_esr){
                    mustHaveEsrs = mustHaveEsrs + nodeid + ";";
                }
                if(mustHaveEsrs.length()>0){
                    mustHaveEsrs = mustHaveEsrs.substring(0, mustHaveEsrs.length()-1);
                }
            }

            String mustNotHaveEsrs = "";
            if(esrGeneralProperties.must_not_esr!=null){
                for (int nodeid : esrGeneralProperties.must_not_esr){
                    mustNotHaveEsrs = mustNotHaveEsrs + nodeid + ";";
                }
                if(mustNotHaveEsrs.length()>0){
                    mustNotHaveEsrs = mustNotHaveEsrs.substring(0, mustNotHaveEsrs.length()-1);
                }
            }

            row = esrGeneralSheet.createRow(esrgeneralrowindex);
            esrGeneralSheet.addMergedRegion(new CellRangeAddress(esrgeneralrowindex,esrgeneralrowindex,0,2));
            esrgeneralrowindex++;
            esrGeneralCell = row.createCell(0);
            esrGeneralCell.setCellValue("Nodes that must have ESRs");
            esrGeneralCell.setCellStyle(cellStyle);
            esrGeneralCell = row.createCell(1);
            esrGeneralCell.setCellStyle(cellStyle);
            esrGeneralCell = row.createCell(2);
            esrGeneralCell.setCellStyle(cellStyle);
            esrGeneralCell = row.createCell(3);
            esrGeneralCell.setCellValue(mustHaveEsrs);
            esrGeneralCell.setCellStyle(integerStyle);

            row = esrGeneralSheet.createRow(esrgeneralrowindex);
            esrGeneralSheet.addMergedRegion(new CellRangeAddress(esrgeneralrowindex,esrgeneralrowindex,0,2));
            esrgeneralrowindex++;
            esrGeneralCell = row.createCell(0);
            esrGeneralCell.setCellValue("Nodes that must not have ESRs");
            esrGeneralCell.setCellStyle(cellStyle);
            esrGeneralCell = row.createCell(1);
            esrGeneralCell.setCellStyle(cellStyle);
            esrGeneralCell = row.createCell(2);
            esrGeneralCell.setCellStyle(cellStyle);
            esrGeneralCell = row.createCell(3);
            esrGeneralCell.setCellValue(mustNotHaveEsrs);
            esrGeneralCell.setCellStyle(integerStyle);


            Sheet esrCostSheet = generalSheet; //wb.createSheet("Cost");

            esrcostrowindex = esrgeneralrowindex + 2;
            if(esrcostrowindex%2==0)
                esrcostrowindex++;
            row = generalSheet.createRow(esrcostrowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(esrcostrowindex,esrcostrowindex,0,3));
            esrcostrowindex++;
            Cell esrCostCell = row.createCell(0);
            esrCostCell.setCellValue("ESR COST DATA");
            esrCostCell.setCellStyle(cellStyle);
            for(int i=1;i<4;i++)
            {
                esrCostCell = row.createCell(i);
                esrCostCell.setCellStyle(cellStyle);
            }
            esrcoststart = esrcostrowindex;
            Row esrCostTitleRow = costSheet.createRow(esrcostrowindex);
            esrcostrowindex++;
            esrCostCell = esrCostTitleRow.createCell(0);
            esrCostCell.setCellValue("Minimum Capacity");
            esrCostCell.setCellStyle(cellStyle);
            esrCostCell = esrCostTitleRow.createCell(1);
            esrCostCell.setCellValue("Maximum Capacity");
            esrCostCell.setCellStyle(cellStyle);
            esrCostCell = esrCostTitleRow.createCell(2);
            esrCostCell.setCellValue("Base Cost");
            esrCostCell.setCellStyle(cellStyle);
            esrCostCell = esrCostTitleRow.createCell(3);
            esrCostCell.setCellValue("Unit Cost");
            esrCostCell.setCellStyle(cellStyle);

            for(EsrCostStruct esrCost : esrCostsArray)
            {
                Row esrCostRow = esrCostSheet.createRow(esrcostrowindex);
                esrcostrowindex++;

                esrCostCell = esrCostRow.createCell(0);
                esrCostCell.setCellValue(esrCost.mincapacity);
                esrCostCell.setCellStyle(integerStyle);

                esrCostCell = esrCostRow.createCell(1);
                esrCostCell.setCellValue(esrCost.maxcapacity);
                esrCostCell.setCellStyle(integerStyle);

                esrCostCell = esrCostRow.createCell(2);
                esrCostCell.setCellValue(esrCost.basecost);
                esrCostCell.setCellStyle(integerStyle);

                esrCostCell = esrCostRow.createCell(3);
                esrCostCell.setCellValue(esrCost.unitcost);
                esrCostCell.setCellStyle(doubleStyle);
            }

            PumpGeneralStruct pumpGeneralProperties = gson.fromJson(request.getParameter("pumpGeneral"), PumpGeneralStruct.class);
            PumpManualStruct[] pumpManualArray = gson.fromJson(request.getParameter("pumpManual"), PumpManualStruct[].class);

            Sheet pumpGeneralSheet = generalSheet;

            pumpgeneralrowindex = esrcostrowindex + 2;
            if(pumpgeneralrowindex%2==0)
                pumpgeneralrowindex++;
            row = pumpGeneralSheet.createRow(pumpgeneralrowindex);
            pumpGeneralSheet.addMergedRegion(new CellRangeAddress(pumpgeneralrowindex,pumpgeneralrowindex,0,3));
            pumpgeneralrowindex++;
            Cell pumpGeneralCell = row.createCell(0);
            pumpGeneralCell.setCellValue("PUMP GENERAL DATA");
            pumpGeneralCell.setCellStyle(cellStyle);
            for(int i=1;i<4;i++)
            {
                pumpGeneralCell = row.createCell(i);
                pumpGeneralCell.setCellStyle(cellStyle);
            }
            pumpgeneralstart = pumpgeneralrowindex;

            row = pumpGeneralSheet.createRow(pumpgeneralrowindex);
            pumpGeneralSheet.addMergedRegion(new CellRangeAddress(pumpgeneralrowindex,pumpgeneralrowindex,0,2));
            pumpgeneralrowindex++;
            pumpGeneralCell = row.createCell(0);
            pumpGeneralCell.setCellValue("Pump Enabled");
            pumpGeneralCell.setCellStyle(cellStyle);
            pumpGeneralCell = row.createCell(1);
            pumpGeneralCell.setCellStyle(cellStyle);
            pumpGeneralCell = row.createCell(2);
            pumpGeneralCell.setCellStyle(cellStyle);
            pumpGeneralCell = row.createCell(3);
            pumpGeneralCell.setCellValue(pumpGeneralProperties.pump_enabled);
            pumpGeneralCell.setCellStyle(integerStyle);

            row = pumpGeneralSheet.createRow(pumpgeneralrowindex);
            pumpGeneralSheet.addMergedRegion(new CellRangeAddress(pumpgeneralrowindex,pumpgeneralrowindex,0,2));
            pumpgeneralrowindex++;
            pumpGeneralCell = row.createCell(0);
            pumpGeneralCell.setCellValue("Minimum Pump Size");
            pumpGeneralCell.setCellStyle(cellStyle);
            pumpGeneralCell = row.createCell(1);
            pumpGeneralCell.setCellStyle(cellStyle);
            pumpGeneralCell = row.createCell(2);
            pumpGeneralCell.setCellStyle(cellStyle);
            pumpGeneralCell = row.createCell(3);
            pumpGeneralCell.setCellValue(pumpGeneralProperties.minpumpsize);
            pumpGeneralCell.setCellStyle(integerStyle);

            row = pumpGeneralSheet.createRow(pumpgeneralrowindex);
            pumpGeneralSheet.addMergedRegion(new CellRangeAddress(pumpgeneralrowindex,pumpgeneralrowindex,0,2));
            pumpgeneralrowindex++;
            pumpGeneralCell = row.createCell(0);
            pumpGeneralCell.setCellValue("Pump Efficiency");
            pumpGeneralCell.setCellStyle(cellStyle);
            pumpGeneralCell = row.createCell(1);
            pumpGeneralCell.setCellStyle(cellStyle);
            pumpGeneralCell = row.createCell(2);
            pumpGeneralCell.setCellStyle(cellStyle);
            pumpGeneralCell = row.createCell(3);
            pumpGeneralCell.setCellValue(pumpGeneralProperties.efficiency);
            pumpGeneralCell.setCellStyle(integerStyle);

            row = pumpGeneralSheet.createRow(pumpgeneralrowindex);
            pumpGeneralSheet.addMergedRegion(new CellRangeAddress(pumpgeneralrowindex,pumpgeneralrowindex,0,2));
            pumpgeneralrowindex++;
            pumpGeneralCell = row.createCell(0);
            pumpGeneralCell.setCellValue("Capital Cost per kW");
            pumpGeneralCell.setCellStyle(cellStyle);
            pumpGeneralCell = row.createCell(1);
            pumpGeneralCell.setCellStyle(cellStyle);
            pumpGeneralCell = row.createCell(2);
            pumpGeneralCell.setCellStyle(cellStyle);
            pumpGeneralCell = row.createCell(3);
            pumpGeneralCell.setCellValue(pumpGeneralProperties.capitalcost_per_kw);
            pumpGeneralCell.setCellStyle(integerStyle);

            row = pumpGeneralSheet.createRow(pumpgeneralrowindex);
            pumpGeneralSheet.addMergedRegion(new CellRangeAddress(pumpgeneralrowindex,pumpgeneralrowindex,0,2));
            pumpgeneralrowindex++;
            pumpGeneralCell = row.createCell(0);
            pumpGeneralCell.setCellValue("Energy Cost per kWh");
            pumpGeneralCell.setCellStyle(cellStyle);
            pumpGeneralCell = row.createCell(1);
            pumpGeneralCell.setCellStyle(cellStyle);
            pumpGeneralCell = row.createCell(2);
            pumpGeneralCell.setCellStyle(cellStyle);
            pumpGeneralCell = row.createCell(3);
            pumpGeneralCell.setCellValue(pumpGeneralProperties.energycost_per_kwh);
            pumpGeneralCell.setCellStyle(doubleStyle);

            row = pumpGeneralSheet.createRow(pumpgeneralrowindex);
            pumpGeneralSheet.addMergedRegion(new CellRangeAddress(pumpgeneralrowindex,pumpgeneralrowindex,0,2));
            pumpgeneralrowindex++;
            pumpGeneralCell = row.createCell(0);
            pumpGeneralCell.setCellValue("Design Lifetime");
            pumpGeneralCell.setCellStyle(cellStyle);
            pumpGeneralCell = row.createCell(1);
            pumpGeneralCell.setCellStyle(cellStyle);
            pumpGeneralCell = row.createCell(2);
            pumpGeneralCell.setCellStyle(cellStyle);
            pumpGeneralCell = row.createCell(3);
            pumpGeneralCell.setCellValue(pumpGeneralProperties.design_lifetime);
            pumpGeneralCell.setCellStyle(integerStyle);

            row = pumpGeneralSheet.createRow(pumpgeneralrowindex);
            pumpGeneralSheet.addMergedRegion(new CellRangeAddress(pumpgeneralrowindex,pumpgeneralrowindex,0,2));
            pumpgeneralrowindex++;
            pumpGeneralCell = row.createCell(0);
            pumpGeneralCell.setCellValue("Discount Rate");
            pumpGeneralCell.setCellStyle(cellStyle);
            pumpGeneralCell = row.createCell(1);
            pumpGeneralCell.setCellStyle(cellStyle);
            pumpGeneralCell = row.createCell(2);
            pumpGeneralCell.setCellStyle(cellStyle);
            pumpGeneralCell = row.createCell(3);
            pumpGeneralCell.setCellValue(pumpGeneralProperties.discount_rate);
            pumpGeneralCell.setCellStyle(doubleStyle);

            row = pumpGeneralSheet.createRow(pumpgeneralrowindex);
            pumpGeneralSheet.addMergedRegion(new CellRangeAddress(pumpgeneralrowindex,pumpgeneralrowindex,0,2));
            pumpgeneralrowindex++;
            pumpGeneralCell = row.createCell(0);
            pumpGeneralCell.setCellValue("Inflation Rate");
            pumpGeneralCell.setCellStyle(cellStyle);
            pumpGeneralCell = row.createCell(1);
            pumpGeneralCell.setCellStyle(cellStyle);
            pumpGeneralCell = row.createCell(2);
            pumpGeneralCell.setCellStyle(cellStyle);
            pumpGeneralCell = row.createCell(3);
            pumpGeneralCell.setCellValue(pumpGeneralProperties.inflation_rate);
            pumpGeneralCell.setCellStyle(doubleStyle);

            String mustNotHavePumps = "";
            if(pumpGeneralProperties.must_not_pump!=null){
                for (int pipeid : pumpGeneralProperties.must_not_pump){
                    mustNotHavePumps = mustNotHavePumps + pipeid + ";";
                }
                if(mustNotHavePumps.length()>0){
                    mustNotHavePumps = mustNotHavePumps.substring(0, mustNotHavePumps.length()-1);
                }
            }

            row = pumpGeneralSheet.createRow(pumpgeneralrowindex);
            pumpGeneralSheet.addMergedRegion(new CellRangeAddress(pumpgeneralrowindex,pumpgeneralrowindex,0,2));
            pumpgeneralrowindex++;
            pumpGeneralCell = row.createCell(0);
            pumpGeneralCell.setCellValue("Pipes that must not have Pumps");
            pumpGeneralCell.setCellStyle(cellStyle);
            pumpGeneralCell = row.createCell(1);
            pumpGeneralCell.setCellStyle(cellStyle);
            pumpGeneralCell = row.createCell(2);
            pumpGeneralCell.setCellStyle(cellStyle);
            pumpGeneralCell = row.createCell(3);
            pumpGeneralCell.setCellValue(mustNotHavePumps);
            pumpGeneralCell.setCellStyle(integerStyle);

            Sheet pumpManualSheet = generalSheet; //wb.createSheet("Cost");

            pumpmanualrowindex = pumpgeneralrowindex + 2;
            if(pumpmanualrowindex%2==0)
                pumpmanualrowindex++;
            row = pumpManualSheet.createRow(pumpmanualrowindex);
            pumpManualSheet.addMergedRegion(new CellRangeAddress(pumpmanualrowindex,pumpmanualrowindex,0,1));
            pumpmanualrowindex++;
            Cell pumpManualCell = row.createCell(0);
            pumpManualCell.setCellValue("MANUAL PUMPS DATA");
            pumpManualCell.setCellStyle(cellStyle);
            for(int i=1;i<2;i++){
                pumpManualCell = row.createCell(i);
                pumpManualCell.setCellStyle(cellStyle);
            }
            pumpmanualstart = pumpmanualrowindex;
            Row pumpManualTitleRow = pumpManualSheet.createRow(pumpmanualrowindex);
            pumpmanualrowindex++;
            pumpManualCell = pumpManualTitleRow.createCell(0);
            pumpManualCell.setCellValue("Pipe ID");
            pumpManualCell.setCellStyle(cellStyle);
            pumpManualCell = pumpManualTitleRow.createCell(1);
            pumpManualCell.setCellValue("Pump Power");
            pumpManualCell.setCellStyle(cellStyle);

            for(PumpManualStruct p : pumpManualArray){
                Row pumpManualRow = pumpManualSheet.createRow(pumpmanualrowindex);
                pumpmanualrowindex++;

                pumpManualCell = pumpManualRow.createCell(0);
                pumpManualCell.setCellValue(p.pipeid);
                pumpManualCell.setCellStyle(integerStyle);

                pumpManualCell = pumpManualRow.createCell(1);
                pumpManualCell.setCellValue(p.pumppower);
                pumpManualCell.setCellStyle(integerStyle);
            }

            ValveStruct[] valves = gson.fromJson(request.getParameter("valves"), ValveStruct[].class);

            Sheet valveSheet = generalSheet; //wb.createSheet("Cost");

            valverowindex = pumpmanualrowindex + 2;
            if(valverowindex%2==0)
                valverowindex++;
            row = valveSheet.createRow(valverowindex);
            valveSheet.addMergedRegion(new CellRangeAddress(valverowindex,valverowindex,0,1));
            valverowindex++;
            Cell valveCell = row.createCell(0);
            valveCell.setCellValue("VALVES DATA");
            valveCell.setCellStyle(cellStyle);
            for(int i=1;i<2;i++){
                valveCell = row.createCell(i);
                valveCell.setCellStyle(cellStyle);
            }
            valvestart = valverowindex;
            Row valveTitleRow = valveSheet.createRow(valverowindex);
            valverowindex++;
            valveCell = valveTitleRow.createCell(0);
            valveCell.setCellValue("Pipe ID");
            valveCell.setCellStyle(cellStyle);
            valveCell = valveTitleRow.createCell(1);
            valveCell.setCellValue("Valve Setting");
            valveCell.setCellStyle(cellStyle);

            for(ValveStruct v : valves){
                Row valveRow = valveSheet.createRow(valverowindex);
                valverowindex++;

                valveCell = valveRow.createCell(0);
                valveCell.setCellValue(v.pipeid);
                valveCell.setCellStyle(integerStyle);

                valveCell = valveRow.createCell(1);
                valveCell.setCellValue(v.valvesetting);
                valveCell.setCellStyle(integerStyle);
            }

            Sheet mapNodeSheet = generalSheet;

            mapnoderowindex = valverowindex + 2;
            if(mapnoderowindex%2==1)
                mapnoderowindex++;

            String mapSourceNode = request.getParameter("mapsource");
            row = generalSheet.createRow(mapnoderowindex);
            mapnoderowindex++;
            Cell mapnodeCell = row.createCell(0);
            mapnodeCell.setCellValue("Map Source Node");
            mapnodeCell.setCellStyle(cellStyle);
            mapnodeCell = row.createCell(1);
            mapnodeCell.setCellValue(mapSourceNode);
            mapnodeCell.setCellStyle(integerStyle);

            row = generalSheet.createRow(mapnoderowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(mapnoderowindex,mapnoderowindex,0,4));
            mapnoderowindex++;
            mapnodeCell = row.createCell(0);
            mapnodeCell.setCellValue("MAP NODE DATA");
            mapnodeCell.setCellStyle(cellStyle);
            for(int i=1;i<5;i++)
            {
                mapnodeCell = row.createCell(i);
                mapnodeCell.setCellStyle(cellStyle);
            }

            mapnodestart = mapnoderowindex;
            Row mapnodeTitleRow = costSheet.createRow(mapnoderowindex);
            mapnoderowindex++;
            mapnodeCell = mapnodeTitleRow.createCell(0);
            mapnodeCell.setCellValue("Node Name");
            mapnodeCell.setCellStyle(cellStyle);
            mapnodeCell = mapnodeTitleRow.createCell(1);
            mapnodeCell.setCellValue("Node ID");
            mapnodeCell.setCellStyle(cellStyle);
            mapnodeCell = mapnodeTitleRow.createCell(2);
            mapnodeCell.setCellValue("Latitude");
            mapnodeCell.setCellStyle(cellStyle);
            mapnodeCell = mapnodeTitleRow.createCell(3);
            mapnodeCell.setCellValue("Longitude");
            mapnodeCell.setCellStyle(cellStyle);
            mapnodeCell = mapnodeTitleRow.createCell(4);
            mapnodeCell.setCellValue("Is ESR");
            mapnodeCell.setCellStyle(cellStyle);

            for(MapNodeStruct mapnode : mapnodesArray)
            {
                Row mapnodeRow = mapNodeSheet.createRow(mapnoderowindex);
                mapnoderowindex++;

                mapnodeCell = mapnodeRow.createCell(0);
                mapnodeCell.setCellValue(mapnode.nodename);
                mapnodeCell.setCellStyle(cellStyle);

                mapnodeCell = mapnodeRow.createCell(1);
                mapnodeCell.setCellValue(mapnode.nodeid);
                mapnodeCell.setCellStyle(integerStyle);

                mapnodeCell = mapnodeRow.createCell(2);
                mapnodeCell.setCellValue(mapnode.latitude);
                mapnodeCell.setCellStyle(doubleStyle);

                mapnodeCell = mapnodeRow.createCell(3);
                mapnodeCell.setCellValue(mapnode.longitude);
                mapnodeCell.setCellStyle(doubleStyle);

                mapnodeCell = mapnodeRow.createCell(4);
                mapnodeCell.setCellValue(mapnode.isesr);
                mapnodeCell.setCellStyle(integerStyle);
            }

            Sheet mapPipeSheet = generalSheet;

            mappiperowindex = mapnoderowindex + 2;
            if(mappiperowindex%2==0)
                mappiperowindex++;
            row = generalSheet.createRow(mappiperowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(mappiperowindex,mappiperowindex,0,3));
            mappiperowindex++;
            Cell mappipeCell = row.createCell(0);
            mappipeCell.setCellValue("MAP PIPE DATA");
            mappipeCell.setCellStyle(cellStyle);
            for(int i=1;i<4;i++)
            {
                mappipeCell = row.createCell(i);
                mappipeCell.setCellStyle(cellStyle);
            }
            mappipestart = mappiperowindex;
            Row mappipeTitleRow = costSheet.createRow(mappiperowindex);
            mappiperowindex++;
            mappipeCell = mappipeTitleRow.createCell(0);
            mappipeCell.setCellValue("Origin ID");
            mappipeCell.setCellStyle(cellStyle);
            mappipeCell = mappipeTitleRow.createCell(1);
            mappipeCell.setCellValue("Destination ID");
            mappipeCell.setCellStyle(cellStyle);
            mappipeCell = mappipeTitleRow.createCell(2);
            mappipeCell.setCellValue("Length");
            mappipeCell.setCellStyle(cellStyle);
            mappipeCell = mappipeTitleRow.createCell(3);
            mappipeCell.setCellValue("Encoded Path");
            mappipeCell.setCellStyle(cellStyle);


            // adding auto size for 4th column here to avoid resizing encoded path column
            generalSheet.autoSizeColumn(3);


            for(MapPipeStruct mappipe : mappipesArray)
            {
                Row mappipeRow = mapPipeSheet.createRow(mappiperowindex);
                mappiperowindex++;

                mappipeCell = mappipeRow.createCell(0);
                mappipeCell.setCellValue(mappipe.originid);
                mappipeCell.setCellStyle(integerStyle);

                mappipeCell = mappipeRow.createCell(1);
                mappipeCell.setCellValue(mappipe.destinationid);
                mappipeCell.setCellStyle(integerStyle);

                mappipeCell = mappipeRow.createCell(2);
                mappipeCell.setCellValue(mappipe.length);
                mappipeCell.setCellStyle(doubleStyle);

                mappipeCell = mappipeRow.createCell(3);
                mappipeCell.setCellValue(mappipe.encodedpath);
                mappipeCell.setCellStyle(doubleStyle);
            }

            SheetConditionalFormatting sheetCF = generalSheet.getSheetConditionalFormatting();

            ConditionalFormattingRule rule1 = sheetCF.createConditionalFormattingRule("MOD(ROW(),2)");
            PatternFormatting fill1 = rule1.createPatternFormatting();
            fill1.setFillBackgroundColor(IndexedColors.PALE_BLUE.index);
            fill1.setFillPattern(PatternFormatting.SOLID_FOREGROUND);

            CellRangeAddress[] regions = {
                    CellRangeAddress.valueOf("A"+generalstart+":D"+rowindex),
                    CellRangeAddress.valueOf("A"+nodestart+":E"+noderowindex),
                    CellRangeAddress.valueOf("A"+pipestart+":G"+piperowindex),
                    CellRangeAddress.valueOf("A"+coststart+":C"+costrowindex),
                    CellRangeAddress.valueOf("A"+esrgeneralstart+":D"+esrgeneralrowindex),
                    CellRangeAddress.valueOf("A"+esrcoststart+":D"+esrcostrowindex),
                    CellRangeAddress.valueOf("A"+pumpgeneralstart+":D"+pumpgeneralrowindex),
                    CellRangeAddress.valueOf("A"+pumpmanualstart+":B"+pumpmanualrowindex),
                    CellRangeAddress.valueOf("A"+valvestart+":B"+valverowindex),
                    CellRangeAddress.valueOf("A"+mapnodestart+":E"+mapnoderowindex),
                    CellRangeAddress.valueOf("A"+mappipestart+":D"+mappiperowindex),
            };

            sheetCF.addConditionalFormatting(regions, rule1);

            for(int i=0;i<7;i++) {
                if(i==3) continue; //4th column resize
                generalSheet.autoSizeColumn(i);
            }
            wb.write(os);
            wb.close();

            os.flush();
            os.close();
        }
        catch (Exception e)
        {
            System.out.println(e);
        }
    }
}
