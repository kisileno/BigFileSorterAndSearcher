import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class FileSorterExternalX3 {


    public static final long TARGET_SIZE = 1L * 1024 * 1024 * 1024;
    public static final long TARGET_SEGMENT_SIZE = 150L * 1024 * 1024;
    public static final Comparator<String> STRING_INTEGER_COMPARATOR = java.util.Comparator.comparingInt(Integer::parseInt);
    public static final Comparator<String> STRING_HASHCODE_COMPARATOR = Comparator.comparingInt(String::hashCode);
    private static final String SORTED_NUMBER_FILE_NUME = "sorted-number.txt";

    public static void main(String[] args) throws IOException {

//        NumberFileGenerator.generateFile(TARGET_SIZE);

        long totalStart = System.currentTimeMillis();

        FileReader fr = new FileReader(NumberFileGenerator.GENERATED_NUMBERS_TXT_FILE_NAME);
        BufferedReader br = new BufferedReader(fr);

        List<String> sortedSegmentFileNames = new ArrayList<>();
        List<String> segmentBuffer = new ArrayList<>();
        readSegment(br, segmentBuffer, TARGET_SEGMENT_SIZE);


        FileWriter fw = new FileWriter(SORTED_NUMBER_FILE_NUME);
        BufferedWriter sortedFileWriter = new BufferedWriter(fw);


        while (!segmentBuffer.isEmpty()) {
            int segmentNumber = sortedSegmentFileNames.size();
            long sortStart = System.currentTimeMillis();
            segmentBuffer.sort(STRING_HASHCODE_COMPARATOR);
            long sortEnd = System.currentTimeMillis();
            System.out.println("Segment " + segmentNumber + " sorted in: " + (sortEnd - sortStart));

            String segmentFileName = "/tmp/" + NumberFileGenerator.GENERATED_NUMBERS_TXT_FILE_NAME + ".seg." + segmentNumber;
            sortedSegmentFileNames.add(segmentFileName);

            long writeStart = System.currentTimeMillis();
            writeSegment(segmentBuffer, segmentFileName);
            segmentBuffer.clear();
            long writeEnd = System.currentTimeMillis();
            System.out.println("Written to " + segmentFileName + " in " + (writeEnd - writeStart));

            long readStart = System.currentTimeMillis();
            segmentBuffer = readSegment(br, segmentBuffer, TARGET_SEGMENT_SIZE);
            long readEnd = System.currentTimeMillis();
            System.out.println("Segment read: " + (segmentNumber + 1) + " in: " + (readEnd - readStart));

        }


        long mergeStart = System.currentTimeMillis();
        List<SegmentReader> readers = new ArrayList<>(sortedSegmentFileNames.size());
        for (String segmentName : sortedSegmentFileNames) {
            readers.add(new SegmentReader(segmentName));
        }
        long currentBufferSize = 0;
        int buffersWritten = 0;
        List<String> outputBuffer = new ArrayList<>();
        List<String> peekBuffer = new ArrayList<>(readers.size());

        for (SegmentReader reader : readers) {
            peekBuffer.add(reader.peek());
        }
        while (true) {
            int minElemIndex = getIndexOfMin(peekBuffer, STRING_HASHCODE_COMPARATOR);
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
                peekBuffer.set(minElemIndex, nextLine);
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

        fr.close();
        br.close();
        sortedFileWriter.close();
        fw.close();

        test(SORTED_NUMBER_FILE_NUME, STRING_HASHCODE_COMPARATOR);

    }

    static List<String> readSegment(BufferedReader br, List<String> buffer, long targetSegmentSize) throws IOException {
        long currentSegmentSize = 0;
        String line = br.readLine();
        while (line != null) {
            buffer.add(line);
            currentSegmentSize += line.length() + 1;
            if (currentSegmentSize > targetSegmentSize) {
                line = null;
            } else {
                line = br.readLine();
            }
        }
        return buffer;
    }

    static long writeSegment(List<String> segment, String filename) throws IOException {
        FileWriter fw = new FileWriter(filename);
        BufferedWriter bw = new BufferedWriter(fw);
        long bytesWritten = 0;
        for (String s : segment) {
            bw.write(s);
            bw.write("\n");
            bytesWritten += s.length() + 1;
        }
        bw.close();
        fw.close();
        return bytesWritten;
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


    static void test(String filename, Comparator<String> comparator) throws IOException {
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

    static int getIndexOfMin(List<String> list, Comparator<String> comparator) {
        int size = list.size();
        if (size == 0) {
            return -1;
        } else if (size == 1) {
            return 0;
        } else {
            String min = list.get(0);
            int minIndex = 0;
            for (int i = 1; i < size; i++) {
                String potentialMin = list.get(i);
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

    static class HashedByteArray  {
        final byte[] array;
        final int hashCode;

        public HashedByteArray(byte[] array) {
            this.array = array;
            hashCode = Arrays.hashCode(array);
        }
    }


}
