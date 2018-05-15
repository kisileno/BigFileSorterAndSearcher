import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class FileSorterExternalX3ByteArrays {


    public static final long TARGET_SIZE = 1L * 1024 * 1024 * 1024;
    public static final long TARGET_SEGMENT_SIZE = 150L * 1024 * 1024;

    public static final int FIS_BUFFER_SIZE = 16 * 1024;
    public static final int FOS_BUFFER_SIZE = 16 * 1024;

    public static final Comparator<String> STRING_INTEGER_COMPARATOR = Comparator.comparingInt(Integer::parseInt);
    public static final Comparator<String> STRING_HASHCODE_COMPARATOR = Comparator.comparingInt(String::hashCode);
    public static final Comparator<HashedByteArray> BYTE_ARRAY_HASHCODE_COMPARATOR = Comparator.comparingInt(HashedByteArray::hashCode);
    public static final String SORTED_NUMBER_FILE_NUME = "sorted-number.txt";
    public static final Charset ASCII_CHARSET = Charset.forName("ASCII");
    public static final byte SLASH_EN_BYTE = System.lineSeparator().getBytes()[0];

    public static void main(String[] args) throws IOException, InterruptedException {

//        long linesInTest = NumberFileGenerator.generateFile(TARGET_SIZE);

        long linesInTest = 97768175;

        long totalStart = System.currentTimeMillis();

        FileInputStream fis = new FileInputStream(NumberFileGenerator.GENERATED_NUMBERS_TXT_FILE_NAME);

        List<String> sortedSegmentFileNames = new ArrayList<>();
        List<HashedByteArray> segmentBuffer = new ArrayList<>();
        System.out.println("INITI ARRAY");
        Thread.sleep(30000);
        byte[] byteBuffer = new byte[FIS_BUFFER_SIZE];
        System.out.println("INIT");
        Thread.sleep(60000);
        int left = readSegment(fis, segmentBuffer, byteBuffer, 0, TARGET_SEGMENT_SIZE);
        System.out.println("FIRST READ");
        Thread.sleep(60000);
        while (!segmentBuffer.isEmpty()) {
            int segmentNumber = sortedSegmentFileNames.size();
            long sortStart = System.currentTimeMillis();
            segmentBuffer.sort(BYTE_ARRAY_HASHCODE_COMPARATOR);
            long sortEnd = System.currentTimeMillis();
            System.out.println("Segment " + segmentNumber + " sorted in: " + (sortEnd - sortStart));

            String segmentFileName = "/tmp/" + NumberFileGenerator.GENERATED_NUMBERS_TXT_FILE_NAME + ".seg." + segmentNumber;
//            String segmentFileName = NumberFileGenerator.GENERATED_NUMBERS_TXT_FILE_NAME + ".seg." + segmentNumber;
            sortedSegmentFileNames.add(segmentFileName);

            long writeStart = System.currentTimeMillis();
            writeSegment(segmentBuffer, segmentFileName, FOS_BUFFER_SIZE);
            segmentBuffer.clear();
            long writeEnd = System.currentTimeMillis();
            System.out.println("Written to " + segmentFileName + " in " + (writeEnd - writeStart));

            long readStart = System.currentTimeMillis();
            left = readSegment(fis, segmentBuffer, byteBuffer, left, TARGET_SEGMENT_SIZE);
            long readEnd = System.currentTimeMillis();
            System.out.println("Segment read: " + (segmentNumber + 1) + " in: " + (readEnd - readStart));
        }
        fis.close();


        long mergeStart = System.currentTimeMillis();
        FileWriter fw = new FileWriter(SORTED_NUMBER_FILE_NUME);
        BufferedWriter sortedFileWriter = new BufferedWriter(fw);

        List<SegmentReader> readers = new ArrayList<>(sortedSegmentFileNames.size());
        for (String segmentName : sortedSegmentFileNames) {
            readers.add(new SegmentReader(segmentName));
        }
        long currentBufferSize = 0;
        int buffersWritten = 0;
        List<String> outputBuffer = new ArrayList<>();
        List<HashedByteArray> peekBuffer = new ArrayList<>(readers.size());

        for (SegmentReader reader : readers) {
            peekBuffer.add(HashedByteArray.fromString(reader.peek()));
        }
        while (true) {
            int minElemIndex = getIndexOfMin(peekBuffer, BYTE_ARRAY_HASHCODE_COMPARATOR);
            if (minElemIndex == -1) {
                break;
            }
            SegmentReader minElemReader = readers.get(minElemIndex);
            String curLine = minElemReader.nextLine();
            outputBuffer.add(curLine);
            currentBufferSize += curLine.length() + 1;

            String nextLine = minElemReader.peek();
            if (nextLine == null) {
                minElemReader.closeAndDelete();
                readers.remove(minElemIndex);
                peekBuffer.remove(minElemIndex);
            } else {
                peekBuffer.set(minElemIndex, HashedByteArray.fromString(nextLine));
            }
            if (currentBufferSize > TARGET_SEGMENT_SIZE) {
                writeSegment(outputBuffer, sortedFileWriter);
                outputBuffer.clear();
                currentBufferSize = 0;
                buffersWritten++;
                double percent = buffersWritten * 100.0 / (TARGET_SIZE / TARGET_SEGMENT_SIZE);
                System.out.write((String.format("\rMerged %3.2f%%", percent).getBytes()));
            }
        }

        writeSegment(outputBuffer, sortedFileWriter);
        long mergeEnd = System.currentTimeMillis();
        System.out.println("\nMerge done in: " + (mergeEnd - mergeStart));

        long totalEnd = System.currentTimeMillis();
        System.out.println("Total time: " + (totalEnd - totalStart));

        sortedFileWriter.close();
        fw.close();


//        numbersTest(SORTED_NUMBER_FILE_NUME, STRING_HASHCODE_COMPARATOR);
        Comparator<String> bytesHashCodeComp = (o1, o2) -> BYTE_ARRAY_HASHCODE_COMPARATOR.compare(HashedByteArray.fromString(o1), HashedByteArray.fromString(o2));
        bytesArrayTest(NumberFileGenerator.GENERATED_NUMBERS_TXT_FILE_NAME, SORTED_NUMBER_FILE_NUME, bytesHashCodeComp, linesInTest);


    }

    public static void main2(String[] args) throws IOException {
        Comparator<String> bytesHashCodeComp = (o1, o2) -> BYTE_ARRAY_HASHCODE_COMPARATOR.compare(HashedByteArray.fromString(o1), HashedByteArray.fromString(o2));

        bytesArrayTest(NumberFileGenerator.GENERATED_NUMBERS_TXT_FILE_NAME, SORTED_NUMBER_FILE_NUME, bytesHashCodeComp, 101);

    }

    static int readSegment(
            FileInputStream fis,
            List<HashedByteArray> result,
            byte[] byteBuffer,
            int offsetInBuffer,
            long targetSegmentSize) throws IOException {

        long currentSegmentSize = 0;
        int bytesRead = fis.read(byteBuffer, offsetInBuffer, byteBuffer.length - offsetInBuffer);
        while (bytesRead > 0 && currentSegmentSize < targetSegmentSize) {

            currentSegmentSize += bytesRead;
            int lineStart = 0;
            int lineEnd = offsetInBuffer;
            int end = offsetInBuffer + bytesRead;
            while (lineEnd < end) {
                if (byteBuffer[lineEnd] == SLASH_EN_BYTE) {
                    int lineLength = lineEnd - lineStart;
                    byte[] line = new byte[lineLength];
                    System.arraycopy(byteBuffer, lineStart, line, 0, lineLength);
                    result.add(new HashedByteArray(line));

                    lineStart = lineEnd + 1;
                    lineEnd = lineEnd + 2;
                } else {
                    lineEnd++;
                }
            }
            if (lineStart != end) {
                offsetInBuffer = byteBuffer.length - lineStart;
                System.arraycopy(byteBuffer, lineStart, byteBuffer, 0, offsetInBuffer);
            } else {
                offsetInBuffer = 0;
            }
            if (currentSegmentSize < targetSegmentSize) {
                bytesRead = fis.read(byteBuffer, offsetInBuffer, byteBuffer.length - offsetInBuffer);
            } else {
                break;
            }
        }
        return offsetInBuffer;
    }

    static void writeSegment(List<HashedByteArray> segment, String filename, int segmentSize) throws IOException {

        FileOutputStream fos = new FileOutputStream(filename);
        writeSegmentToFos(segment, fos, segmentSize);
        fos.flush();
        fos.close();
    }

    static void writeSegmentToFos(List<HashedByteArray> segment, FileOutputStream fos, int bufferSize) throws IOException {
        byte[] writeBuffer = new byte[bufferSize];
        int bufferOffset = 0;
        for (HashedByteArray hashedByteArray : segment) {
            byte[] arr = hashedByteArray.array;
            int finalDest = arr.length + bufferOffset + 1;
            if (finalDest == writeBuffer.length) {
                System.arraycopy(arr, 0, writeBuffer, bufferOffset, arr.length);
                writeBuffer[writeBuffer.length - 1] = SLASH_EN_BYTE;
                fos.write(writeBuffer);
                bufferOffset = 0;
            } else if (finalDest > writeBuffer.length) {
                int goesInThisWrite = writeBuffer.length - bufferOffset;
                System.arraycopy(arr, 0, writeBuffer, bufferOffset, goesInThisWrite);
                fos.write(writeBuffer);
                int leftForNextWrite = arr.length - goesInThisWrite;
                System.arraycopy(arr, goesInThisWrite, writeBuffer, 0, leftForNextWrite);
                bufferOffset = leftForNextWrite;
                writeBuffer[bufferOffset] = SLASH_EN_BYTE;
                bufferOffset++;
            } else {
                System.arraycopy(arr, 0, writeBuffer, bufferOffset, arr.length);
                bufferOffset += arr.length;
                writeBuffer[bufferOffset] = SLASH_EN_BYTE;
                bufferOffset += 1;
            }
        }
        if (bufferOffset != 0) {
            fos.write(writeBuffer, 0, bufferOffset);
        }
    }

    static long writeSegment(List<String> segment, BufferedWriter bw) throws IOException {
        long bytesWritten = 0;
        for (String s : segment) {
            bw.write(s);
            bw.write("\n");
            bytesWritten += s.length() + 1;
        }
        byte[] asd = new byte[1];
        Arrays.hashCode(asd);
        return bytesWritten;
    }


    static void numbersTest(String filename, Comparator<String> comparator) throws IOException {
        boolean success = true;
        int numberOfLines = 0;

        FileReader fr = new FileReader(filename);
        BufferedReader br = new BufferedReader(fr);

        String prevLine = br.readLine();
        numberOfLines++;
        String line = br.readLine();
        while (line != null) {
            if (comparator.compare(prevLine, line) > 0) {
                success = false;
            }
            prevLine = line;
            line = br.readLine();
            numberOfLines++;
        }
        if (success) {
            System.out.println("Numbers file test OK: " + numberOfLines);
        } else {
            System.out.println("Numbers file test FAILED!!1");
        }
    }

    static void bytesArrayTest(String originalFile, String compareToFilename, Comparator<String> comparator, long expectedlinesInTest) throws IOException {
        boolean success = true;
        int numberOfLines = 0;

        FileReader fr = new FileReader(compareToFilename);
        BufferedReader br = new BufferedReader(fr);

        String prevLine = br.readLine();
        numberOfLines++;
        String line = br.readLine();
        while (line != null) {
            if (comparator.compare(prevLine, line) > 0) {
                success = false;
            }
            prevLine = line;
            line = br.readLine();
            numberOfLines++;
        }
        if (success) {
            if (numberOfLines == expectedlinesInTest) {
                System.out.println("Numbers file test OK: " + numberOfLines);
            } else {
                System.out.println("Sorted Ok, but test failed lines number do not match: " + numberOfLines + " vs exp: " + expectedlinesInTest);
            }
            RandomAccessFile raf = new RandomAccessFile(compareToFilename, "r");
            FileReader ofr = new FileReader(originalFile);
            BufferedReader obfr = new BufferedReader(ofr);
            String lineFromOriginalFile = obfr.readLine();
            int numberOfFails = 0;
            List<String> missing = new ArrayList<>();
            int testNumber = 0;
            boolean finalTestSucc = true;
            while (lineFromOriginalFile != null) {
                testNumber++;
                if (testNumber % (numberOfLines / 10000) == 0) {
                    boolean present = RAFBinarySearcher.isPresent(lineFromOriginalFile, raf, comparator);
                    if (!present) {
                        numberOfFails++;
                        finalTestSucc = false;
                        missing.add(lineFromOriginalFile);
                    }
                }
                lineFromOriginalFile = obfr.readLine();
            }
            raf.close();
            ofr.close();
            obfr.close();
            if (finalTestSucc) {
                System.out.println("FINAL TEST FUCKING DONE!");
            } else {
                System.out.println("Final test failed. Number of missing items: " + numberOfFails);
                for (String miss : missing) {
                    System.out.println(miss);
                }
            }


        } else {
            System.out.println("Numbers file test FAILED!!1");
        }


    }

    static <T> int getIndexOfMin(List<T> list, Comparator<T> comparator) {
        int size = list.size();
        if (size == 0) {
            return -1;
        } else if (size == 1) {
            return 0;
        } else {
            T min = list.get(0);
            int minIndex = 0;
            for (int i = 1; i < size; i++) {
                T potentialMin = list.get(i);
                if (comparator.compare(min, potentialMin) > 0) {
                    min = potentialMin;
                    minIndex = i;
                }
            }
            return minIndex;
        }
    }

    static class SegmentReader {
        final String filename;
        FileReader fr;
        BufferedReader br;
        String cache;

        public SegmentReader(String filename) {
            try {
                this.filename = filename;
                fr = new FileReader(this.filename);
                br = new BufferedReader(fr);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        //this is most comlecated kod evar
        public String nextLine() throws IOException {
            String res = cache;
            cache = null;
            peek();
            return res;
        }

        public String peek() throws IOException {
            if (cache == null) {
                cache = br.readLine();
            }
            return cache;
        }

        public void closeAndDelete() throws IOException {
            br.close();
            fr.close();
            new File(filename).delete();
        }
    }

    public static class HashedByteArray {
        public final byte[] array;
        final int hashCode;

        public HashedByteArray(byte[] array) {
            this.array = array;
            hashCode = Arrays.hashCode(array);
        }

        @Deprecated
        public static HashedByteArray fromString(String s) {
            if (s == null) {
                return null;
            }
            byte[] arr = s.getBytes(ASCII_CHARSET);
            return new HashedByteArray(arr);
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            HashedByteArray that = (HashedByteArray) o;

            return Arrays.equals(array, that.array);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }


}
