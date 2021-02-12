/* Natan Zamanzadeh
*   Trie implementation for our DocumentStore. 
*   Stores each letter in a node and links it to its word map.
*/



package edu.yu.cs.com1320.project.Impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Comparator;

public class TrieImpl<Value> implements edu.yu.cs.com1320.project.Trie<Value>
{
    private static final int alphabetSize = 256; // extended ASCII
    private Node root; // root of trie
    private Comparator comparator;


    public static class Node<Value>
    {
        protected ArrayList<Value> valSet = new ArrayList<>();
        protected Node[] links = new Node[TrieImpl.alphabetSize];

    }
    //comparator allowing for .compareTo functionality when operating within the trie
    public TrieImpl(Comparator<Value> comparator){
        this.comparator = comparator;
    }

    /**
     * Returns the value associated with the given key.
     *
     * @param key the key
     * @return the value associated with the given key if the key is in the trie and {@code null} if not
     */
    @Override
    public List<Value> getAllSorted(String key)
    {
        Node x = this.get(this.root, key, 0);
        if (x == null)
        {
            return null;
        }
        x.valSet.sort(comparator);

        return (List<Value>)Arrays.asList((Value)x.valSet);
    }

    /**
     * A char in java has an int value.
     * see http://docs.oracle.com/javase/8/docs/api/java/lang/Character.html#getNumericValue-char-
     * see http://docs.oracle.com/javase/specs/jls/se7/html/jls-5.html#jls-5.1.2
     */
    private Node get(Node x, String key, int d)
    {
        //link was null - return null, indicating a miss
        if (x == null)
        {
            return null;
        }
        //we've reached the last node in the key,
        //return the node
        if (d == key.length())
        {
            return x;
        }
        //proceed to the next node in the chain of nodes that
        //forms the desired key
        char c = key.charAt(d);
        return this.get(x.links[c], key, d + 1);
    }

    @Override
    public void put(String key, Value val)
    {
        //deleteAll the value from this key
        if (val == null)
        {
            this.deleteAll(key);
        }
        else
        {
            this.root = put(this.root, key, val, 0);
        }
    }
    /**
     *
     * @param x
     * @param key
     * @param val
     * @param d
     * @return
     */
    private Node put(Node x, String key, Value val, int d)
    {
        //create a new node
        if (x == null)
        {
            x = new Node();
        }
        //we've reached the last node in the key,
        //set the value for the key and return the node
        if (d == key.length())
        {
            x.valSet.add(val);
            return x;
        }
        //proceed to the next node in the chain of nodes that
        //forms the desired key
        char c = key.charAt(d);
        x.links[c] = this.put(x.links[c], key, val, d + 1);
        return x;
    }

    @Override
    public void deleteAll(String key)
    {
        this.root = deleteAll(this.root, key, 0);
    }

    private Node deleteAll(Node x, String key, int d)
    {
        if (x == null)
        {
            return null;
        }
        //we're at the node to del - set the val to null
        if (d == key.length())
        {
           // x.valSet = null;
        }
        //continue down the trie to the target node
        else
        {
            char c = key.charAt(d);
            x.links[c] = this.deleteAll(x.links[c], key, d + 1);
        }
        //this node has a val – do nothing, return the node
        if (x.valSet != null)
        {
            return x;
        }
        //remove subtrie rooted at x if it is completely empty
        for (int c = 0; c <TrieImpl.alphabetSize; c++)
        {
            if (x.links[c] != null)
            {
                return x; //not empty
            }
        }
        //empty - set this link to null in the parent
        return null;
    }

    public void delete(String key, Value val){
        if(key == null) {
            deleteAll(key);
        }
        else{
            delete(this.root, key, val, 0);
        }
    }

    //modified delete
    //removes the deleted word tables and associated information from the trie
    private Node delete(Node x, String key, Value val, int d){
        if (x == null){
            return null;
        }
        if (d == key.length()){
            if(x.valSet.contains(val)){
                x.valSet.remove(val);
            }
            else{
                return null;
            }
        }
        else
        {
            char c = key.charAt(d);
            x.links[c] = this.delete(x.links[c], key, val, d + 1);
        }
        if (x.valSet != null) {
            return x;
        }
        for (int c = 0; c <TrieImpl.alphabetSize; c++) {
            if (x.links[c] != null) {
                return x;
            }
        }
        return null;
    }
}