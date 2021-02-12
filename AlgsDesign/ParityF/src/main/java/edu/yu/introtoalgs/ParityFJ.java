package edu.yu.introtoalgs;

import java.util.concurrent.*;

public class ParityFJ extends RecursiveTask<Boolean>{

    int[] array;
    int low;
    int high;

    public ParityFJ(int[] arr, int low, int high){
        this.array = arr;
        this.low = low;
        this.high = high;
    }

    public static boolean parity(int[] arr){
        ForkJoinPool pool = new ForkJoinPool();
        ForkJoinTask<Boolean> task = new ParityFJ(arr, 0, arr.length);
        return pool.invoke(task);
    }

    /*
        Calcluates if the array sent in has an even number of evens: True if yes, False if no.
        Optimized for larger than 1 threshhold changes.
     */
    private boolean hasEEsTest(int[] array, int low, int high){
        int count = 0;
        for(int i = low; i < high; i++) {
            if (array[i] % 2 == 0)
                count++;
        }
        if (count % 2 == 0) {
            return true;
        }
        else
            return false;
    }

    // OVerrides the compute method  RecursiveTask method for my desired computing values and threshhold/base case.
    // threshhold is set to 1, can be changed for greater optimization but as per assignment reqs, it is set to 1.
    @Override
    protected Boolean compute() {


        if (high - low <= 1) {
             return hasEEsTest(array, low, high);
        }
        else {
            int mid = (low + high)/2;
            ParityFJ left = new ParityFJ(array, low, mid);
            ParityFJ right = new ParityFJ(array, mid, high);
            left.fork();

            if (right.compute() == left.join()) //Was confusing to find this, but F|F = true, as if both are odd == an even amount of evens.
                                                // (special case: 0 evens, but as 0 is an even number to the JDK, it works.)
                                                // && would not work, as if both are odd, it will not return a true as F&&F == false.
                return true;
            else
                return false;
        }

    }

//    public static void main(String[] args) {
//
//        int j = 1000;
//        int[] array = new int[j];
//        for(int i = 0; i < j; i++)
//            array[i] = i;
//
//        for(int temp : array)
//            System.out.println(temp + " ");
//
//        System.out.println(parity(array));
//    }
}



