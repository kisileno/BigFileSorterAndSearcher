import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class FileSorterExternalX3ByteABuffer {


    public static final long TARGET_SIZE = 12L * 1024 * 1024 * 1024;
    public static final int TARGET_SEGMENT_SIZE = 275 * 1024 * 1024;

    public static final int FIS_BUFFER_SIZE = 32 * 1024;
    public static final int FOS_BUFFER_SIZE = 32 * 1024;

    public static final Comparator<String> STRING_INTEGER_COMPARATOR = Comparator.comparingInt(Integer::parseInt);
    public static final Comparator<String> STRING_HASHCODE_COMPARATOR = Comparator.comparingInt(String::hashCode);
    public static final Comparator<FileSorterExternalX3ByteArrays.HashedByteArray> BYTE_ARRAY_HASHCODE_COMPARATOR_OLD = Comparator.comparingInt(FileSorterExternalX3ByteArrays.HashedByteArray::hashCode);
    public static final String SORTED_NUMBER_FILE_NUME = "sorted-number.txt";
    public static final Charset ASCII_CHARSET = Charset.forName("ASCII");
    public static final byte SLASH_EN_BYTE = System.lineSeparator().getBytes()[0];
    public static final byte[] SLASH_AN_BYTE_ARRAY = {SLASH_EN_BYTE};

    public static void main(String[] args) throws IOException, InterruptedException {

        long linesInTest = NumberFileGenerator.generateFile(TARGET_SIZE);
//
//        long linesInTest = 97766477;

        long totalStart = System.currentTimeMillis();

        FileInputStream fis = new FileInputStream(NumberFileGenerator.GENERATED_NUMBERS_TXT_FILE_NAME);

        List<String> sortedSegmentFileNames = new ArrayList<>();
        SegmentInfo segmentInfo = new SegmentInfo(1000);
        byte[] segmentByteBuffer = new byte[TARGET_SEGMENT_SIZE + 2 * FIS_BUFFER_SIZE];
        readSegment(fis, segmentInfo, segmentByteBuffer, TARGET_SEGMENT_SIZE);
        System.out.println("FIRST READ DONE");
//        Thread.sleep(60000);

        while (segmentInfo.actualSize > 0) {
            int segmentNumber = sortedSegmentFileNames.size();
            long sortStart = System.currentTimeMillis();
            segmentInfo.sort();
            long sortEnd = System.currentTimeMillis();
            System.out.println("Segment " + segmentNumber + " sorted in: " + (sortEnd - sortStart));

            String segmentFileName = "/tmp/" + NumberFileGenerator.GENERATED_NUMBERS_TXT_FILE_NAME + ".seg." + segmentNumber;
//            String segmentFileName = NumberFileGenerator.GENERATED_NUMBERS_TXT_FILE_NAME + ".seg." + segmentNumber;
            sortedSegmentFileNames.add(segmentFileName);

            long writeStart = System.currentTimeMillis();
            writeSegment(segmentInfo, segmentFileName, segmentByteBuffer, FOS_BUFFER_SIZE);
            segmentInfo.clear();

            long writeEnd = System.currentTimeMillis();
            System.out.println("Written to " + segmentFileName + " in " + (writeEnd - writeStart));

            long readStart = System.currentTimeMillis();
            readSegment(fis, segmentInfo, segmentByteBuffer, TARGET_SEGMENT_SIZE);
            long readEnd = System.currentTimeMillis();
            System.out.println("Segment read: " + (segmentNumber + 1) + " in: " + (readEnd - readStart));
        }
        segmentByteBuffer = null;
        fis.close();
        System.out.println("MERGE START");

        long mergeStart = System.currentTimeMillis();
        FileOutputStream sortedFileOutputStream = new FileOutputStream(SORTED_NUMBER_FILE_NUME);

        List<SegmentReader> readers = new ArrayList<>(sortedSegmentFileNames.size());
        for (String segmentName : sortedSegmentFileNames) {
            readers.add(new SegmentReader(segmentName));
        }
        long commit_every_hack = TARGET_SEGMENT_SIZE / 4;
        int buffersWritten = 0;
        System.gc();
        List<Integer> peekBuffer = new ArrayList<>(readers.size());
        OutputSegment outputSegment = new OutputSegment(TARGET_SEGMENT_SIZE + FIS_BUFFER_SIZE);
        for (SegmentReader reader : readers) {
            peekBuffer.add(reader.peekHashCode());
        }
        while (true) {
            int minElemIndex = getIndexOfMin(peekBuffer, Integer::compare);
            if (minElemIndex == -1) {
                break;
            }
            SegmentReader minElemReader = readers.get(minElemIndex);
            byte[] curLine = minElemReader.nextLine();
            outputSegment.add(curLine);
            outputSegment.add(SLASH_AN_BYTE_ARRAY);

            byte[] nextLine = minElemReader.peek();
            if (nextLine == null) {
                minElemReader.closeAndDelete();
                readers.remove(minElemIndex);
                peekBuffer.remove(minElemIndex);
            } else {
                peekBuffer.set(minElemIndex, minElemReader.peekHashCode());
            }
            if (outputSegment.actualSize > commit_every_hack) {
                writeSortedSegment(outputSegment, sortedFileOutputStream);
                outputSegment.clear();
                buffersWritten++;
                double percent = buffersWritten * 100.0 / (TARGET_SIZE / commit_every_hack);
                System.out.write((String.format("\rMerged %3.2f%%", percent).getBytes()));
            }
        }

        writeSortedSegment(outputSegment, sortedFileOutputStream);
        long mergeEnd = System.currentTimeMillis();
        System.out.println("\nMerge done in: " + (mergeEnd - mergeStart));

        long totalEnd = System.currentTimeMillis();
        System.out.println("Total time: " + (totalEnd - totalStart));

        sortedFileOutputStream.close();


//        numbersTest(SORTED_NUMBER_FILE_NUME, STRING_HASHCODE_COMPARATOR);
        Comparator<String> bytesHashCodeComp = (o1, o2) -> BYTE_ARRAY_HASHCODE_COMPARATOR_OLD.compare(FileSorterExternalX3ByteArrays.HashedByteArray.fromString(o1), FileSorterExternalX3ByteArrays.HashedByteArray.fromString(o2));
        bytesArrayTest(NumberFileGenerator.GENERATED_NUMBERS_TXT_FILE_NAME, SORTED_NUMBER_FILE_NUME, bytesHashCodeComp, linesInTest);


    }

    public static void main2(String[] args) throws IOException {
        Comparator<String> bytesHashCodeComp = (o1, o2) -> BYTE_ARRAY_HASHCODE_COMPARATOR_OLD.compare(FileSorterExternalX3ByteArrays.HashedByteArray.fromString(o1), FileSorterExternalX3ByteArrays.HashedByteArray.fromString(o2));

        bytesArrayTest(NumberFileGenerator.GENERATED_NUMBERS_TXT_FILE_NAME, SORTED_NUMBER_FILE_NUME, bytesHashCodeComp, 101);

    }

    static void readSegment(
            FileInputStream fis,
            SegmentInfo result,
            byte[] segmentByteBuffer,
            int targetSegmentSize) throws IOException {

        byte[] readByteBuffer = new byte[FIS_BUFFER_SIZE];
        int segmentBufferWritePos = 0;
        int lineStartInReadBuffer = 0;
        int lastLineLength = 0;
        int bytesRead = fis.read(readByteBuffer, 0, readByteBuffer.length);
        while (segmentBufferWritePos < targetSegmentSize && bytesRead > 0) {
            for (int i = 0; i < bytesRead; i++) {
                byte pidar = readByteBuffer[i];
                segmentByteBuffer[segmentBufferWritePos++] = pidar;
                if (pidar == SLASH_EN_BYTE && lastLineLength > 0) {
                    int hashCode = Arrays.hashCode(getArray(segmentByteBuffer, lineStartInReadBuffer, lastLineLength));
                    result.add(hashCode, lineStartInReadBuffer, lastLineLength);
                    lineStartInReadBuffer = segmentBufferWritePos;
                    lastLineLength = 0;
                } else {
                    lastLineLength++;
                }
            }
            if (segmentBufferWritePos < targetSegmentSize) {
                bytesRead = fis.read(readByteBuffer, 0, readByteBuffer.length);
            }
        }
        if (lastLineLength > 0) {
            int pidarEbani = fis.read();
            while (pidarEbani != -1 && pidarEbani != SLASH_EN_BYTE) {
                segmentByteBuffer[segmentBufferWritePos++] = (byte) pidarEbani;
                pidarEbani = fis.read();
            }
            int hashCode = Arrays.hashCode(getArray(segmentByteBuffer, lineStartInReadBuffer, lastLineLength));
            result.add(hashCode, lineStartInReadBuffer, lastLineLength);
        }
    }

    static void writeSegment(SegmentInfo segmentInfo, String filename, byte[] segmentByteBuffer, int segmentSize) throws IOException {

        FileOutputStream fos = new FileOutputStream(filename);
        writeSegmentToFos(segmentInfo, fos, segmentByteBuffer, segmentSize);
        fos.flush();
        fos.close();
    }

    static void writeSegmentToFos(SegmentInfo segmentInfo, FileOutputStream fos, byte[] segmentByteBuffer, int bufferSize) throws IOException {
        byte[] writeBuffer = new byte[bufferSize];
        int bufferOffset = 0;
        for (int i = 0; i < segmentInfo.actualSize; i++) {
            byte[] arr = getArray(segmentByteBuffer, segmentInfo.offsets[i], segmentInfo.lengths[i]);
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

    static void writeSortedSegment(OutputSegment segment, FileOutputStream fos) throws IOException {
        fos.write(segment.arr, 0, segment.actualSize);
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
//            if (true)
//                throw new IllegalArgumentException("asdad");
            RandomAccessFile raf = new RandomAccessFile(compareToFilename, "r");
            FileReader ofr = new FileReader(originalFile);
            BufferedReader obfr = new BufferedReader(ofr);
            String lineFromOriginalFile = obfr.readLine();
            int numberOfFails = 0;
            List<String> missing = new ArrayList<>();
            int testNumber = 0;
            boolean finalTestSucc = true;
            while (lineFromOriginalFile != null && false) {
                testNumber++;
                if (testNumber % (numberOfLines / 100000) == 0) {
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
        final byte[] readBuffer;
        final FileInputStream fis;
        final Deque<byte[]> lineCache;
        final Deque<Integer> hashCodeCache;

        public SegmentReader(String filename) {
            try {
                this.filename = filename;
                fis = new FileInputStream(filename);
                readBuffer = new byte[FIS_BUFFER_SIZE];
                lineCache = new ArrayDeque<>();
                hashCodeCache = new ArrayDeque<>();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        public byte[] nextLine() throws IOException {
            if (lineCache.isEmpty()) {
                read();
            }
            if (lineCache.isEmpty()) {
                return null;
            } else {
                hashCodeCache.poll();
                return lineCache.poll();
            }
        }

        private void read() throws IOException {
            int bytesRead = fis.read(readBuffer);
            int lineStart = 0;
            int lineEnd = 0;
            while (lineEnd < bytesRead) {
                byte pidar = readBuffer[lineEnd++];
                if (pidar == SLASH_EN_BYTE) {
                    byte[] arr = new byte[lineEnd - lineStart - 1];
                    System.arraycopy(readBuffer, lineStart, arr, 0, arr.length);
                    lineCache.add(arr);
                    hashCodeCache.add(Arrays.hashCode(arr));
                    lineStart = lineEnd;
                    lineEnd += 1;
                }
            }
            if (lineStart != bytesRead && bytesRead > 0) {
                int length = bytesRead - lineStart;
                System.arraycopy(readBuffer, lineStart, readBuffer, 0, length);
                lineEnd = length;
                int pidar = fis.read();
                while (pidar > 0 && pidar != SLASH_EN_BYTE) {
                    readBuffer[lineEnd++] = (byte) pidar;
                    pidar = fis.read();
                }

                byte[] arr = new byte[lineEnd];
                if (arr.length == 1) {
                    System.out.println("SUKA BLIAT");
                }
                System.arraycopy(readBuffer, 0, arr, 0, arr.length);
                lineCache.add(arr);
                hashCodeCache.add(Arrays.hashCode(arr));
            }
        }

        public byte[] peek() throws IOException {
            if (lineCache.isEmpty()) {
                read();
            }
            return lineCache.peek();
        }

        public int peekHashCode() throws IOException {
            if (lineCache.isEmpty()) {
                read();
            }
            return hashCodeCache.peek();
        }

        public void closeAndDelete() throws IOException {
            fis.close();
            new File(filename).delete();
        }
    }

    static byte[] getArray(byte[] source, int offset, int length) {
        byte[] res = new byte[length];
        System.arraycopy(source, offset, res, 0, length);
        return res;
    }

    public static class SegmentInfo {
        public static final double FACTOR = 1.25;
        int[] hashCodes;
        int[] offsets;
        int[] lengths;
        int actualSize;

        public SegmentInfo(int initialCapacity) {
            hashCodes = new int[initialCapacity];
            offsets = new int[initialCapacity];
            lengths = new int[initialCapacity];
            actualSize = 0;
        }

        public void add(int hashCode, int offset, int length) {
            if (actualSize + 1 == hashCodes.length) {
                hashCodes = Arrays.copyOf(hashCodes, (int) (actualSize * FACTOR));
                offsets = Arrays.copyOf(offsets, (int) (actualSize * FACTOR));
                lengths = Arrays.copyOf(lengths, (int) (actualSize * FACTOR));
            }
            hashCodes[actualSize] = hashCode;
            offsets[actualSize] = offset;
            lengths[actualSize] = length;
            actualSize += 1;
        }

        public void clear() {
            actualSize = 0;
        }

        public void sort() {
            quicksort(0, actualSize - 1);
        }

        private void quicksort(int low, int high) {
            int i = low, j = high;
            int pivot = hashCodes[low + (high - low) / 2];
            while (i <= j) {
                while (hashCodes[i] < pivot) {
                    i++;
                }
                while (hashCodes[j] > pivot) {
                    j--;
                }
                if (i <= j) {
                    exchange(i, j);
                    i++;
                    j--;
                }
            }
            if (low < j)
                quicksort(low, j);
            if (i < high)
                quicksort(i, high);
        }

        private void exchange(int i, int j) {
            int temp = hashCodes[i];
            hashCodes[i] = hashCodes[j];
            hashCodes[j] = temp;

            temp = offsets[i];
            offsets[i] = offsets[j];
            offsets[j] = temp;

            temp = lengths[i];
            lengths[i] = lengths[j];
            lengths[j] = temp;
        }
    }

    public static class OutputSegment {
        byte[] arr;
        int actualSize;

        public OutputSegment(int capacity) {
            arr = new byte[capacity];
            actualSize = 0;
        }

        public void add(byte[] arrToAdd) {
            System.arraycopy(arrToAdd, 0, arr, actualSize, arrToAdd.length);
            actualSize += arrToAdd.length;
        }

        public void clear() {
            actualSize = 0;
        }
    }


}
