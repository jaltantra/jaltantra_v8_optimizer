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

import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


@Component
public class ExcelOutputUploader {

    //generate and upload Excel output network file
    @Value("${JALTANTRA_VERSION}")
    private static String version;



    public void uploadExcelOutputFile(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        try
        {
            OutputStream os = response.getOutputStream();

            Gson gson = new Gson();

            GeneralStruct generalProperties = gson.fromJson(request.getParameter("general"), GeneralStruct.class);
            NodeStruct[] resultNodesArray = gson.fromJson(request.getParameter("resultnodes"), NodeStruct[].class);
            PipeStruct[] resultPipesArray = gson.fromJson(request.getParameter("resultpipes"), PipeStruct[].class);
            CommercialPipeStruct[] resultCostsArray = gson.fromJson(request.getParameter("resultcosts"), CommercialPipeStruct[].class);
            EsrGeneralStruct esrGeneralProperties = gson.fromJson(request.getParameter("esrGeneral"), EsrGeneralStruct.class);
            ResultEsrStruct[] resultEsrsArray = gson.fromJson(request.getParameter("resultesrs"), ResultEsrStruct[].class);
            PumpGeneralStruct pumpGeneralProperties = gson.fromJson(request.getParameter("pumpGeneral"), PumpGeneralStruct.class);
            ResultPumpStruct[] resultPumpsArray = gson.fromJson(request.getParameter("resultpumps"), ResultPumpStruct[].class);
            //ValveStruct[] valves = gson.fromJson(request.getParameter("valves"), ValveStruct[].class);

            if(generalProperties.name_project==null || generalProperties.name_project.isEmpty())
                generalProperties.name_project="JalTantra Project";

            String filename = generalProperties.name_project+"_output.xls";

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

            CellStyle redColorCellStyle = wb.createCellStyle();
            redColorCellStyle.setFont(font);
            //redColorCellStyle.setAlignment(HorizontalAlignment.CENTER);
            redColorCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            redColorCellStyle.setBorderBottom(BorderStyle.THIN);
            redColorCellStyle.setBorderRight(BorderStyle.THIN);
            redColorCellStyle.setBorderLeft(BorderStyle.THIN);
            redColorCellStyle.setBorderTop(BorderStyle.THIN);
            redColorCellStyle.setWrapText(true);
            redColorCellStyle.setFillBackgroundColor(IndexedColors.RED.getIndex());

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

            CellStyle doubleStyle2 = wb.createCellStyle();
            DataFormat doubleFormat2 = wb.createDataFormat();
            doubleStyle2.setDataFormat(doubleFormat2.getFormat("#,##0.000"));
            doubleStyle2.setAlignment(HorizontalAlignment.CENTER);
            doubleStyle2.setVerticalAlignment(VerticalAlignment.CENTER);
            doubleStyle2.setBorderBottom(BorderStyle.THIN);
            doubleStyle2.setBorderRight(BorderStyle.THIN);
            doubleStyle2.setBorderLeft(BorderStyle.THIN);
            doubleStyle2.setBorderTop(BorderStyle.THIN);

            Sheet generalSheet = wb.createSheet("General");

            PrintSetup ps = generalSheet.getPrintSetup();

            ps.setScale((short)80);

            int rowindex = 0;
            int noderowindex , piperowindex, costrowindex, esrcostrowindex, pumpcostrowindex;
            int generalstart, nodestart , pipestart, coststart, esrcoststart, pumpcoststart;

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
            generalCell.setCellStyle(doubleStyle2);

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
            generalCell.setCellStyle(doubleStyle2);

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
            if(generalProperties.max_water_speed>0)
                generalCell.setCellValue(generalProperties.max_water_speed);
            generalCell.setCellStyle(doubleStyle2);

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
            if(generalProperties.max_pipe_pressure>0)
                generalCell.setCellValue(generalProperties.max_pipe_pressure);
            generalCell.setCellStyle(doubleStyle2);

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
            generalCell.setCellStyle(doubleStyle);

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
            generalCell.setCellStyle(doubleStyle);

            row = generalSheet.createRow(rowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
            rowindex++;
            generalCell = row.createCell(0);
            generalCell.setCellValue("Number of Nodes");
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(1);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(2);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(3);
            generalCell.setCellValue(resultNodesArray.length);
            generalCell.setCellStyle(integerStyle);

            row = generalSheet.createRow(rowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
            rowindex++;
            generalCell = row.createCell(0);
            generalCell.setCellValue("ESR Enabled");
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(1);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(2);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(3);
            generalCell.setCellValue(esrGeneralProperties.esr_enabled);
            generalCell.setCellStyle(integerStyle);

            if(esrGeneralProperties.esr_enabled){

                row = generalSheet.createRow(rowindex);
                generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
                rowindex++;
                generalCell = row.createCell(0);
                generalCell.setCellValue("Secondary Supply Hours");
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(1);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(2);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(3);
                generalCell.setCellValue(esrGeneralProperties.secondary_supply_hours);
                generalCell.setCellStyle(integerStyle);

                row = generalSheet.createRow(rowindex);
                generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
                rowindex++;
                generalCell = row.createCell(0);
                generalCell.setCellValue("ESR Capacity Factor");
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(1);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(2);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(3);
                generalCell.setCellValue(esrGeneralProperties.esr_capacity_factor);
                generalCell.setCellStyle(doubleStyle);

                row = generalSheet.createRow(rowindex);
                generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
                rowindex++;
                generalCell = row.createCell(0);
                generalCell.setCellValue("Maximum ESR Height");
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(1);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(2);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(3);
                generalCell.setCellValue(esrGeneralProperties.max_esr_height);
                generalCell.setCellStyle(integerStyle);

                row = generalSheet.createRow(rowindex);
                generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
                rowindex++;
                generalCell = row.createCell(0);
                generalCell.setCellValue("Allow ESRs at zero demand nodes");
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(1);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(2);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(3);
                generalCell.setCellValue(esrGeneralProperties.allow_dummy);
                generalCell.setCellStyle(integerStyle);

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

                row = generalSheet.createRow(rowindex);
                generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
                rowindex++;
                generalCell = row.createCell(0);
                generalCell.setCellValue("Nodes that must have ESRs");
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(1);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(2);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(3);
                generalCell.setCellValue(mustHaveEsrs);
                generalCell.setCellStyle(integerStyle);

                row = generalSheet.createRow(rowindex);
                generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
                rowindex++;
                generalCell = row.createCell(0);
                generalCell.setCellValue("Nodes that must not have ESRs");
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(1);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(2);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(3);
                generalCell.setCellValue(mustNotHaveEsrs);
                generalCell.setCellStyle(integerStyle);
            }

            row = generalSheet.createRow(rowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
            rowindex++;
            generalCell = row.createCell(0);
            generalCell.setCellValue("Pump Enabled");
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(1);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(2);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(3);
            generalCell.setCellValue(pumpGeneralProperties.pump_enabled);
            generalCell.setCellStyle(integerStyle);

            if(pumpGeneralProperties.pump_enabled){
                row = generalSheet.createRow(rowindex);
                generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
                rowindex++;
                generalCell = row.createCell(0);
                generalCell.setCellValue("Minimum Pump Size");
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(1);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(2);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(3);
                generalCell.setCellValue(pumpGeneralProperties.minpumpsize);
                generalCell.setCellStyle(doubleStyle);

                row = generalSheet.createRow(rowindex);
                generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
                rowindex++;
                generalCell = row.createCell(0);
                generalCell.setCellValue("Pump Efficiency");
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(1);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(2);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(3);
                generalCell.setCellValue(pumpGeneralProperties.efficiency);
                generalCell.setCellStyle(integerStyle);

                row = generalSheet.createRow(rowindex);
                generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
                rowindex++;
                generalCell = row.createCell(0);
                generalCell.setCellValue("Capital Cost per kW");
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(1);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(2);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(3);
                generalCell.setCellValue(pumpGeneralProperties.capitalcost_per_kw);
                generalCell.setCellStyle(integerStyle);

                row = generalSheet.createRow(rowindex);
                generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
                rowindex++;
                generalCell = row.createCell(0);
                generalCell.setCellValue("Energy Cost per kWh");
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(1);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(2);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(3);
                generalCell.setCellValue(pumpGeneralProperties.energycost_per_kwh);
                generalCell.setCellStyle(doubleStyle);

                row = generalSheet.createRow(rowindex);
                generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
                rowindex++;
                generalCell = row.createCell(0);
                generalCell.setCellValue("Design Lifetime");
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(1);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(2);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(3);
                generalCell.setCellValue(pumpGeneralProperties.design_lifetime);
                generalCell.setCellStyle(integerStyle);

                row = generalSheet.createRow(rowindex);
                generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
                rowindex++;
                generalCell = row.createCell(0);
                generalCell.setCellValue("Discount Rate");
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(1);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(2);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(3);
                generalCell.setCellValue(pumpGeneralProperties.discount_rate);
                generalCell.setCellStyle(doubleStyle);

                row = generalSheet.createRow(rowindex);
                generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
                rowindex++;
                generalCell = row.createCell(0);
                generalCell.setCellValue("Inflation Rate");
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(1);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(2);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(3);
                generalCell.setCellValue(pumpGeneralProperties.inflation_rate);
                generalCell.setCellStyle(doubleStyle);

                String mustNotHavePumps = "";
                if(pumpGeneralProperties.must_not_pump!=null){
                    for (int pipeid : pumpGeneralProperties.must_not_pump){
                        mustNotHavePumps = mustNotHavePumps + pipeid + ";";
                    }
                    if(mustNotHavePumps.length()>0){
                        mustNotHavePumps = mustNotHavePumps.substring(0, mustNotHavePumps.length()-1);
                    }
                }

                row = generalSheet.createRow(rowindex);
                generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
                rowindex++;
                generalCell = row.createCell(0);
                generalCell.setCellValue("Pipes that must not have Pumps");
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(1);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(2);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(3);
                generalCell.setCellValue(mustNotHavePumps);
                generalCell.setCellStyle(integerStyle);
            }

            Sheet nodeSheet = generalSheet;//wb.createSheet("Nodes");
            noderowindex = rowindex + 10;
            if(noderowindex%2==0)
                noderowindex++;

            row = nodeSheet.createRow(noderowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(noderowindex,noderowindex,0,6));
            noderowindex++;
            Cell nodeCell = row.createCell(0);
            nodeCell.setCellValue("NODE RESULTS");
            nodeCell.setCellStyle(cellStyle);
            for(int i=1;i<7;i++)
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
            nodeCell.setCellValue("Demand");
            nodeCell.setCellStyle(cellStyle);
            nodeCell = nodeTitleRow.createCell(3);
            nodeCell.setCellValue("Elevation");
            nodeCell.setCellStyle(cellStyle);
            nodeCell = nodeTitleRow.createCell(4);
            nodeCell.setCellValue("Head");
            nodeCell.setCellStyle(cellStyle);
            nodeCell = nodeTitleRow.createCell(5);
            nodeCell.setCellValue("Pressure");
            nodeCell.setCellStyle(cellStyle);
            nodeCell = nodeTitleRow.createCell(6);
            nodeCell.setCellValue("Min. Pressure");
            nodeCell.setCellStyle(cellStyle);

            for(NodeStruct node : resultNodesArray)
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
                nodeCell.setCellValue(node.demand);
                nodeCell.setCellStyle(doubleStyle2);

                nodeCell = nodeRow.createCell(3);
                nodeCell.setCellValue(node.elevation);
                nodeCell.setCellStyle(doubleStyle);

                nodeCell = nodeRow.createCell(4);
                nodeCell.setCellValue(node.head);
                nodeCell.setCellStyle(doubleStyle);

                nodeCell = nodeRow.createCell(5);
                nodeCell.setCellValue(node.pressure);
                nodeCell.setCellStyle(doubleStyle);

                nodeCell = nodeRow.createCell(6);
                nodeCell.setCellValue(node.minpressure);
                nodeCell.setCellStyle(integerStyle);
            }

            Sheet pipeSheet = generalSheet;// wb.createSheet("Pipes");

            piperowindex = noderowindex + 2;
            if(piperowindex%2==0)
                piperowindex++;
            row = generalSheet.createRow(piperowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(piperowindex,piperowindex,0,14));
            piperowindex++;
            Cell pipeCell = row.createCell(0);
            pipeCell.setCellValue("PIPE RESULTS");
            pipeCell.setCellStyle(cellStyle);
            for(int i=1;i<15;i++)
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
            pipeCell.setCellValue("Flow");
            pipeCell.setCellStyle(cellStyle);
            pipeCell = pipeTitleRow.createCell(5);
            pipeCell.setCellValue("Speed");
            pipeCell.setCellStyle(cellStyle);
            pipeCell = pipeTitleRow.createCell(6);
            pipeCell.setCellValue("Diameter");
            pipeCell.setCellStyle(cellStyle);
            pipeCell = pipeTitleRow.createCell(7);
            pipeCell.setCellValue("Roughness 'C'");
            pipeCell.setCellStyle(cellStyle);
            pipeCell = pipeTitleRow.createCell(8);
            pipeCell.setCellValue("Headloss");
            pipeCell.setCellStyle(cellStyle);
            pipeCell = pipeTitleRow.createCell(9);
            pipeCell.setCellValue("Headloss per KM");
            pipeCell.setCellStyle(cellStyle);
            pipeCell = pipeTitleRow.createCell(10);
            pipeCell.setCellValue("Cost");
            pipeCell.setCellStyle(cellStyle);
            pipeCell = pipeTitleRow.createCell(11);
            pipeCell.setCellValue("Pump Head");
            pipeCell.setCellStyle(cellStyle);
            pipeCell = pipeTitleRow.createCell(12);
            pipeCell.setCellValue("Pump Power");
            pipeCell.setCellStyle(cellStyle);
            pipeCell = pipeTitleRow.createCell(13);
            pipeCell.setCellValue("Valve Setting");
            pipeCell.setCellStyle(cellStyle);
            pipeCell = pipeTitleRow.createCell(14);
            pipeCell.setCellValue("Status");
            pipeCell.setCellStyle(cellStyle);

            Font tempFont = wb.createFont();
            tempFont.setColor(IndexedColors.RED.getIndex());

            CellStyle tempIntegerStyle = wb.createCellStyle();
            tempIntegerStyle.setDataFormat(integerFormat.getFormat("#,##0"));
            tempIntegerStyle.setAlignment(HorizontalAlignment.CENTER);
            tempIntegerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            tempIntegerStyle.setBorderBottom(BorderStyle.THIN);
            tempIntegerStyle.setBorderRight(BorderStyle.THIN);
            tempIntegerStyle.setBorderLeft(BorderStyle.THIN);
            tempIntegerStyle.setBorderTop(BorderStyle.THIN);
            tempIntegerStyle.setFont(tempFont);

            CellStyle tempDoubleStyle = wb.createCellStyle();
            tempDoubleStyle.setDataFormat(doubleFormat.getFormat("#,##0.00"));
            tempDoubleStyle.setAlignment(HorizontalAlignment.CENTER);
            tempDoubleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            tempDoubleStyle.setBorderBottom(BorderStyle.THIN);
            tempDoubleStyle.setBorderRight(BorderStyle.THIN);
            tempDoubleStyle.setBorderLeft(BorderStyle.THIN);
            tempDoubleStyle.setBorderTop(BorderStyle.THIN);
            tempDoubleStyle.setFont(tempFont);

            CellStyle tempDoubleStyle2 = wb.createCellStyle();
            tempDoubleStyle2.setDataFormat(doubleFormat2.getFormat("#,##0.000"));
            tempDoubleStyle2.setAlignment(HorizontalAlignment.CENTER);
            tempDoubleStyle2.setVerticalAlignment(VerticalAlignment.CENTER);
            tempDoubleStyle2.setBorderBottom(BorderStyle.THIN);
            tempDoubleStyle2.setBorderRight(BorderStyle.THIN);
            tempDoubleStyle2.setBorderLeft(BorderStyle.THIN);
            tempDoubleStyle2.setBorderTop(BorderStyle.THIN);
            tempDoubleStyle2.setFont(tempFont);

            CellStyle useIntegerStyle, useDoubleStyle, useDoubleStyle2;

            double cumulativeOldLength = 0;
            for(PipeStruct pipe : resultPipesArray)
            {
                Row pipeRow = pipeSheet.createRow(piperowindex);
                piperowindex++;

                if(pipe.pressureexceeded){
                    useIntegerStyle = tempIntegerStyle;
                    useDoubleStyle = tempDoubleStyle;
                    useDoubleStyle2 = tempDoubleStyle2;
                }
                else{
                    useIntegerStyle = integerStyle;
                    useDoubleStyle = doubleStyle;
                    useDoubleStyle2 = doubleStyle2;
                }

                if(!pipe.parallelallowed)
                {
                    pipeCell = pipeRow.createCell(0);
                    pipeCell.setCellValue(pipe.pipeid);
                    pipeCell.setCellStyle(useIntegerStyle);

                    pipeCell = pipeRow.createCell(1);
                    pipeCell.setCellValue(pipe.startnode);
                    pipeCell.setCellStyle(useIntegerStyle);

                    pipeCell = pipeRow.createCell(2);
                    pipeCell.setCellValue(pipe.endnode);
                    pipeCell.setCellStyle(useIntegerStyle);
                }
                else
                {
                    pipeCell = pipeRow.createCell(0);
                    pipeCell.setCellStyle(useIntegerStyle);

                    pipeCell = pipeRow.createCell(1);
                    pipeCell.setCellStyle(useIntegerStyle);

                    pipeCell = pipeRow.createCell(2);
                    pipeCell.setCellStyle(useIntegerStyle);
                }

                pipeCell = pipeRow.createCell(3);
                pipeCell.setCellValue(pipe.length);
                pipeCell.setCellStyle(useIntegerStyle);

                pipeCell = pipeRow.createCell(4);
                pipeCell.setCellValue(pipe.flow);
                pipeCell.setCellStyle(useDoubleStyle);

                pipeCell = pipeRow.createCell(5);
                pipeCell.setCellValue(pipe.speed);
                pipeCell.setCellStyle(useDoubleStyle2);

                pipeCell = pipeRow.createCell(6);
                pipeCell.setCellValue(pipe.diameter);
                pipeCell.setCellStyle(useIntegerStyle);

                pipeCell = pipeRow.createCell(7);
                pipeCell.setCellValue(pipe.roughness);
                pipeCell.setCellStyle(useIntegerStyle);

                pipeCell = pipeRow.createCell(8);
                pipeCell.setCellValue(pipe.headloss);
                pipeCell.setCellStyle(useDoubleStyle2);

                pipeCell = pipeRow.createCell(9);
                pipeCell.setCellValue(pipe.headlossperkm);
                pipeCell.setCellStyle(useDoubleStyle2);

                pipeCell = pipeRow.createCell(10);
                pipeCell.setCellValue(pipe.cost);
                pipeCell.setCellStyle(useIntegerStyle);

                pipeCell = pipeRow.createCell(11);
                if(pipe.pumphead>0)
                    pipeCell.setCellValue(pipe.pumphead);
                pipeCell.setCellStyle(useDoubleStyle);

                pipeCell = pipeRow.createCell(12);
                if(pipe.pumppower>0)
                    pipeCell.setCellValue(pipe.pumppower);
                pipeCell.setCellStyle(useDoubleStyle);

                pipeCell = pipeRow.createCell(13);
                if(pipe.valvesetting>0)
                    pipeCell.setCellValue(pipe.valvesetting);
                pipeCell.setCellStyle(useDoubleStyle);

                pipeCell = pipeRow.createCell(14);
                pipeCell.setCellStyle(useIntegerStyle);
                if(pipe.parallelallowed)
                {
                    pipeCell.setCellValue("Parallel");
                    cumulativeOldLength += pipe.length;
                }
            }

            Sheet costSheet = generalSheet; //wb.createSheet("Cost");

            costrowindex = piperowindex + 2;
            if(costrowindex%2==0)
                costrowindex++;
            row = generalSheet.createRow(costrowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(costrowindex,costrowindex,0,3));
            costrowindex++;
            Cell costCell = row.createCell(0);
            costCell.setCellValue("COST RESULTS OF NEW PIPES");
            costCell.setCellStyle(cellStyle);
            for(int i=1;i<4;i++)
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
            costCell.setCellValue("Length");
            costCell.setCellStyle(cellStyle);
            costCell = costTitleRow.createCell(2);
            costCell.setCellValue("Cost");
            costCell.setCellStyle(cellStyle);
            costCell = costTitleRow.createCell(3);
            costCell.setCellValue("Cumulative Cost");
            costCell.setCellStyle(cellStyle);

            double cumulativeCostValue = 0;
            double cumulativeLength = 0;
            for(CommercialPipeStruct commercialpipe : resultCostsArray)
            {
                Row costRow = costSheet.createRow(costrowindex);
                costrowindex++;

                costCell = costRow.createCell(0);
                costCell.setCellValue(commercialpipe.diameter);
                costCell.setCellStyle(doubleStyle);

                costCell = costRow.createCell(1);
                costCell.setCellValue(commercialpipe.length);
                costCell.setCellStyle(integerStyle);

                costCell = costRow.createCell(2);
                costCell.setCellValue(commercialpipe.cost);
                costCell.setCellStyle(integerStyle);

                cumulativeCostValue += commercialpipe.cost;
                cumulativeLength += commercialpipe.length;
                costCell = costRow.createCell(3);
                costCell.setCellValue(cumulativeCostValue);
                costCell.setCellStyle(integerStyle);
            }
            Row costTotalRow = costSheet.createRow(costrowindex);
            //costrowindex++;
            costCell = costTotalRow.createCell(0);
            costCell.setCellValue("Total");
            costCell.setCellStyle(cellStyle);
            costCell = costTotalRow.createCell(1);
            costCell.setCellValue(cumulativeLength);
            costCell.setCellStyle(integerStyle);
            costCell = costTotalRow.createCell(2);
            costCell.setCellValue(cumulativeCostValue);
            costCell.setCellStyle(integerStyle);

            esrcostrowindex = costrowindex + 2;
            esrcoststart = esrcostrowindex;

            double cumulativeEsrCostValue = 0;
            if(esrGeneralProperties.esr_enabled){

                Sheet esrCostSheet = generalSheet; //wb.createSheet("Cost");

                if(esrcostrowindex%2==0)
                    esrcostrowindex++;
                row = esrCostSheet.createRow(esrcostrowindex);
                esrCostSheet.addMergedRegion(new CellRangeAddress(esrcostrowindex,esrcostrowindex,0,7));
                esrcostrowindex++;
                Cell esrCostCell = row.createCell(0);
                esrCostCell.setCellValue("COST RESULTS OF ESRS");
                esrCostCell.setCellStyle(cellStyle);
                for(int i=1;i<8;i++)
                {
                    esrCostCell = row.createCell(i);
                    esrCostCell.setCellStyle(cellStyle);
                }
                esrcoststart = esrcostrowindex;
                Row esrCostTitleRow = esrCostSheet.createRow(esrcostrowindex);
                esrcostrowindex++;
                esrCostCell = esrCostTitleRow.createCell(0);
                esrCostCell.setCellValue("ESR Node ID");
                esrCostCell.setCellStyle(cellStyle);
                esrCostCell = esrCostTitleRow.createCell(1);
                esrCostCell.setCellValue("ESR Child ID");
                esrCostCell.setCellStyle(cellStyle);
                esrCostCell = esrCostTitleRow.createCell(2);
                esrCostCell.setCellValue("Node Name");
                esrCostCell.setCellStyle(cellStyle);
                esrCostCell = esrCostTitleRow.createCell(3);
                esrCostCell.setCellValue("Elevation (m)");
                esrCostCell.setCellStyle(cellStyle);
                esrCostCell = esrCostTitleRow.createCell(4);
                esrCostCell.setCellValue("Capacity (l)");
                esrCostCell.setCellStyle(cellStyle);
                esrCostCell = esrCostTitleRow.createCell(5);
                esrCostCell.setCellValue("ESR Height (m)");
                esrCostCell.setCellStyle(cellStyle);
                esrCostCell = esrCostTitleRow.createCell(6);
                esrCostCell.setCellValue("Cost (Rs)");
                esrCostCell.setCellStyle(cellStyle);
                esrCostCell = esrCostTitleRow.createCell(7);
                esrCostCell.setCellValue("Cumulative Cost (Rs)");
                esrCostCell.setCellStyle(cellStyle);


                for(ResultEsrStruct esr : resultEsrsArray)
                {
                    Row esrCostRow = esrCostSheet.createRow(esrcostrowindex);
                    esrcostrowindex++;

                    esrCostCell = esrCostRow.createCell(0);
                    esrCostCell.setCellValue(esr.nodeid);
                    esrCostCell.setCellStyle(integerStyle);

                    esrCostCell = esrCostRow.createCell(1);
                    esrCostCell.setCellStyle(integerStyle);

                    esrCostCell = esrCostRow.createCell(2);
                    esrCostCell.setCellValue(esr.nodename);
                    esrCostCell.setCellStyle(integerStyle);

                    esrCostCell = esrCostRow.createCell(3);
                    esrCostCell.setCellValue(esr.elevation);
                    esrCostCell.setCellStyle(integerStyle);

                    esrCostCell = esrCostRow.createCell(4);
                    esrCostCell.setCellValue(esr.capacity);
                    esrCostCell.setCellStyle(integerStyle);

                    esrCostCell = esrCostRow.createCell(5);
                    esrCostCell.setCellValue(esr.esrheight);
                    esrCostCell.setCellStyle(integerStyle);

                    esrCostCell = esrCostRow.createCell(6);
                    esrCostCell.setCellValue(esr.cost);
                    esrCostCell.setCellStyle(integerStyle);

                    esrCostCell = esrCostRow.createCell(7);
                    esrCostCell.setCellValue(esr.cumulativecost);
                    esrCostCell.setCellStyle(integerStyle);

                    cumulativeEsrCostValue = esr.cumulativecost;

                    int noofchildren = 0;
                    for(NodeStruct node : resultNodesArray){
                        if(node.esr == esr.nodeid && node.dailydemand>0)
                            noofchildren++;
                    }

                    if(noofchildren > 1){
                        for(NodeStruct node : resultNodesArray){

                            if(node.esr == esr.nodeid && node.dailydemand>0){
                                esrCostRow = esrCostSheet.createRow(esrcostrowindex);
                                esrcostrowindex++;

                                esrCostCell = esrCostRow.createCell(0);
                                esrCostCell.setCellStyle(integerStyle);

                                esrCostCell = esrCostRow.createCell(1);
                                esrCostCell.setCellValue(node.nodeid);
                                esrCostCell.setCellStyle(integerStyle);

                                esrCostCell = esrCostRow.createCell(2);
                                esrCostCell.setCellValue(node.nodename);
                                esrCostCell.setCellStyle(integerStyle);

                                esrCostCell = esrCostRow.createCell(3);
                                esrCostCell.setCellValue(node.elevation);
                                esrCostCell.setCellStyle(integerStyle);

                                esrCostCell = esrCostRow.createCell(4);
                                esrCostCell.setCellValue(node.dailydemand);
                                esrCostCell.setCellStyle(integerStyle);

                                esrCostCell = esrCostRow.createCell(5);
                                esrCostCell.setCellStyle(integerStyle);

                                esrCostCell = esrCostRow.createCell(6);
                                esrCostCell.setCellStyle(integerStyle);

                                esrCostCell = esrCostRow.createCell(7);
                                esrCostCell.setCellStyle(integerStyle);
                            }
                        }
                    }
                }
            }

            pumpcostrowindex = esrcostrowindex + 2;
            pumpcoststart = pumpcostrowindex;

            double cumulativePumpCostValue = 0;
            if(pumpGeneralProperties.pump_enabled){

                Sheet pumpCostSheet = generalSheet;

                if(pumpcostrowindex%2==0)
                    pumpcostrowindex++;
                row = pumpCostSheet.createRow(pumpcostrowindex);
                pumpCostSheet.addMergedRegion(new CellRangeAddress(pumpcostrowindex,pumpcostrowindex,0,5));
                pumpcostrowindex++;
                Cell pumpCostCell = row.createCell(0);
                pumpCostCell.setCellValue("COST RESULTS OF PUMPS");
                pumpCostCell.setCellStyle(cellStyle);
                for(int i=1;i<6;i++){
                    pumpCostCell = row.createCell(i);
                    pumpCostCell.setCellStyle(cellStyle);
                }
                pumpcoststart = pumpcostrowindex;
                Row pumpCostTitleRow = pumpCostSheet.createRow(pumpcostrowindex);
                pumpcostrowindex++;
                pumpCostCell = pumpCostTitleRow.createCell(0);
                pumpCostCell.setCellValue("Pipe ID");
                pumpCostCell.setCellStyle(cellStyle);
                pumpCostCell = pumpCostTitleRow.createCell(1);
                pumpCostCell.setCellValue("Pump Head (m)");
                pumpCostCell.setCellStyle(cellStyle);
                pumpCostCell = pumpCostTitleRow.createCell(2);
                pumpCostCell.setCellValue("Pump Power (kW)");
                pumpCostCell.setCellStyle(cellStyle);
                pumpCostCell = pumpCostTitleRow.createCell(3);
                pumpCostCell.setCellValue("Energy Cost (Rs)");
                pumpCostCell.setCellStyle(cellStyle);
                pumpCostCell = pumpCostTitleRow.createCell(4);
                pumpCostCell.setCellValue("Capital Cost (Rs)");
                pumpCostCell.setCellStyle(cellStyle);
                pumpCostCell = pumpCostTitleRow.createCell(5);
                pumpCostCell.setCellValue("Total Cost (Rs)");
                pumpCostCell.setCellStyle(cellStyle);

                for(ResultPumpStruct pump : resultPumpsArray){
                    Row pumpCostRow = pumpCostSheet.createRow(pumpcostrowindex);
                    pumpcostrowindex++;

                    pumpCostCell = pumpCostRow.createCell(0);
                    pumpCostCell.setCellValue(pump.pipeid);
                    pumpCostCell.setCellStyle(integerStyle);

                    pumpCostCell = pumpCostRow.createCell(1);
                    pumpCostCell.setCellValue(pump.pumphead);
                    pumpCostCell.setCellStyle(integerStyle);

                    pumpCostCell = pumpCostRow.createCell(2);
                    pumpCostCell.setCellValue(pump.pumppower);
                    pumpCostCell.setCellStyle(integerStyle);

                    pumpCostCell = pumpCostRow.createCell(3);
                    pumpCostCell.setCellValue(pump.energycost);
                    pumpCostCell.setCellStyle(integerStyle);

                    pumpCostCell = pumpCostRow.createCell(4);
                    pumpCostCell.setCellValue(pump.capitalcost);
                    pumpCostCell.setCellStyle(integerStyle);

                    pumpCostCell = pumpCostRow.createCell(5);
                    pumpCostCell.setCellValue(pump.totalcost);
                    pumpCostCell.setCellStyle(integerStyle);

                    cumulativePumpCostValue += pump.totalcost;
                }
            }

            row = generalSheet.createRow(rowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
            rowindex++;
            generalCell = row.createCell(0);
            generalCell.setCellValue("Total Length of Network");
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(1);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(2);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(3);
            generalCell.setCellValue(cumulativeLength+cumulativeOldLength);
            generalCell.setCellStyle(integerStyle);

            row = generalSheet.createRow(rowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
            rowindex++;
            generalCell = row.createCell(0);
            generalCell.setCellValue("Total Length of New Pipes");
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(1);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(2);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(3);
            generalCell.setCellValue(cumulativeLength);
            generalCell.setCellStyle(integerStyle);

            row = generalSheet.createRow(rowindex);
            generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
            rowindex++;
            generalCell = row.createCell(0);
            generalCell.setCellValue("Total Pipe Cost");
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(1);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(2);
            generalCell.setCellStyle(cellStyle);
            generalCell = row.createCell(3);
            generalCell.setCellValue(cumulativeCostValue);
            generalCell.setCellStyle(integerStyle);

            boolean additionalcost = false;
            if(esrGeneralProperties.esr_enabled){
                row = generalSheet.createRow(rowindex);
                generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
                rowindex++;
                generalCell = row.createCell(0);
                generalCell.setCellValue("Total ESR Cost");
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(1);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(2);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(3);
                generalCell.setCellValue(cumulativeEsrCostValue);
                generalCell.setCellStyle(integerStyle);
                additionalcost = true;
            }

            if(pumpGeneralProperties.pump_enabled){
                row = generalSheet.createRow(rowindex);
                generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
                rowindex++;
                generalCell = row.createCell(0);
                generalCell.setCellValue("Total Pump Cost");
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(1);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(2);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(3);
                generalCell.setCellValue(cumulativePumpCostValue);
                generalCell.setCellStyle(integerStyle);
                additionalcost = true;
            }

            if(additionalcost){
                row = generalSheet.createRow(rowindex);
                generalSheet.addMergedRegion(new CellRangeAddress(rowindex,rowindex,0,2));
                rowindex++;
                generalCell = row.createCell(0);
                generalCell.setCellValue("Total Cost");
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(1);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(2);
                generalCell.setCellStyle(cellStyle);
                generalCell = row.createCell(3);
                generalCell.setCellValue(cumulativeCostValue+cumulativeEsrCostValue+cumulativePumpCostValue);
                generalCell.setCellStyle(integerStyle);
            }

            SheetConditionalFormatting sheetCF = generalSheet.getSheetConditionalFormatting();

            ConditionalFormattingRule rule1 = sheetCF.createConditionalFormattingRule("MOD(ROW(),2)");
            PatternFormatting fill1 = rule1.createPatternFormatting();
            fill1.setFillBackgroundColor(IndexedColors.PALE_BLUE.index);
            fill1.setFillPattern(PatternFormatting.SOLID_FOREGROUND);

            CellRangeAddress[] regions = {
                    CellRangeAddress.valueOf("A"+generalstart+":D"+rowindex),
                    CellRangeAddress.valueOf("A"+nodestart+":G"+noderowindex),
                    CellRangeAddress.valueOf("A"+pipestart+":O"+piperowindex),
                    CellRangeAddress.valueOf("A"+coststart+":D"+costrowindex)
            };
            sheetCF.addConditionalFormatting(regions, rule1);

            if(esrGeneralProperties.esr_enabled){
                CellRangeAddress[] esrregion = {
                        CellRangeAddress.valueOf("A"+esrcoststart+":H"+esrcostrowindex)
                };
                sheetCF.addConditionalFormatting(esrregion, rule1);
            }

            if(pumpGeneralProperties.pump_enabled){
                CellRangeAddress[] pumpregion = {
                        CellRangeAddress.valueOf("A"+pumpcoststart+":F"+pumpcostrowindex)
                };
                sheetCF.addConditionalFormatting(pumpregion, rule1);
            }


            for(int i=0;i<14;i++)
                generalSheet.autoSizeColumn(i);

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
