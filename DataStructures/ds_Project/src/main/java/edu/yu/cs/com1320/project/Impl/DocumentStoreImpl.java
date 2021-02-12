package edu.yu.cs.com1320.project.Impl;


/*
Natan Zamanzadeh
The DocumentStore,
* Takes file input and compresses it VIA using one of the 7 Apache Commons Compress functions.
* The documentStore, stores data in our hashTable as a resource for all our documents, and a minHeap acting as Memory for faster document lookup. 
* The DocumentStore also supports removing these documents completely from the documentStore and its relational structures.
* the DocumentStore also supports undoing and redoing actions commited to the documentStore, such as put and delete.
* The DocumentStore stores all these actions, per use, as Lambda Functions and stores them in a stack, allowing the DS undo/redo functionality.
 */

import edu.yu.cs.com1320.project.*;
import org.apache.commons.compress.archivers.*;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;

import static java.lang.Integer.MIN_VALUE;


public class DocumentStoreImpl implements DocumentStore {

    public static final int	flagVariable = MIN_VALUE;

    private HashTableImpl<URI, DocumentImpl> map = new HashTableImpl<>();
    private CompressionFormat defaultFormat = CompressionFormat.ZIP;
    private StackImpl<Command> doneStack = new StackImpl<>();
    private StackImpl<Command> undoneStack = new StackImpl<>();
    private RecycledNode topOfBin = new RecycledNode();
    private Comparator<DocumentImpl> compare;
    private String wordField;
    private int sysTime;
    private TrieImpl trie;
    private  MinHeapImpl<DocumentImpl> minHeap =  new MinHeapImpl<>();
    private long curDocLimit = 0;
    private long curByteLimit = 0;
    private long maxDocLimit = 0;
    private long maxByteLimit = 0;
    private boolean isThereByteLimit = false;
    private boolean isThereDocLimit = false;
    private DocumentImpl docToBeWiped;

    //private class storing the nodes that are deleted or overwritten by puts/deletes
    private class RecycledNode {

        private DocumentImpl docObj;
        private RecycledNode next;
        private RecycledNode prev;

        private RecycledNode() {
            this.next = null;
            this.prev = null;
        }

        private RecycledNode(DocumentImpl docObj) {

            this.docObj = docObj;
            topOfBin.prev = this;
            this.next = topOfBin;
            topOfBin = this;
        }

        private RecycledNode getNext() {
            return this.next;
        }

        private RecycledNode getPrev() {
            return this.prev;
        }

        private void removeNode() {
            if (this.prev == null && this.next == null) { //only one in the list
                topOfBin = new RecycledNode();
            } else if (this.prev == null && this.next != null) {// if first in list
                topOfBin.prev.next = topOfBin.next;
                topOfBin.next.prev = topOfBin.prev;
                topOfBin = topOfBin.next;
                this.next.prev = this.prev;
            } else if (this.prev != null && this.next != null) { //in the middle of the list
                this.prev.next = this.next;
                this.next.prev = this.prev;
            } else if (this.prev != null && this.next == null) { // if last in list
                this.prev.next = this.next;
            }
        }
    }

    public DocumentStoreImpl(){
        this.trie = new TrieImpl(docComparator);
    }

