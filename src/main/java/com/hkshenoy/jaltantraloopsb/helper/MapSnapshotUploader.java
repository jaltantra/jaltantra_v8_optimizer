package com.hkshenoy.jaltantraloopsb.helper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;


@Component
public class MapSnapshotUploader {

    //generate and upload image snapshot of map tab
    public static void uploadMapSnapshotFile(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        try{
            OutputStream os = response.getOutputStream();

            response.setContentType("image/png"); // Set up mime type
            response.setHeader("Content-Disposition", "attachment; filename=map_image.png");


            String mapSnapshotString = request.getParameter("imagestring");


            os.flush();
            os.close();
        }
        catch (Exception e){
            System.out.println(e);
        }
    }
}
