package com.example.WordDocumentsFiller.service;


import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class LibreOfficePdfConverter {

    public static void convertDocxToPdf(
            String sofficePath,
            Path docxPath,
            Path outputDir
    ) throws IOException, InterruptedException {

        Files.createDirectories(outputDir);

        Path loProfileDir = outputDir.resolve(".lo-profile");
        Files.createDirectories(loProfileDir);

        List<String> cmd = new ArrayList<>();
        cmd.add(sofficePath);
        cmd.add("--headless");
        cmd.add("--nologo");
        cmd.add("--nolockcheck");
        cmd.add("--nodefault");
        cmd.add("--nofirststartwizard");
        cmd.add("-env:UserInstallation=file:///" +
                loProfileDir.toAbsolutePath().toString().replace("\\", "/"));
        cmd.add("--convert-to");
        cmd.add("pdf");
        cmd.add("--outdir");
        cmd.add(outputDir.toAbsolutePath().toString());
        cmd.add(docxPath.toAbsolutePath().toString());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException("LibreOffice PDF conversion failed. Exit code=" + exitCode);
        }
    }

}