    protected void setField(String word) {
        this.wordField = word;

    }
    //doc compare, allowing comparTo functionality.
    Comparator<DocumentImpl> docComparator = new Comparator<DocumentImpl>() {
        private String field;
        @Override
        public int compare(DocumentImpl e1, DocumentImpl e2) {
            return e1.wordCount(field) - e2.wordCount(field);
        }
    };
    //searching for compressed document through the trie by keyword.
    public List<byte[]> searchCompressed(String keyword){
        setField(keyword);
        ArrayList<DocumentImpl> valList = (ArrayList<DocumentImpl>)trie.getAllSorted(keyword);
        valList.sort(docComparator);
        ArrayList<byte[]> sortedByteList = new ArrayList<>();
        for(DocumentImpl tempDoc : valList){
            tempDoc.setLastUseTime(System.currentTimeMillis()); //*****
            minHeap.reHeapify(tempDoc);
            sortedByteList.add(tempDoc.getDocument());
        }
        return sortedByteList;
    }
    //searching for uncompressed document through the trie by keyword
    public List<String> search(String keyword){
        setField(keyword);
        ArrayList<DocumentImpl> valList = (ArrayList<DocumentImpl>)trie.getAllSorted(keyword);
        valList.sort(docComparator);
        ArrayList<String> sortedStringList = new ArrayList<>();
        for(DocumentImpl tempDoc : valList){
            tempDoc.setLastUseTime(System.currentTimeMillis());
            minHeap.reHeapify(tempDoc);
            String s = new String(makeBigger(tempDoc.getCompressionFormat(),tempDoc.getDocument()));
            sortedStringList.add(s);
        }
        return sortedStringList;
    }

    @Override
    public void setDefaultCompressionFormat(CompressionFormat format) {
        this.defaultFormat = format;
    }

    @Override
    public CompressionFormat getDefaultCompressionFormat(){
        return this.defaultFormat;
    }
    
    //put without CF
    public int putDocument(InputStream input, URI uri) {
        if (uri == null){
            throw new IllegalArgumentException("noNullValues");
        }
        return this.putDocument(input, uri, defaultFormat);
    }

    //toString input 
    private String inputToString(InputStream input){
        String text = "";
        Scanner scanner = new Scanner(input, StandardCharsets.UTF_8.name());
        while(scanner.hasNext()) {
            text = text + scanner.useDelimiter("\\A").next();
        }
        return text;
    }

    //Method had to fit 35 Line restraint.
    //taking input as an inputstream and a document's unqiue URI that is created, and the desired compression format.
    //the document is madeSmall (compressed) by the desired function using one of the ACC comp/decomp generators.
    //Wordmaps for each document are also created and stored in the trie.
    //Memory 'limits' are also checked in this method to detemine whether data should be removed from heap.
    public int putDocument(InputStream input, URI uri, CompressionFormat format) {
        HashMap<String, Integer> wordTable;
        if (uri == null || format == null)
            throw new IllegalArgumentException("noNullValues");
        if (input == null)
            return putDelete(uri);

            //parsing input into a byte array and formatted string to store in the Document obj.
        String formattedStr = inputToString(input);
        byte[] docValue = new byte[]{};
        docValue = formattedStr.getBytes();

            //hashing
        int docStrHash = Math.abs(new String(docValue).hashCode());
        DocumentImpl checker = map.get(uri);
        if (checker != null && checker.getDocumentHashCode() == docStrHash)
            return docStrHash;

            //compression
        byte[] compressedDoc = makeSmall(format, docValue);
        formattedStr = formattedStr.toLowerCase().replaceAll("[^A-Za-z]", " ");
        
        //wordmap and Document obj creation
        wordTable = addToMap(formattedStr);
        DocumentImpl docObj = new DocumentImpl(uri, compressedDoc, format, docStrHash, wordTable);
        DocumentImpl replacedNode = map.put(uri, docObj);
        //checking for recycled nodes to store.
        if(replacedNode != null)
            new RecycledNode(replacedNode);
        Set<String> wordSet = wordTable.keySet();
        for(String tempWord : wordSet)
            trie.put(tempWord, docObj);
        
            //updating the last used time for the min-Heap's memory management.
        docObj.setLastUseTime(System.currentTimeMillis());
        makePutLambda(replacedNode, docObj, uri);
        minHeap.insert(docObj);
        curByteLimit += docObj.getDocument().length;
        curDocLimit++;
        if(isThereByteLimit || isThereDocLimit)
            isOverLimit();
        return docStrHash; }

