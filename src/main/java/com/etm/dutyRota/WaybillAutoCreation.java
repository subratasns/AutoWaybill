package com.etm.dutyRota;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.hibernate.type.IntegerType;
import org.json.JSONObject;

import com.etm.util.HibernateUtil;

public class WaybillAutoCreation {
	 WaybillAutoCreationDAO dao=new WaybillAutoCreationDAO();
	 String waybill_no="";

	@SuppressWarnings("unchecked")
	public boolean generateAutoWayBillWithOutDate(String depotId, HttpServletResponse response) {
    	
	      boolean flag=false;String startDate="";
	      try {
	    	 
	     	 List<Map<String, Object>> waybillDataList = getDataToCreateWaybill(depotId);

	         for (Map<String, Object> row : waybillDataList) {
	             String scheduleTypeId = String.valueOf(row.get("schedule_type_id"));
	             String serviceTypeId = String.valueOf(row.get("service_type_id"));
	             String scheduleId = String.valueOf(row.get("schedule_no"));
	             String scheduleName = String.valueOf(row.get("schedule_name"));
	             String vehicleId = String.valueOf(row.get("vehicle_id"));
	             String vehicleNo = String.valueOf(row.get("vehicle_no"));
	             String deviceId = String.valueOf(row.get("device_id"));
	             String deviceName = String.valueOf(row.get("device_name"));
	             String ticketBagId = String.valueOf(row.get("ticket_bag_id"));
	             String ticketBagName = String.valueOf(row.get("ticket_bag_name"));
	             String conductorId = String.valueOf(row.get("conductor_id"));
	             String conductorName = String.valueOf(row.get("conductor_name"));
	             String driverId = String.valueOf(row.get("driver_id"));
	             String driverName = String.valueOf(row.get("driver_name"));
	             String driverDC = String.valueOf(row.get("driver_dc"));
	             String conductorDC = String.valueOf(row.get("conductor_dc"));
	             String dc_id = String.valueOf(row.get("dc_id"));
	             String dc_name = String.valueOf(row.get("dc_name"));
	     		List<String> scheduleData = new ArrayList<String>();
	     		String schedule_type_id = null,service_type_id,scheduleCode = null,scheduleNo = null,ScheduleTypeName = null,shiftTypeId="";
	     		
				ArrayList listOfParametersForWaybillCreation = new ArrayList();
				 Session session = HibernateUtil.getSessionFactory().openSession();
	             if (scheduleId!=null) {
	           	  String sql="select MAX(generated_Date) from waybill_details wd inner join form_four ff on wd.schedule_No=ff.form_four_id "
	           	  		+ "where depot_id='"+depotId+"' and schedule_number_id ='"+scheduleId+"'";
	              Query query = session.createSQLQuery(sql); 
	              Object result = query.uniqueResult();
//		           	String maxGeneratedDate = query.uniqueResult();
		           	
		            if (result == null) {
		                startDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		            } else {
		            	java.sql.Date maxGeneratedDate = (java.sql.Date) result;
		                String formatted = new SimpleDateFormat("dd-MM-yyyy").format(maxGeneratedDate);
		                if (formatted.matches("\\d{2}-\\d{2}-\\d{4}")) {
		                    try {
		                        Date parsedDate = new SimpleDateFormat("dd-MM-yyyy").parse(formatted);
		                        startDate = new SimpleDateFormat("yyyy-MM-dd").format(parsedDate);
		                    } catch (ParseException e) {
		                        e.printStackTrace();
		                        startDate = formatted; // fallback
		                    }
		                } else {
		                    startDate = formatted;
		                }
		            }
		            
	            	scheduleData = dao.getFormFourId(scheduleId,startDate);
	            	 
	    			schedule_type_id = scheduleData.get(0);
	    			service_type_id = scheduleData.get(1);
	    			scheduleCode = scheduleData.get(2);
	    			scheduleNo = scheduleData.get(3);
	    			ScheduleTypeName = scheduleData.get(4);
	    			shiftTypeId = scheduleData.get(5);
				}
	            System.out.println("Schedule No: " + scheduleNo + ", Conductor: " + conductorName);

	            char isEtim = 'N';
	 			char isBag = 'Y';
	 			if (!deviceId.equals("null")) {
					isEtim = 'Y';
				}
	 			String nextdate = null;
	 			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	 			Date parsedDate = sdf.parse(startDate);

	 			Calendar c = Calendar.getInstance();
	 			c.setTime(parsedDate);
	 			c.add(Calendar.DATE, 1);
	 			
	 			Date nextDatestr = c.getTime();
	 			String nextDate = sdf.format(nextDatestr);
	 			
	            String dutyTypeId=schedule_type_id;
	            System.out.println("etim flag and bag flag details>>>>>>"+isEtim+"...."+isBag);
	            if (dao.checkScheduleStatusBeforeWaybillCreation(scheduleNo)== true) {
					System.out.println("insdie the if>>>>>>>>");
					listOfParametersForWaybillCreation.add(conductorId);// 0
					listOfParametersForWaybillCreation.add(driverId);// 1
					listOfParametersForWaybillCreation.add(deviceId);// 2
					listOfParametersForWaybillCreation.add(ticketBagId);// 3
					listOfParametersForWaybillCreation.add(vehicleId);// 4
					listOfParametersForWaybillCreation.add(scheduleTypeId);// 5
					listOfParametersForWaybillCreation.add(serviceTypeId);// 6
					listOfParametersForWaybillCreation.add(scheduleNo);// 7
					listOfParametersForWaybillCreation.add(isEtim);// 8
					listOfParametersForWaybillCreation.add(isBag);// 9
					listOfParametersForWaybillCreation.add(dutyTypeId);// 10
					listOfParametersForWaybillCreation.add(scheduleCode);// 11
					listOfParametersForWaybillCreation.add(startDate);// 12
					listOfParametersForWaybillCreation.add(scheduleNo);// 13
					listOfParametersForWaybillCreation.add(nextdate);// 14
					listOfParametersForWaybillCreation.add(ScheduleTypeName);
					listOfParametersForWaybillCreation.add(shiftTypeId);

					dao.createWayBill(listOfParametersForWaybillCreation, "Y", dutyTypeId, 1,depotId,startDate,response);
					System.out.println("fare chart is available");

					flag = true;
				}else {
					System.out.println("Please check the route id or the fare chart for the selected scheduler");

					flag = false;

				}
				
			} 
	      }catch (Exception e) {
				e.printStackTrace();
				flag = false;
			}
		return flag;
		}

