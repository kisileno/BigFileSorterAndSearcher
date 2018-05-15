import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FileSorter {


    public static final long TARGET_SIZE = 1L * 1024;
    public static final long TARGET_SEGMENT_SIZE = TARGET_SIZE / 10;
    public static final byte[] SLASH_EN = "\n".getBytes();
    public static final Comparator<String> STRING_INTEGER_COMPARATOR = Comparator.comparingInt(Integer::parseInt);

    public static void main(String[] args) throws IOException {

        NumberFileGenerator.generateFile(TARGET_SIZE);

        FileReader fr = new FileReader(NumberFileGenerator.GENERATED_NUMBERS_TXT_FILE_NAME);
        BufferedReader br = new BufferedReader(fr);

        RandomAccessFile raf = new RandomAccessFile(NumberFileGenerator.GENERATED_NUMBERS_TXT_FILE_NAME, "rw");

        List<SegmentOffset> segmentOffsets = new ArrayList<>();

        List<String> segment = readSegment(br, TARGET_SEGMENT_SIZE);
        while (!segment.isEmpty()) {
            System.out.println("Segment read: " + segmentOffsets.size());

            segment.sort(STRING_INTEGER_COMPARATOR);
            System.out.println("Segment sorted");

            long start = raf.getFilePointer();
            writeSegment(segment, raf);
            long end = raf.getFilePointer();
            segmentOffsets.add(new SegmentOffset(start, end));

            System.out.println("Sorted Segment written.");
            segment = readSegment(br, TARGET_SEGMENT_SIZE);
        }
        raf.seek(0);


        List<SegmentReader> readers = new ArrayList<>();
        for (SegmentOffset segmentOffset : segmentOffsets) {
            readers.add(new SegmentReader(NumberFileGenerator.GENERATED_NUMBERS_TXT_FILE_NAME, segmentOffset.startOffset));
        }

        long bytesRead = 0;
        int nextSegmentReader = 0;
        
        for (SegmentOffset segmentOffset : segmentOffsets) {

            List<String> lines = new ArrayList<>();
            while (bytesRead < segmentOffset.endOffset) {
                SegmentReader reader = readers.get(nextSegmentReader++ % readers.size());
                String line = reader.nextLine();
                if (line != null && !line.isEmpty()) {
                    lines.add(line);
                    bytesRead += line.length() + 1;
                }
            }

            lines.sort(STRING_INTEGER_COMPARATOR);
            writeSegment(lines, raf);

        }


        raf.close();
        fr.close();
        br.close();

        test();

    }

    static List<String> readSegment(BufferedReader br, long targetSegmentSize) throws IOException {
        long currentSegmentSize = 0;
        List<String> resultSegment = new ArrayList<>();
        String line = br.readLine();
        while (line != null) {
            resultSegment.add(line);
            currentSegmentSize += line.length() + 1;
            if (currentSegmentSize > targetSegmentSize) {
                line = null;
            } else {
                line = br.readLine();
            }
        }
        return resultSegment;
    }

    static void writeSegment(List<String> segment, RandomAccessFile raf) throws IOException {
        for (String s : segment) {
            raf.write(s.getBytes());
            raf.write(SLASH_EN);
        }
    }


    static void test() throws IOException {
        boolean success = true;
        int numberOfLines = 0;

        FileReader fr = new FileReader(NumberFileGenerator.GENERATED_NUMBERS_TXT_FILE_NAME);
        BufferedReader br = new BufferedReader(fr);

        String prevLine = br.readLine();
        String line = br.readLine();
        while (line != null) {
            if (Integer.parseInt(prevLine) > Integer.parseInt(line)) {
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

    static class SegmentOffset {
        long startOffset;
        long endOffset;

        public SegmentOffset(long startOffset, long endOffset) {
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }

        public void shift(long howMuch) {
            endOffset += howMuch;
            startOffset += howMuch;
        }

    }

    static class SegmentReader {
        RandomAccessFile raf;

        public SegmentReader(String filename, long offset) {
            try {
                raf = new RandomAccessFile(filename, "r");
                raf.seek(offset);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public String nextLine() throws IOException {
            return raf.readLine();
        }

        public void shift(long howMuch) throws IOException {
            raf.seek(raf.getFilePointer() + howMuch);
        }
    }


}
