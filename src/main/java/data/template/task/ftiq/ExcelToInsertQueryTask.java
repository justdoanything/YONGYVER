package data.template.task.ftiq;

import data.exception.ParseException;
import data.template.task.QueryTaskTemplate;
import data.util.ExcelUtil;
import data.util.FileUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Map;

import static data.constant.FileConstant.FILE_EXTENSION_XLS;

public class ExcelToInsertQueryTask extends QueryTaskTemplate {

    private Workbook workbook = null;

    @Override
    public void preTask(String tableName) {
        queryHeader = new StringBuilder("INSERT INTO " + tableName + " (");
        queryBody = new StringBuilder();
    }

    @Override
    public void handleTask(Map<String, Map<String, String>> codeMap, String readFilePath, int startWithLine, boolean isBulkInsert, String splitter, int bulkInsertCnt) {
        try (FileInputStream fis = new FileInputStream(readFilePath)) {
            workbook = FileUtil.getFileExtension(readFilePath).equals(FILE_EXTENSION_XLS) ? new HSSFWorkbook(fis) : new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null)
                throw new ParseException("There is no sheet in file");

            if (sheet.getRow(startWithLine - 1) == null)
                throw new ParseException("startWithLine over than the row range.");

            if (isBulkInsert)
                handleBulkTask(codeMap, sheet, bulkInsertCnt);
            else
                handleNonBulkTask(codeMap, sheet);
        } catch (Exception e) {
            throw new ParseException(e.getMessage());
        }

    }

    /**
     * IsFirst 문제 해결 필요 !!
     */
    private void handleBulkTask(Map<String, Map<String, String>> codeMap, Sheet sheet, int bulkInsertCnt) {
        boolean isFirst = true;
        Row row;
        int cellCount = -1;
        int index = 0;

        for (int rowNum = 0; rowNum < sheet.getPhysicalNumberOfRows(); rowNum++) {
            StringBuilder queryBodyLine = new StringBuilder();
            row = sheet.getRow(rowNum);
            int cellIndex = 0;

            if (isFirst) {
                while (row.getCell(cellIndex) != null) {
                    queryHeader.append(ExcelUtil.getCellValue(row.getCell(cellIndex)).replace("'", ""));
                    if (row.getCell(cellIndex + 1) != null) queryHeader.append(", ");
                    cellIndex++;
                }
                cellCount = cellIndex;
            } else {
                while (cellIndex < cellCount) {
                    queryBodyLine.append(("'" + ExcelUtil.getCellValue(row.getCell(cellIndex)).replace("'", "") + "'").replace("''", "null"));
                    if (cellIndex + 1 != cellCount) queryBodyLine.append(", ");
                    cellIndex++;
                }
            }

            if (isFirst) {
                queryHeader.append(") VALUES \r\n");
                isFirst = false;
            } else {
                if (index > 0 && (index + 1) % bulkInsertCnt == 0) {
                    queryBody.append("(").append(queryBodyLine).append(");\r\n\r\n");
                    queryBody.append(queryHeader);
                } else {
                    queryBody.append("(").append(queryBodyLine).append("),\r\n");
                }
                index++;
            }
        }

        queryBody.replace(queryBody.lastIndexOf(","), queryBody.lastIndexOf(",") + 1, ";");
    }

    private void handleNonBulkTask(Map<String, Map<String, String>> codeMap, Sheet sheet) {
        boolean isFirst = true;
        Row row;
        int cellCount = -1;

        for (int rowNum = 0; rowNum < sheet.getPhysicalNumberOfRows(); rowNum++) {
            StringBuilder queryBodyLine = new StringBuilder();
            row = sheet.getRow(rowNum);
            int cellIndex = 0;

            if (isFirst) {
                while (row.getCell(cellIndex) != null) {
                    queryHeader.append(ExcelUtil.getCellValue(row.getCell(cellIndex)).replace("'", ""));
                    if (row.getCell(cellIndex + 1) != null) queryHeader.append(", ");
                    cellIndex++;
                }
                cellCount = cellIndex;
            } else {
                while (cellIndex < cellCount) {
                    queryBodyLine.append(("'" + ExcelUtil.getCellValue(row.getCell(cellIndex)).replace("'", "") + "'").replace("''", "null"));
                    if (cellIndex + 1 != cellCount) queryBodyLine.append(", ");
                    cellIndex++;
                }
            }

            if (isFirst) {
                queryHeader.append(") VALUES ");
                isFirst = false;
            } else {
                queryBody.append(queryHeader).append("('").append(queryBodyLine).append("');\r\n");
            }
        }
    }

    @Override
    public String doTask(boolean isWriteFile, boolean isGetString, boolean isOpenFile, String writeFilePath, boolean isBulkInsert) {
        String resultString = null;
        if (isWriteFile)
            writeResultFile(writeFilePath, isOpenFile, isBulkInsert);
        if (isGetString)
            resultString = writeResultString(isBulkInsert);
        return resultString;
    }

    @Override
    protected void writeResultFile(String writeFilePath, boolean isOpenFile, boolean isBulkInsert) {
        writeFilePath = writeFilePath.replace("xlsx", "txt").replace("xls", "txt");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(writeFilePath))) {
            if (isBulkInsert) {
                bw.write(queryHeader.toString());
                bw.flush();
            }

            bw.write(queryBody.toString());
            bw.flush();

            if (isOpenFile)
                Desktop.getDesktop().edit(new File(writeFilePath));
        } catch (Exception e) {
            throw new ParseException(e.getMessage());
        }
    }

    @Override
    protected String writeResultString(boolean isBulkInsert) {
        return isBulkInsert ? queryBody.append(queryBody).toString() : queryBody.toString();
    }
}