		@SuppressWarnings("unchecked")
		public boolean generateAutoWayBill(String depotId, String startDate, HttpServletResponse response) {
	    	 List<Map<String, Object>> waybillDataList = getDataToCreateWaybill(depotId);
	    	 boolean flag=false;
	      try {
	         for (Map<String, Object> row : waybillDataList) {
	             String scheduleTypeId = String.valueOf(row.get("schedule_type_id"));
	             String serviceTypeId = String.valueOf(row.get("service_type_id"));
	             String scheduleId = String.valueOf(row.get("schedule_no"));
	             String scheduleName = String.valueOf(row.get("schedule_name"));
	             String vehicleId = String.valueOf(row.get("vehicle_id"));
	             String vehicleNo = String.valueOf(row.get("vehicle_no"));
	             String deviceId = String.valueOf(row.get("device_id"));
	             String deviceName = String.valueOf(row.get("device_name"));
	             String ticketBagId = String.valueOf(row.get("ticket_bag_id"));
	             String ticketBagName = String.valueOf(row.get("ticket_bag_name"));
	             String conductorId = String.valueOf(row.get("conductor_id"));
	             String conductorName = String.valueOf(row.get("conductor_name"));
	             String driverId = String.valueOf(row.get("driver_id"));
	             String driverName = String.valueOf(row.get("driver_name"));
	             String driverDC = String.valueOf(row.get("driver_dc"));
	             String conductorDC = String.valueOf(row.get("conductor_dc"));
	             String dc_id = String.valueOf(row.get("dc_id"));
	             String dc_name = String.valueOf(row.get("dc_name"));
	     		List<String> scheduleData = new ArrayList<String>();
	     		String schedule_type_id = null,service_type_id,scheduleCode = null,scheduleNo = null,ScheduleTypeName = null,shiftTypeId="";
	     		
				ArrayList listOfParametersForWaybillCreation = new ArrayList();

	             if (scheduleId!=null) {
	            	scheduleData = dao.getFormFourId(scheduleId,startDate);
	            	 
	    			schedule_type_id = scheduleData.get(0);
	    			service_type_id = scheduleData.get(1);
	    			scheduleCode = scheduleData.get(2);
	    			scheduleNo = scheduleData.get(3);
	    			ScheduleTypeName = scheduleData.get(4);
	    			shiftTypeId = scheduleData.get(5);
				}
	            System.out.println("Schedule No: " + scheduleNo + ", Conductor: " + conductorName);

	            char isEtim = 'N';
	 			char isBag = 'Y';
	 			if (!deviceId.equals("null")) {
					isEtim = 'Y';
				}
	 			String nextdate = null;
	 			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	 			Date parsedDate = sdf.parse(startDate);

	 			Calendar c = Calendar.getInstance();
	 			c.setTime(parsedDate);
	 			c.add(Calendar.DATE, 1);
	 			
	 			Date nextDatestr = c.getTime();
	 			String nextDate = sdf.format(nextDatestr);
	 			
	            String dutyTypeId=schedule_type_id;
	            System.out.println("etim flag and bag flag details>>>>>>"+isEtim+"...."+isBag);
	            if (dao.checkScheduleStatusBeforeWaybillCreation(scheduleNo)== true) {
					System.out.println("insdie the if>>>>>>>>");
					listOfParametersForWaybillCreation.add(conductorId);// 0
					listOfParametersForWaybillCreation.add(driverId);// 1
					listOfParametersForWaybillCreation.add(deviceId);// 2
					listOfParametersForWaybillCreation.add(ticketBagId);// 3
					listOfParametersForWaybillCreation.add(vehicleId);// 4
					listOfParametersForWaybillCreation.add(scheduleTypeId);// 5
					listOfParametersForWaybillCreation.add(serviceTypeId);// 6
					listOfParametersForWaybillCreation.add(scheduleNo);// 7
					listOfParametersForWaybillCreation.add(isEtim);// 8
					listOfParametersForWaybillCreation.add(isBag);// 9
					listOfParametersForWaybillCreation.add(dutyTypeId);// 10
					listOfParametersForWaybillCreation.add(scheduleCode);// 11
					listOfParametersForWaybillCreation.add(startDate);// 12
					listOfParametersForWaybillCreation.add(scheduleNo);// 13
					listOfParametersForWaybillCreation.add(nextdate);// 14
					listOfParametersForWaybillCreation.add(ScheduleTypeName);
					listOfParametersForWaybillCreation.add(shiftTypeId);

					dao.createDateWiseWayBill(listOfParametersForWaybillCreation, "Y", dutyTypeId, 1,depotId,startDate,response);
					System.out.println("fare chart is available");
					 String msg="Waybill created successfully";
		    		 String msg1="";

					flag = true;
				}else {
					System.out.println("Please check the route id or the fare chart for the selected scheduler");
					String msg="Please check the route id or the fare chart for the selected scheduler";

					flag = false;

				}
				
			} 
	      }catch (Exception e) {
				e.printStackTrace();
				flag = false;
			}
		return flag;
		}

@SuppressWarnings("unchecked")
public Map<String, Object> generateAutoWayBillOfVehicles(String depotId, String startDate, List<String> vehicleNumbers,
		HttpServletResponse response) {
	 Map<String, Object> result = new HashMap<>();
	    List<Map<String, Object>> waybillDataList = null;
	    List<Map<String, Object>> vehicleWaybillList = new ArrayList<>();
	    boolean anyCreated = false;
	    boolean anyDuplicate = false;
	    int retryCount = 0;
	    final int MAX_RETRIES = 3;

	    while (retryCount < MAX_RETRIES) {
	        try {
	            // Attempt to get data for waybill creation
	            waybillDataList = getDataToCreateWaybill(depotId, vehicleNumbers);

	            // If no data found, log and retry
	            if (waybillDataList == null || waybillDataList.isEmpty()) {
	                retryCount++;
	                if (retryCount < MAX_RETRIES) {
	                    System.out.println("No data available, retrying... Attempt " + retryCount);
	                    Thread.sleep(1000); // Wait for 1 second before retrying
	                    continue;
	                } else {
	                    result.put("status", "error");
	                    result.put("message", "No data available for the given vehicle numbers in depot: " + depotId);
	                    return result;
	                }
	            }
	         for (Map<String, Object> row : waybillDataList) {
	             String scheduleTypeId = String.valueOf(row.get("schedule_type_id"));
	             String serviceTypeId = String.valueOf(row.get("service_type_id"));
	             String scheduleId = String.valueOf(row.get("schedule_no"));
	             String scheduleName = String.valueOf(row.get("schedule_name"));
	             String vehicleId = String.valueOf(row.get("vehicle_id"));
	             String vehicleNo = String.valueOf(row.get("vehicle_no"));
	             String deviceId = String.valueOf(row.get("device_id"));
	             String deviceName = String.valueOf(row.get("device_name"));
	             String ticketBagId = String.valueOf(row.get("ticket_bag_id"));
	             String ticketBagName = String.valueOf(row.get("ticket_bag_name"));
	             String conductorId = String.valueOf(row.get("conductor_id"));
	             String conductorName = String.valueOf(row.get("conductor_name"));
	             String driverId = String.valueOf(row.get("driver_id"));
	             String driverName = String.valueOf(row.get("driver_name"));
	             String driverDC = String.valueOf(row.get("driver_dc"));
	             String conductorDC = String.valueOf(row.get("conductor_dc"));
	             String dc_id = String.valueOf(row.get("dc_id"));
	             String dc_name = String.valueOf(row.get("dc_name"));
	     		List<String> scheduleData = new ArrayList<String>();
	     		String schedule_type_id = null,service_type_id,scheduleCode = null,scheduleNo = null,ScheduleTypeName = null,shiftTypeId="";
	     		
				ArrayList listOfParametersForWaybillCreation = new ArrayList();

	             if (scheduleId!=null) {
	            	scheduleData = dao.getFormFourId(scheduleId,startDate);
	            	 
	    			schedule_type_id = scheduleData.get(0);
	    			service_type_id = scheduleData.get(1);
	    			scheduleCode = scheduleData.get(2);
	    			scheduleNo = scheduleData.get(3);
	    			ScheduleTypeName = scheduleData.get(4);
	    			shiftTypeId = scheduleData.get(5);
				}
	            System.out.println("Schedule No: " + scheduleNo + ", Conductor: " + conductorName);

	            char isEtim = 'N';
	 			char isBag = 'Y';
	 			if (!deviceId.equals("null")) {
					isEtim = 'Y';
				}
	 			String nextdate = null;
	 			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	 			Date parsedDate = sdf.parse(startDate);

	 			Calendar c = Calendar.getInstance();
	 			c.setTime(parsedDate);
	 			c.add(Calendar.DATE, 1);
	 			
	 			Date nextDatestr = c.getTime();
	 			String nextDate = sdf.format(nextDatestr);
	 			
	            String dutyTypeId=schedule_type_id;
	            System.out.println("etim flag and bag flag details>>>>>>"+isEtim+"...."+isBag);
	            if (dao.checkScheduleStatusBeforeWaybillCreation(scheduleNo)== true) {
					System.out.println("insdie the if>>>>>>>>");
					listOfParametersForWaybillCreation.add(conductorId);// 0
					listOfParametersForWaybillCreation.add(driverId);// 1
					listOfParametersForWaybillCreation.add(deviceId);// 2
					listOfParametersForWaybillCreation.add(ticketBagId);// 3
					listOfParametersForWaybillCreation.add(vehicleId);// 4
					listOfParametersForWaybillCreation.add(scheduleTypeId);// 5
					listOfParametersForWaybillCreation.add(serviceTypeId);// 6
					listOfParametersForWaybillCreation.add(scheduleNo);// 7
					listOfParametersForWaybillCreation.add(isEtim);// 8
					listOfParametersForWaybillCreation.add(isBag);// 9
					listOfParametersForWaybillCreation.add(dutyTypeId);// 10
					listOfParametersForWaybillCreation.add(scheduleCode);// 11
					listOfParametersForWaybillCreation.add(startDate);// 12
					listOfParametersForWaybillCreation.add(scheduleNo);// 13
					listOfParametersForWaybillCreation.add(nextdate);// 14
					listOfParametersForWaybillCreation.add(ScheduleTypeName);
					listOfParametersForWaybillCreation.add(shiftTypeId);

					 boolean resultFlag = createDateWiseWayBillOfVehicle(listOfParametersForWaybillCreation, "Y", dutyTypeId, 1, depotId, startDate, null);
		                if (resultFlag) {
		                	Map<String, Object> vehicleMap = new HashMap<>();
		                	vehicleMap.put("vehicle_no", vehicleNo);
		                	vehicleMap.put("waybill_no", waybill_no);
		                	vehicleWaybillList.add(vehicleMap);

		                    anyCreated = true;
		                } else {
		                    anyDuplicate = true;
		                }
		            } else {
		                System.out.println("Please check the route id or the fare chart for the selected scheduler");
		            }
		        }
	         break;
	       } catch (Exception e) {
		        e.printStackTrace();
		        result.put("status", "error");
		        result.put("message", "Exception occurred during waybill creation: " + e.getMessage());
		        return result;
		    }
	         
    	  }
		        if (anyCreated && !anyDuplicate) {
		            result.put("status", "success");
		            result.put("data", vehicleWaybillList);
		            result.put("message", "Waybill created for selected vehicles in depot: " + depotId);
		        } else if (!anyCreated && anyDuplicate) {
		            result.put("status", "error");
		            result.put("message", "Waybill already exists for selected vehicles and duty Started, in depot: " + depotId);
		        } else if (anyCreated && anyDuplicate) {
		            result.put("status", "partial");
		            result.put("message", "Some waybills were already present; rest created for depot: " + depotId);
		        } else {
		            result.put("status", "failure");
		            result.put("message", "Waybill creation failed for selected vehicles in depot: " + depotId);
		        }

		    return result;
		}
		
