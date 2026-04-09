import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import java.io.*;
import java.util.*;

public class InspectWorkbook {
  public static void main(String[] args) throws Exception {
    try (InputStream in = new FileInputStream(args[0]); Workbook wb = WorkbookFactory.create(in)) {
      Sheet s = wb.getSheet("payments");
      DataFormatter f = new DataFormatter(Locale.US);
      System.out.println("lastRow=" + s.getLastRowNum());
      for (int r = 0; r <= Math.min(s.getLastRowNum(), 25); r++) {
        Row row = s.getRow(r);
        if (row == null) continue;
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < 14; c++) {
          Cell cell = row.getCell(c);
          if (cell == null) continue;
          String txt = f.formatCellValue(cell);
          String formula = cell.getCellType() == CellType.FORMULA ? cell.getCellFormula() : "";
          if (!txt.isBlank() || !formula.isBlank()) sb.append("C").append(c+1).append("=").append(txt).append("{F=").append(formula).append("} |");
        }
        if (sb.length() > 0) System.out.println("ROW " + (r+1) + ": " + sb);
      }
      for (int r = Math.max(0, s.getLastRowNum()-12); r <= s.getLastRowNum(); r++) {
        Row row = s.getRow(r);
        if (row == null) continue;
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < 14; c++) {
          Cell cell = row.getCell(c);
          if (cell == null) continue;
          String txt = f.formatCellValue(cell);
          String formula = cell.getCellType() == CellType.FORMULA ? cell.getCellFormula() : "";
          if (!txt.isBlank() || !formula.isBlank()) sb.append("C").append(c+1).append("=").append(txt).append("{F=").append(formula).append("} |");
        }
        if (sb.length() > 0) System.out.println("TAIL ROW " + (r+1) + ": " + sb);
      }
      System.out.println("merged=" + s.getNumMergedRegions());
      for (int i=0;i<s.getNumMergedRegions();i++) {
        CellRangeAddress a = s.getMergedRegion(i);
        if (a.getFirstRow() <= 25 || a.getLastRow() >= s.getLastRowNum()-12) System.out.println("MERGE " + a.formatAsString());
      }
    }
  }
}
