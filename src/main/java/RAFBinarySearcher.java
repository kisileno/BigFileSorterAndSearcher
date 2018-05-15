import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class RAFBinarySearcher {

    public static final long TEST_SIZE_IN_BYTES = 1L * 1024 * 1024 * 1024;
    public static final String SORTED_STRINGS_TXT = "sorted-strings.txt";


    public static void main(String[] args) throws IOException {
        long genStart = System.currentTimeMillis();
        List<String> testData = generateOrderedByHashStringFile(SORTED_STRINGS_TXT);
        long genEnd = System.currentTimeMillis();
        System.out.println("Generation done in: " + (genEnd - genStart));

        RandomAccessFile raf = new RandomAccessFile(SORTED_STRINGS_TXT, "r");
        Collections.shuffle(testData);
        boolean testOk = true;
        int percent = 0;
        long testStart = System.currentTimeMillis();
        int testSize = testData.size() / 10000;
        for (int i = 0; i < testSize; i++) {
            if (!isPresent(testData.get(i), raf, Comparator.comparingInt(String::hashCode))) {
                testOk = false;
                break;
            }
            if (i % (testSize / 100) == 0) {
                System.out.write(String.format("\rTest done: %3.0f%%", (double) percent++).getBytes());
            }
        }
        long testEnd = System.currentTimeMillis();
        if (testOk) {
            System.out.println("\nTest Ok!");
            long time = testEnd - testStart;
            System.out.println("Done in: " + time + " speed: " + (time * 1.0 / testSize) + " ms/string");
        } else {
            System.out.println("Test failed!!!!!!1");
        }
        raf.close();
    }

    public static boolean isPresent(String stringToSearch, RandomAccessFile raf, Comparator<String> comparator) throws IOException {
        long lower = 0;
        long upper = raf.length();
        boolean found = false;
        while (lower + 1 < upper) {
            long mid = (lower + upper) / 2;
            byte[] lineAsBytesAt = getLineAsBytesAt(raf, mid);
            if (lineAsBytesAt.length == 0) {
                break;
            } else {
                String cur = new String(lineAsBytesAt);
                int comp = comparator.compare(cur, stringToSearch);
                if (comp == 0) {
                    found = findInDirection(stringToSearch, raf, comparator, mid, -1L) || findInDirection(stringToSearch, raf, comparator, mid, +1L);


                    break;
                } else if (comp < 0) {
                    lower = mid;
                } else {
                    upper = mid;
                }
            }
        }
        return found;
    }

    private static boolean findInDirection(String stringToSearch, RandomAccessFile raf, Comparator<String> comparator, long offset, long direction) throws IOException {
        long multiplier = 1;
        byte[] lineAsBytesAt = getLineAsBytesAt(raf, offset + direction * multiplier);
        String cur = new String(lineAsBytesAt);
        int comp = comparator.compare(stringToSearch, cur);
        while (comp == 0) {
            boolean equals = cur.equals(stringToSearch);
            if (equals) {
                return true;
            } else {
                cur = new String(getLineAsBytesAt(raf, offset + (direction * ++multiplier)));
                comp = comparator.compare(stringToSearch, cur);
            }
        }
        return false;
    }

    private static byte[] getLineAsBytesAt(RandomAccessFile raf, long at) throws IOException {
        long offset = 0;
        raf.seek(at + offset);
        try {
            byte charAtOffset = raf.readByte();
            if (charAtOffset == '\n') {
                offset--;
            }
            do {
                raf.seek(at + offset);
                charAtOffset = raf.readByte();
            }
            while (charAtOffset != '\n' && (at + --offset) >= 0);

        } catch (EOFException eof) {
        }
        raf.seek(at + offset + 1);
        ArrayList<Byte> currentByteList = new ArrayList<>();
        try {
            byte cur = raf.readByte();
            while (cur != '\n') {
                currentByteList.add(cur);
                cur = raf.readByte();
            }
        } catch (EOFException eof) {
        }
        byte[] lineAsBytes = new byte[currentByteList.size()];
        for (int i = 0; i < lineAsBytes.length; i++) {
            lineAsBytes[i] = currentByteList.get(i);
        }
        return lineAsBytes;
    }

    private static List<String> generateOrderedByHashStringFile(String fileName) throws IOException {
        FileWriter fw = new FileWriter(fileName);
        BufferedWriter bw = new BufferedWriter(fw);

        Random random = new Random();
        List<String> strings = new ArrayList<>();
        long bytesWritten = 0;
        while (bytesWritten < TEST_SIZE_IN_BYTES) {
            String s = UUID.randomUUID().toString() + random.nextInt();
            strings.add(s);
            bytesWritten += s.length() + 1;
        }
        strings.sort(Comparator.comparingInt(String::hashCode));
        for (String s : strings) {
            bw.write(s);
            bw.write('\n');
        }
        bw.close();
        fw.close();
        return strings;
    }

}