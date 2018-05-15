import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;

class PasswordFileGenerator {


    public static final int FLUSH_EVERY = 64  * 1024;
    public static final long TARGET_SIZE = 1L * 1024 * 1024 * 1024;
    //        public static final long TARGET_SIZE = 10L * 1024 * 1024;
    public static final int TEST_EVERY = 1000;
    public static final String GENERETED_PASSWORDS_TXT_FILE_NAME = "genereted-passwords.txt";
    public static final String GENERETED_PASSWORDS_TESTFILE_NAME = "genereted-passwords_test";

    public static void main(final String[] args) throws IOException {
        Random random = new Random();

        FileWriter fw = new FileWriter(GENERETED_PASSWORDS_TXT_FILE_NAME);
        FileWriter testFw = new FileWriter(GENERETED_PASSWORDS_TESTFILE_NAME);
        BufferedWriter bw = new BufferedWriter(fw);
        BufferedWriter testBw = new BufferedWriter(testFw);

        long bytesWritten = 0;
        long linesWritten = 0;
        while (bytesWritten < TARGET_SIZE) {
            String str = UUID.randomUUID().toString();
            bw.write(str);
            bytesWritten += str.length();
            bw.write("\n");
            bytesWritten += 1;
            linesWritten += 1;

            if (bytesWritten % (str.length() * FLUSH_EVERY) == 0) {
                bw.flush();
                System.out.write(("\rBytes written " + bytesWritten + " / " + TARGET_SIZE +
                        " Left: " + (TARGET_SIZE - bytesWritten)).getBytes());
            }
            int randomTestLineOffset = random.nextInt(TEST_EVERY);
            if (linesWritten == 1 || linesWritten % (TEST_EVERY + randomTestLineOffset) == 0) {
                testBw.write(str + "," + linesWritten + "\n");
                testBw.flush();
            }
        }
        System.out.println("\nLines written: " + linesWritten);

        bw.close();
        testBw.close();

        fw.close();
        testFw.close();
    }


}