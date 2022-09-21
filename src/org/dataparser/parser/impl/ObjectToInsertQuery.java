package org.dataparser.parser.impl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.ValidationException;

import org.dataparser.msg.MsgCode;
import org.dataparser.parser.ObjectToInsertQueryInterface;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.Gson;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ObjectToInsertQuery implements ObjectToInsertQueryInterface {
	/**
	 * Initial Values
	 */
	private String writeFilePath;
	private String tableName = MsgCode.MSG_CODE_STRING_BLANK;
	@Builder.Default private int bulkInsertCnt = 100;
	@Builder.Default private boolean isWriteFile = true;
	@Builder.Default private boolean isOpenFile = false;
	@Builder.Default private boolean isGetString = false;
	@Builder.Default private boolean isBulkInsert = true;
	@Builder.Default private Map<String, String> addtionalFieldMap = new HashMap<>();
	
	/**
	 * Valid private values
	 * @throws NullPointerException
	 * @throws ValidationException
	 */
	private void validRequiredValues() throws NullPointerException, ValidationException {
		if(this.tableName.length() < 1)
			throw new NullPointerException("A required value has an exception : tableName must be set.");
		
		if(this.isWriteFile && (this.writeFilePath == null || this.writeFilePath.length() < 1))
			throw new NullPointerException("A required value has an exception : writeFilePath must be set if you want to write a file.");

		if(!this.isWriteFile && !this.isGetString)
			throw new ValidationException("A required value has an exception : Either isWriteFile or isGetString must be true.");
		
		if(!this.isWriteFile && this.isOpenFile)
			throw new ValidationException("A required value has an exception : isOpenFile must be false if isWriteFile is true.");		
	}

	/**
	 * Add additional field to Map
	 * @param key
	 * @param value
	 */
	public void addAddtionalFieldMap(String key, String value) {
		addtionalFieldMap.put(key, value);
	}
	
	/**
	 * Write bulk insert query by using list object (VO, DTO, ...)
	 * @param listObj
	 * @return
	 * @throws Exception
	 */
	public List<String> getQuery(List<Object> listObj) throws Exception {
		this.validRequiredValues();
		List<String> resultList = this.writeBulkInsertQuery(listObj);
		return resultList;
	}
	
	/**
	 * Put each JSON data to object (VO, DTO, ...) and write bulk insert query
	 * @param obj
	 * @param jsonArray
	 * @return
	 * @throws Exception
	 */
	public List<String> getQuery(Object obj, JSONArray jsonArray) throws Exception {
		this.validRequiredValues();
		List<String> resultList = this.writeBulkInsertQuery(obj, jsonArray);
		return resultList;
	}
	
	/**
	 * Write bulk insert query by using list object (VO, DTO, ...)
	 * @param listObj
	 * @return
	 * @throws Exception
	 */
	private List<String> writeBulkInsertQuery(List<Object> listObj) throws Exception {
		List<String> bulkInsertQueryList = new ArrayList<>();
		StringBuilder bulkInsertQuery = new StringBuilder("INSERT INTO " + this.tableName.trim() + "(" + this.getAllFields(listObj.get(0)).toString() + ") VALUES\r\n");

		for(int index = 0; index < listObj.size(); index++) {

			bulkInsertQuery.append(this.getAllValues(listObj.get(index)));
		
			if (index > 0 && (index + 1) % this.bulkInsertCnt == 0) {
				bulkInsertQueryList.add(bulkInsertQuery.toString());
				bulkInsertQuery = new StringBuilder("INSERT INTO " + this.tableName + "(" + this.getAllFields(listObj.get(0)).toString() + ") VALUES\r\n");
			} else {
				if (index != listObj.size() - 1)
					bulkInsertQuery.append(", ");
				else{
					bulkInsertQueryList.add(bulkInsertQuery.toString());
				}
			}
		}
		
		return bulkInsertQueryList;
	}
	
	/**
	 * Put each JSON data to obj (VO, DTO, ...) and write bulk insert query
	 * @param obj
	 * @param jsonArray
	 * @return
	 * @throws Exception
	 */
	private List<String> writeBulkInsertQuery(Object obj, JSONArray jsonArray) throws Exception {
		List<String> bulkInsertQueryList = new ArrayList<>();
		StringBuilder bulkInsertQuery = new StringBuilder("INSERT INTO " + this.tableName + "(" + this.getAllFields(obj).toString() + ") VALUES\r\n");

		for (int index = 0; index < jsonArray.length(); index++) {
			JSONObject json = jsonArray.getJSONObject(index);

			// Put additional key/value to JSONObject
			if(addtionalFieldMap != null) {
				for(Object key : addtionalFieldMap.keySet()) {
					json.put(key.toString(), addtionalFieldMap.get(key).toString());
				}
			}
			
			// JSONObject to Object (VO, DTO, ...)
			Gson gson = new Gson();
			obj = gson.fromJson(json.toString(), obj.getClass());
			bulkInsertQuery.append(this.getAllValues(obj));
			
			// Write bulk insert query
			if (index > 0 && (index+1) % this.bulkInsertCnt == 0) {
				bulkInsertQueryList.add(bulkInsertQuery.toString());
				bulkInsertQuery = new StringBuilder("INSERT INTO " + this.tableName + "(" + this.getAllFields(obj).toString() + ") VALUES\r\n");
			} else {
				if (index != jsonArray.length() - 1)
					bulkInsertQuery.append(", ");
				else{
					bulkInsertQueryList.add(bulkInsertQuery.toString());
				}
			}
		}

		return bulkInsertQueryList;
	}
	
	/**
	 * Get the values ​​of variables in object
	 * @param obj
	 * @return
	 */
	private String getAllValues(Object obj) {
    	String toString = "(";

    	int length = obj.getClass().getDeclaredFields().length;
		for(int index = 0; index < length; index++) {	
			Field field = obj.getClass().getDeclaredFields()[index];
			field.setAccessible(true);
			Object value;
			try {
				value = field.get(obj);
				if(index != length - 1)
					toString += value == null ? value + "," : "'" + value.toString().replace("'", "").replace("',","") + "',";
				else
					toString += value == null ? value 		: "'" + value.toString().replace("'", "").replace("'","") + "'";
				
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		
		toString += ")";
		return toString;
    }
	
	/**
	 * Get field name ​​of variables in object
	 * @param obj
	 * @return
	 */
	private List<String> getAllFields(Object obj) {
		List<String> fieldList = new ArrayList<>();
		
		int length = obj.getClass().getDeclaredFields().length;
		for(int index = 0; index < length; index++) {	
			Field field = obj.getClass().getDeclaredFields()[index];
			field.setAccessible(true);
			fieldList.add(field.getName());
		}
		
		return fieldList;
	}
}
