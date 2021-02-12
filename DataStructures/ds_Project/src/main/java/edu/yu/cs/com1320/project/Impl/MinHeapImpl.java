package edu.yu.cs.com1320.project.Impl;
import edu.yu.cs.com1320.project.*;
import java.util.*;

/*
*Natan Zamanzadeh
* A min-Heap implementation that acts as our documentStore's 'memory'/'memory manager'
* the min heap will store each document used's info including its lasst time used
* it will rehipify per action and toss the min out when it reaches capacity
*/

public class MinHeapImpl<E extends Comparable> extends MinHeap<E>{

    protected int size = 17;
    protected E[] elements;
    protected Map<E,Integer> elementsToArrayIndex;

    public MinHeapImpl(){

       this.elements = (E[]) new Comparable[size];
       this.elementsToArrayIndex = new HashMap<>();
    }

    public void reHeapify(E element) { // not aways down heap??
        downHeap(getArrayIndex(element));
        upHeap(getArrayIndex(element));

    }

    protected int getArrayIndex(E element){
        return elementsToArrayIndex.get(element);
    }

    @SuppressWarnings("unchecked")
    protected void doubleArraySize(){
        E[] tempElements = elements;
        elements = (E[]) new Object[size*2];
        int i = 0;
        for(E tempElement : tempElements){
            elements[i] = tempElement;
            i++;
        }
    }
    protected  boolean isEmpty()
    {
        return this.count == 0;
    }
    /**
     * is elements[i] > elements[j]?
     */
    protected  boolean isGreater(int i, int j)
    {
        return this.elements[i].compareTo(this.elements[j]) > 0;
    }


    //swap elements within the heap
    @Override
    protected void swap(int i, int j) {
        E temp = this.elements[i];
        this.elements[i] = this.elements[j];
        this.elements[j] = temp;

        int tempint = (int) elementsToArrayIndex.get(elements[i]);
        elementsToArrayIndex.replace(elements[i], elementsToArrayIndex.get(elements[j]));
        elementsToArrayIndex.replace(elements[j], tempint);
    }

    /**
     *while the key at index k is less than its
     *parent's key, swap its contents with its parent’s
     */
    protected void upHeap(int k)
    {
        while (k > 1 && this.isGreater(k / 2, k))
        {
            this.swap(k, k / 2);
            k = k / 2;
        }
    }

    /**
     * move an element down the heap until it is less than
     * both its children or is at the bottom of the heap
     */
    protected void downHeap(int k)
    {
        while (2 * k <= this.count)
        {
            //identify which of the 2 children are smaller
            int j = 2 * k;
            if (j < this.count && this.isGreater(j, j + 1))
            {
                j++;
            }
            //if the current value is < the smaller child, we're done
            if (!this.isGreater(k, j))
            {
                break;
            }
            //if not, swap and continue testing
            this.swap(k, j);
            k = j;
        }
    }

    public void insert(E x)
    {
        // double size of array if necessary
        if (this.count >= this.elements.length - 1)
        {
            this.doubleArraySize();
        }
        //add x to the bottom of the heap
        this.elements[++this.count] = x;
        //percolate it up to maintain heap order property
        this.upHeap(this.count);
        elementsToArrayIndex.put(x, count);

    }

    public E removeMin()
    {
        if (isEmpty())
        {
            throw new NoSuchElementException("Heap is empty");
        }
        E min = this.elements[1];
        //swap root with last, decrement count
        this.swap(1, this.count--);
        //move new root down as needed
        this.downHeap(1);
        this.elements[this.count + 1] = null; //null it to prepare for GC
     //   elementsToArrayIndex.remove(min); //word map editing functionality can be changed in Documentstore.
        return min;

    }

    // private void fixPlaces(){

    // }
}
