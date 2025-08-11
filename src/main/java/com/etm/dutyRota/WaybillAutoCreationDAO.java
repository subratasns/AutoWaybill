package com.etm.dutyRota;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.PersistenceException;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.query.NativeQuery;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.hibernate.type.IntegerType;
import org.json.JSONObject;

import com.etm.util.HibernateUtil;

public class WaybillAutoCreationDAO {
	 public void createWayBill(List paramList, String type, String duty_type, int userid, String depotId, String startDate, HttpServletResponse response) {
	        int waybillCount = 0;

	        Common common = new Common();
	        Transaction transaction = null;
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
		        for (int i = 0; i < 3; i++) {
		        	startDateStr = sdf.format(start);
		            
		        if (transaction == null || !transaction.isActive()) {
	                transaction = session.beginTransaction();
	            }
	            boolean alreadyCreated = isWaybillCreatedAlready(response, common, session,
	                    paramList.get(13).toString(),
	                    paramList.get(16).toString(),
	                    startDateStr,
	                    paramList.get(5).toString());
	            if (!alreadyCreated) {
	            String sqlQueryForCounterValue = "SELECT IFNULL(MAX(waybill_Counter + 1), 1) AS count FROM waybill_gen_logic WHERE waybill_Date = :date";
	            NativeQuery<?> queryForCounterValue = session.createNativeQuery(sqlQueryForCounterValue)
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
	            String depotCode = getUserDepot(depotId);
	            String wayBillNo = generateWayBillNo(lastId, startDateStr, depotCode);

	            String sql1 = "SELECT IFNULL(MAX(logsheet_Counter+1), 1) count FROM waybill_gen_logic WHERE logsheet_Date = '" + startDateStr + "'";
	            int loghseetCount = common.getDBResultInt(session, sql1, "count");
	            String Innersql = " UPDATE waybill_gen_logic set logsheet_Counter=" + loghseetCount + " WHERE logsheet_Date='" + startDateStr + "'";
	            Query qry2 = session.createSQLQuery(Innersql);
	            qry2.executeUpdate();
	            String logsheetNo = "L"+wayBillNo;
	            
	            String insertQuery = getWaybillDetailsInsertQuery(paramList, startDateStr, wayBillNo, userIDString,depotId,session);
	            session.createNativeQuery(insertQuery).executeUpdate();
	            int insertedWaybillId = ((Number) session.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult()).intValue();
	            String detailInsertQuery = "";
	            
	            String vehicleId=paramList.get(4).toString();

	            if ((paramList.get(15).toString()).equalsIgnoreCase("NIGHT OUT")) {
	                int formFourOne = getFormFourForNightOut(common, session, startDateStr, paramList.get(13).toString(), "Day1");
	                int formFourTwo = getFormFourForNightOut(common, session, startDateStr, paramList.get(13).toString(), "Day2");
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

	            int resultOfDetailQuery = getDBExcute(session, detailInsertQuery);
	            if (resultOfDetailQuery == 0) {
	                transaction.rollback();
	            }
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
	            			System.out.println(getDBExcute(session, updateLogsheetQuery));
	            		}else{
	            			 String sqlQueryForLogsheet = " INSERT INTO `gen_logsheet` (`gen_logsheet_no`,`gen_logsheet_date`, `conductor1_id`, "
	                                 + " `driver1_id`,`vehicle_id`,`schedule_no`,`schedule_type`,`service_type`, "
	                                 + " `created_by`,`updated_by`,`created_date`,`updated_date`,`status`,Duty_Rota_No,waybill_id) VALUES "
	                                 + " ('" + logsheetNo + "','" + startDateStr + "','" + conductorId + "','" + driverId + "','" + vehicleId + "','" + formFourId + "','" + paramList.get(5).toString() + "',"
	                                 + " '" + paramList.get(6).toString() + "','" + userId + "','0',now(),NULL,'IN PROCESS','" + driverBlockId + "','" + insertedWaybillId + "')";

	                         int logid=getDBExcute(session, sqlQueryForLogsheet);
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

	                         int resultOfDetailQuery1 = getDBExcute(session, logSheetDetailsInsertQuery);
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
	            			getDBExcute(session, updateLogsheetQuery);
	            		}else{
	            			 String sqlQueryForLogsheet = " INSERT INTO `gen_logsheet` (`gen_logsheet_no`,`gen_logsheet_date`, `conductor2_id`, "
	                                 + " `driver2_id`,`vehicle_id`,`schedule_no`,`schedule_type`,`service_type`, "
	                                 + " `created_by`,`updated_by`,`created_date`,`updated_date`,`status`,Duty_Rota_No,waybill_id) VALUES "
	                                 + " ('" + logsheetNo + "','" + startDateStr + "','" + conductorId + "','" + driverId + "','" + vehicleId + "','" + formFourId + "','" + paramList.get(5).toString() + "',"
	                                 + " '" + paramList.get(6).toString() + "','" + userId + "','0',now(),NULL,'IN PROCESS','" + driverBlockId + "','" + insertedWaybillId + "')";

	                         int logid=getDBExcute(session, sqlQueryForLogsheet);
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

	                         int resultOfDetailQuery2 = getDBExcute(session, logSheetDetailsInsertQuery);
	                         if (resultOfDetailQuery2 == 0) {
	                             transaction.rollback();
	                         }
	                         }
	            		}
	            	}
	            } else {
	            	int availableLogsheetCount = common.getDBResultInt(session, "SELECT count(*) as count from gen_logsheet where gen_logsheet_date ='" + startDateStr + "'  AND schedule_no='" + formFourId + "' AND status!='INACTIVE'","count");
	        		if(availableLogsheetCount>0){
	        			String updateLogsheetQuery = "UPDATE gen_logsheet SET waybill_id='"+insertedWaybillId+"',conductor1_id='"+conductorId+"', driver1_id ='"+driverId+"',vehicle_id='"+vehicleId+"',updated_date=now() WHERE gen_logsheet_date ='" + startDateStr + "'  AND schedule_no='" + formFourId + "' AND status!='INACTIVE' ";
	        			getDBExcute(session, updateLogsheetQuery);
	        		}else{
	                String sqlQueryForLogsheet = " INSERT INTO `gen_logsheet` (`gen_logsheet_no`,`gen_logsheet_date`, `conductor1_id`, "
	                        + " `driver1_id`,`vehicle_id`,`schedule_no`,`schedule_type`,`service_type`, "
	                        + " `created_by`,`updated_by`,`created_date`,`updated_date`,`status`,Duty_Rota_No,waybill_id) VALUES "
	                        + " ('" + logsheetNo + "','" + startDateStr + "','" + conductorId + "','" + driverId + "','" + vehicleId + "','" + formFourId + "','" + paramList.get(5).toString() + "',"
	                        + " '" + paramList.get(6).toString() + "','" + userId + "','0',now(),NULL,'IN PROCESS','" + driverBlockId + "','" + insertedWaybillId + "')";
	                int logid=getDBExcute(session, sqlQueryForLogsheet);
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

	                int resultOfDetailQuery3 = getDBExcute(session, logSheetDetailsInsertQuery);
	                if (resultOfDetailQuery3 == 0) {
	                    transaction.rollback();
	                }
	                }
	        		}
	            }
	            String sql3="INSERT INTO auto_waybill (running_date,depot_id,status) values ('"+startDateStr+"','"+depotId+"','success')";
				int countt=getDBExcute(session,sql3);
				if (countt==0) {
					transaction.rollback();
				}
	            transaction.commit();
	        }
	            else {
	                System.out.println("Waybill already exists for: " + startDateStr);
	            }
	            Calendar cal = Calendar.getInstance();
	            cal.setTime(start);
	            cal.add(Calendar.DATE, 1);
	            start = cal.getTime();
			waybillCount++;
		   }
	    }catch (Exception e) {
				e.printStackTrace();
			}
		}
	 
	 public boolean createDateWiseWayBill(List paramList, String type, String duty_type, int userid, String depotId, String startDate, HttpServletResponse response) {
	        int waybillCount = 0;

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

	            boolean alreadyCreated = isWaybillCreatedAlready(response, common, session,
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
	            String depotCode = getUserDepot(depotId);
	            String wayBillNo = generateWayBillNo(lastId, startDateStr, depotCode);

	            String sql1 = "SELECT IFNULL(MAX(logsheet_Counter+1), 1) count FROM waybill_gen_logic WHERE logsheet_Date = '" + startDateStr + "'";
	            int loghseetCount = common.getDBResultInt(session, sql1, "count");
	            String Innersql = " UPDATE waybill_gen_logic set logsheet_Counter=" + loghseetCount + " WHERE logsheet_Date='" + startDateStr + "'";
	            Query qry2 = session.createSQLQuery(Innersql);
	            qry2.executeUpdate();
	            String logsheetNo = "L"+wayBillNo;
	            
	            String insertQuery = getWaybillDetailsInsertQuery(paramList, startDateStr, wayBillNo, userIDString,depotId,session);
	            System.out.println("Final Insert Query: " + insertQuery);
	            session.createSQLQuery(insertQuery).executeUpdate();
//	            transaction.commit(); // commit after insert
	            int insertedWaybillId = ((Number) session.createSQLQuery("SELECT LAST_INSERT_ID()").getSingleResult()).intValue();
	            System.out.println("last id-----"+insertedWaybillId);
	            String detailInsertQuery = "";
	            
	            String vehicleId=paramList.get(4).toString();

	            if ((paramList.get(15).toString()).equalsIgnoreCase("NIGHT OUT")) {
	                int formFourOne = getFormFourForNightOut(common, session, startDateStr, paramList.get(13).toString(), "Day1");
	                int formFourTwo = getFormFourForNightOut(common, session, startDateStr, paramList.get(13).toString(), "Day2");
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
	            			System.out.println(getDBExcute(session, updateLogsheetQuery));
	            		}else{
	            			 String sqlQueryForLogsheet = " INSERT INTO `gen_logsheet` (`gen_logsheet_no`,`gen_logsheet_date`, `conductor1_id`, "
	                                 + " `driver1_id`,`vehicle_id`,`schedule_no`,`schedule_type`,`service_type`, "
	                                 + " `created_by`,`updated_by`,`created_date`,`updated_date`,`status`,Duty_Rota_No,waybill_id) VALUES "
	                                 + " ('" + logsheetNo + "','" + startDateStr + "','" + conductorId + "','" + driverId + "','" + vehicleId + "','" + formFourId + "','" + paramList.get(5).toString() + "',"
	                                 + " '" + paramList.get(6).toString() + "','" + userId + "','0',now(),NULL,'IN PROCESS','" + driverBlockId + "','" + insertedWaybillId + "')";

	                         int logid=getDBExcute(session, sqlQueryForLogsheet);
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

	                         int resultOfDetailQuery1 = getDBExcute(session, logSheetDetailsInsertQuery);
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
	            			getDBExcute(session, updateLogsheetQuery);
	            		}else{
	            			 String sqlQueryForLogsheet = " INSERT INTO `gen_logsheet` (`gen_logsheet_no`,`gen_logsheet_date`, `conductor2_id`, "
	                                 + " `driver2_id`,`vehicle_id`,`schedule_no`,`schedule_type`,`service_type`, "
	                                 + " `created_by`,`updated_by`,`created_date`,`updated_date`,`status`,Duty_Rota_No,waybill_id) VALUES "
	                                 + " ('" + logsheetNo + "','" + startDateStr + "','" + conductorId + "','" + driverId + "','" + vehicleId + "','" + formFourId + "','" + paramList.get(5).toString() + "',"
	                                 + " '" + paramList.get(6).toString() + "','" + userId + "','0',now(),NULL,'IN PROCESS','" + driverBlockId + "','" + insertedWaybillId + "')";

	                         int logid=getDBExcute(session, sqlQueryForLogsheet);
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

	                         int resultOfDetailQuery2 = getDBExcute(session, logSheetDetailsInsertQuery);
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
	        			getDBExcute(session, updateLogsheetQuery);
	        		}else{
	                String sqlQueryForLogsheet = " INSERT INTO `gen_logsheet` (`gen_logsheet_no`,`gen_logsheet_date`, `conductor1_id`, "
	                        + " `driver1_id`,`vehicle_id`,`schedule_no`,`schedule_type`,`service_type`, "
	                        + " `created_by`,`updated_by`,`created_date`,`updated_date`,`status`,Duty_Rota_No,waybill_id) VALUES "
	                        + " ('" + logsheetNo + "','" + startDateStr + "','" + conductorId + "','" + driverId + "','" + vehicleId + "','" + formFourId + "','" + paramList.get(5).toString() + "',"
	                        + " '" + paramList.get(6).toString() + "','" + userId + "','0',now(),NULL,'IN PROCESS','" + driverBlockId + "','" + insertedWaybillId + "')";
	                int logid=getDBExcute(session, sqlQueryForLogsheet);
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

	                int resultOfDetailQuery3 = getDBExcute(session, logSheetDetailsInsertQuery);
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

	            String sql = "UPDATE waybill_details SET Status = 'ONLINE', Duty_Satus='Y', duty_start_date=now() "
	                    + " WHERE waybil_Id = '" + insertedWaybillId + "' AND Status IN ('NEW','PROCESSED') and TIMESTAMPDIFF(HOUR,now(),CONCAT(generated_Date,' 00:00:00')) <16";

	            Query query = session.createSQLQuery(sql);
	            int dbflag = query.executeUpdate();

	            if (dbflag > 0) {
	                transaction.commit(); // commit after successful update
	                boolean flagg = updateDriverAttendane(wayBillNo, session);
	                // handle flagg if needed
	            } else {
	                transaction.rollback(); // rollback if nothing updated
	            }

	            // If you're going to use transaction again below, re-init it
	            if (transaction == null || !transaction.isActive()) {
	                transaction = session.beginTransaction();
	            }
	            String sql3="INSERT INTO auto_waybill (running_date,depot_id,status) values ('"+startDateStr+"','"+depotId+"','success')";
				int countt=getDBExcute(session,sql3);
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

	 public boolean updateDriverAttendane(String waybillNo, Session session) {
	        boolean flag = false;
	        Transaction transaction = null;
	        transaction = session.beginTransaction();
	        try {
	            String sql = "UPDATE gen_logsheet gl "
	                    + "INNER JOIN waybill_details wd ON wd.waybil_Id = gl.waybill_id "
	                    + "SET gl.status='ONLINE' "
	                    + "WHERE wd.waybill_No='" + waybillNo + "' ;";

	            System.out.println("SQL----> " + sql);
	            Query query=session.createSQLQuery(sql);
	            int dbflag=query.executeUpdate();

	            if (dbflag>0) {
	            	transaction.commit();
	                flag = true;
	            } else {
	                System.out.println("\n \t updateDriverAttendane ******** Ohh Noooo Proble In query Execution.............");
	                flag = false;
	            }
	        } catch (Exception e) {
	            if (transaction != null) transaction.rollback();
	           e.printStackTrace();
	        }
	        return flag;
	    }

	public boolean isWaybillCreatedAlready(HttpServletResponse response, Common common, Session session,
			String scheduleNo, Object shiftTypeId, String waybillForDate, String scheduleType) throws IOException {
		String extraCondition = "";
		if ("DAY OUT".equalsIgnoreCase(scheduleType)) {
			extraCondition = " AND wd.Shift_Type = '" + shiftTypeId + "'"; 
		}

		try {
			String sql = "SELECT COUNT(*) AS count,ifnull(waybill_no,'') waybill_no FROM waybill_details wd "
					+ "INNER JOIN form_four ff ON ff.form_four_id = wd.Schedule_NO "
					+ "INNER JOIN schedule s ON s.schedule_id = ff.schedule_number_id " + "WHERE wd.schedule_No = '"
					+ scheduleNo + "' " + extraCondition + " AND generated_Date = '" + waybillForDate + "' "
					+ "AND wd.Status NOT IN ( 'INACTIVE','ONLINE')";

//			int count = common.getDBResultInt(session, sql, "count"); 
            Query qry2=session.createSQLQuery(sql);
			qry2.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
			List<Map<String, Object>> list1=qry2.list();

			if (list1.size()>0) {
				int count = Integer.parseInt(list1.get(0).get("count").toString());
				String waybill_no = list1.get(0).get("waybill_no").toString();
			if (count >= 1) {
				String updateSql = "UPDATE waybill_details wd SET Status = 'ONLINE', Duty_Satus = 'Y', duty_start_date = now() "
						+ "WHERE wd.waybill_no = '" + waybill_no + "' AND generated_Date = '"
						+ waybillForDate + "' " + "AND Status IN ('NEW', 'PROCESSED') "
						+ "AND TIMESTAMPDIFF(HOUR, now(), CONCAT(generated_Date, ' 00:00:00')) < 16";

				Query query = session.createSQLQuery(updateSql);
				if (!session.getTransaction().isActive()) {
	                session.beginTransaction();  // Only start a transaction if it's not already active
	            }
				int dbFlag = query.executeUpdate(); 
				
				if (dbFlag > 0) {
					session.getTransaction().commit(); 
				    return true;
				}else {
	                return false; 
	            } 
				
			} else {
				return false; 
			}
		  }else {
              return false; 
          } 
		} catch (SQLGrammarException e) {
			System.err.println("SQL Error: " + e.getMessage());
			throw new PersistenceException("Error in SQL query: " + e.getMessage(), e);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public int getDBExcute(Session objSession, String sql) throws Exception {
        int result = 0;
        Query query = objSession.createSQLQuery(sql);
        result = query.executeUpdate();
        return result;
    }

	public int getFormFourForNightOut(Common common, Session session, String waybillDate, String scheduleNo, String whichDay) throws ParseException {
        int formFourId = 0;
        Calendar cal = Calendar.getInstance();
        if (whichDay.equalsIgnoreCase("Day2")) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Calendar c = Calendar.getInstance();
            c.setTime(sdf.parse(waybillDate));
            c.add(Calendar.DATE, 1);  // number of days to add
            waybillDate = sdf.format(c.getTime());
//	            System.out.println(waybillDate);
        }
        int count = checkHolidayMaster(session, waybillDate);
        String queryForFormFourNo = "";
        String dayOfCurrentDate = common.changeDataFormat(waybillDate, "yyyy-MM-dd", "EEEE");

        if (count > 0) {

            queryForFormFourNo = " select ff.form_four_id formFourId from  schedule s "
                    + " inner join form_four ff on ff.schedule_number_id = s.schedule_id "
                    + " left join weeklyChart wc on ff.form_four_id =  wc.form_four_id "
                    + " where s.schedule_id ='" + scheduleNo + "' and wc.holiday=1 "
                    + " AND ff.deleted_status=0 AND ff.current_status='ACTIVE' AND s.status='NEW' and " +
                    "(ff.effective_end_date>= '"+waybillDate+"' or ff.effective_end_date is null) " +
                    "AND s.current_status='CASE WORKER' and s.deleted_status=0";

        } else {
            queryForFormFourNo = "select ff.form_four_id  formFourId  from schedule s "
                    + " inner join form_four ff on ff.schedule_number_id = s.schedule_id  "
                    + " inner join weeklyChart wc on ff.form_four_id =  wc.form_four_id "
                    + " where  s.schedule_id='" + scheduleNo + "'   and wc." + dayOfCurrentDate + "='1' "
                    + " and wc.deleted_status='0' AND ff.deleted_status=0 AND ff.current_status='ACTIVE' AND s.status='NEW' and " +
                    "(ff.effective_end_date>= '"+waybillDate+"' or ff.effective_end_date is null) " +
                    "AND s.current_status='CASE WORKER' and s.deleted_status=0";
        }

        formFourId = common.getDBResultInt(session, queryForFormFourNo, "formFourId");

        return formFourId;
    }

	public int checkHolidayMaster(Session session, String wayBillDate) {
        int count = 0;
        String sql = "select count(*) counts from holiday_master where holiday_date='" + wayBillDate + "' and status='ACTIVE' and deleted_status='0'";

        Query query = session.createSQLQuery(sql);
        query.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);

        List<Map<String, Object>> aliasToValueMapList = query.list();

        if (aliasToValueMapList.size() > 0) {
            Map<String, Object> resultSet = aliasToValueMapList.get(0);
            count = Integer.parseInt(resultSet.get("counts").toString());
        }

        return count;
    }

	public String getWaybillDetailsInsertQuery(List paramList, String todaysDate, String wayBillNo, String userID, String depotId, Session session) {

		System.out.println("depot id is>>>>>>>>>>"+depotId);
		String returnQuery = "";
		String scheduleTypeCode = String.valueOf(paramList.get(11));
		int shiftType = Integer.parseInt(String.valueOf(paramList.get(10)));
		System.out.println("<<<<schedule type code ====" + scheduleTypeCode + "---Shift Type----" + shiftType);
		if (scheduleTypeCode.equalsIgnoreCase("GN") || scheduleTypeCode.equalsIgnoreCase("SS")) {

			returnQuery = "INSERT INTO waybill_details(waybill_No, generated_Date, conductor_Id, driver_Id, ETM_No"
					+ " , Bag_No,Shift_Type, Vehicle_No, Schedult_Type, Service_Type, schedule_No, Created_By, Status, Audited_By,Duty_Rota_No,etim_flag,bag_flag,depot_id) "
					+ "VALUES ('" + wayBillNo + "', '" + todaysDate + "', '" + paramList.get(0) + "', '"
					+ paramList.get(1) + "', " + paramList.get(2) + ", " + paramList.get(3) + ",'1', '"
					+ paramList.get(4) + "',  '" + paramList.get(5) + "'," + " '" + paramList.get(6) + "','"
					+ paramList.get(7) + "','" + userID + "','NEW', '" + userID + "',null,'" + paramList.get(8) + "','"
					+ paramList.get(9) + "','"+depotId+"')";
			System.out.println(returnQuery);
			return returnQuery;
		} else if (scheduleTypeCode.equalsIgnoreCase("NO")) {
			String shiftTypeName = getShiftTypeIdFromDefaultSystemVariable(shiftType, session);
			if (shiftTypeName.equalsIgnoreCase("DAY_1") || shiftTypeName.equalsIgnoreCase("DAY_2")) {
				returnQuery = "INSERT INTO waybill_details(waybill_No, generated_Date, conductor_Id, driver_Id, ETM_No"
						+ " , Bag_No,Shift_Type, Vehicle_No, Schedult_Type, Service_Type, schedule_No, Created_By, Status, Audited_By,Duty_Rota_No,etim_flag,bag_flag,depot_id) "
						+ "VALUES ('" + wayBillNo + "', '" + todaysDate + "', '" + paramList.get(0) + "', '"
						+ paramList.get(1) + "', " + paramList.get(2) + ", " + paramList.get(3) + ",'2', '"
						+ paramList.get(4) + "',  '" + paramList.get(5) + "'," + " '" + paramList.get(6) + "','"
						+ paramList.get(7) + "','" + userID + "','NEW', '" + userID + "',null,'" + paramList.get(8)
						+ "','" + paramList.get(9) + "','"+depotId+"')";
				return returnQuery;
			}
		} else if (scheduleTypeCode.equalsIgnoreCase("GNO")) {
			String shiftTypeName = getShiftTypeIdFromDefaultSystemVariable(shiftType, session);
			if (shiftTypeName.equalsIgnoreCase("General_DAY_1") || shiftTypeName.equalsIgnoreCase("General_DAY_2")) {
				returnQuery = "INSERT INTO waybill_details(waybill_No, generated_Date, conductor_Id, driver_Id, ETM_No"
						+ " , Bag_No,Shift_Type, Vehicle_No, Schedult_Type, Service_Type, schedule_No, Created_By, Status, Audited_By,Duty_Rota_No,etim_flag,bag_flag,depot_id) "
						+ "VALUES ('" + wayBillNo + "', '" + todaysDate + "', '" + paramList.get(0) + "', '"
						+ paramList.get(1) + "', " + paramList.get(2) + ", " + paramList.get(3) + ",'" + shiftType
						+ "', '" + paramList.get(4) + "',  '" + paramList.get(5) + "'," + " '" + paramList.get(6)
						+ "','" + paramList.get(7) + "','" + userID + "','NEW', '" + userID + "',null,'"
						+ paramList.get(8) + "','" + paramList.get(9) + "','"+depotId+"')";
				return returnQuery;
			}
		} else if (scheduleTypeCode.equalsIgnoreCase("DO")) {
			String shiftTypeName = getShiftTypeIdFromDefaultSystemVariable(shiftType, session);
			if (shiftTypeName.equalsIgnoreCase("SHIFT_1") || shiftTypeName.equalsIgnoreCase("SHIFT_2")) {
				returnQuery = "INSERT INTO waybill_details(waybill_No, generated_Date, conductor_Id, driver_Id, ETM_No"
						+ " , Bag_No,Shift_Type, Vehicle_No, Schedult_Type, Service_Type, schedule_No, Created_By, Status, Audited_By,Duty_Rota_No,etim_flag,bag_flag,depot_id) "
						+ "VALUES ('" + wayBillNo + "', '" + todaysDate + "', '" + paramList.get(0) + "', '"
						+ paramList.get(1) + "', " + paramList.get(2) + ", " + paramList.get(3) + ",'" + shiftType
						+ "', '" + paramList.get(4) + "',  '" + paramList.get(5) + "'," + " '" + paramList.get(6)
						+ "','" + paramList.get(7) + "','" + userID + "','NEW', '" + userID + "',null,'"
						+ paramList.get(8) + "','" + paramList.get(9) + "','"+depotId+"')";
				return returnQuery;
			}
		} else if (scheduleTypeCode.equalsIgnoreCase("NS")) {
			returnQuery = "INSERT INTO waybill_details(waybill_No, generated_Date, conductor_Id, driver_Id, ETM_No"
					+ " , Bag_No, Vehicle_No, Schedult_Type, Service_Type, schedule_No, Created_By, Status, Audited_By,Duty_Rota_No,etim_flag,bag_flag,depot_id) "
					+ "VALUES ('" + wayBillNo + "', '" + todaysDate + "', '" + paramList.get(0) + "', '"
					+ paramList.get(1) + "', " + paramList.get(2) + ", " + paramList.get(3) + ", '" + paramList.get(4)
					+ "',  '" + paramList.get(5) + "'," + " '" + paramList.get(6) + "','" + paramList.get(7) + "','"
					+ userID + "','NEW', '" + userID + "',null,'" + paramList.get(8) + "','" + paramList.get(9) + "','"+depotId+"')";
			return returnQuery;
		}

		return returnQuery;
	}
	
	public String getShiftTypeIdFromDefaultSystemVariable(int shiftType, Session session) {
	    String result = "";

	    try {
	        String query = "SELECT sys_key FROM default_system_veriable " +
	                       "WHERE sys_value = '" + shiftType + "' " +
	                       "AND sys_key IN ('DAY_1', 'DAY_2', 'SHIFT_1', 'SHIFT_2', 'GENERAL_SHIFT', 'NIGHT_SHIFT', 'Split_Service')";

	        System.out.println("Query => " + query);

	        List<?> list = session.createSQLQuery(query).getResultList();

	        if (!list.isEmpty()) {
	            result = (String) list.get(0); // expecting sys_key to be String
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return result;
	}

	public String getUserDepot(String org_chart_id) {
		Session session = HibernateUtil.getSessionFactory().openSession();
        String sql = " SELECT LPAD(SUBSTRING(org_code,1,3),2,'0') org_code  FROM depot where org_chart_id='" +org_chart_id+ "' ";
        Query query = session.createSQLQuery(sql);
        String parent_name = "";
        if (query.list().size() > 0) {
            parent_name = query.uniqueResult().toString();
        }
        return parent_name;
    }
	
	public String generateWayBillNo(int wayBill, String date2, String depotCode) {

        String waybill_Date = getDateForWaybill_Gen(date2);
        String Waybill_No = String.format("%03d", wayBill);
        String finalWayBill;
        try {
            finalWayBill = depotCode + waybill_Date + Waybill_No;
        } catch (Exception e) {
            finalWayBill = "";
        }
        return finalWayBill;
    }
    public String getDateForWaybill_Gen(String date) {
        String date1 = date.substring(2, 4) + date.substring(5, 7) + date.substring(8, 10);
        return date1;
    }

	@SuppressWarnings("deprecation")
	public boolean checkScheduleStatusBeforeWaybillCreation(String scheduleNo) {
		boolean flag = false;
        Session session=HibernateUtil.getSessionFactory().openSession();
		String route_No;

		try {

			String query1 = "SELECT distinct(sd.route_number_id) route_number_id, schedule_service_type "
					+ "FROM schedule_details sd " + "INNER JOIN schedule sc ON sd.schedule_number = sc.schedule_id "
					+ "INNER JOIN form_four ff ON sd.form_four_id = ff.form_four_id "
					+ "INNER JOIN route r ON r.route_id =sd.route_number_id "
					+ "WHERE sd.is_dread_trip='0' AND sd.trip_type!=3 AND r.route_type_id != 5 AND ff.form_four_id='"
					+ scheduleNo + "' AND sd.deleted_status='0' "
					+ " AND ff.current_status='ACTIVE' and ff.deleted_status='0';";
			System.out.println("1 >" + query1);
			Query qry=session.createSQLQuery(query1);
			qry.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);

			List<Map<String, Object>> list=qry.list();
               for (int i = 0; i < list.size(); i++) {
//					route_No = rs1.getString("route_number_id");
                route_No = list.get(i).get("route_number_id").toString();
				// **************** For Checking The Route Start *********************
				String query2 = "SELECT count(route_number) count, ifnull(route_number, 'M2G') route_number "
						+ "FROM route " + "WHERE route_Id='" + list.get(i).get("route_number_id").toString()
						+ "' AND deleted_status = '0' "
						+ "AND (curdate() between effective_from and ifnull((CASE WHEN effective_till='0000-00-00 00:00:00' "
						+ "THEN null else effective_till end),(curdate()+1)) OR ( curdate() >= effective_from and ifnull((CASE WHEN effective_till='0000-00-00 00:00:00' "
						+ "THEN null else effective_till end),(DATE_ADD(curdate(),INTERVAL 1 DAY)))>=curdate()));";
                Query qry2=session.createSQLQuery(query2);
    			qry2.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
    			List<Map<String, Object>> list1=qry2.list();

    			if (list1.size()>0) {
					flag = false;

					int count = Integer.parseInt(list1.get(0).get("count").toString());
					route_No = list1.get(0).get("route_number").toString();
					if (count == 1) {
						 System.out.println("The Route with ID : " + list.get(i).get("route_number_id").toString() + " used in Schedule is active");
						flag = true;
					} else {
						String msg="The Route with ID : " + list.get(i).get("route_number_id").toString() + " used in Schedule is either in Inactive or Deleted Mode";

						return false;
						
					}
				}
				// **************** For Checking The Route End *********************

//------------------------------------------------------------------------------------------------------------------------
				// **************** For Checking The Fare Chart Start *********************
				String query3 = "SELECT count(farechart_master_id) count " + "FROM farechart_master fc "
						+ "INNER JOIN rate_master rm ON fc.rate_master_id = rm.rate_master_id " + "WHERE route_id='"
						+ list.get(i).get("route_number_id").toString() + "' AND fc.deleted_status='0' " + "AND fc.service_type_id='"
						+ list.get(i).get("schedule_service_type").toString() + "' "
						+ "AND now() BETWEEN fc.effect_start_date AND ifnull(fc.effect_end_date, date_add(now(), INTERVAL 1 DAY)) "
						+ "AND now() BETWEEN rm.effective_start_date AND ifnull(rm.effective_end_date, date_add(now(), INTERVAL 1 DAY));";
				System.out.println("Farechaart" + query3);
				Query qry3=session.createSQLQuery(query3);
    			qry3.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
    			List<Map<String, Object>> list2=qry3.list();
                    if (list2.size()>0) {

					flag = false;
					int count = Integer.parseInt(list2.get(0).get("count").toString());
					if (count == 1) {
						 System.out.println("Only 1 Fare Chart For Route : " + list.get(i).get("route_number_id").toString());
						flag = true;
					} else if (count == 0) {
						String msg="No Fare Chart For Route : "+list.get(i).get("route_number_id").toString();
						System.out.println("No Fare Chart For Route : " + list.get(i).get("route_number_id").toString());
						return false;
					} else if (count > 1) {
						String msg="Waybill can not be created As More than one i.e. " + count + " Fare Chart present in system for Route Number : " + route_No;
						 System.out.println(" Very Bad... " + count + " Fare Chart For Route : "+ list.get(i).get("route_number_id").toString());
						return false;
					}
				}

				// **************** For Checking The Fare Chart End ***********************
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
           session.close();
		}
		// TODO Auto-generated method stub
		return flag;
	}

	@SuppressWarnings("unchecked")
    public List<String> getFormFourId(String scheduleNo, String startDate) {
    	
//	    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//	    	String date1 = sdf.format(startDate);
    	
        List<String> listOfObject = new ArrayList<>();
        String whichDay = "";
        String day;

        if (scheduleNo != null && startDate != null) {
            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                System.out.println("date>>>>>>>" + startDate);

                SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
                Date dt1 = format1.parse(startDate);

                SimpleDateFormat format2 = new SimpleDateFormat("EEEE");
                day = format2.format(dt1).toLowerCase();

                String strHolyday = new SimpleDateFormat("yyyy-MM-dd").format(dt1);
                System.out.println("Formatted holiday date >>>>> " + strHolyday);
                System.out.println("Day of week >>>>> " + day);

                // Check holiday
                String holidaySql = "SELECT holiday_day FROM holiday_master WHERE status='ACTIVE' AND holiday_date = :holidayDate";
                Query qry = session.createSQLQuery(holidaySql)
                                   .setParameter("holidayDate", strHolyday)
                                   .setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);

                List<Map<String, Object>> holidayList = qry.list();
                if (!holidayList.isEmpty()) {
                    whichDay = String.valueOf(holidayList.get(0).get("holiday_day"));
                } 
                if (!whichDay.equals("")) {
					day = "holiday";
				} else {
					whichDay = day;
				}

                // Get form_four_id
//	                String formFourSql = "SELECT ff.form_four_id FROM schedule sc "
//	                        + "INNER JOIN form_four ff ON ff.schedule_number_id = sc.schedule_id "
//	                        + "INNER JOIN weeklyChart wc ON wc.form_four_id = ff.form_four_id "
//	                        + "WHERE sc.schedule_id = :scheduleId AND sc.deleted_status = '0' AND sc.status = 'NEW' "
//	                        + "AND IF(:day = '1', :day = '1', :whichDay = '1') "
//	                        + "AND ff.deleted_status = '0' AND ff.current_status = 'ACTIVE' "
//	                        + "AND wc.deleted_status = '0' AND wc.status = 'ACTIVE'";
                String formFourSql="SELECT ff.schedule_number_name, ff.form_four_id FROM schedule sc "
								+ "INNER JOIN form_four ff ON ff.schedule_number_id = sc.schedule_id "
//									+ "INNER JOIN weeklyChart wc ON wc.form_four_id = ff.form_four_id "
								+ "WHERE sc.schedule_id='"
								+ scheduleNo + "' AND sc.deleted_status='0'" + " AND sc.status = 'NEW'"
//									+ " AND if(" + day + "='1'," + day + "='1'," + whichDay + "='1') "
								+ " AND ff.deleted_status='0' AND ff.current_status='ACTIVE' ";
//									+ "AND wc.deleted_status='0' AND wc.status='ACTIVE' ";
                Query formFourQuery = session.createSQLQuery(formFourSql)
                        .setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);

                List<Map<String, Object>> formFourList = formFourQuery.list();
                String form_four_id = "";
                if (!formFourList.isEmpty()) {
                    form_four_id = String.valueOf(formFourList.get(0).get("form_four_id"));
                }

                if (!form_four_id.isEmpty()) {
                    String sql = "SELECT service_type_name, schedule_code, service.service_type_id, schedule_type_name, "
                            + "shedule.schedule_type_id, s.schedule_type, shift.shift_type_id, ff.start_time, ff.schedule_number_name "
                            + "FROM form_four ff "
                            + "INNER JOIN schedule_details sd ON sd.form_four_id = ff.form_four_id "
                            + "INNER JOIN schedule s ON s.schedule_id = sd.schedule_number "
                            + "INNER JOIN shift_type shift ON shift.shift_type_id = sd.shift_type_id "
                            + "INNER JOIN service_type service ON service.service_type_id = s.schedule_service_type "
                            + "INNER JOIN schedule_type shedule ON shedule.schedule_type_id = s.schedule_type "
                            + "WHERE s.status = 'NEW' AND s.deleted_status = '0' "
                            + "AND ff.current_status = 'ACTIVE' AND ff.deleted_status = '0' "
                            + "AND ff.form_four_id = :formFourId "
                            + "GROUP BY ff.form_four_id LIMIT 1";

                    Query query2 = session.createSQLQuery(sql)
                            .setParameter("formFourId", form_four_id)
                            .setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);

                    List<Map<String, Object>> detailsList = query2.list();

                    for (Map<String, Object> row : detailsList) {
                        listOfObject.add(String.valueOf(row.get("schedule_type_id")));
                        listOfObject.add(String.valueOf(row.get("service_type_id")));
                        listOfObject.add(String.valueOf(row.get("schedule_code")));
                        listOfObject.add(form_four_id);
                        listOfObject.add(String.valueOf(row.get("schedule_type_name")));
                        listOfObject.add(String.valueOf(row.get("shift_type_id")));                    }

//	                    System.out.println("Output: " + new JSONArray(listOfObject));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return listOfObject;
    }
    
    public List<Object> getScheduleNoDetails(String scheduleId, Date startDate) {
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    	String selectDateStr = sdf.format(startDate);
    	
        Map<Integer, String> resultMap = new LinkedHashMap<>();
        List<Object> listOfObject = new ArrayList<>();
        String formfour_id = "";
        String day = "";
        String whichDay = "";
        String service_type_name = "";
        String scheduleTypeId = "";

        SimpleDateFormat inputFormat = new SimpleDateFormat("dd-MM-yyyy");
        SimpleDateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // Parse and format date
            Date parsedDate = inputFormat.parse(selectDateStr);
            String strHolyday = dbFormat.format(parsedDate);
            day = dayFormat.format(parsedDate).toLowerCase();

            // Step 1: Check if holiday
            String holidaySql = "SELECT holiday_day FROM holiday_master WHERE status='ACTIVE' AND holiday_date = :holidayDate";
            Query holidayQuery = session.createSQLQuery(holidaySql);
            holidayQuery.setParameter("holidayDate", strHolyday);
            holidayQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);

            List<Map<String, Object>> holidayList = holidayQuery.list();
            if (!holidayList.isEmpty()) {
                whichDay = String.valueOf(holidayList.get(0).get("holiday_day"));
                day = "holiday";
            } else {
                whichDay = day;
            }

            // Step 2: Get form_four_id
            String formFourSql = "SELECT ff.schedule_number_name, ff.form_four_id FROM schedule sc " +
                    "INNER JOIN form_four ff ON ff.schedule_number_id = sc.schedule_id " +
                    "INNER JOIN weeklyChart wc ON wc.form_four_id = ff.form_four_id " +
                    "WHERE sc.schedule_id = :scheduleId AND sc.deleted_status = '0' AND sc.status = 'NEW' " +
                    "AND IF(:day = '1', :day = '1', :whichDay = '1') " +
                    "AND ff.deleted_status = '0' AND ff.current_status = 'ACTIVE' " +
                    "AND wc.deleted_status = '0' AND wc.status = 'ACTIVE'";

            Query formFourQuery = session.createSQLQuery(formFourSql);
            formFourQuery.setParameter("scheduleId", scheduleId);
            formFourQuery.setParameter("day", day);
            formFourQuery.setParameter("whichDay", whichDay);
            formFourQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);

            List<Map<String, Object>> formFourList = formFourQuery.list();
            if (!formFourList.isEmpty()) {
                formfour_id = String.valueOf(formFourList.get(0).get("form_four_id"));
            }

            // Step 3: Get schedule details
            String scheduleDetailsSql = "SELECT service_type_name, schedule_code, service.service_type_id, schedule_type_name, " +
                    "shedule.schedule_type_id, s.schedule_type, shift.shift_type_id, ff.start_time, ff.schedule_number_name " +
                    "FROM form_four ff " +
                    "INNER JOIN schedule_details sd ON sd.form_four_id = ff.form_four_id " +
                    "INNER JOIN schedule s ON s.schedule_id = sd.schedule_number " +
                    "INNER JOIN shift_type shift ON shift.shift_type_id = sd.shift_type_id " +
                    "INNER JOIN service_type service ON service.service_type_id = sd.schedule_service_type " +
                    "INNER JOIN schedule_type shedule ON shedule.schedule_type_id = s.schedule_type " +
                    "WHERE s.status = 'NEW' AND s.deleted_status = '0' " +
                    "AND ff.current_status = 'ACTIVE' AND ff.deleted_status = '0' " +
                    "AND ff.form_four_id = :formfourId " +
                    "GROUP BY ff.form_four_id LIMIT 1";

            Query detailsQuery = session.createSQLQuery(scheduleDetailsSql);
            detailsQuery.setParameter("formfourId", formfour_id);
            detailsQuery.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);

            List<Map<String, Object>> scheduleDataList = detailsQuery.list();

            for (Map<String, Object> data : scheduleDataList) {
                listOfObject.add(data.get("start_time").toString());
                listOfObject.add(data.get("schedule_number_name").toString());
                listOfObject.add(data.get("schedule_type_name").toString());
                listOfObject.add(data.get("service_type_name").toString());

                service_type_name = String.valueOf(data.get("service_type_name"));
                scheduleTypeId = String.valueOf(data.get("schedule_type"));

                Map<String, String> shiftDetails = getDutyType(scheduleTypeId);
                listOfObject.add(shiftDetails);

                System.out.println("Shift Details: " + shiftDetails);
            }

//	            System.out.println("Collected Data: " + new JSONArray(listOfObject));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return listOfObject;
    }
    
    @SuppressWarnings("unchecked")
	private Map<String, String> getDutyType(String scheduleTypeId) {
		System.out.println("set duty type>>>>>>>" + scheduleTypeId);
		Map<String, String> shiftList = new LinkedHashMap<String, String>();
		// HttpServletResponse response=ServletActionContext.getResponse();
		PrintWriter out = null;
		Session session = HibernateUtil.getSessionFactory().openSession();
		try {// out=response.getWriter();
			Query qry = session.createSQLQuery("SELECT shift_type_id, shift_type_name, sys_key FROM shift_type s "
					+ "INNER JOIN default_system_veriable dst ON dst.sys_value = s.shift_type_id "
					+ "AND sys_key IN ('DAY_1', 'DAY_2', 'SHIFT_1', 'SHIFT_2', 'GENERAL_SHIFT', 'NIGHT_SHIFT','Split_Service') "
					+ "WHERE schedule_type_id = '" + scheduleTypeId
					+ "' AND s.status = 'ACTIVE' AND s.deleted_status = '0' ");
			qry.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
			List<Map<String, Object>> shiftData = qry.list();
			for (int i = 0; i < shiftData.size(); i++) {
				shiftList.put(shiftData.get(i).get("shift_type_id").toString(),
						shiftData.get(i).get("shift_type_name").toString());
			}
			System.out.println("List of shift------" + shiftList);
			// out.print(shiftList);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		} finally {
			HibernateUtil.shutdown();
		}
		return shiftList;
	}
    
public boolean updateWaybillOnline(int insertedWaybillId, Session session, Transaction transaction, String wayBillNo) {
	try {
		 String sql = "UPDATE waybill_details SET Status = 'ONLINE', Duty_Satus='Y', duty_start_date=now() "
	             + " WHERE waybil_Id = '" + insertedWaybillId + "' AND Status IN ('NEW','PROCESSED') and TIMESTAMPDIFF(HOUR,now(),CONCAT(generated_Date,' 00:00:00')) <16";
	
	     Query query = session.createSQLQuery(sql);
			if (!session.getTransaction().isActive()) {
	            session.beginTransaction();  // Only start a transaction if it's not already active
	        }
			int dbFlag = query.executeUpdate(); 

	     if (dbFlag > 0) {
	    	 session.getTransaction().commit();  
	         boolean flagg = updateDriverAttendane(wayBillNo, session);
	         if (flagg) {
				return true;
			}else {
				return false;
			}
	     } else {
	         transaction.rollback(); 
				return false;
	     }
	
	} catch (Exception e) {
		e.printStackTrace();
		return false;
	}
           
  }
}
