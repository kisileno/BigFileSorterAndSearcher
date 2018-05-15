import sun.security.pkcs.PKCS8Key;
import sun.security.rsa.RSAPrivateKeyImpl;
import sun.security.rsa.RSASignature;
import sun.security.util.DerValue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

class TESSST {

    public static final int TARGET_SIZE = 200 * 1024 * 1024;

    public static void main2(String[] args) throws IOException, InterruptedException {
        final int oneArraySize = 20;
        final int numberOfArrays = TARGET_SIZE / oneArraySize;
        byte[][] arrays = new byte[numberOfArrays][];
        for (int i = 0; i < numberOfArrays; i++) {
            arrays[i] = new byte[oneArraySize];
        }
        System.gc();
        System.out.println("Arrays size: " + arrays.length);
        System.out.println("HeapSize: " + Runtime.getRuntime().totalMemory());
        Thread.sleep(60000);
    }
    //        for 20
//        Arrays size: 10485760
//        HeapSize: 761790464

    public static void main3(String[] args) throws InterruptedException {
        byte[][] arr = new byte[TARGET_SIZE][1];
        System.gc();
        System.out.println("Array size: " + arr.length);
        System.out.println("HeapSize: " + Runtime.getRuntime().totalMemory());
        Thread.sleep(60000);


    }

    public static void main(String[] args) throws NoSuchAlgorithmException, IOException, NoSuchProviderException {
//        int offset = 150 * 1024 * 1024;
        int offset = 100;
        System.out.println(offset);
        System.out.println(Integer.toBinaryString(offset));
        System.out.println(Integer.toBinaryString(offset).length());

    }

//        Array size: 209715200
//        HeapSize: 467664896
}