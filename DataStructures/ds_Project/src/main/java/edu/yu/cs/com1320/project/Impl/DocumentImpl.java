package edu.yu.cs.com1320.project.Impl;

/*
Natan Zamanzadeh
Document OBJ Implementation
stores the document, and all relevant data. 
updated as the project developed.
 */
import edu.yu.cs.com1320.project.Impl.*;
import edu.yu.cs.com1320.project.*;
import java.net.URI;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Scanner;

public class DocumentImpl implements Document {

    private URI key;
    private long sysUsgTime;
    private byte[] value;
    private DocumentStore.CompressionFormat format;
    private int hashCode;
    private HashMap<String, Integer> wordTable;
    private Scanner scanner = new Scanner(System.in);

    public DocumentImpl(URI key, byte[] value, DocumentStore.CompressionFormat format, int cashHode, HashMap<String, Integer> wordTable) {
        if (key == null || value == null || format == null) {
            throw new IllegalArgumentException("noNullValues");
        }
        this.key = key;  //unique ID
        this.value = value; //byte array of document
        this.format = format; //format type that it was compressed
        this.hashCode = cashHode; //the hashcode of this object
        this.wordTable = wordTable; //its own personal word/meta-info look up table. 
    }

    private HashMap<String, Integer> getWordTable(){
        return this.wordTable;
    }

    public byte[] getDocument(){
        return this.value;
    }

    public int getDocumentHashCode(){
        return this.hashCode;
    }

    public URI getKey(){
        return this.key;
    }

    public DocumentStore.CompressionFormat getCompressionFormat(){
        return this.format;
    }

    public int wordCount(String word){
        int wordCount = wordTable.get(word);
        return wordCount;
    }

    public long getLastUseTime() {
       return this.sysUsgTime;
    }


    public void setLastUseTime(long timeInMilliseconds){
        this.sysUsgTime = timeInMilliseconds;
    }

    @Override
    public int compareTo(Document docObj){
        if ((this.sysUsgTime - docObj.getLastUseTime()) < 0){
            return -1;
        }
        else {
            return 1;
        }
    }

    // deleted hashcode override because hashcode of the object is never used.
    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (this.getClass() != that.getClass()) {
            return false;
        }
        DocumentImpl otherNode = (DocumentImpl) that;
        return (this.getKey() == otherNode.getKey()) && (this.getDocumentHashCode() == otherNode.getDocumentHashCode());
    }

    @Override
    public String toString() {
        return ("Key: " + this.getKey() + "Value: " + this.getDocument()); // builds a string of the value and the compressdocument.. this should never be used?
    }
}