        //if memory limit is reached, the Document at the top of the Min-Heap, with the flastUsedTime, is removed.
    private void isOverLimit(){
        if((isThereDocLimit) && (curDocLimit > maxDocLimit)) {
            while(curDocLimit > maxDocLimit){
                docToBeWiped = minHeap.removeMin();
                curDocLimit--;
                curByteLimit = curByteLimit - docToBeWiped.getDocument().length;
                wipeMethod(docToBeWiped);
            }
         }
        if(isThereByteLimit && (curByteLimit > maxByteLimit)) {
            while (curByteLimit > maxByteLimit) {
                docToBeWiped = minHeap.removeMin();
                curDocLimit--;
                curByteLimit = curByteLimit - docToBeWiped.getDocument().length;
                wipeMethod(docToBeWiped);
            }
        }
    }

    //completely wiping the Document from the DocStore and all relational structures 
    private void wipeMethod(DocumentImpl docToBeWiped){
        if (docToBeWiped == null){
            throw new IllegalArgumentException("okay...");
        }
        delete(docToBeWiped.getKey());
        delFromStack(docToBeWiped.getKey());
        removeTrieWord(docToBeWiped);
    }

    //removing the Doc OBJ's word map from the trie.
    private void removeTrieWord(DocumentImpl docObj){
        Set<String> wordSet = new HashSet<>();
        byte[] uncompressedDoc = makeBigger(docObj.getCompressionFormat(), docObj.getDocument());
        String formattedStr = uncompressedDoc.toString().toLowerCase().replaceAll("[^A-Za-z]", " ");
        Scanner scanner2 = new Scanner(formattedStr);
        while(scanner2.hasNext()){
            wordSet.add(scanner2.next());
        }
        for(String tempWord : wordSet){
            trie.delete(tempWord, docObj);
        }
    }

    //document retrieval by URI
    public String getDocument(URI uri){
        if (uri == null ){
            throw new IllegalArgumentException("noNullValues");
        }
        DocumentImpl docObj = map.get(uri);
        if (docObj == null) {
            return null;
        }
        else {
            String strDoc = new String(makeBigger(docObj.getCompressionFormat(), docObj.getDocument()));
            docObj.setLastUseTime(System.currentTimeMillis());
            minHeap.reHeapify(docObj);
            return strDoc;
        }
    }

    //retrieves the document by URI
    public byte[] getCompressedDocument(URI uri){
        if (uri == null ){
            throw new IllegalArgumentException("noNullValues");
        }
        DocumentImpl docObj = map.get(uri);
        if (docObj == null) {
            return null;
        }
        else {
            docObj.setLastUseTime(System.currentTimeMillis());
            minHeap.reHeapify(docObj);
            return docObj.getDocument();
        }
    }

    //Deletes the document from the documentStore and returns whether it was successful or not.
    // is completed as an action and stored on the stack for undo/redo
    public boolean deleteDocument(URI uri){
        if (uri == null ){
            throw new IllegalArgumentException("noNullValues");
        }
        HashSet<String> wordSet = new HashSet<>();
        byte[] uncompressedDoc = makeBigger(map.get(uri).getCompressionFormat(), map.get(uri).getDocument());
        String formattedStr = uncompressedDoc.toString().toLowerCase().replaceAll("[^A-Za-z]", "");
        Scanner scanner3 = new Scanner(formattedStr);
        while(scanner3.hasNext()){
            wordSet.add(scanner3.next());
        }
        DocumentImpl deletedNode = map.get(uri);
        if (deletedNode != null){
            deletedNode.setLastUseTime(0);
            minHeap.reHeapify(deletedNode);
            minHeap.removeMin();
            for(String tempWord : wordSet){
                trie.delete(tempWord, map.get(uri));
            }
            if((map.put(uri, null)) != null) {
                new RecycledNode(deletedNode);
                Function<URI, Boolean> undo = (key) -> {
                    deletedNode.setLastUseTime(System.currentTimeMillis());
                    minHeap.insert(deletedNode); //****
                    minHeap.reHeapify(deletedNode);
                    map.put(uri, deletedNode);
                    for(String tempWord : wordSet){
                        trie.put(tempWord, deletedNode);
                    }
                    isOverLimit();
                    return true;
                };
                Function<URI, Boolean> redo = (key) -> {
                    deletedNode.setLastUseTime(0);
                    minHeap.reHeapify(deletedNode);
                    minHeap.removeMin();
                    for(String tempWord : wordSet){
                        trie.delete(tempWord, deletedNode);
                    }
                    return delete(key);
                };

                Command newCmdNode = new Command(uri, undo, redo);
                doneStack.push(newCmdNode);

                return true;
            }
            else{
                return false;
            }
        }
        return false;
    }

