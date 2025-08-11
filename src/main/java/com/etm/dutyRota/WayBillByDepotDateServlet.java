package com.etm.dutyRota;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.json.JSONArray;


@SuppressWarnings("serial")
public class WayBillByDepotDateServlet extends HttpServlet {
//	String scheduleTypeId = "";
	
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

    	BufferedReader reader = request.getReader();
    	StringBuilder jsonBuilder = new StringBuilder();
    	String line;
    	while ((line = reader.readLine()) != null) {
    	    jsonBuilder.append(line);
    	}
    	String json = jsonBuilder.toString();

    	System.out.println("data------"+json.toString());
    	
    	JSONObject obj = new JSONObject(json);
    	String depotId = obj.getString("depot_id");
    	String dutyDate = obj.getString("duty_date");
    	
    	List<String> vehicleNumbers = new ArrayList<>();
    	if (obj.has("vehicle_numbers")) {
    	    JSONArray vehicles = obj.getJSONArray("vehicle_numbers");
    	    for (int i = 0; i < vehicles.length(); i++) {
    	        vehicleNumbers.add(vehicles.getString(i));
    	    }
    	}
//        String depotId = request.getParameter("depot_id");
//        String dutyDate = request.getParameter("duty_date");
//        String vehicleNumbersParam = request.getParameter("vehicle_numbers");  // e.g., "GA01T1234,GA01T5678"
//        List<String> vehicleNumbers = new ArrayList<>();
//
//        if (vehicleNumbersParam != null && !vehicleNumbersParam.trim().isEmpty()) {
//            vehicleNumbers = Arrays.asList(vehicleNumbersParam.split(","));
//        }

        response.setContentType("text/plain");

        System.out.println("Depot---"+depotId);
        System.out.println("duty date-----"+dutyDate);
        System.out.println("vehicleNumbers------"+vehicleNumbers);
        
        try {                
//        	WaybillAutoCreation action=new WaybillAutoCreation();
//        	
//        	if (depotId != null && dutyDate != null && vehicleNumbers != null && !vehicleNumbers.isEmpty()) {
//        	    // Parse date
//        	    SimpleDateFormat inputFormat = new SimpleDateFormat("dd-MM-yyyy");
//        	    Date parsedDate = inputFormat.parse(dutyDate);
//        	    SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
//        	    String startDate = outputFormat.format(parsedDate);
//
//        	    Map<String, String> success = action.generateAutoWayBillOfVehicles(depotId, startDate, vehicleNumbers, response);
//        	    if (success != null) {
//        	        response.getWriter().write("Waybill created for selected vehicles in depot: " + depotId);
//        	    } else {
//        	        response.getWriter().write("Waybill creation failed for selected vehicles in depot: " + depotId);
//        	    }
//
//        	} else if (depotId != null && dutyDate != null) {
//        	    SimpleDateFormat inputFormat = new SimpleDateFormat("dd-MM-yyyy");
//        	    Date parsedDate = inputFormat.parse(dutyDate);
//        	    SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
//        	    String startDate = outputFormat.format(parsedDate);
//
//        	    boolean success = action.generateAutoWayBill(depotId, startDate, response);
//        	    if (success) {
//        	        response.getWriter().write("Waybill created for depot: " + depotId);
//        	    } else {
//        	        response.getWriter().write("Waybill creation failed for depot: " + depotId);
//        	    }
//
//        	} else if (depotId != null && dutyDate == null) {
//        	    boolean success = action.generateAutoWayBillWithOutDate(depotId, response);
//        	    if (success) {
//        	        response.getWriter().write("Waybill created for depot: " + depotId);
//        	    } else {
//        	        response.getWriter().write("Waybill creation failed for depot: " + depotId);
//        	    }
//
//        	} else {
//        	    response.getWriter().write("Missing parameters: depot_id, duty_date");
//        	}

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("Error: " + e.getMessage());
        }
    }

    


}
