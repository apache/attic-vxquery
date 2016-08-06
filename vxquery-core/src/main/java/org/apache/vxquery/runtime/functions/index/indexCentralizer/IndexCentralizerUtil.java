/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.vxquery.runtime.functions.index.indexCentralizer;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class for maintaining the centralized index information file.
 *
 * Index centralization procedure.
 * User can specify the collection directory in local.xml file.
 * Then all the indexes will be created in that particular directory in sub-folders corresponding to collections.
 * There will be a single xml file, located in the directory specified in local.xml, which contains all information
 * about what existing indexes.
 * This class can be used to read, add, delete, modify the entries and write the file back to the disk.
 */
public class IndexCentralizerUtil {

    private String INDEX_LOCATION;
    private String LOCATION = "./vxquery-server/src/main/resources/conf/local.xml";
    private final String FILE_NAME = "VXQuery-Index-Directory.xml";
    private ConcurrentHashMap<String, IndexLocator> indexCollectionMap = new ConcurrentHashMap<>();
    private final File XML_FILE;
    private final List<String> collections = new ArrayList<>();

    public IndexCentralizerUtil() throws IOException, SAXException, ParserConfigurationException {
        XML_FILE = new File(getIndexLocation()+"/"+FILE_NAME);
    }

    /**
     * Get the index directory containing index of the given collection
     * @param collection : Collection folder
     * @return Index folder.
     */
    public String getIndexForCollection(String collection) {
        return this.indexCollectionMap.get(collection).getIndex();
    }

    /**
     * Put the index location corresponding to given collection.
     * Index location is created by using the last 100 characters of collection.
     * @param collection : Collection directory
     */
    public String putIndexForCollection(String collection) {
        int length = collection.replaceAll("/", "").length();
        String index = collection.replaceAll("/", "");
        index = INDEX_LOCATION + "/" + (length > 100 ? index.substring(length - 100) : index);
        System.out.println(index);
        IndexLocator il = new IndexLocator();
        il.setCollection(collection);
        il.setIndex(index);
        this.indexCollectionMap.put(collection, il);
        return index;
    }

    /**
     * Remove the entry for given collection directory.
     * @param collection : Collection directory
     */
    public void deleteEntryForCollection(String collection) {
        this.indexCollectionMap.remove(collection);
    }

    /**
     * Get all collections which have index created.
     * @return List of collection directories.
     */
    public List<String> getAllCollections () {
        for (String s : collections) {
            System.out.println(s);
        }
        return this.collections;
    }

    /**
     * Get the collection location which is specified in local.xml file.
     * @return : Collection location
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    private String getIndexLocation() throws ParserConfigurationException, IOException, SAXException {
        File f = new File(LOCATION);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(f);

        doc.getDocumentElement().normalize();

        NodeList nList = doc.getElementsByTagName("indexDirectory");
        Node nNode = nList.item(0);
        this.INDEX_LOCATION = nNode.getTextContent();
        return INDEX_LOCATION;
    }

    /**
     * Read the collection, index directory file and populate the HashMap.
     * @throws JAXBException
     */
    public void readIndexDirectory() throws JAXBException {
        if (this.XML_FILE.exists()) {
            JAXBContext jaxbContext = JAXBContext.newInstance(IndexDirectory.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            IndexDirectory indexDirectory = (IndexDirectory) jaxbUnmarshaller.unmarshal(this.XML_FILE);

            for (IndexLocator il : indexDirectory.getDirectory()) {
                this.indexCollectionMap.put(il.getCollection(), il);
                this.collections.add(il.getCollection());
            }
        }

    }

    /**
     * Write back the contents of the HashMap to the file.
     * @throws JAXBException
     * @throws FileNotFoundException
     */
    public void writeIndexDirectory() throws JAXBException, FileNotFoundException {
        IndexDirectory id = new IndexDirectory();
        List<IndexLocator> indexLocators = new ArrayList<>(this.indexCollectionMap.values());
        id.setDirectory(indexLocators);

        FileOutputStream fileOutputStream = new FileOutputStream(this.XML_FILE);
        JAXBContext context = JAXBContext.newInstance(IndexDirectory.class);
        Marshaller jaxbMarshaller = context.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.marshal(id, fileOutputStream);
    }

}