    //undo, stores actions in a completed actions stack and one with actions that are successfully undone for Redo()
    public boolean undo() throws IllegalStateException {
        Command cmdNode = doneStack.pop();
        if (cmdNode != null) {
            undoneStack.push(cmdNode);
            return cmdNode.undo();
        }
        else {

            throw new IllegalStateException();
        }
    }

    //undo directly by URI
    //Same functionality but will look within the stack for the action most recently done associated with that URI and undo it.
    public boolean undo(URI uri) throws IllegalStateException {
        StackImpl<Command> tempStack = new StackImpl<>();
        Command tempCmd;

        while ((doneStack.size() != 0)) {
            if (doneStack.peek().getUri() != uri) {
                tempCmd = doneStack.pop();
                tempCmd.undo();
                tempStack.push(tempCmd);
            }
            else if (doneStack.peek().getUri() == uri) {
                undo();
                while (tempStack.peek() != null) {
                    tempCmd = tempStack.pop();
                    tempCmd.redo();
                    doneStack.push(tempCmd);
                    return true;
                }
            }
        }
        if(doneStack.peek() == null && doneStack.size() == 0) {
            tempCmd = tempStack.pop();
            tempCmd.redo();
            doneStack.push(tempCmd);
            throw new IllegalStateException();
        }
        return true;
    }

    //API for setting the maximum documents the can be stored in memory
    public void setMaxDocumentCount(int limit){
        if(limit < 0 )
            throw new IllegalArgumentException("Limit cannot be less than 0");
        this.maxDocLimit = limit;
        isThereDocLimit = true;
        isOverLimit();
    }

    //Setting the maximum bytes that can be stored in memory.
    public void setMaxDocumentBytes(int limit){
        if(limit < 0 )
            throw new IllegalArgumentException("Limit cannot be less than 0");
        this.maxByteLimit = limit;
        isThereByteLimit = true;
        isOverLimit();
    }


