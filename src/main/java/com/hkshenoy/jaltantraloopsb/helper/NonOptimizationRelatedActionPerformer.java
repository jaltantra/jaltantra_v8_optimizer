package com.hkshenoy.jaltantraloopsb.helper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class NonOptimizationRelatedActionPerformer {

    @Autowired
    ExcelUploader excelUploader;

    @Autowired
    ExcelOutputUploader excelOutputUploader;

    @Autowired
    EPANetUploader epaNetUploader;

    @Autowired
    MapSnapshotUploader mapSnapshotUploader;

    @Autowired
    CustomLogger customLogger;



    public void performNonOptimizationRelatedAction(HttpServletRequest request, HttpServletResponse response, String action) throws IOException {
        if (action.equalsIgnoreCase("saveInputXml")) {
            XMLUploader.uploadXmlInputFile(request, response);
        } else if (action.equalsIgnoreCase("saveInputExcel")) {
            excelUploader.uploadExcelInputFile(request, response);
        } else if (action.equalsIgnoreCase("saveOutputExcel")) {
            excelOutputUploader.uploadExcelOutputFile(request, response);
        } else if (action.equalsIgnoreCase("saveOutputEpanet")) {
            EPANetUploader.uploadEpanetOutputFile(request, response);
        } else if (action.equalsIgnoreCase("saveMapSnapshot")) {
            MapSnapshotUploader.uploadMapSnapshotFile(request, response);
        } else {
            customLogger.loge("FIXME: unknow action=" + action);
        }
    }

}
