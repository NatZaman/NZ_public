package edu.yu.cs.com1320.project.Impl;
/*
Natan Zamanzadeh
*The stack takes a Generic input
*in our case, the stack is taking an function of occurences in our DocumentStore
*it allows a redo/undo functionality, including memory management and trie reformating.
 */
import edu.yu.cs.com1320.project.*;


public class StackImpl<T> implements Stack<T> {

    private stackNode topOfStack = new stackNode();
    private int size = 0;

    //pointers to the current, next, and previous nodes
    private class stackNode {

        private T cmdElement;
        private stackNode next; 
        private stackNode prev;

        private stackNode(T cmdElement){

            this.cmdElement = cmdElement;
        }

        private T getElement(){
            return this.cmdElement;
        }

        private stackNode() {
            this.next = null;
            this.prev = null;
        }
    }


    //pushes a complete action onto the stack
    public void push(T element){

        stackNode newStackNode = new stackNode(element);
        newStackNode.next = topOfStack;
        topOfStack.prev = newStackNode;
        topOfStack = newStackNode;
        size++;
    }
    //pops the top element off the stack, and returns the element
    //either undoing or deleting the action
    public T pop(){

        if(size == 0){
            return null;
        }
        T cmdElement = topOfStack.getElement();
        topOfStack.next.prev = topOfStack.prev;
        topOfStack = topOfStack.next;
        size--;
        return cmdElement;
    }
    //takin a look at the top element
    public T peek() {

        if(size == 0){
            return null;
        }
        return topOfStack.getElement();
    }
    //size
    public int size() {
        return this.size;
    }

}
