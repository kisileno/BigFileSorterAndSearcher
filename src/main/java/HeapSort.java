import java.util.*;

public class HeapSort {

    public static void main(String[] args) {


        int N = 5;
        Random random = new Random();
        int[] arr = new int[N];

        ArrayList<Integer> list = new ArrayList<>();
        for (int i = 0; i < arr.length; i++) {
//            list.add(arr[i]);
            list.add(random.nextInt(1000));
        }
        Collections.shuffle(list);
        for (int i = 0; i < arr.length; i++) {
            arr[i] = list.get(i);
        }

        System.out.println(Arrays.toString(arr));

        makeASortTree(arr);

        System.out.println(Arrays.toString(arr));

        heapSort(arr);

        System.out.println(Arrays.toString(arr));

        for (int i = 0; i< arr.length -1;i++) {
            if (arr[i] > arr[i++]) {
                throw new IllegalStateException("PIZDA RULU");
            }
        }

    }

    public static void makeASortTree(int[] arr) {


        for (int i = arr.length / 2; i >= 0; i--) {
            sortHeapFrom(arr, i, arr.length);
        }
    }

    public static void sortHeapFrom(int[] arr, int i, int heapLength) {
        Deque<Integer> stack = new LinkedList<>();
        stack.push(i);
        while (!stack.isEmpty()) {
            Integer prevMax = stack.pop();
            int left = 2 * prevMax + 1;
            int right = 2 * prevMax + 2;
            int max = prevMax;

            if (left < heapLength && arr[left] > arr[max]) {
                max = left;
            }

            if (right < heapLength && arr[right] > arr[max]) {
                max = right;
            }

            if (max != prevMax) {
                swap(arr, prevMax, max);
                stack.push(max);
            }
        }
    }

    public static void swap(int[] arr, Integer i, int j) {
        int tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }

    public static void heapSort(int[] heap) {
        
        int heapLength = heap.length;
        for (int i = heap.length - 1; i > 0; i--) {
            swap(heap, 0, i);
            sortHeapFrom(heap, 0, --heapLength);

        }

    }
}
