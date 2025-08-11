package com.etm.dutyRota;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.hibernate.type.IntegerType;

import com.etm.util.HibernateUtil;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

//@WebServlet("/waybillCreationAuto")
public class SaveWayBillAction extends HttpServlet {
	@Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String depotIdParam = request.getParameter("depot_id");
        System.out.println("Received depot_id: " + depotIdParam);

        response.setContentType("text/plain");
        if (depotIdParam != null) {
        	boolean success = createWayBillAuto("","","",new Date(),new Date(), 0,depotIdParam);
            response.getWriter().write("Received depot_id: " + depotIdParam);
        } else {
            response.getWriter().write("No depot_id received");
        }
    }
	
	@SuppressWarnings("finally")
    public boolean createWayBillAuto(String reqDutyRotaId, String fromDay, String tillDay, Date fromDate, Date tillDate,int passcount,String depot_id) {
//    	Logger logger = Logge.getLogger();
        String loggerText = "";
        Session session = null;
        boolean isSuccess = false;
        Common common = new Common();
        Transaction transaction = null;
        int waybillCount = 0;
        int userId = 1;
        List<Map<String, String>> aliasToValueList = new ArrayList<Map<String, String>>();
        SimpleDateFormat formatter2 = new SimpleDateFormat("dd-MM-yyyy");
        boolean isAuto = true;
        if(!reqDutyRotaId.equals("")){
        	isAuto = false;
        }
        try {
            Calendar todays = Calendar.getInstance();
            session = HibernateUtil.getSessionFactory().openSession();
            int waybillCreateFordays = common.getDBResultInt(session, "SELECT `sys_value`  FROM `local_system_variable` WHERE `sys_key` = 'WAYBILL_CREATE_FOR_DAYS' AND status='Y' ", "sys_value");
            for (int y = 0; y < waybillCreateFordays; y++) {
                //waybillCount = 0;
                Date date = todays.getTime();
                System.out.println("date>>>>>>>>>>>"+date);
                String reqDutyRotaIdCondition="";
                if(!isAuto){
                	date = fromDate;
                	reqDutyRotaIdCondition = " AND duty_rota_block_id ='" + reqDutyRotaId + "'";
                	Calendar till= Calendar.getInstance();
                	till.setTime(tillDate);
                	
                	
                	Date date2 = till.getTime();
                	System.out.print(formatter2.format(date));
                	System.out.print(formatter2.format(date2));
                	if (common.compareDates(formatter2.format(date),formatter2.format(date2))>0) {
                		break;
                	}
                }
               
                SimpleDateFormat formatter1 = new SimpleDateFormat("dd-MM-yyyy");
                String currentdate = formatter1.format(date);
                String queryForDutyRota = "Select s.duty_rota_block_id, concat(s.duty_rota_block_name,'-',  DATE_FORMAT(effective_start_date,  '%d-%m-%Y')) duty_rota_block_name  "
                        + " from duty_rota_block_master s   "
                        + " inner join schedule_type schedule on  schedule.schedule_type_id = s.schedule_type_id WHERE  "
                        + "  (effective_start_date<=str_to_date('" + currentdate + "','%d-%m-%Y') AND (effective_end_date >= str_to_date('" + currentdate + "','%d-%m-%Y') "
                        + " or effective_end_date ='0000-00-00' or effective_end_date is null) )  "+reqDutyRotaIdCondition+" and s.deleted_status='0' and depot_id='"+depot_id+"' and duty_rota_type in (1,4,3,6)  order by duty_rota_type,duty_rota_block_id";

                Query queryList = session.createSQLQuery(queryForDutyRota);
                queryList.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
//                System.out.println(queryForDutyRota);
                aliasToValueList = queryList.list();
                System.out.println("size of dutyrota list........"+aliasToValueList.size());

                if (aliasToValueList.size() > 0) {
                    for (int z = 0; z < aliasToValueList.size(); z++) {
                        try {
                            Map<String, String> map = aliasToValueList.get(z);
                            String dutyRotaId = String.valueOf(map.get("duty_rota_block_id"));
                            if(!reqDutyRotaId.equals(dutyRotaId) && !isAuto){
                            	continue;
                            }
                            String blockName = String.valueOf(map.get("duty_rota_block_name"));

                            if (transaction == null || !transaction.isActive()) {
                                transaction = session.beginTransaction();
                            }

                            String dayOfCurrentDate = getDayFromdate(date);
                            DateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd");
                            String crewaDetailsQuery = "";
                            DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
                            String dutyRotaType = common.getDBResultStr(session, "SELECT `duty_rota_type` FROM `duty_rota_block_master` WHERE `duty_rota_block_id` = '" + dutyRotaId + "'", "duty_rota_type");
                            String scheduleType = getscheduleType(common, session, dutyRotaId);
                            if (scheduleType.equalsIgnoreCase("NIGHT OUT") && !(dutyRotaType.equals("3") || dutyRotaType.equals("6"))) {
                            	crewaDetailsQuery = " SELECT d1.duty_rota_block_details_id conductorRotaId,d2.duty_rota_block_details_id driverRotaId,d1.conductor_id,d1.device_id,d2.driver_id,d1.schedule_type_id,dm.service_type_id,d2.duty_rota_block_id,dm.duty_type,"
                                        + " d2.vehicle_id,d1.ticket_bag_id,d1.dc_id,d1.thursday,dm.depot_id,dm.service_type_id,dm.duty_rota_block_name,dm2.duty_rota_block_name,"
                                        + " d1.monday,d1.tuesday,d1.wednesday,d1.thursday,d1.friday,d1.saturday,d1.sunday FROM duty_rota_block_details d1 "
                                        + " INNER JOIN duty_rota_block_master dm ON d1.duty_rota_block_id = dm.duty_rota_block_id AND dm.deleted_status = '0'"
                                        + " INNER JOIN duty_rota_block_details d2 ON d2." + dayOfCurrentDate + " = d1." + dayOfCurrentDate 
                                        + " AND d1." + dayOfCurrentDate + " not in('-1','-2','0') "
                                        + " INNER JOIN duty_rota_block_master dm2 ON d2.duty_rota_block_id = dm2.duty_rota_block_id AND dm2.deleted_status='0'"
                                        + " WHERE d1.duty_rota_block_id ='" + dutyRotaId + "' AND d2.duty_rota_block_id !='" + dutyRotaId + "' "
                                        + " AND d1.duty_rota_block_id < d2.duty_rota_block_id " 
                                        + " AND dm.status='ACTIVE' AND dm2.schedule_type_id = dm.schedule_type_id and dm.depot_id='"+depot_id+"' "
                                        + " AND dm.duty_type=dm2.duty_type "
                                        + " AND (d1." + dayOfCurrentDate + "_no = '1'  AND d2." + dayOfCurrentDate + "_no='1' )  "
                                        + " AND date_format(str_to_date('" + currentdate + "','%d-%m-%Y'),'%Y-%m-%d') "
                                        + " BETWEEN dm2.effective_start_date AND if(dm2.effective_end_date IS NULL, date_format(str_to_date('" + currentdate + "','%d-%m-%Y'),'%Y-%m-%d'),"
                                        + " dm2.effective_end_date)";

                            } else if (!(dutyRotaType.equals("3") || dutyRotaType.equals("6"))) {
                            	crewaDetailsQuery = "SELECT d1.duty_rota_block_details_id conductorRotaId,d2.duty_rota_block_details_id driverRotaId,d1.conductor_id,d1.device_id,d2.driver_id,d1.schedule_type_id,dm.service_type_id,d2.duty_rota_block_id,dm.duty_type,"
                                        + " d2.vehicle_id,d1.ticket_bag_id,d1.dc_id,d1.thursday,dm.depot_id,dm.service_type_id,dm.duty_rota_block_name,dm2.duty_rota_block_name,"
                                        + " d1.monday,d1.tuesday,d1.wednesday,d1.thursday,d1.friday,d1.saturday,d1.sunday FROM duty_rota_block_details d1 "
                                        + " INNER JOIN duty_rota_block_master dm ON d1.duty_rota_block_id = dm.duty_rota_block_id AND dm.deleted_status = '0' "
                                        + " INNER JOIN duty_rota_block_details d2 ON d2." + dayOfCurrentDate + " = d1." + dayOfCurrentDate + " "
                                        + " AND d1." + dayOfCurrentDate + " not in('-1','-2','0') "
                                        + " INNER JOIN duty_rota_block_master dm2 ON d2.duty_rota_block_id = dm2.duty_rota_block_id   AND dm2.deleted_status='0' "
                                        + " WHERE d1.duty_rota_block_id ='" + dutyRotaId + "' AND d2.duty_rota_block_id !='" + dutyRotaId + "'"
                                        + "AND d1.duty_rota_block_id < d2.duty_rota_block_id AND dm.status='ACTIVE'   "
                                        + " AND dm2.schedule_type_id = dm.schedule_type_id AND dm.duty_type=dm2.duty_type and dm.depot_id='"+depot_id+"' "
                                        + " AND date_format(str_to_date('" + currentdate + "','%d-%m-%Y'),'%Y-%m-%d') "
                                        + " BETWEEN dm2.effective_start_date AND if(dm2.effective_end_date IS NULL, date_format(str_to_date('" + currentdate + "','%d-%m-%Y'),'%Y-%m-%d'),"
                                        + " dm2.effective_end_date)";
                            }
                            System.out.println("crewaDetailsQuery>>>>>>>>>"+crewaDetailsQuery);
                            Query query = session.createSQLQuery(crewaDetailsQuery).addScalar("conductorRotaId").addScalar("driverRotaId")
                            		.addScalar("duty_rota_block_id")
                            		.addScalar("conductor_id").addScalar("driver_id").addScalar("device_id")
                            		.addScalar("dc_id").addScalar("schedule_type_id").addScalar("service_type_id")
                            		.addScalar("ticket_bag_id").addScalar("duty_type").addScalar("vehicle_id")
                            		.addScalar("monday").addScalar("tuesday").addScalar("wednesday")
                            		.addScalar("thursday").addScalar("friday").addScalar("saturday").addScalar("sunday");
                            		
                            query.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
                            List<Map<String, Object>> aliasToValueMapList = query.list();
                            for (int i = 0; i < aliasToValueMapList.size(); i++) {
                            	System.out.println("dutyRotaId........"+dutyRotaId);
                            	System.out.println("dutyRotaId..size......"+aliasToValueMapList.size());
                                try {
                                    Map<String, Object> resultSet = aliasToValueMapList.get(i);
                                    if (resultSet != null) {
                                        String driverId = "", conductorId = "";
                                        int driverBlockId = Integer.parseInt(resultSet.get("duty_rota_block_id").toString());
                                        int conductorDutyRotaDetId = Integer.parseInt(resultSet.get("conductorRotaId").toString());
                                        int driverBlockDetId = Integer.parseInt(resultSet.get("driverRotaId").toString());
                                        String isConductorOff = common.getDBResultStr(session, "SELECT is_off_releaver FROM duty_rota_block_details WHERE duty_rota_block_details_id='" + conductorDutyRotaDetId + "'", "is_off_releaver");
                                        String isDriverOff = common.getDBResultStr(session, "SELECT is_off_releaver FROM duty_rota_block_details WHERE duty_rota_block_details_id='" + driverBlockDetId + "'", "is_off_releaver");

                                        if (!(dutyRotaType.equals("3") || dutyRotaType.equals("6"))) {
                                            conductorId = resultSet.get("conductor_id").toString();
                                            driverId = resultSet.get("driver_id").toString();
                                        } else {
                                            conductorId = resultSet.get("dc_id").toString();
                                            driverId = resultSet.get("dc_id").toString();
                                        }
                                        String vehicleid=resultSet.get("vehicle_id").toString();
                                        boolean isConductorActive = isEmloyeeActive(common, session,conductorId);
                                        boolean isDriverActive = isEmloyeeActive(common, session,driverId);
                                        boolean isVehicleScrap = isVehiclScrap(common, session,vehicleid);
                                        if (isVehicleScrap && isConductorActive && isDriverActive && resultSet.size() != 0 && conductorId != null && driverId != null && !conductorId.equals("0") && !driverId.equals("0")) {
//                                        	System.out.println("vehicle conductor driver are active........");
                                        	String offBagId = "", offDeviceId = "";
                                            int offVehicleId = 0;
                                            if (scheduleType.equalsIgnoreCase("NIGHT OUT")) {
                                                offDeviceId = common.getDBResultStr(session, "SELECT `device_id` FROM `duty_rota_block_details` WHERE `duty_rota_block_id` = '" + dutyRotaId + "' AND " + dayOfCurrentDate + "_no = '0' AND `is_off_releaver` = 'N'", "device_id");
                                                offVehicleId = common.getDBResultInt(session, "SELECT `vehicle_id` FROM `duty_rota_block_details` WHERE `duty_rota_block_id`  IN (SELECT duty_rota_block_id FROM duty_rota_block_details WHERE `duty_rota_block_details_id` = '"+driverBlockDetId+"'  ) AND " 
                                                					+ dayOfCurrentDate + "_no = '0' AND `is_off_releaver` = 'N'", "vehicle_id");
                                            } else {
                                                offBagId = common.getDBResultStr(session, "SELECT `ticket_bag_id` FROM `duty_rota_block_details` WHERE `duty_rota_block_id` = '" + dutyRotaId + "' AND " + dayOfCurrentDate + " = '-2' ", "ticket_bag_id");
                                                offDeviceId = common.getDBResultStr(session, "SELECT `device_id` FROM `duty_rota_block_details` WHERE `duty_rota_block_id` = '" + dutyRotaId + "' AND " + dayOfCurrentDate + "= '-2' ", "device_id");
                                                offVehicleId = common.getDBResultInt(session, "SELECT vehicle_id FROM `duty_rota_block_details`  WHERE "+dayOfCurrentDate+"= '-2' AND duty_rota_block_id IN (SELECT duty_rota_block_id FROM duty_rota_block_details WHERE `duty_rota_block_details_id` = '"+driverBlockDetId+"'  ) ", "vehicle_id");

                                            }
                                            String waybillForDate = getTodaysDate(date);
                                            System.out.println("waybilldate>>>>>>>>>>"+waybillForDate);
                                            String deviceId =  isConductorOff.equals("Y") ? offDeviceId : resultSet.get("device_id").toString();
                                            Integer vehicleId = isDriverOff.equals("Y")  ? offVehicleId : Integer.parseInt(resultSet.get("vehicle_id").toString());
                                            String scheduleTypeId = resultSet.get("schedule_type_id").toString();
                                            String serviceTypeId = resultSet.get("service_type_id").toString();
                                            String scheduleNo = resultSet.get(dayOfCurrentDate).toString();
                                            String bagNo = "";
                                            if (scheduleType.equalsIgnoreCase("NIGHT OUT")) {
                                                bagNo = returnBagNoFromMapping(common,session, scheduleNo, waybillForDate);
                                                deviceId = returnETMNoFromMapping(common, session, scheduleNo, waybillForDate);
                                            } else {
                                                bagNo = resultSet.get("ticket_bag_id").toString().equals("0") ? offBagId : resultSet.get("ticket_bag_id").toString();
                                            }
                                            Object shiftTypeId = resultSet.get("duty_type");

                                            String dayOutCondition = "";

                                            if (shiftTypeId != null) {
                                                if (getscheduleType(common, session, dutyRotaId).equalsIgnoreCase("DAY OUT")) {
                                                    dayOutCondition = " and sd.shift_type_id ='" + shiftTypeId + "'";
                                                }
                                            }
                                            if (!isWaybillCreatedAlready(common, session, scheduleNo, shiftTypeId, waybillForDate, scheduleType)) {
//                                            	System.out.println("waybill is creating for that duty roata......."+dutyRotaType);
//                                            	transaction = session.beginTransaction();
//                                                    String sqlQueryForCounterValue = " SELECT IFNULL(MAX(waybill_Counter+1), 1) count FROM waybill_gen_logic WHERE waybill_Date = '" + waybillForDate + "';";
//                                                    Query queryForCounterValue = session.createSQLQuery(sqlQueryForCounterValue).addScalar("count", Hibernate.INTEGER);
//                                                    List<Integer> list = queryForCounterValue.list();
//                                                    int lastId = list.get(0);
                                                    

                                                    // Step 1: Get next waybill counter
                                                    String sqlQueryForCounterValue = "SELECT IFNULL(MAX(waybill_Counter + 1), 1) AS count FROM waybill_gen_logic WHERE waybill_Date = :date";
                                                    NativeQuery<?> queryForCounterValue = session.createNativeQuery(sqlQueryForCounterValue)
                                                            .addScalar("count", IntegerType.INSTANCE)
                                                            .setParameter("date", waybillForDate);

                                                    List<?> resultList = queryForCounterValue.list();
                                                    int lastId = (resultList != null && !resultList.isEmpty()) ? (Integer) resultList.get(0) : 1;

                                                    if (lastId == 1) {
                                                        String Innersql = " INSERT INTO waybill_gen_logic (waybill_Date, waybill_Counter, created_By, created_Date,logsheet_Date)"
                                                                + " VALUES('" + waybillForDate + "'," + lastId + ",	" + userId + ",	now(),'" + waybillForDate + "');";
                                                        Query qry2 = session.createSQLQuery(Innersql);
                                                        qry2.executeUpdate();
                                                    } else {
                                                        String Innersql = " UPDATE waybill_gen_logic set waybill_Counter=" + lastId + " WHERE waybill_Date='" + waybillForDate + "'";
                                                        Query qry2 = session.createSQLQuery(Innersql);
                                                        qry2.executeUpdate();
                                                    }
                                                    String depotCode = getUserDepot(depot_id);
//                                                    System.out.println("depot code for autowaybill>>>>"+depotCode);
                                                    String wayBillNo = generateWayBillNo(lastId, waybillForDate, depotCode);
                                                    //For Create LogSheet No
                                                    String countLogsheet = " SELECT COUNT(*) count FROM waybill_gen_logic WHERE logsheet_Date = '" + waybillForDate + "';";
                                                    int logsheetCounter = common.getDBResultInt(session, countLogsheet, "count");

                                                    if (logsheetCounter == 0) {
                                                        String Innersql = " INSERT INTO waybill_gen_logic (logsheet_Date, logsheet_Counter, created_By, created_Date)"
                                                                + " VALUES('" + waybillForDate + "','0',	" + userId + ",	now());";
                                                        Query qry2 = session.createSQLQuery(Innersql);
                                                        qry2.executeUpdate();
                                                    } else {
                                                        String sql1 = "SELECT IFNULL(MAX(logsheet_Counter+1), 1) count FROM waybill_gen_logic WHERE logsheet_Date = '" + waybillForDate + "'";
                                                        int loghseetCount = common.getDBResultInt(session, sql1, "count");
                                                        String Innersql = " UPDATE waybill_gen_logic set logsheet_Counter=" + loghseetCount + " WHERE logsheet_Date='" + waybillForDate + "'";
                                                        Query qry2 = session.createSQLQuery(Innersql);
                                                        qry2.executeUpdate();
                                                        //}
                                                        String logsheetNo = "L"+wayBillNo;

                                                        int count = checkHolidayMaster(session, waybillForDate);
                                                        String queryForFormFourNo = "";
                                                        if (count > 0) {
                                                            queryForFormFourNo = " select ff.form_four_id formFourId from  schedule s "
                                                                    + " inner join form_four ff on ff.schedule_number_id = s.schedule_id "
                                                                    + " left join weeklyChart wc on ff.form_four_id =  wc.form_four_id "
                                                                    + " where s.schedule_id ='" + scheduleNo + "' and wc.holiday=1 and s.depot_id='"+depot_id+"' "
                                                                    + " AND ff.deleted_status=0 AND ff.current_status='ACTIVE' AND s.status='NEW' and " +
                                                                    "(ff.effective_end_date>= '"+waybillForDate+"' or ff.effective_end_date is null) " +
                                                                    "AND s.current_status='CASE WORKER' and s.deleted_status=0";
                                                        } else {
                                                            queryForFormFourNo = "select ff.form_four_id  formFourId  from schedule s "
                                                                    + " inner join form_four ff on ff.schedule_number_id = s.schedule_id  "
                                                                    + " inner join weeklyChart wc on ff.form_four_id =  wc.form_four_id "
                                                                    + " where  s.schedule_id='" + scheduleNo + "'   and wc." + dayOfCurrentDate + "='1' and s.depot_id='"+depot_id+"' "
                                                                    + "  and wc.deleted_status='0' AND ff.deleted_status=0 AND ff.current_status='ACTIVE' AND s.status='NEW' and " +
                                                                    "(ff.effective_end_date>= '"+waybillForDate+"' or ff.effective_end_date is null) " +
                                                                    "AND s.current_status='CASE WORKER' and s.deleted_status=0";
                                                        }
                                                        int formFourId = common.getDBResultInt(session, queryForFormFourNo, "formFourId");
                                                        int res1 = 0;
                                                        if (formFourId != 0) {
                                                            String queryForWaybillCreation = "";
                                                            String bagFlag = "", etmFlag = "";
                                                            if (bagNo.equals("0")) {
                                                                bagFlag = "N";
                                                            } else {
                                                                bagFlag = "Y";
                                                            }
                                                            if (deviceId.equals("0") || deviceId.equals("")) {
                                                                deviceId = "0";
                                                                etmFlag = "N";
                                                            } else {
                                                                etmFlag = "Y";
                                                            }
                                                            if (shiftTypeId == null || shiftTypeId.toString().equals("0")) {
                                                                shiftTypeId = getShiftTypeId(common, session, getscheduleType(common, session, dutyRotaId));

                                                            }
                                                          System.out.println("shifttypeid>>>>"+shiftTypeId);

                                                            queryForWaybillCreation="INSERT INTO waybill_details(waybill_No, generated_Date, conductor_Id ,driver_Id,ETM_No ,"
                                                            		+ "Bag_No, Vehicle_No, Duty_Rota_No, Driver_Duty_Rota_No,Schedult_Type, Shift_Type,Service_Type, schedule_No, Created_By, Status, Audited_By,etim_flag,bag_flag,depot_id)  "
                                                            		+ "VALUES ('"+wayBillNo+"', '"+waybillForDate+"', '"+conductorId+"', '"+driverId+"','"+deviceId+"', '"+bagNo+"', '"+vehicleId+"', "
                                                            				+ "'"+dutyRotaId+"','"+driverBlockId+"', '"+scheduleTypeId+"', '"+shiftTypeId+"','"+serviceTypeId+"','"+formFourId+"', '1','NEW', '"+userId+"','"+etmFlag+"','"+bagFlag+"','"+depot_id+"');";
                                                            res1 = getDBExcute(session, queryForWaybillCreation);
                                                            int insertedWaybillId =0;
                                                            if(res1!=0){
                                                             insertedWaybillId = common.getDBResultInt(session, "select max(waybil_Id) as id from waybill_details ", "id");
                                                            }else{
                                                            	
                                                            }

                                                            if (getscheduleType(common, session, dutyRotaId).contains("Night Out")) {

                                                            }
                                                            String detailInsertQuery = "";
                                                            if (scheduleType.equalsIgnoreCase("NIGHT OUT")) {
                                                                int formFourOne = getFormFourForNightOut(common, session, waybillForDate, scheduleNo, "Day1");
                                                                int formFourTwo = getFormFourForNightOut(common, session, waybillForDate, scheduleNo, "Day2");
                                                                if(formFourOne==0 || formFourTwo==0){
                                                                	transaction.rollback();
                                                                    continue;
                                                                }
                                                                String dayOneId = common.getDBResultStr(session, "SELECT sys_value FROM default_system_veriable WHERE sys_key ='DAY_1'", "sys_value");
                                                                String dayTwoId = common.getDBResultStr(session, "SELECT sys_value FROM default_system_veriable WHERE sys_key ='DAY_2'", "sys_value");
//                                                                System.out.println("----------------->>>" + formFourOne + " ------------->>>" + formFourTwo);
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
                                                                        + " where wd.waybill_No ='" + wayBillNo + "' and  schedule_No ='" + formFourId + "' and sd.deleted_status=0 "
                                                                        + dayOutCondition + "";
                                                            }

                                                            int resultOfDetailQuery = getDBExcute(session, detailInsertQuery);
                                                            if (resultOfDetailQuery == 0) {
                                                                loggerText += "\n no trip details found for " + scheduleNo + " so waybill not created ";
                                                                transaction.rollback();
                                                                continue;
                                                            }

                                                            if (getscheduleType(common, session, dutyRotaId).equalsIgnoreCase("DAY OUT")) {

                                                            	int shift1Id = common.getDBResultInt(session, "SELECT sys_value FROM default_system_veriable WHERE sys_key='SHIFT_1'", "sys_value");
                                                            	int shiftTypeI = (Integer) shiftTypeId;

                                                            	if(shiftTypeI==shift1Id){
                                                            		int availableLogsheetCount = common.getDBResultInt(session, "SELECT count(*) as count from gen_logsheet where gen_logsheet_date ='" + waybillForDate + "'  AND schedule_no='" + formFourId + "' AND status!='INACTIVE'","count");
                                                            		if(availableLogsheetCount>0){
                                                            			String updateLogsheetQuery = "UPDATE gen_logsheet SET waybill_id='"+insertedWaybillId+"',conductor1_id='"+conductorId+"', driver1_id ='"+driverId+"',vehicle_id='"+vehicleId+"',updated_date=now() WHERE gen_logsheet_date ='" + waybillForDate + "'  AND schedule_no='" + formFourId + "' AND status!='INACTIVE' ";
                                                            			System.out.println(getDBExcute(session, updateLogsheetQuery));
                                                            		}else{
                                                            			 String sqlQueryForLogsheet = " INSERT INTO `gen_logsheet` (`gen_logsheet_no`,`gen_logsheet_date`, `conductor1_id`, "
                                                                                 + " `driver1_id`,`vehicle_id`,`schedule_no`,`schedule_type`,`service_type`, "
                                                                                 + " `created_by`,`updated_by`,`created_date`,`updated_date`,`status`,Duty_Rota_No,waybill_id) VALUES "
                                                                                 + " ('" + logsheetNo + "','" + waybillForDate + "','" + conductorId + "','" + driverId + "','" + vehicleId + "','" + formFourId + "','" + scheduleTypeId + "',"
                                                                                 + " '" + serviceTypeId + "','" + userId + "','0',now(),NULL,'IN PROCESS','" + driverBlockId + "','" + insertedWaybillId + "')";

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
                                                                             loggerText += "\n no logsheet trip details found for " + scheduleNo + " so waybill not created ";
                                                                             transaction.rollback();
                                                                             continue;
                                                                         }
                                                                         }
                                                            		}
                                                            	}
                                                            	else {
                                                            		int availableLogsheetCount = common.getDBResultInt(session, "SELECT count(*) as count from gen_logsheet where gen_logsheet_date ='" + waybillForDate + "' AND schedule_no='" + formFourId + "' AND status!='INACTIVE'","count");
                                                            		if(availableLogsheetCount>0){
                                                            			String updateLogsheetQuery = "UPDATE gen_logsheet SET waybill_id='"+insertedWaybillId+"',conductor2_id='"+conductorId+"', driver2_id ='"+driverId+"',vehicle_id='"+vehicleId+"',updated_date=now() WHERE gen_logsheet_date ='" + waybillForDate + "'  AND schedule_no='" + formFourId + "' AND status!='INACTIVE' ";
                                                            			getDBExcute(session, updateLogsheetQuery);
                                                            		}else{
                                                            			 String sqlQueryForLogsheet = " INSERT INTO `gen_logsheet` (`gen_logsheet_no`,`gen_logsheet_date`, `conductor2_id`, "
                                                                                 + " `driver2_id`,`vehicle_id`,`schedule_no`,`schedule_type`,`service_type`, "
                                                                                 + " `created_by`,`updated_by`,`created_date`,`updated_date`,`status`,Duty_Rota_No,waybill_id) VALUES "
                                                                                 + " ('" + logsheetNo + "','" + waybillForDate + "','" + conductorId + "','" + driverId + "','" + vehicleId + "','" + formFourId + "','" + scheduleTypeId + "',"
                                                                                 + " '" + serviceTypeId + "','" + userId + "','0',now(),NULL,'IN PROCESS','" + driverBlockId + "','" + insertedWaybillId + "')";

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
                                                                             loggerText += "\n no logsheet trip details found for " + scheduleNo + " so waybill not created ";
                                                                             transaction.rollback();
                                                                             continue;
                                                                         }
                                                                         }
                                                            		}
                                                            	}
                                                            } else {
                                                            	int availableLogsheetCount = common.getDBResultInt(session, "SELECT count(*) as count from gen_logsheet where gen_logsheet_date ='" + waybillForDate + "'  AND schedule_no='" + formFourId + "' AND status!='INACTIVE'","count");
                                                        		if(availableLogsheetCount>0){
                                                        			String updateLogsheetQuery = "UPDATE gen_logsheet SET waybill_id='"+insertedWaybillId+"',conductor1_id='"+conductorId+"', driver1_id ='"+driverId+"',vehicle_id='"+vehicleId+"',updated_date=now() WHERE gen_logsheet_date ='" + waybillForDate + "'  AND schedule_no='" + formFourId + "' AND status!='INACTIVE' ";
                                                        			getDBExcute(session, updateLogsheetQuery);
                                                        		}else{
                                                                String sqlQueryForLogsheet = " INSERT INTO `gen_logsheet` (`gen_logsheet_no`,`gen_logsheet_date`, `conductor1_id`, "
                                                                        + " `driver1_id`,`vehicle_id`,`schedule_no`,`schedule_type`,`service_type`, "
                                                                        + " `created_by`,`updated_by`,`created_date`,`updated_date`,`status`,Duty_Rota_No,waybill_id) VALUES "
                                                                        + " ('" + logsheetNo + "','" + waybillForDate + "','" + conductorId + "','" + driverId + "','" + vehicleId + "','" + formFourId + "','" + scheduleTypeId + "',"
                                                                        + " '" + serviceTypeId + "','" + userId + "','0',now(),NULL,'IN PROCESS','" + driverBlockDetId + "','" + insertedWaybillId + "')";
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
                                                                    loggerText += "\n no logsheet trip details found for " + scheduleNo + " so waybill not created ";
                                                                    transaction.rollback();
                                                                    continue;
                                                                }
                                                                }
                                                        		}
                                                            }
                                                            String sql3="INSERT INTO auto_waybill (running_date,depot_id,status) values ('"+waybillForDate+"','"+depot_id+"','success')";
                                            				int countt=getDBExcute(session,sql3);
                                            				if (countt==0) {
                                            					transaction.rollback();
                                                                continue;
															}
                                                            transaction.commit();
                                                        }
                                                        waybillCount++;
                                                    }
                                                }
                                            }
                                        }

                                } catch (Exception e) {
                                    e.printStackTrace();
                                    loggerText += "\n" + e.toString() + " error For" + blockName;
                                    continue;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            loggerText += "\n" + e.toString();
                            continue;
                        }
                    }
                }
