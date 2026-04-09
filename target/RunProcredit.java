import com.example.WordDocumentsFiller.service.*;
import org.springframework.mock.web.MockMultipartFile;
import java.io.*;

public class RunProcredit {
  public static void main(String[] args) throws Exception {
    ProcreditService svc = new ProcreditService();
    byte[] input = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(args[0]));
    MockMultipartFile file = new MockMultipartFile("file", "test Procredit.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", input);
    ProcreditService.GeneratedWorkbook wb = svc.generatePayments(file);
    java.nio.file.Files.write(java.nio.file.Path.of(args[1]), wb.content());
    System.out.println(wb.fileName());
    System.out.println(wb.content().length);
  }
}