    //Put Lambda function that is created to allow for multiple actions to be completed per 'action' completed by the Document Store. 
    // functions and their actions are placed on the stock for undo/redo functionality
    private void makePutLambda(DocumentImpl replacedNode, DocumentImpl docObj, URI uri) {
        Function<URI, Boolean> undo;
        Function<URI, Boolean> redo;

        //formatting document and data.
        HashSet<String> wordSet = new HashSet<>();
        byte[] uncompressedDoc = makeBigger(docObj.getCompressionFormat(), docObj.getDocument());
        String formattedStr = uncompressedDoc.toString().toLowerCase().replaceAll("[^A-Za-z]", "");
        Scanner scanner4 = new Scanner(formattedStr);
        while(scanner4.hasNext()){
            wordSet.add(scanner4.next());
        }

        
        long docObjLUT = docObj.getLastUseTime(); //last time used for object 
        if (replacedNode == null) {
            undo = (inputKey) -> { //undoing a put
                docObj.setLastUseTime(0);
                minHeap.reHeapify(docObj);
                minHeap.removeMin();
                curDocLimit--;
                curByteLimit -= docObj.getDocument().length;
//                minHeap.reHeapify(docObj);
                for(String tempStr : wordSet){
                    trie.delete(tempStr, docObj);
                }
               // isOverLimit();
                return delete(inputKey);
            };
            redo = (inputKey) -> { //redo'ing a put
                docObj.setLastUseTime(docObjLUT);
                minHeap.insert(docObj);
                curDocLimit++;
                curByteLimit += docObj.getDocument().length;
                minHeap.reHeapify(docObj);
                map.put(inputKey, docObj);
                for(String tempStr : wordSet){
                    trie.put(tempStr, docObj);
                }
                return true;
            };
        } else {
            long repDocLUT = replacedNode.getLastUseTime();
            HashSet<String> replacedWordSet = new HashSet<>();
            byte[] replacedUncompDoc = makeBigger(replacedNode.getCompressionFormat(), docObj.getDocument());
            String replacedformattedStr = replacedUncompDoc.toString().toLowerCase().replaceAll("[^A-Za-z]", "");
            Scanner scanner5 = new Scanner(replacedformattedStr);
            while(scanner5.hasNext()){
                replacedWordSet.add(scanner5.next());
            }
            //Undoing a put if there is a node that is overwritten by puyt
            undo = (inputKey) -> {
                docObj.setLastUseTime(0);
                minHeap.reHeapify(docObj);
                minHeap.removeMin();
                curDocLimit--;
                curByteLimit -= docObj.getDocument().length;
                replacedNode.setLastUseTime(repDocLUT);
                minHeap.insert(replacedNode);
                curDocLimit++;
                curByteLimit += replacedNode.getDocument().length;
                isOverLimit();
                minHeap.reHeapify(replacedNode);
                map.put(replacedNode.getKey(), replacedNode);
                for(String tempStr : wordSet){
                    trie.delete(tempStr, docObj);
                }
                for(String tempStr : replacedWordSet){
                    trie.put(tempStr, replacedNode);
                }
                new RecycledNode(docObj);
               //isOverLimit();
                return recycleBin(replacedNode);
            };
            //redoing a put if there is a node that we origionally overwrote.
            redo = (key) -> {
                replacedNode.setLastUseTime(0);
                minHeap.reHeapify(replacedNode);
                minHeap.removeMin();
                curDocLimit--;
                curByteLimit -= replacedNode.getDocument().length;
                docObj.setLastUseTime(docObjLUT);
                minHeap.insert(docObj);
                curDocLimit++;
                curByteLimit += docObj.getDocument().length;
                isOverLimit();
                minHeap.reHeapify(docObj);
                map.put(docObj.getKey(), docObj);
                for(String tempStr : replacedWordSet){
                    trie.delete(tempStr, replacedNode);
                }
                for(String tempStr : wordSet){
                    trie.put(tempStr, docObj);
                }
                new RecycledNode(docObj);
                return recycleBin(docObj);
            };
        }
        //push the command/action onto the stack.
        Command newCmdNode = new Command(uri, undo, redo);
        doneStack.push(newCmdNode);
    }

    //the delete function that put uses to not use the action Delete method
    private int putDelete(URI uri) {
        DocumentImpl nullTester = map.get(uri);
        if(nullTester != null) {
            if (deleteDocument(uri)) {
                return nullTester.getDocumentHashCode();
            }
            else {
                return flagVariable;
            }
        }
        return flagVariable;
    }

    private HashMap<String, Integer> addToMap(String formattedStr) {
        Scanner scan = new Scanner(formattedStr);
        HashMap<String, Integer> wordList = new HashMap<>();
        while(scan.hasNext()) {
            String str = scan.next();
            if (wordList.get(str) != null) {
                int i = wordList.get(str);
                wordList.put(str, i++);
            } else {
                wordList.put(str, 1);
            }
        }
        return wordList;

    }

    //recycled nodes storing
    private boolean recycleBin(DocumentImpl recycledObj){
        RecycledNode current = topOfBin;

        while (current.prev != null) {
            if(current.docObj.getDocumentHashCode() != recycledObj.getDocumentHashCode()){
                current = current.prev;
            }
            else if (current.docObj.getDocumentHashCode() == recycledObj.getDocumentHashCode()){
                current.removeNode();
                return true;
            }
        }
        return false;
    }

    //deleting Documen from DS by URI 
    private boolean delete(URI uri) {
        if(uri == null){
            throw new IllegalArgumentException("NoNullValues");
        }
        DocumentImpl docObj = map.get(uri);
        if(docObj != null){
            map.put(uri, null);
            new RecycledNode(docObj);
            if(map.get(uri)==null) {
                return true;
            }
            else{
                return false;
            }

        }
        else  {
            return false;
        }
    }

