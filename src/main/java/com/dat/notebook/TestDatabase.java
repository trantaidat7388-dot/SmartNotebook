package com.dat.notebook;

import com.dat.notebook.util.DatabaseTestUtil;

/**
 * Test Database Connection và Queries
 * 
 * Chạy qua Maven để có đầy đủ dependencies:
 * mvn exec:java -Dexec.mainClass="com.dat.notebook.TestDatabase"
 */
public class TestDatabase {
    
    public static void main(String[] args) {
        System.out.println("\n🚀 STARTING DATABASE TESTS VIA MAVEN\n");
        
        // Run comprehensive tests
        DatabaseTestUtil.runAllTests();
        
        System.out.println("\n✅ ALL TESTS COMPLETED!\n");
    }
}