//                System.out.println(">>>>>>>time>>>>"+todays.getTime());
//                System.out.println(">>>>>count waybill>>>>"+waybillCount);
                todays.add(Calendar.DAY_OF_MONTH, 1);
            }
            isSuccess = true;
        } catch (Exception e) {
            e.printStackTrace();
            loggerText += "\n" + e.toString();
        } finally {
            System.out.println("passcount >>>>> " + passcount);

            // Only close session if it was opened and `passcount != 1`
            if (passcount != 1 && session != null && session.isOpen()) {
                session.close();
            }
            System.out.println(loggerText);

//            logger.debug(loggerText);
            return isSuccess;
        }

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
//            System.out.println(waybillDate);
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

	public int getDBExcute(Session objSession, String sql) throws Exception {
        int result = 0;
        Query query = objSession.createSQLQuery(sql);
        result = query.executeUpdate();
        return result;
    }

	public String getShiftTypeId(Common common, Session session, String scheduleType) {
        String scheduleshift = "";
        String shiftId = "0";
        if (scheduleType.toLowerCase().contains("General Shift".toLowerCase())) {
            scheduleshift = "general_shift_type_id";
        } else if (scheduleType.toLowerCase().contains("Night Out".toLowerCase())) {
            scheduleshift = "night_out_shift_type_id";
        } else if (scheduleType.toLowerCase().contains("Night Service".toLowerCase())) {
            scheduleshift = "night_service_shift_type_id";
        } else if (scheduleType.toLowerCase().contains("Split Service".toLowerCase())) {
            scheduleshift = "Split_Service";
        }

        String query = "SELECT `sys_value` value FROM `default_system_veriable` WHERE `sys_key` = '" + scheduleshift + "'";
        try {
            shiftId = common.getDBResultStr(session, query, "value");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return shiftId;

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

	private boolean isWaybillCreatedAlready( Common common, Session session, String scheduleNo, Object shiftTypeId,
			String waybillForDate, String scheduleType) {
		String extraCondition = "";
        if (scheduleType.equalsIgnoreCase("DAY OUT")) {
            extraCondition = " AND wd.Shift_Type = '" + shiftTypeId + "'";
        }

        String sql = " SELECT COUNT(*) AS count FROM waybill_details wd "
                + " INNER JOIN form_four ff ON ff.form_four_id = wd.Schedule_NO "
                + " INNER JOIN schedule s ON s.schedule_id = ff.schedule_number_id "
                + " WHERE s.schedule_id = '" + scheduleNo + "'  " + extraCondition + "  AND generated_Date ='" + waybillForDate + "' "
                + " AND wd.Status!='INACTIVE'";
        int count = common.getDBResultInt(session, sql, "count");

        if (count >= 1) {
            return true;
        } else {
            return false;
        }
	}

//	@SuppressWarnings("unchecked")
//	    public String returnETMNoFromMapping(Common common, Session session, String scheduleNo, String waybillDate) throws Exception {
//
//	        String etmNo = "";
//	        String prevETM = common.getDBResultStr(session, "SELECT d.device_id from waybill_details wd "
//	                + " RIGHT JOIN device d ON d.device_id = wd.ETM_No "
//	                + " INNER JOIN form_four ff ON ff.form_four_id = wd.Schedule_No "
//	                + " INNER JOIN schedule s ON s.schedule_id = ff.schedule_number_id "
//	                + " where s.schedule_id ='" + scheduleNo + "' AND wd.status!='INACTIVE' AND "
//	                + " DATE_FORMAT(DATE_SUB('" + waybillDate + "',INTERVAL 1 DAY),'%Y-%m-%d') order by wd.waybil_Id DESC limit 1", "device_id");
//
//	        String sqlForBagsFromMapping = " SELECT sm.Etm1_no as etmCode1,sm.Etm2_no as etmCode2 FROM ScheduleMapping sm "
//	                + " WHERE schedule_no = '" + scheduleNo + "' AND sm.status = 'ACTIVE' ";
//
//	        List<Map<String, String>> list = session.createSQLQuery(sqlForBagsFromMapping).addScalar("etmCode1", Hibernate.STRING)
//	                .addScalar("etmCode2", Hibernate.STRING).setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE).list();
//
//	        String fixedDate = common.getDBResultStr(session, "SELECT sys_value date FROM local_system_variable WHERE sys_key='NIGH_OUT_BAG_FROM' AND status='Y'", "date");
//	        int dateDiff = common.differenceOfDates(common.getDateFromDatePicker(common.changeDataFormat(fixedDate, "yyyy-MM-dd", "dd-MM-yyyy")), common.getDateFromDatePicker(common.changeDataFormat(waybillDate, "yyyy-MM-dd", "dd-MM-yyyy")));
//	        if (list.size() > 0) {
//	            Map<String, String> rs = list.get(0);
//	            if (dateDiff % 2 == 0) {
//	                etmNo = rs.get("etmCode1");
//	            } else {
//	                etmNo = rs.get("etmCode2");
//	            }
//	        }
//
//	        return etmNo;
//
//	    }
	
	@SuppressWarnings("unchecked")
	public String returnETMNoFromMapping(Common common, Session session, String scheduleNo, String waybillDate) throws Exception {

	    String etmNo = "";

	    // 1. Get previous ETM
	    String prevETMSql = "SELECT d.device_id FROM waybill_details wd " +
	            "RIGHT JOIN device d ON d.device_id = wd.ETM_No " +
	            "INNER JOIN form_four ff ON ff.form_four_id = wd.Schedule_No " +
	            "INNER JOIN schedule s ON s.schedule_id = ff.schedule_number_id " +
	            "WHERE s.schedule_id = :scheduleNo AND wd.status != 'INACTIVE' " +
	            "ORDER BY wd.waybil_Id DESC LIMIT 1";

	    List<Object> prevEtmList = session.createNativeQuery(prevETMSql)
	            .setParameter("scheduleNo", scheduleNo)
	            .list();

	    if (!prevEtmList.isEmpty()) {
	        etmNo = prevEtmList.get(0).toString();
	    }

	    // 2. Get ETM mappings
	    String mappingSql = "SELECT sm.Etm1_no, sm.Etm2_no FROM ScheduleMapping sm " +
	            "WHERE sm.schedule_no = :scheduleNo AND sm.status = 'ACTIVE'";

	    List<Object[]> mappingList = session.createNativeQuery(mappingSql)
	            .setParameter("scheduleNo", scheduleNo)
	            .list();

	    // 3. Get fixed system date
	    String sysDateSql = "SELECT sys_value FROM local_system_variable WHERE sys_key = 'NIGH_OUT_BAG_FROM' AND status = 'Y'";
	    List<String> sysDateList = session.createNativeQuery(sysDateSql).list();
	    String fixedDate = sysDateList.isEmpty() ? null : sysDateList.get(0);

	    if (fixedDate != null && !mappingList.isEmpty()) {
	        Object[] etmRow = mappingList.get(0);
	        String etmCode1 = etmRow[0] != null ? etmRow[0].toString() : "";
	        String etmCode2 = etmRow[1] != null ? etmRow[1].toString() : "";

	        int dateDiff = common.differenceOfDates(
	                common.getDateFromDatePicker(common.changeDataFormat(fixedDate, "yyyy-MM-dd", "dd-MM-yyyy")),
	                common.getDateFromDatePicker(common.changeDataFormat(waybillDate, "yyyy-MM-dd", "dd-MM-yyyy"))
	        );

	        etmNo = (dateDiff % 2 == 0) ? etmCode1 : etmCode2;
	    }

	    return etmNo;
	}
	
	@SuppressWarnings("unchecked")
	public String returnBagNoFromMapping(Common common, Session session, String scheduleNo, String waybillDate) throws Exception {
	    String bagNo = "";

	    // 1. Get bag mappings from ScheduleMapping table
	    String sqlForBagsFromMapping = "SELECT sm.bag1_no, sm.bag2_no FROM ScheduleMapping sm " +
	            "WHERE sm.schedule_no = :scheduleNo AND sm.status = 'ACTIVE'";

	    List<Object[]> list = session.createNativeQuery(sqlForBagsFromMapping)
	            .setParameter("scheduleNo", scheduleNo)
	            .list();

	    if (list.isEmpty()) {
	        return ""; // No mapping found
	    }

	    String bagCode1 = list.get(0)[0] != null ? list.get(0)[0].toString() : "";
	    String bagCode2 = list.get(0)[1] != null ? list.get(0)[1].toString() : "";

	    // 2. Get fixed config date
	    String fixedDate = common.getDBResultStr(session,
	            "SELECT sys_value FROM local_system_variable WHERE sys_key='NIGH_OUT_BAG_FROM' AND status='Y'",
	            "sys_value");

	    int dateDiff = common.differenceOfDates(
	            common.getDateFromDatePicker(common.changeDataFormat(fixedDate, "yyyy-MM-dd", "dd-MM-yyyy")),
	            common.getDateFromDatePicker(common.changeDataFormat(waybillDate, "yyyy-MM-dd", "dd-MM-yyyy"))
	    );

	    // 3. Try to fetch Bag_No from previous week
	    String prevBagNoSql = "SELECT wd.Bag_No FROM waybill_details wd " +
	            "WHERE wd.generated_Date = DATE_SUB(:waybillDate, INTERVAL 7 DAY) " +
	            "AND wd.status != 'INACTIVE' " +
	            "AND wd.schedule_No IN (SELECT ff.form_four_id FROM form_four ff " +
	            "INNER JOIN schedule s ON s.schedule_id = ff.schedule_number_id " +
	            "WHERE s.schedule_id = :scheduleNo)";

	    List<String> prevBagList = session.createNativeQuery(prevBagNoSql)
	            .setParameter("waybillDate", waybillDate)
	            .setParameter("scheduleNo", scheduleNo)
	            .list();

	    String prevBagNo = prevBagList.isEmpty() ? "" : prevBagList.get(0);

	    // 4. If not found, fallback to previous day
	    if (prevBagNo == null || prevBagNo.trim().isEmpty()) {
	        String fallbackBagNoSql = "SELECT wd.Bag_No FROM waybill_details wd " +
	                "WHERE wd.generated_Date = DATE_SUB(:waybillDate, INTERVAL 1 DAY) " +
	                "AND wd.status != 'INACTIVE' " +
	                "AND wd.schedule_No IN (SELECT ff.form_four_id FROM form_four ff " +
	                "INNER JOIN schedule s ON s.schedule_id = ff.schedule_number_id " +
	                "WHERE s.schedule_id = :scheduleNo)";

	        List<String> fallbackList = session.createNativeQuery(fallbackBagNoSql)
	                .setParameter("waybillDate", waybillDate)
	                .setParameter("scheduleNo", scheduleNo)
	                .list();

	        prevBagNo = fallbackList.isEmpty() ? "" : fallbackList.get(0);
	    }

	    // 5. Pick alternate bag based on previous bag number
	    if (prevBagNo != null && prevBagNo.equals(bagCode1)) {
	        bagNo = bagCode2;
	    } else {
	        bagNo = bagCode1;
	    }

	    return bagNo;
	}



//	@SuppressWarnings("unchecked")
//	    public String returnBagNoFromMapping(Common common, Session session, String scheduleNo, String waybillDate) throws Exception {
//
//	        String bagNo = "";
//	        String prevBag = common.getDBResultStr(session, "SELECT tm.ticketbag_id from waybill_details wd "
//	                + " RIGHT JOIN ticketbag_master tm ON tm.ticketbag_id = wd.Bag_No "
//	                + " INNER JOIN form_four ff ON ff.form_four_id = wd.Schedule_No "
//	                + " INNER JOIN schedule s ON s.schedule_id = ff.schedule_number_id "
//	                + " where s.schedule_id ='" + scheduleNo + "' AND wd.status!='INACTIVE' AND "
//	                + " DATE_FORMAT(DATE_SUB('" + waybillDate + "',INTERVAL 1 DAY),'%Y-%m-%d') order by wd.waybil_Id DESC limit 1", "ticketbag_id");
//
//	        String sqlForBagsFromMapping = " SELECT sm.bag1_no as bagCode1,sm.bag2_no as bagCode2 FROM ScheduleMapping sm "
//	                + " WHERE schedule_no = '" + scheduleNo + "' AND sm.status = 'ACTIVE' ";
//
//	        List<Map<String, String>> list = session.createSQLQuery(sqlForBagsFromMapping).addScalar("bagCode1", Hibernate.STRING)
//	                .addScalar("bagCode2", Hibernate.STRING).setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE).list();
//
//	        String fixedDate = common.getDBResultStr(session, "SELECT sys_value date FROM local_system_variable WHERE sys_key='NIGH_OUT_BAG_FROM' AND status='Y'", "date");
//	        int dateDiff = common.differenceOfDates(common.getDateFromDatePicker(common.changeDataFormat(fixedDate, "yyyy-MM-dd", "dd-MM-yyyy")), common.getDateFromDatePicker(common.changeDataFormat(waybillDate, "yyyy-MM-dd", "dd-MM-yyyy")));
//	        String prevBagNo="";
//	        prevBagNo = common.getDBResultStr(session, "SELECT Bag_No FROM waybill_details wd WHERE wd.generated_Date = DATE_SUB('"+waybillDate+"',INTERVAL 7 DAY) AND status!='INACTIVE' AND schedule_No IN (SELECT form_four_id FROM form_four ff INNER JOIN schedule s ON s.schedule_id = ff.schedule_number_id WHERE s.schedule_id='"+scheduleNo+"')", "Bag_No");
//
//	        if(prevBagNo==null || prevBagNo.equals("")){
//	        prevBagNo = common.getDBResultStr(session, "SELECT Bag_No FROM waybill_details wd WHERE wd.generated_Date = DATE_SUB('"+waybillDate+"',INTERVAL 1 DAY) AND status!='INACTIVE' AND schedule_No IN (SELECT form_four_id FROM form_four ff INNER JOIN schedule s ON s.schedule_id = ff.schedule_number_id WHERE s.schedule_id='"+scheduleNo+"')", "Bag_No");
//	        }
//	        
//	        Map<String, String> rs = list.get(0);
//	        if(prevBagNo.equals(rs.get("bagCode1")))
//	        	bagNo = rs.get("bagCode2");
//	        else
//	        	bagNo = rs.get("bagCode1");
//	        	
//	        return bagNo;
//
//	    }

	public String getTodaysDate(Date date) {

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		String formattedDate = formatter.format(date);
	        return formattedDate;
	    }

	public boolean isVehiclScrap(Common common, Session session,String vehicleid){
    	boolean isValid = false;
    	int count = common.getDBResultInt(session,"SELECT COUNT(*) count FROM vehicle WHERE cause_status='S' AND vehicle_id='"+vehicleid+"'","count");
    	if(count==0){
    		isValid = true;
    	}
    	return isValid;
    	
    }

	public boolean isEmloyeeActive(Common common, Session session,String employeeId){
	    	boolean isValid = false;
	    	int count = common.getDBResultInt(session,"SELECT COUNT(*) count FROM employee WHERE status='ACTIVE' AND EMPLOYEE_ID='"+employeeId+"'","count");
	    	if(count>0){
	    		isValid = true;
	    	}
	    	return isValid;
	    	
	    }

	public String getscheduleType(Common common, Session session, String dutyRotaId) {

        String queryForScheduleType = " select 	st.schedule_type_name from duty_rota_block_master dm  "
                + " inner join schedule_type st on dm.schedule_type_id = st.schedule_type_id "
                + " where dm.duty_rota_block_id='" + dutyRotaId + "'";
        String sheduleTypeName = null;
        try {
            sheduleTypeName = common.getDBResultStr(session, queryForScheduleType, "schedule_type_name");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sheduleTypeName;
    }

	public String getDayFromdate(Date date) {

	        SimpleDateFormat formatter12 = new SimpleDateFormat("EEEE");
	        String dayOfWeek = formatter12.format(date).toLowerCase();

	        return dayOfWeek;
	    }
}

//    @Override
//    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        // Get depot_id from the request
//        String depotIdParam = request.getParameter("depot_id");
//        System.out.println("Received depot_id: " + depotIdParam);
//
//
//        if (depotIdParam != null) {
//            try {
//                int depotId = Integer.parseInt(depotIdParam);
//                // Call the waybill creation logic
//                boolean success = createWayBillForDepot(depotId);
//
//                // Respond with a message (this could be a JSON object)
//                response.setContentType("application/json");
//                if (success) {
//                    response.getWriter().write("{\"status\":\"success\", \"message\":\"Waybill created successfully.\"}");
//                } else {
//                    response.getWriter().write("{\"status\":\"fail\", \"message\":\"Failed to create waybill.\"}");
//                }
//            } catch (NumberFormatException e) {
//                response.getWriter().write("{\"status\":\"fail\", \"message\":\"Invalid depot ID.\"}");
//            }
//        } else {
//            response.getWriter().write("{\"status\":\"fail\", \"message\":\"Depot ID is required.\"}");
//        }
//    }
//
//	private boolean createWayBillForDepot(int depotId) {
//		// TODO Auto-generated method stub
//		return false;
//	}
//}