    //Compression format switch statment for compression method calling.
    private byte[] makeSmall(DocumentStore.CompressionFormat format, byte[] input) {
        try {
            switch (format) {
                case ZIP:
                    return smallererArch("zip", input);

                case JAR:
                    return smallererArch("jar", input);

                case SEVENZ:
                    return szSmaller(input);

                case GZIP:
                    return smallererComp("gz", input);

                case BZIP2:
                    return smallererComp("bzip2", input);

                default:
                    throw new IllegalArgumentException();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (ArchiveException e) {
            e.printStackTrace();
        }
        return new byte[]{'0'};
    }

        //Dempression format switch statment for compression method calling.
    private byte[] makeBigger(DocumentStore.CompressionFormat format, byte[] input) {
        try {
            switch (format) {
                case ZIP:
                    return biggererArch("zip", input);

                case JAR:
                    return biggererArch("jar", input);

                case SEVENZ:
                    return szBigger(input);

                case GZIP:
                    return biggererComp("gz", input);

                case BZIP2:
                    return biggererComp("bzip2", input);

                default:
                    throw new IllegalArgumentException();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (ArchiveException e) {
            e.printStackTrace();
        }
        return new byte[]{'0'};
    }

    //Using the compresor Factory to compress all types of compression in the ACC functions. 
    //Takes input as a byte array input stream.
    private byte[] smallererComp(String formatType, byte[] input) {
        ByteArrayInputStream uncompressed = new ByteArrayInputStream(input);
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        CompressorOutputStream cos;
        try {
            cos = new CompressorStreamFactory().createCompressorOutputStream(formatType, compressed);
            IOUtils.copy(uncompressed, cos);
            cos.close();
        }
        catch (CompressorException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return compressed.toByteArray();
    }

    //Compressor Factory for all types of compression in the ACC functions. 
    //outputs input as a byte array stream.
    private byte[] biggererComp(String format, byte[] input) throws IOException, ArchiveException {
        ByteArrayInputStream compressed = new ByteArrayInputStream(input);
        ByteArrayOutputStream uncompressed = new ByteArrayOutputStream();
        CompressorInputStream dcos;
        try {
            dcos = new CompressorStreamFactory().createCompressorInputStream(format, compressed);
            IOUtils.copy(dcos, uncompressed);
            dcos.close();
        }
        catch (CompressorException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return uncompressed.toByteArray();
    }

    //Archive Factory to compress all types of Archive compressions in the ACC functions. 
    //Takes input as a byte array input stream.
    private byte[] smallererArch(String formatType, byte[] input) {
        ByteArrayInputStream uncompressed = new ByteArrayInputStream(input);
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        ArchiveOutputStream aos;
        try {
            aos = new ArchiveStreamFactory().createArchiveOutputStream(formatType, compressed);
            aos.putArchiveEntry(new ZipArchiveEntry(""));
            IOUtils.copy(uncompressed, aos);
            aos.closeArchiveEntry();
            aos.close();
        }
        catch (org.apache.commons.compress.archivers.ArchiveException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return compressed.toByteArray();
    }
    //Archive Factory for all types of Archive compressions in the ACC functions. 
    //outputs input as a byte array stream.
    private byte[] biggererArch(String format, byte[] input) {
        ByteArrayInputStream compressed = new ByteArrayInputStream(input);
        ByteArrayOutputStream uncompressed = new ByteArrayOutputStream();

        ArchiveInputStream decomp;
        try {
            decomp = new ArchiveStreamFactory().createArchiveInputStream(format, compressed);
            decomp.getNextEntry();
            IOUtils.copy(decomp, uncompressed);
            decomp.close();
        }
        catch (org.apache.commons.compress.archivers.ArchiveException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return uncompressed.toByteArray();
    }

    //method made specifically to compress 7zip files as the ACC factories did not apply to 7zip compression type.
    private byte[] szSmaller(byte[] input) throws ArchiveException, IOException {
        File tempFileIn, tempFileOut;
        tempFileIn = File.createTempFile("infile", "tmp", null);
        tempFileOut = File.createTempFile("outfile", "tmp", null);

        ByteArrayInputStream uncompressed = new ByteArrayInputStream(input);
        FileOutputStream compressed = new FileOutputStream(tempFileIn);
        IOUtils.copy(uncompressed, compressed);

        SevenZOutputFile sevenZOutput = new SevenZOutputFile(tempFileOut);
        try {
            sevenZOutput.putArchiveEntry(sevenZOutput.createArchiveEntry(tempFileIn, ""));
            sevenZOutput.write(input);
            sevenZOutput.closeArchiveEntry();
            sevenZOutput.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        byte[] foByte = Files.readAllBytes(tempFileOut.toPath());
        return foByte;
    }
    
    //method made specifically to decompress 7zip files as the ACC factories did not apply to 7zip compression type.
    private byte[] szBigger(byte[] input) throws ArchiveException, IOException {
        File tempFile = File.createTempFile("tempfile", "tmp", null);

        ByteArrayInputStream compressed = new ByteArrayInputStream(input);
        FileOutputStream uncompressed = new FileOutputStream(tempFile);
        IOUtils.copy(compressed, uncompressed);

        SevenZFile sevenZFile = new SevenZFile(tempFile);
        SevenZArchiveEntry entry = sevenZFile.getNextEntry();

        byte[] uncompBytes = new byte[(int) entry.getSize()];
        sevenZFile.read(uncompBytes);
        sevenZFile.close();
        return uncompBytes;
    }

    //deleting actions from the undo/redo stack
    public void delFromStack(URI uri) throws IllegalStateException {
        StackImpl<Command> tempStack = new StackImpl<>();

        Command tempCmd;

        while ((doneStack.size() != 0)) {
            if (doneStack.peek().getUri() != uri) {
                tempCmd = doneStack.pop();
                tempStack.push(tempCmd);
            }
            else if (doneStack.peek().getUri() == uri) {
                doneStack.pop();
                while (tempStack.peek() != null) {
                    tempCmd = tempStack.pop();
                    doneStack.push(tempCmd);
                }
                break;
            }
        }
        if(doneStack.peek() == null && doneStack.size() == 0) {
            while (tempStack.peek() != null) {
                tempCmd = tempStack.pop();
                doneStack.push(tempCmd);
                tempCmd.redo();
            }

        }
    }
    
    
    //main() test code.

    // public static void main(String[] args) {

    //     try {
    //         DocumentStoreImpl dsi = new DocumentStoreImpl();

    //         //create and add doc1
    //         String str1 = "this is doc#1";
    //         URI uri1 = new URI("http://www.yu.edu/doc1");
    //         ByteArrayInputStream bis = new ByteArrayInputStream(str1.getBytes());
    //         dsi.putDocument(bis, uri1);
    //         Thread.sleep(50);

    //         //create and add doc2
    //         String str2 = "this is doc#2";
    //         URI uri2 = new URI("http://www.yu.edu/doc2");
    //         bis = new ByteArrayInputStream(str2.getBytes());
    //         dsi.putDocument(bis, uri2);
    //         Thread.sleep(50);

    //         //create and add doc3
    //         String str3 = "this is doc#3";
    //         URI uri3 = new URI("http://www.yu.edu/doc3");
    //         bis = new ByteArrayInputStream(str3.getBytes());
    //         dsi.putDocument(bis, uri3);
    //         Thread.sleep(50);

    //         //create and add doc4
    //         String str4 = "this is doc#4";
    //         URI uri4 = new URI("http://www.yu.edu/doc4");
    //         bis = new ByteArrayInputStream(str4.getBytes());
    //         dsi.putDocument(bis, uri4);
    //         Thread.sleep(50);

    //         //this should push doc #1 and #2 out of memory, but 3 and 4 should remain
    //         dsi.setMaxDocumentCount(2);

    //     }
    //     catch (Exception e){
    //         throw new IllegalArgumentException("lol");
    //     }
    // }
}



