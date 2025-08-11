package com.etm.dutyRota;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WayBillVehicleServlet extends HttpServlet {
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	    response.setContentType("application/json");
	    response.setCharacterEncoding("UTF-8");
	    
	    JSONObject jsonResponse = new JSONObject();

	    String token = request.getHeader("Authorization");
	    if (token == null || token.isEmpty()) {
	        token = request.getHeader("token");
	    }
	    if (token == null || token.isEmpty()) {
	        token = request.getParameter("token");
	    }

	    final String validToken = "auto-waybill-f47ac10b-58cc-43a72";

	    if (token == null || !token.equals(validToken)) {
	        jsonResponse.put("status", "unauthorized");
	        jsonResponse.put("message", "Invalid or missing token.");
	        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	        response.getWriter().write(jsonResponse.toString());
	        return;
	    }
	    BufferedReader reader = request.getReader();
	    StringBuilder sb = new StringBuilder();
	    String line;
	    while ((line = reader.readLine()) != null) {
	        sb.append(line);
	    }

	    JSONObject json;
	    try {
	        json = new JSONObject(sb.toString());
	    } catch (Exception e) {
	        writeError(response, "Invalid JSON format");
	        return;
	    }

	    String depotId = json.optString("depot_id", "").trim();
	    String dutyDate = json.optString("duty_date", "").trim();
	    JSONArray vehicleData = json.optJSONArray("vehicle_numbers");

	    try {
	        // Check depot_id
	        if (depotId == null || depotId.trim().isEmpty()) {
	            jsonResponse.put("status", "error");
	            jsonResponse.put("message", "Depot ID is required.");
	            response.getWriter().write(jsonResponse.toString());
	            return;
	        }

	        // Check duty_date
	        if (dutyDate == null || dutyDate.trim().isEmpty()) {
	            jsonResponse.put("status", "error");
	            jsonResponse.put("message", "Duty date is required.");
	            response.getWriter().write(jsonResponse.toString());
	            return;
	        }

	        // Check vehicle_numbers
	        if (vehicleData == null || vehicleData.length() == 0) {
	            jsonResponse.put("status", "error");
	            jsonResponse.put("message", "Vehicle number list is required.");
	            response.getWriter().write(jsonResponse.toString());
	            return;
	        }
	        if (depotId == null || depotId.trim().isEmpty() ||
	                dutyDate == null || dutyDate.trim().isEmpty() ||
	                vehicleData == null || vehicleData.length()==0) {

	                jsonResponse.put("status", "error");
	                jsonResponse.put("message", "All fields (depot_id, duty_date, vehicle_numbers) are required.");
	                response.getWriter().write(jsonResponse.toString());
	                return;
	         }
	     // Parse vehicle list from JSON array string
	        JSONArray vehicleNumbers;
	        try {
	            vehicleNumbers = new JSONArray(vehicleData);
	        } catch (JSONException je) {
	            jsonResponse.put("status", "error");
	            jsonResponse.put("message", "Vehicle number list must be a valid JSON array.");
	            response.getWriter().write(jsonResponse.toString());
	            return;
	        }

	        if (vehicleNumbers.length() == 0) {
	            jsonResponse.put("status", "error");
	            jsonResponse.put("message", "Vehicle number should be passed.");
	            response.getWriter().write(jsonResponse.toString());
	            return;
	        }

	        // Convert to List<String>
	        List<String> vehicleNumbersList = new ArrayList<>();
	        for (int i = 0; i < vehicleNumbers.length(); i++) {
	            vehicleNumbersList.add(vehicleNumbers.getString(i));
	        }

	        // Format the date
	        SimpleDateFormat inputFormat = new SimpleDateFormat("dd-MM-yyyy");
            Date parsedDate;
            try {
                parsedDate = inputFormat.parse(dutyDate);
            } catch (Exception ex) {
                jsonResponse.put("status", "error");
                jsonResponse.put("message", "Invalid duty_date format. Expected dd-MM-yyyy.");
                response.getWriter().write(jsonResponse.toString());
                return;
            }
	        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
	        String startDate = outputFormat.format(parsedDate);

	        WaybillAutoCreation action = new WaybillAutoCreation();
	        Map<String, Object> result = action.generateAutoWayBillOfVehicles(depotId, startDate, vehicleNumbersList, response);

//	        if (success) {
//	            jsonResponse.put("status", "success");
//	            jsonResponse.put("message", "Waybill created for selected vehicles in depot: " + depotId);
//	        } else {
//	            jsonResponse.put("status", "failure");
//	            jsonResponse.put("message", "Waybill creation failed for selected vehicles in depot: " + depotId);
//	        }
	        jsonResponse.put("status", result.get("status"));
	        jsonResponse.put("message", result.get("message"));
	        jsonResponse.put("data", result.get("data"));

	        response.getWriter().write(jsonResponse.toString());

	    } catch (Exception e) {
	        e.printStackTrace();
	        jsonResponse.put("status", "error");
	        jsonResponse.put("message", "Exception occurred: " + e.getMessage());
	        response.getWriter().write(jsonResponse.toString());
	    }

	}
	
	private void writeError(HttpServletResponse response, String message) throws IOException {
	    response.setContentType("application/json");
	    JSONObject errorObj = new JSONObject();
	    errorObj.put("status", "error");
	    errorObj.put("message", message);
	    response.getWriter().write(errorObj.toString());
	}


}
