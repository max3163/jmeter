package org.apache.jmeter.services;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.junit.Test;

public class FileServerTest {
    
    FileServer server = FileServer.getFileServer();

    @Test
    public void randomAcess() throws IOException {
//        String line = "";
//        
//        line = server.reserveFile("C:/temp/test.csv", "UTF-8", "test", true);
//        System.out.println("header -> " + line);
//        line = server.readLine("test", true);
//        System.out.println("1 -> " + line);
//        line = server.readRandomLine("test", true);
//        System.out.println("2 -> " + line);
//        System.out.println("--------------------");
        
        FastRandomAccessFile fraf = new FastRandomAccessFile(new File("C:/temp/test.csv"), Charset.forName("UTF-16LE"));
        /*
        for (int i = 0 ; i < fraf.length() ; i++) {
            System.out.println("Index => ------------------" + i);
            System.out.println( fraf.readRandomLine(i) );
            if ( fraf.isFirstLine() ) {
                System.out.println( fraf.readLine() );
            }
            System.out.println("--------------------");
        }
        */
        int i = 16;
        System.out.println("Index => ------------------" + i);
        System.out.println( fraf.readRandomLine(i, true) );
        System.out.println("--------------------");
        fraf.close();
    }

}
