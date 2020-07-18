package com.lebinh.skeleton.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class SpreadSheetUtil {

  public static boolean checkNumberOfSheet(Workbook workbook, int allowNumberOfSheets) {
    if (workbook != null) {
      return workbook.getNumberOfSheets() == allowNumberOfSheets;
    }
    return false;
  }

  public static boolean checkSheets(Workbook workbook, String... sheetsNames) {
    boolean result = false;
    if (workbook != null) {
      result =
          Arrays.asList(sheetsNames)
              .stream()
              .anyMatch(sheetName -> workbook.getSheet(sheetName) == null);
    }

    return result;
  }

  public static String getDataInCell(Workbook workbook, String sheetName, int row, int column) {
    if (workbook != null) {
      Sheet sheet = workbook.getSheet(sheetName);
      if (sheet != null) {
        return getDataInCell(sheet, row, column);
      }
    }

    return "";
  }

  public static String getDataInCell(Sheet sheet, int rowNo, int columnNo) {
    if (sheet != null) {
      Row row = sheet.getRow(rowNo);
      if (row != null) {
        Cell cell = row.getCell(columnNo, MissingCellPolicy.CREATE_NULL_AS_BLANK);
        return cell.getStringCellValue();
      }
    }

    return "";
  }

  public static List<Map<String, String>> getDataInSheet(
      Workbook workbook,
      String sheetName,
      Map<Integer, String> dataInfo,
      int limit,
      int offset) {

    Sheet sheet = workbook.getSheet(sheetName);
    if (sheet != null) {
      return getDataInSheet(sheet, dataInfo, limit, offset);
    }

    return new ArrayList<>();
  }

  public static List<Map<String, String>> getDataInSheet(
      Sheet sheet, Map<Integer, String> dataInfo, int limit, int offset) {

    List<Map<String, String>> result = new ArrayList<>();
    // if from = 0 to = 0 -> get all
    int rowTotal = sheet.getPhysicalNumberOfRows();
    if (offset < 0 || limit < 0 || rowTotal <= offset) {
      return result;
    } else {
      for (int i = offset; i <= limit; i++) {
        final int rowNo = i;
        Map<String, String> record = new HashMap<>();
        dataInfo.forEach((k, v) -> record.put("label", getDataInCell(sheet, rowNo, k)));
        result.add(record);
      }
    }

    return result;
  }
}
