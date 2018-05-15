import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.AdditionalMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class FileSorterExternalX3ByteArraysTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private FileInputStream fis;

    @Mock
    private FileOutputStream fos;

    static byte SLASH_N = System.lineSeparator().getBytes()[0];
    ;
    List<FileSorterExternalX3ByteArrays.HashedByteArray> result;

    @Before
    public void setUp() {
        result = new ArrayList<>();
    }

    @Test
    public void readSegmentTest1() throws Exception {
        byte[] bytes = new byte[10];
        when(fis.read(bytes, 0, 10)).thenReturn(10);
        int left = FileSorterExternalX3ByteArrays.readSegment(fis, result, bytes, 0, 1000);
        assertThat(left, is(10));
    }


    @Test
    public void readSegmentTest2() throws Exception {
        byte[] bytes = new byte[]{1, 2, 3, 0, 0, 0};
        when(fis.read(bytes, 3, 6)).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                bytes[3] = SLASH_N;
                bytes[4] = 12;
                bytes[5] = SLASH_N;
                return 3;
            }
        });
        when(fis.read(bytes, 0, 6)).thenReturn(0);
        int left = FileSorterExternalX3ByteArrays.readSegment(fis, result, bytes, 3, 1000);
        assertThat(left, is(0));
        assertThat(result.size(), is(2));

        byte[] firstLine = result.get(0).array;
        byte[] secondLine = result.get(1).array;

        assertArrayEquals(new byte[]{1, 2, 3}, firstLine);
        assertArrayEquals(new byte[]{12}, secondLine);
    }

    @Test
    public void readSegmentTest3() throws Exception {
        byte[] bytes = new byte[]{0, 0, 0, 0, 0};
        when(fis.read(bytes, 0, 5)).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                bytes[0] = 1;
                bytes[1] = 2;
                bytes[2] = 3;
                bytes[3] = 4;
                bytes[4] = SLASH_N;
                return 5;
            }
        });
//        when(fis.read(bytes, 0, 6)).thenReturn(0);
        int left = FileSorterExternalX3ByteArrays.readSegment(fis, result, bytes, 0, 4);
        assertThat(left, is(0));
        assertThat(result.size(), is(1));

        byte[] firstLine = result.get(0).array;

        assertArrayEquals(new byte[]{1, 2, 3, 4}, firstLine);
    }


    @Test
    public void readSegmentTest4() throws Exception {
        byte[] bytes = new byte[]{100, 0, 0, 0, 0};
        when(fis.read(bytes, 1, 4)).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                bytes[1] = 101;
                bytes[2] = 102;
                bytes[3] = SLASH_N;
                return 3;
            }
        });
        int left = FileSorterExternalX3ByteArrays.readSegment(fis, result, bytes, 1, 1000);
        assertThat(left, is(0));
        assertThat(result.size(), is(1));

        byte[] firstLine = result.get(0).array;

        assertArrayEquals(new byte[]{100, 101, 102}, firstLine);
    }


    @Test
    public void readSegmentTest5() throws Exception {
        byte[] bytes = new byte[]{100, 0, 0, 0, 0, 0, 0};
        when(fis.read(bytes, 1, 6)).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                bytes[1] = 101;
                bytes[2] = SLASH_N;
                bytes[3] = 102;
                bytes[4] = 103;
                bytes[5] = 104;
                bytes[6] = 105;
                return 6;
            }
        });

        when(fis.read(bytes, 4, 3)).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                bytes[4] = 106;
                bytes[5] = SLASH_N;
                return 2;
            }
        });


        int left = FileSorterExternalX3ByteArrays.readSegment(fis, result, bytes, 1, 8);
        assertThat(left, is(0));
        assertThat(result.size(), is(2));

        byte[] firstLine = result.get(0).array;
        byte[] secondLine = result.get(1).array;

        assertArrayEquals(new byte[]{100, 101}, firstLine);
        assertArrayEquals(new byte[]{102, 103, 104, 105, 106}, secondLine);
    }


    @Test
    public void readSegmentTest6() throws Exception {
        byte[] bytes = new byte[]{0, 0, 0, 0};
        when(fis.read(bytes, 0, 4)).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                bytes[0] = 1;
                bytes[1] = 2;
                bytes[2] = SLASH_N;
                bytes[3] = 3;
                return 4;
            }
        });

        when(fis.read(bytes, 1, 3)).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                bytes[1] = 4;
                bytes[2] = SLASH_N;
                bytes[3] = 5;
                return 3;
            }
        });


        int left = FileSorterExternalX3ByteArrays.readSegment(fis, result, bytes, 0, 7);
        assertThat(left, is(1));
        assertThat(result.size(), is(2));

        byte[] firstLine = result.get(0).array;
        byte[] secondLine = result.get(1).array;

        assertArrayEquals(new byte[]{1, 2}, firstLine);
        assertArrayEquals(new byte[]{3, 4}, secondLine);
    }


    @Test
    public void testForWrite() throws IOException {
        List<FileSorterExternalX3ByteArrays.HashedByteArray> segment =
                Stream.of("-561209695", "-712622750", "817990330")
                        .map(FileSorterExternalX3ByteArrays.HashedByteArray::fromString)
                        .collect(Collectors.toList());


        FileSorterExternalX3ByteArrays.writeSegmentToFos(segment, fos, 32);
        byte[] firstBuff = new byte[]{
                45, 53, 54, 49, 50, 48, 57, 54, 57, 53, 10, //11 bytes
                45, 55, 49, 50, 54, 50, 50, 55, 53, 48, 10, //11 bytes
                56, 49, 55, 57, 57, 48, 51, 51, 48, 10      //10 bytes (32 in total)


        };

//        verify(fos).write(AdditionalMatchers.aryEq(firstBuff), ArgumentMatchers.eq(0), ArgumentMatchers.eq(32));
        verify(fos).write(AdditionalMatchers.aryEq(firstBuff));
        verifyNoMoreInteractions(fos);


    }


}