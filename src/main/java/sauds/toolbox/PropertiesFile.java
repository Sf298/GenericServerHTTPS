/*
 * To change this license header, choose License Headers in Project PropertiesFile.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sauds.toolbox;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Saud
 */
public class PropertiesFile {
    
    private final File file;
    private String separator;
    private TreeMap<String, String> map = new TreeMap();
    
    public PropertiesFile(File f) {
        this(f, " - ");
    }
    
    public PropertiesFile(File f, String separator) {
        file = f;
        this.separator = separator;
    }
    
    public PropertiesFile(PropertiesFile p) {
        file = p.file;
        separator = p.separator;
        map = new TreeMap(map);
    }
    
    public void cloneDataFrom(PropertiesFile p) {
        p.separator = separator;
        p.map = new TreeMap(map);
    }
    
    
    public String get(String key) {
		String passHash = map.get(key);
		if(passHash == null) return null;
        return passHash.replaceAll("<!thisIsNewLine!>", "\n");
    }
    public int getAsInt(String key) {
        return Integer.parseInt(map.get(key));
    }
    public double getAsDouble(String key) {
        return Double.parseDouble(map.get(key));
    }
    
    public TreeMap<String, String> getMap() {
        return map;
    }
    
    public void put(String key, String value) {
		if(value==null) value = "";
        map.put(key, value.replaceAll("\n", "<!thisIsNewLine!>"));
    }
    
    public String remove(String key) {
        return map.remove(key);
    }
    
    public boolean hasKey(String key) {
        return map.containsKey(key);
    }
    
    
    public boolean fileExists() {
        return file.exists();
    }
    
    public void load() {
        load(null);
    }
    public void load(String encryptionKey) {
        if(!file.exists()) {
            return;
        }
        try {
            String str = new String ( Files.readAllBytes(file.toPath()));
            if(encryptionKey == null) {
                parseString(str);
            } else {
                parseString(Encryptor.decrypt(str, encryptionKey));
            }
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PropertiesFile.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PropertiesFile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private void parseString(String str) {
        Scanner in = new Scanner(str);
        while(in.hasNextLine()) {
            String line = in.nextLine();
            if(line.contains(separator)) {
                String[] lineArr = line.split(separator, 2);
                map.put(lineArr[0], lineArr[1]);
            }
        }
    }
    
    public void save() {
        save(null);
    }
    public void save(String encryptionKey) {
        if(!file.exists()) {
            file.getParentFile().mkdirs();
        }
        try {
            FileWriter fw = new FileWriter(file);
            if(encryptionKey == null) {
                fw.write(toString());
            } else {
                fw.write(Encryptor.encrypt(toString(), encryptionKey));
            }
            fw.flush();
            fw.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PropertiesFile.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PropertiesFile.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(String key : map.keySet()) {
            sb.append(key)
                    .append(separator)
                    .append(map.get(key))
                    .append("\n");
        }
        return sb.toString();
    }
}
