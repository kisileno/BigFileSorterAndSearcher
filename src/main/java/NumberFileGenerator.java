import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;

class NumberFileGenerator {


    public static final int FLUSH_EVERY = 64  * 1024;
//    public static final long TARGET_SIZE = 1L * 1024 * 1024 * 1024;
            public static final long TARGET_SIZE = 1L * 1024 * 1024;
    public static final String GENERATED_NUMBERS_TXT_FILE_NAME = "generated-numbers.txt";

    public static void main(final String[] args) throws IOException {
        generateFile(TARGET_SIZE);
    }

    public static long generateFile(long targetSize) throws IOException {
        Random random = new Random();

        FileWriter fw = new FileWriter(GENERATED_NUMBERS_TXT_FILE_NAME);
        BufferedWriter bw = new BufferedWriter(fw);

        long bytesWritten = 0;
        long linesWritten = 0;
        while (bytesWritten < targetSize) {
            String str = String.valueOf(UUID.randomUUID().toString());
            bw.write(str);
            bytesWritten += str.length();
            bw.write("\n");
            bytesWritten += 1;
            linesWritten += 1;

            if (bytesWritten % (str.length() * FLUSH_EVERY) == 0) {
                bw.flush();
                System.out.write(("\rBytes written " + bytesWritten + " / " + targetSize +
                        " Left: " + (targetSize - bytesWritten)).getBytes());
            }
        }
        System.out.println("\nLines written: " + linesWritten);

        bw.close();

        fw.close();

        return linesWritten;
    }


}