		@SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
		private List<Map<String, Object>> getDataToCreateWaybill(String depotId, List<String> vehicleNumbers) {
		    List<Map<String, Object>> aliasToValueList = new ArrayList<>();
		    Session session = null;

		    try {
		    	session = HibernateUtil.getSessionFactory().openSession();

		    	StringBuilder sql = new StringBuilder("SELECT * FROM auto_waybill_creation WHERE depot_id = :depotId AND deleted_status = '0'");

		    	if (vehicleNumbers != null && !vehicleNumbers.isEmpty()) {
		    	    // Manually join vehicle numbers into comma-separated quoted values
		    	    String joinedVehicles = vehicleNumbers.stream()
		    	        .map(v -> "'" + v.trim().replace("'", "''") + "'")  // escape single quotes
		    	        .collect(Collectors.joining(","));
		    	    
		    	    sql.append(" AND vehicle_no IN (" + joinedVehicles + ")");
		    	}

		    	Query query = session.createSQLQuery(sql.toString());
		    	query.setParameter("depotId", depotId);

		    	query.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		    	aliasToValueList = query.list();

		    } catch (Exception e) {
		        e.printStackTrace();
		    } finally {
		        if (session != null) session.close();
		    }

		    return aliasToValueList;
		}

		@SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
		private List<Map<String, Object>> getDataToCreateWaybill(String depotId) {
	        List<Map<String, Object>> aliasToValueList = new ArrayList<>();

	        Session session = null;
	        try {
	            session = HibernateUtil.getSessionFactory().openSession();

	            String sql = "SELECT * FROM auto_waybill_creation WHERE depot_id = :depotId AND deleted_status = '0'";

	            Query query = session.createSQLQuery(sql);
	            query.setParameter("depotId", depotId);
	            query.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);

	            aliasToValueList = query.list(); 

	        } catch (Exception e) {
	            e.printStackTrace(); 
	        } finally {
	            if (session != null) {
	                session.close();
	            }
	        }

