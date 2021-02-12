package edu.yu.introtoalgs;

public class Bubblesort implements Sorter {

  /** No-argument constructor: should not be instantiated outside package
   */
  Bubblesort() {
  }

  @Override
  public void sortIt(final Comparable array[]) {

    int n = array.length;
    Comparable temp = 0;
    for(int i=0; i < n; i++) {
      for (int j = 1; j < (n - i); j++) {
        if (array[j - 1].compareTo( array[j]) > 0) {
          temp = array[j - 1];
          array[j - 1] = array[j];
          array[j] = temp;
        }
      }
    }
  }       // sortIt


} // class
