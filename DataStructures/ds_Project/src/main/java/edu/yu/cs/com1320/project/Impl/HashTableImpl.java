package edu.yu.cs.com1320.project.Impl;

/*
Natan Zamanzadeh
* HashTable implementation that acts as a our initial point of storage for our data, until a 'Disk' in installed
* the hashtable allows for put, delete, functionality and adapts its size to fit data needs. Risk of overload**
 */
import edu.yu.cs.com1320.project.*;

public class HashTableImpl<Key, Value> implements HashTable<Key, Value> {

    public class HashNode<Key, Value> {

        private Key key;
        private Value value;

        HashNode<Key, Value> next;
        HashNode<Key, Value> prev;

        HashNode(Key key, Value value){

            this.key = key;
            this.value = value;
        }

        private Key getKey(){
            return this.key;
        }

        private Value getValue(){
            return this.value;
        }
    }

    private HashNode<Key, Value>[] hashArray;
    private int cap;
    private int size;
    private int counter = 0;

    @SuppressWarnings("unchecked")
    public HashTableImpl()  {

        this.cap = 17;
        this.size = 0;
        hashArray = new HashNode[cap];

    }
    //returns the index by modding it over the cap to find the next available spot.
    private int getIndex(Key key)   {
        int hashCode = key.hashCode();
        int index = hashCode % cap;
        return Math.abs(index);
    }

    public Value get(Key key){

        int index = getIndex(key); //next open location 
        HashNode<Key, Value> current = hashArray[index];

        //null check
        while (current != null){
            if (current.key.equals(key))    {
                return current.value;
            }
            else {
                current = current.next;
            }
        }
        return null;
    }

    /* Put
    * stores the value using a unique key.
    * The map will always check if there is something in the location it is attempting to fill
    *  If the location is full, the location's data will be overwritten by the new value 
    *  and @Returned by the function.
    */
    public Value put(Key key, Value value) {
        if (key == null) {
            throw new IllegalArgumentException("NoNullValues");
        }
        int index = getIndex(key);
        HashNode<Key, Value> current = hashArray[index];
        HashNode<Key, Value> newNode = new HashNode(key, value);

        //checking location availability.
        while (current != null){
            if (current.getKey() == key) {
                if (value == null) {
                    Value vHolder = current.value;
                    if (delete(current)) { 
                        return vHolder; // return overwritten node.
                    }
                    else {
                        return null; //open spot return
                    } 
                }
                //refitting the node pointer to our current(new) value
                HashNode<Key, Value> tempValue = current;
                tempValue.value = current.value;
                current.value = value;
                return tempValue.value;
            }
            if (current.next != null){
                current = current.next;
            }
            else {
                break;
            }
        }
        //SPECIAL CASE: If get to this point then the inputted key is not in the map, and if value == null,
        //then im attempting to delete. So return null as the requested key to delete does not exist.
        if (value == null) {
            return null;
        }
        if(hashArray[index] == null){
            hashArray[index] = newNode;
            size++;
            return null;
        }

        current.next = newNode;
        newNode.prev = current;
        size++;
        if ((double) size / cap >= .7) {
           resizeArray();
        }
        counter++;
        return null;
    }

    //Second implementation for Put, but unformatted.
//    public Value put(Key key, Value value) {
//        int index = getIndex(key);
//        HashNode<Key, Value> newNode = new HashNode(key, value);
//        HashNode<Key, Value> current = hashArray[index];
//
//
//
//        if (current == null) {
//            if (value == null) {
//                return (Value) new int [-1];
//            }
//            size++;
//            hashArray[index] = newNode;
//        } else {
//            while (current.next != null) {
//                if (current.key == key) {
//                    if (value == null) {
//                        delete(current);
//                    }
//                    HashNode<Key, Value> tempValue = current;
//                    tempValue.value = current.value;
//                    current.value = value;
//                    return tempValue.value;
//                } else {
//                    current = current.next;
//                }
//            }
//            if (current.key == key) { //if current.next was null it still has to check if current has same "k" val
//                if (value == null) {
//                    size--;
//                    current.prev.next = null;
//                    current.prev = null;
//                    return current.value;
//                }
//                HashNode<Key, Value> tempValue = current;
//                tempValue.value = current.value;
//                current.value = value;
//                return tempValue.value;
//            }
//            size++;
//            current.next = newNode;
//            newNode.prev = current;
//        }
//        if ((double) size / cap >= .5) {
//            resizeArray();
//        }
//        return null;
//    }

    /*
    *   delete function logic.
    */
    @SuppressWarnings("unchecked")
    private boolean delete(HashNode current) {
        if (current.prev == null && current.next == null){ //only one in the map
            hashArray[getIndex((Key)current.getKey())] = null;
        }
        else if (current.prev == null && current.next != null) {// if first
            (current.next).prev = current.prev;
            hashArray[getIndex((Key)current.getKey())] = current.next;
        }
        else if (current.prev != null && current.next != null) { //in the middle of the map
            current.next.prev = current.prev;
            current.prev.next = current.next;
        }
        else if (current.prev != null && current.next == null) { // if last
            current.prev.next = current.next;
        }
        if (get((Key)current.getKey()) == null) { //final check and size reduction + return
            size--;
            return true;
        }
        return false;
    }

    //Resize the array if at x% of capacity.
    @SuppressWarnings("unchecked")
    private void resizeArray() {
        HashNode[] tempHashArray = hashArray;
        cap = 2 * cap;
        size = 0;
        hashArray = new HashNode[cap];
        for (HashNode<Key, Value> tempNode : tempHashArray) {
            while (tempNode != null) {
                put(tempNode.key, tempNode.value);
                tempNode = tempNode.next;
            }
        }
    }
}