package com.ybs.pullapidata.seoultrafficinfo;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.ybs.pullapidata.seoultrafficinfo.ApiConnection;
import com.ybs.pullapidata.seoultrafficinfo.DbConnection;

public class SeoulTrafficInfo 
{
	static public ApiConnection apiconnection;
	static public String BaseDate, BaseTime;
	public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException 
	{
		// 기본 설정
		long time = System.currentTimeMillis();
		SimpleDateFormat _date = new SimpleDateFormat("YYYYMMdd");
		SimpleDateFormat _time = new SimpleDateFormat("HHmmss");
		BaseDate = _date.format(new Date(time));
		BaseTime = _time.format(new Date( time));
		List<String> column = new ArrayList<String>();
		column.add("link_id");
		column.add("prcs_spd");
		column.add("prcs_trv_time");
		
		// DB 연결
		String host = "192.168.0.53";
		String name = "HVI_DB";
		String user = "root";
		String pass = "dlatl#001";
		DbConnection dbconnection = new DbConnection(host, name, user, pass);
	    dbconnection.Connect();
	    
	   // 링크ID 가져오기
	    List<String> Link_id = new ArrayList<String>();
	    String sql = "select LINK_ID from SEOUL_LINK";
	    dbconnection.runQuery(sql);
	    while(dbconnection.getResult().next())
	    {
	    	Link_id.add(dbconnection.getResult().getString("LINK_ID"));
	    }
	    // api data 받아서 csv파일 생성
	    String FileName = "SEOUL_TRAFFIC_INFO_" + BaseDate + BaseTime + ".csv";
	    BufferedWriter bufWriter = new BufferedWriter(new FileWriter(FileName));
	    CreateCSV(bufWriter);
	    List<String> result;
	    apiconnection = new ApiConnection();
	    for(String s : Link_id)
	    {
	    	apiconnection.setUrl("http://openapi.seoul.go.kr:8088/7a7466686f73756e3937547067797a/xml/TrafficInfo/1/999/" + s);
			apiconnection.pullData();
	    	System.out.println(apiconnection.urlBuilder);
	    	result = apiconnection.getResult("CODE");
	    	if(result.get(0).equalsIgnoreCase("INFO-200") != true)
	    	{
	    		try
				{
					List<List<String>> datalist = new ArrayList<List<String>>();
					
					for(String c:column)
					{
						datalist.add(apiconnection.getResult(c));
					}
					WriteCSV(bufWriter, datalist);
					}
					catch(Exception e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	    	}
	    }
	    bufWriter.close();
	    
	    // DB에 입력
	    sql = "LOAD DATA LOCAL INFILE '" + FileName + "' INTO TABLE SEOUL_TRAFFIC_INFO FIELDS TERMINATED BY ',' ENCLOSED BY '\"' LINES TERMINATED BY '\n' IGNORE 1 LINES(LINK_ID, AVG_SPEED, TRAVEL_TIME, BASE_TIME)";
	    dbconnection.LoadLocalData(sql);
	}
	
	public static void CreateCSV(BufferedWriter bufWriter)
	{
		try
		{
			bufWriter.write("\"LINK_ID\",\"AVG_SPEED\",\"TRAVEL_TIME\",\"BASE_TIME\"");
			bufWriter.newLine();
		} catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void WriteCSV(BufferedWriter bufWriter, List<List<String>> datalist) throws IOException
	{
//		System.out.println(datalist.get(0).size() + " " + datalist.get(1).size()+ " " + datalist.get(2).size()+ " " + datalist.get(3).size()+ " " + datalist.get(4).size()+ " " + datalist.get(5).size()+ " " + datalist.get(6).size());
		String buffer = "";
		for(int i = 0; i < datalist.get(0).size(); i++)
		{
			int j = 0;
			for(; j < datalist.size() - 1; j++)
			{
				if(datalist.get(j).get(i).contains("</"))
				{
					buffer += "\"" + datalist.get(j).get(i).substring(0,datalist.get(j).get(i).indexOf('<') ) + "\",";
				}
				else
				{
					buffer += "\"" + datalist.get(j).get(i) + "\",";
				}
			}
			if(datalist.get(j).get(i).contains("</"))
			{
				buffer += "\"" + datalist.get(j).get(i).substring(0,datalist.get(j).get(i).indexOf('<') );
			}
			else
			{
				buffer += "\"" + datalist.get(j).get(i);
			}
			buffer += "\",\"" + BaseTime + "\"\n";
		}
		bufWriter.write(buffer);
	}
}
