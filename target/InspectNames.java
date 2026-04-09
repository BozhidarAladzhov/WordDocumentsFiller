import org.apache.poi.ss.usermodel.*;
import java.io.*;
public class InspectNames {
  public static void main(String[] args) throws Exception {
    try (InputStream in = new FileInputStream(args[0]); Workbook wb = WorkbookFactory.create(in)) {
      for (Name n : wb.getAllNames()) {
        System.out.println(n.getNameName() + " => " + n.getRefersToFormula());
      }
    }
  }
}