	        return aliasToValueList;
	    }
		public boolean createDateWiseWayBillOfVehicle(List paramList, String type, String duty_type, int userid, String depotId, String startDate, HttpServletResponse response) {
	        int waybillCount = 0;
	        WaybillAutoCreationDAO creation= new WaybillAutoCreationDAO();
	        Common common = new Common();
//	    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//	    	String startDateStr = sdf.format(startDate);
//	    	String startDateStr=startDate;
	    	String dutyRotaId="1";
	    	String driverBlockId="1";
			System.out.println("inside the create waybill>>>>>>");
			String userIDString = String.valueOf(userid);
			String BagNo; String startDateStr="";
			int userId = 1;
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				Date start = sdf.parse(startDate);
				
		        Session session=HibernateUtil.getSessionFactory().openSession();
		        	startDateStr = sdf.format(start);
			    Transaction transaction = session.beginTransaction();

	            boolean alreadyCreated = creation.isWaybillCreatedAlready(response, common, session,
	                    paramList.get(13).toString(),
	                    paramList.get(16).toString(),
	                    startDateStr,
	                    paramList.get(5).toString());
	            if (!alreadyCreated) {
	            String sqlQueryForCounterValue = "SELECT IFNULL(MAX(waybill_Counter + 1), 1) AS count FROM waybill_gen_logic WHERE waybill_Date = :date";
	            Query<?> queryForCounterValue = session.createSQLQuery(sqlQueryForCounterValue)
	                    .addScalar("count", IntegerType.INSTANCE)
	                    .setParameter("date", startDateStr);

	            List<?> resultList = queryForCounterValue.list();
	            int lastId = (resultList != null && !resultList.isEmpty()) ? (Integer) resultList.get(0) : 1;

	            if (lastId == 1) {
	                String Innersql = " INSERT INTO waybill_gen_logic (waybill_Date, waybill_Counter, created_By, created_Date,logsheet_Date)"
	                        + " VALUES('" + startDateStr + "'," + lastId + ",	" + userid + ",	now(),'" + startDateStr + "');";
	                Query qry2 = session.createSQLQuery(Innersql);
	                qry2.executeUpdate();
	            } else {
	                String Innersql = " UPDATE waybill_gen_logic set waybill_Counter=" + lastId + " WHERE waybill_Date='" + startDate + "'";
	                Query qry2 = session.createSQLQuery(Innersql);
	                qry2.executeUpdate();
	            }
	            String dayOutCondition = "";
	            String shiftTypeId= "1";
	            if (shiftTypeId != null) {
	                if (paramList.get(15).toString().equalsIgnoreCase("DAY OUT")) {
	                    dayOutCondition = " and sd.shift_type_id ='" + shiftTypeId + "'";
	                }
	            }
	            String depotCode = creation.getUserDepot(depotId);
	            String wayBillNo = creation.generateWayBillNo(lastId, startDateStr, depotCode);
                waybill_no=wayBillNo;
                
	            String sql1 = "SELECT IFNULL(MAX(logsheet_Counter+1), 1) count FROM waybill_gen_logic WHERE logsheet_Date = '" + startDateStr + "'";
	            int loghseetCount = common.getDBResultInt(session, sql1, "count");
	            String Innersql = " UPDATE waybill_gen_logic set logsheet_Counter=" + loghseetCount + " WHERE logsheet_Date='" + startDateStr + "'";
	            Query qry2 = session.createSQLQuery(Innersql);
	            qry2.executeUpdate();
	            String logsheetNo = "L"+wayBillNo;
	            
	            String insertQuery = creation.getWaybillDetailsInsertQuery(paramList, startDateStr, wayBillNo, userIDString,depotId,session);
	            System.out.println("Final Insert Query: " + insertQuery);
	            session.createSQLQuery(insertQuery).executeUpdate();
//	            transaction.commit(); // commit after insert
	            int insertedWaybillId = ((Number) session.createSQLQuery("SELECT LAST_INSERT_ID()").getSingleResult()).intValue();
	            System.out.println("last id-----"+insertedWaybillId);
	            String detailInsertQuery = "";
	            
	            String vehicleId=paramList.get(4).toString();

	            if ((paramList.get(15).toString()).equalsIgnoreCase("NIGHT OUT")) {
	                int formFourOne = creation.getFormFourForNightOut(common, session, startDateStr, paramList.get(13).toString(), "Day1");
	                int formFourTwo = creation.getFormFourForNightOut(common, session, startDateStr, paramList.get(13).toString(), "Day2");
	                if(formFourOne==0 || formFourTwo==0){
	                	transaction.rollback();
	                }
	                String dayOneId = common.getDBResultStr(session, "SELECT sys_value FROM default_system_veriable WHERE sys_key ='DAY_1'", "sys_value");
	                String dayTwoId = common.getDBResultStr(session, "SELECT sys_value FROM default_system_veriable WHERE sys_key ='DAY_2'", "sys_value");
//	                System.out.println("----------------->>>" + formFourOne + " ------------->>>" + formFourTwo);
	                detailInsertQuery = " INSERT INTO  Waybill_Trip_Details  (  schedule_details_id ,  form_four_id , "
	                        + " schedule_number ,  number_of_trips ,  list_item_number ,  trip_number ,  customer_id ,  trip_type , "
	                        + " start_point ,  end_point ,  route_number_id ,  distance ,  start_time ,  end_time ,  running_time ,  break_type_id ,"
	                        + " break_time ,  crew_change ,  night_halt ,  shift_type_id ,  is_dread_trip ,  crew_change_location ,  night_halt_location , "
	                        + " break_location ,  operation_type ,  remarks ,  created_by ,  created_date  ,  updated_by ,  deleted_status, waybil_Id , "
	                        + " waybill_No ,vehicleid)  select  sd.schedule_details_id, sd.form_four_id,sd.schedule_number ,  sd.number_of_trips ,  sd.list_item_number , "
	                        + " sd.trip_number ,  sd.customer_id ,  sd.trip_type , sd.start_point, sd.end_point, sd.route_number_id, sd.distance,  sd.start_time, sd.end_time ,"
	                        + " sd.running_time , break_type_id ,  break_time ,  crew_change ,  night_halt ,   shift_type_id ,  is_dread_trip ,  crew_change_location ,"
	                        + " sd.night_halt_location ,  sd.break_location ,   sd.operation_type ,  sd.remarks , '" + userId + "' ,  now() ,  '0' ,   sd.deleted_status ,"
	                        + " '" + insertedWaybillId + "','" + wayBillNo + "','" + vehicleId + "' from schedule_details sd "
	                        + " inner join schedule s on s.schedule_id=sd.schedule_number   "
	                        + " where  shift_type_id ='" + dayOneId + "' AND sd.form_four_id='" + formFourOne + "' and sd.deleted_status=0 " 
	                        + " UNION ALL select  sd.schedule_details_id, sd.form_four_id,sd.schedule_number ,  sd.number_of_trips ,  sd.list_item_number ,  "
	                        + " sd.trip_number ,  sd.customer_id ,  sd.trip_type , sd.start_point, sd.end_point, sd.route_number_id, sd.distance,  sd.start_time, sd.end_time ,"
	                        + " sd.running_time , break_type_id ,  break_time ,  crew_change ,  night_halt ,   shift_type_id ,  is_dread_trip ,  crew_change_location , "
	                        + " sd.night_halt_location ,  sd.break_location ,   sd.operation_type ,  sd.remarks , '" + userId + "' ,  now() ,  '0' ,   sd.deleted_status ,"
	                        + "'" + insertedWaybillId + "','" + wayBillNo + "','" + vehicleId + "' from schedule_details sd  "
	                        + " inner join schedule s on s.schedule_id=sd.schedule_number "
	                        + " where shift_type_id ='" + dayTwoId + "' AND sd.form_four_id='" + formFourTwo + "' and sd.deleted_status=0 order by shift_type_id,trip_number";
	            } else {
	                detailInsertQuery = " INSERT INTO  Waybill_Trip_Details  (  schedule_details_id ,  form_four_id ,  "
	                        + " schedule_number ,  number_of_trips ,  list_item_number ,  trip_number ,  customer_id ,  trip_type ,"
	                        + " start_point ,  end_point ,  route_number_id ,  distance ,  start_time ,  end_time ,  running_time ,"
	                        + " break_type_id ,  break_time ,  crew_change ,  night_halt ,  shift_type_id ,  is_dread_trip ,  crew_change_location ,"
	                        + " night_halt_location ,  break_location ,  operation_type ,  remarks ,  created_by ,  created_date  ,"
	                        + " updated_by ,  deleted_status, waybil_Id ,  waybill_No ,vehicleid)"
	                        + " select  sd.schedule_details_id, sd.form_four_id,sd.schedule_number ,  sd.number_of_trips ,  sd.list_item_number , "
	                        + " sd.trip_number ,  sd.customer_id ,  sd.trip_type , sd.start_point, sd.end_point, sd.route_number_id, sd.distance,"
	                        + " sd.start_time, sd.end_time ,  sd.running_time , break_type_id ,  break_time ,  crew_change ,  night_halt , "
	                        + " shift_type_id ,  is_dread_trip ,  crew_change_location , sd.night_halt_location ,  sd.break_location , "
	                        + " sd.operation_type ,  sd.remarks , '" + userId + "' ,  now() ,  '0' , "
	                        + " sd.deleted_status ,'" + insertedWaybillId + "','" + wayBillNo + "','" + vehicleId + "' from schedule_details sd "
	                        + " inner join form_four ff on ff.form_four_id = sd.form_four_id "
	                        + " inner join schedule s on s.schedule_id=sd.schedule_number "
	                        + " inner join waybill_details wd on wd.schedule_No = ff.form_four_id "
	                        + " where wd.waybill_No ='" + wayBillNo + "' and  schedule_No ='" + paramList.get(13).toString() + "' and sd.deleted_status=0 "
	                        + dayOutCondition + "";
	            }

	            int detailInsertResult = session.createSQLQuery(detailInsertQuery).executeUpdate();
	            if (detailInsertResult == 0) {
	                transaction.rollback();
	                return false; // Or handle it accordingly
	            }
	            transaction.commit();
	            String formFourId=paramList.get(13).toString();
	            String conductorId = paramList.get(0).toString();
	            String driverId = paramList.get(1).toString();
	            if (paramList.get(15).toString().equalsIgnoreCase("DAY OUT")) {

	            	int shift1Id = common.getDBResultInt(session, "SELECT sys_value FROM default_system_veriable WHERE sys_key='SHIFT_1'", "sys_value");
	            	int shiftTypeI = Integer.parseInt(shiftTypeId);

	            	if(shiftTypeI==shift1Id){
	            		int availableLogsheetCount = common.getDBResultInt(session, "SELECT count(*) as count from gen_logsheet where gen_logsheet_date ='" + startDateStr + "'  AND schedule_no='" + formFourId + "' AND status!='INACTIVE'","count");
	            		if(availableLogsheetCount>0){
	            			String updateLogsheetQuery = "UPDATE gen_logsheet SET waybill_id='"+insertedWaybillId+"',conductor1_id='"+conductorId+"', driver1_id ='"+driverId+"',vehicle_id='"+vehicleId+"',updated_date=now() WHERE gen_logsheet_date ='" + startDateStr + "'  AND schedule_no='" + formFourId + "' AND status!='INACTIVE' ";
	            			System.out.println(creation.getDBExcute(session, updateLogsheetQuery));
	            		}else{
	            			 String sqlQueryForLogsheet = " INSERT INTO `gen_logsheet` (`gen_logsheet_no`,`gen_logsheet_date`, `conductor1_id`, "
	                                 + " `driver1_id`,`vehicle_id`,`schedule_no`,`schedule_type`,`service_type`, "
	                                 + " `created_by`,`updated_by`,`created_date`,`updated_date`,`status`,Duty_Rota_No,waybill_id) VALUES "
	                                 + " ('" + logsheetNo + "','" + startDateStr + "','" + conductorId + "','" + driverId + "','" + vehicleId + "','" + formFourId + "','" + paramList.get(5).toString() + "',"
	                                 + " '" + paramList.get(6).toString() + "','" + userId + "','0',now(),NULL,'IN PROCESS','" + driverBlockId + "','" + insertedWaybillId + "')";

	                         int logid=creation.getDBExcute(session, sqlQueryForLogsheet);
	                         if(logid!=0){
	                         int insertedLogsheetId = common.getDBResultInt(session, "select max(gen_logsheet_id) as id from gen_logsheet", "id");

	                         String logSheetDetailsInsertQuery = " INSERT INTO  gen_logsheet_details  (  schedule_details_id ,  form_four_id ,  "
	                                 + " schedule_number ,  number_of_trips ,  list_item_number ,  trip_number ,  customer_id ,  trip_type ,"
	                                 + " start_point ,  end_point ,  route_number_id ,  distance ,  start_time ,  end_time ,  running_time ,"
	                                 + " break_type_id ,  break_time ,  crew_change ,  night_halt ,  shift_type_id ,  is_dread_trip ,  crew_change_location ,"
	                                 + " night_halt_location ,  break_location ,  operation_type ,  remarks ,  inserted_by ,  inserted_date ,  updated_by ,"
	                                 + " deleted_status, logsheet_id  )"
	                                 + " select  sd.schedule_details_id, sd.form_four_id,sd.schedule_number ,  sd.number_of_trips ,  sd.list_item_number , "
	                                 + " sd.trip_number ,  sd.customer_id ,  sd.trip_type , sd.start_point, sd.end_point, sd.route_number_id, sd.distance,"
	                                 + " sd.start_time, sd.end_time ,  sd.running_time , break_type_id ,  break_time ,  crew_change ,  night_halt , "
	                                 + " shift_type_id ,  is_dread_trip ,  crew_change_location , sd.night_halt_location ,  sd.break_location , "
	                                 + " sd.operation_type ,  sd.remarks ,  '" + userId + "' ,  now() ,  '0' , "
	                                 + " '0','" + insertedLogsheetId + "'  from schedule_details sd "
	                                 + " inner join form_four ff on ff.form_four_id = sd.form_four_id "
	                                 + " inner join schedule s on s.schedule_id=sd.schedule_number "
	                                 + " inner join gen_logsheet gl on gl.schedule_no = ff.form_four_id "
	                                 + " where   gen_logsheet_id = '" + insertedLogsheetId + "'";

	                         int resultOfDetailQuery1 = creation.getDBExcute(session, logSheetDetailsInsertQuery);
	                         if (resultOfDetailQuery1 == 0) {
	                             transaction.rollback();
	                         }
	                         }
	            		}
	            	}
	            	else {
	            		int availableLogsheetCount = common.getDBResultInt(session, "SELECT count(*) as count from gen_logsheet where gen_logsheet_date ='" + startDateStr + "' AND schedule_no='" + formFourId + "' AND status!='INACTIVE'","count");
	            		if(availableLogsheetCount>0){
	            			String updateLogsheetQuery = "UPDATE gen_logsheet SET waybill_id='"+insertedWaybillId+"',conductor2_id='"+conductorId+"', driver2_id ='"+driverId+"',vehicle_id='"+vehicleId+"',updated_date=now() WHERE gen_logsheet_date ='" + startDateStr + "'  AND schedule_no='" + formFourId + "' AND status!='INACTIVE' ";
	            			creation.getDBExcute(session, updateLogsheetQuery);
	            		}else{
	            			 String sqlQueryForLogsheet = " INSERT INTO `gen_logsheet` (`gen_logsheet_no`,`gen_logsheet_date`, `conductor2_id`, "
	                                 + " `driver2_id`,`vehicle_id`,`schedule_no`,`schedule_type`,`service_type`, "
	                                 + " `created_by`,`updated_by`,`created_date`,`updated_date`,`status`,Duty_Rota_No,waybill_id) VALUES "
	                                 + " ('" + logsheetNo + "','" + startDateStr + "','" + conductorId + "','" + driverId + "','" + vehicleId + "','" + formFourId + "','" + paramList.get(5).toString() + "',"
	                                 + " '" + paramList.get(6).toString() + "','" + userId + "','0',now(),NULL,'IN PROCESS','" + driverBlockId + "','" + insertedWaybillId + "')";

	                         int logid=creation.getDBExcute(session, sqlQueryForLogsheet);
	                         if(logid!=0){
	                         int insertedLogsheetId = common.getDBResultInt(session, "select max(gen_logsheet_id) as id from gen_logsheet", "id");
	                         String logSheetDetailsInsertQuery = " INSERT INTO  gen_logsheet_details  (  schedule_details_id ,  form_four_id ,  "
	                                 + " schedule_number ,  number_of_trips ,  list_item_number ,  trip_number ,  customer_id ,  trip_type ,"
	                                 + " start_point ,  end_point ,  route_number_id ,  distance ,  start_time ,  end_time ,  running_time ,"
	                                 + " break_type_id ,  break_time ,  crew_change ,  night_halt ,  shift_type_id ,  is_dread_trip ,  crew_change_location ,"
	                                 + " night_halt_location ,  break_location ,  operation_type ,  remarks ,  inserted_by ,  inserted_date ,  updated_by ,"
	                                 + " deleted_status, logsheet_id  )"
	                                 + " select  sd.schedule_details_id, sd.form_four_id,sd.schedule_number ,  sd.number_of_trips ,  sd.list_item_number , "
	                                 + " sd.trip_number ,  sd.customer_id ,  sd.trip_type , sd.start_point, sd.end_point, sd.route_number_id, sd.distance,"
	                                 + " sd.start_time, sd.end_time ,  sd.running_time , break_type_id ,  break_time ,  crew_change ,  night_halt , "
	                                 + " shift_type_id ,  is_dread_trip ,  crew_change_location , sd.night_halt_location ,  sd.break_location , "
	                                 + " sd.operation_type ,  sd.remarks ,  '" + userId + "' ,  now() ,  '0' , "
	                                 + " '0','" + insertedLogsheetId + "'  from schedule_details sd "
	                                 + " inner join form_four ff on ff.form_four_id = sd.form_four_id "
	                                 + " inner join schedule s on s.schedule_id=sd.schedule_number "
	                                 + " inner join gen_logsheet gl on gl.schedule_no = ff.form_four_id "
	                                 + " where   gen_logsheet_id = '" + insertedLogsheetId + "'";

	                         int resultOfDetailQuery2 = creation.getDBExcute(session, logSheetDetailsInsertQuery);
	                         if (resultOfDetailQuery2 == 0) {
	                             transaction.rollback();
	                         }
	                         transaction.commit();
	                         }
	            		}
	            	}
	            } else {
	                if (transaction==null || !transaction.isActive()) {
						transaction=session.beginTransaction();
					}
	            	int availableLogsheetCount = common.getDBResultInt(session, "SELECT count(*) as count from gen_logsheet where gen_logsheet_date ='" + startDateStr + "'  AND schedule_no='" + formFourId + "' AND status!='INACTIVE'","count");
	        		if(availableLogsheetCount>0){
	        			String updateLogsheetQuery = "UPDATE gen_logsheet SET waybill_id='"+insertedWaybillId+"',conductor1_id='"+conductorId+"', driver1_id ='"+driverId+"',vehicle_id='"+vehicleId+"',updated_date=now() WHERE gen_logsheet_date ='" + startDateStr + "'  AND schedule_no='" + formFourId + "' AND status!='INACTIVE' ";
	        			creation.getDBExcute(session, updateLogsheetQuery);
	        		}else{
	                String sqlQueryForLogsheet = " INSERT INTO `gen_logsheet` (`gen_logsheet_no`,`gen_logsheet_date`, `conductor1_id`, "
	                        + " `driver1_id`,`vehicle_id`,`schedule_no`,`schedule_type`,`service_type`, "
	                        + " `created_by`,`updated_by`,`created_date`,`updated_date`,`status`,Duty_Rota_No,waybill_id) VALUES "
	                        + " ('" + logsheetNo + "','" + startDateStr + "','" + conductorId + "','" + driverId + "','" + vehicleId + "','" + formFourId + "','" + paramList.get(5).toString() + "',"
	                        + " '" + paramList.get(6).toString() + "','" + userId + "','0',now(),NULL,'IN PROCESS','" + driverBlockId + "','" + insertedWaybillId + "')";
	                int logid=creation.getDBExcute(session, sqlQueryForLogsheet);
	                if(logid!=0){
	                int insertedLogsheetId = common.getDBResultInt(session, "select max(gen_logsheet_id) as id from gen_logsheet", "id");
	                String logSheetDetailsInsertQuery = " INSERT INTO  gen_logsheet_details  (  schedule_details_id ,  form_four_id ,  "
	                        + " schedule_number ,  number_of_trips ,  list_item_number ,  trip_number ,  customer_id ,  trip_type ,"
	                        + " start_point ,  end_point ,  route_number_id ,  distance ,  start_time ,  end_time ,  running_time ,"
	                        + " break_type_id ,  break_time ,  crew_change ,  night_halt ,  shift_type_id ,  is_dread_trip ,  crew_change_location ,"
	                        + " night_halt_location ,  break_location ,  operation_type ,  remarks ,  inserted_by ,  inserted_date ,  updated_by ,"
	                        + " deleted_status, logsheet_id  )"
	                        + " select  sd.schedule_details_id, sd.form_four_id,sd.schedule_number ,  sd.number_of_trips ,  sd.list_item_number , "
	                        + " sd.trip_number ,  sd.customer_id ,  sd.trip_type , sd.start_point, sd.end_point, sd.route_number_id, sd.distance,"
	                        + " sd.start_time, sd.end_time ,  sd.running_time , break_type_id ,  break_time ,  crew_change ,  night_halt , "
	                        + " shift_type_id ,  is_dread_trip ,  crew_change_location , sd.night_halt_location ,  sd.break_location , "
	                        + " sd.operation_type ,  sd.remarks ,  '" + userId + "' ,  now() ,  '0' , "
	                        + " '0','" + insertedLogsheetId + "'  from schedule_details sd "
	                        + " inner join form_four ff on ff.form_four_id = sd.form_four_id "
	                        + " inner join schedule s on s.schedule_id=sd.schedule_number "
	                        + " inner join gen_logsheet gl on gl.schedule_no = ff.form_four_id "
	                        + " where   gen_logsheet_id = '" + insertedLogsheetId + "'";

	                int resultOfDetailQuery3 = creation.getDBExcute(session, logSheetDetailsInsertQuery);
	                if (resultOfDetailQuery3 == 0) {
	                    transaction.rollback();
	                }
	                transaction.commit();
	                }
	        		}
	            }

	            if (transaction == null || !transaction.isActive()) {
	                transaction = session.beginTransaction();
	            }
	            boolean updateFlag= creation.updateWaybillOnline(insertedWaybillId, session, transaction,wayBillNo);

	            if (transaction == null || !transaction.isActive()) {
	                transaction = session.beginTransaction();
	            }
	            String sql3="INSERT INTO auto_waybill (running_date,depot_id,status) values ('"+startDateStr+"','"+depotId+"','success')";
				int countt=creation.getDBExcute(session,sql3);
				if (countt==0) {
					transaction.rollback();
				}
	            transaction.commit();
	            return true;
	        }
	            else {
	                System.out.println("Waybill already exists for: " + startDateStr);
	                return false;
	            }
	    }catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}

}
