package org.apache.jmeter.services;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;

import org.apache.jmeter.services.BufferedLine.Direction;

public class FastRandomAccessFile implements Closeable {
    
    static final int DEFAULT_CHAR_BUFFER_SIZE = 16;
    private final Charset encoding;
    private final BufferedLine line;
    private boolean isFirstLine = false;
    RandomAccessFile raf;
    byte[] buffer = new byte[DEFAULT_CHAR_BUFFER_SIZE];
    
    public FastRandomAccessFile(File f, Charset encoding) throws FileNotFoundException {
        raf = new RandomAccessFile(f, "r");
        line = new BufferedLine(encoding);
        this.encoding = encoding;
    }    
    
    public boolean isFirstLine() {
        return isFirstLine;
    }

    public void setFirstLine(boolean isFirstLine) {
        this.isFirstLine = isFirstLine;
    }
   
    public String readRandomLine(long randomPosition, boolean ignoreFirstLine) throws IOException {
        if (randomPosition > raf.length()) {
            throw new IOException("position is outside the file size");
        }
        line.clear();
        // Then seek to the random position
        raf.seek(randomPosition);
        for (;;) {
            int count = raf.read(buffer);
            if (count == -1) {
                raf.seek(0);
                isFirstLine = true;
                line.clear();
                count = raf.read(buffer);
            }
            if ( line.addByte(buffer, count)) {
                break;
            }
        }
        byte[] l = line.getLine();
        return new String(l, 0 , l.length, encoding);
    }

    @Override
    public void close() throws IOException {
       raf.close();
    }

    public long length() throws IOException {
        return raf.length();
    }
    
}
