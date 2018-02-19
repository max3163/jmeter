package org.apache.jmeter.services;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BufferedLine {

    public enum Direction {
        Backward, Forward
    }

    private List<byte[]> line = new ArrayList<>();
    // A probable sequence of byte corresponding of a eol
    private byte[] lastSequence;
    // A mark corresponding of a pointer to the last byte which don't contain a
    // eol sequence
    private int endMark = 0;
    // A mark corresponding of a pointer to the first byte which don't contain a
    // eol sequence
    private int startMark = 0;
    // Array of all sequences of eol
    private final List<byte[]> newLineSequences;
    // eol length
    private int eolSize;

    public BufferedLine(Charset encoding) {
        newLineSequences = new ArrayList<byte[]>(
                Arrays.asList("\r\n".getBytes(encoding), "\n".getBytes(encoding), "\r".getBytes(encoding)));
        lastSequence = new byte[0];
    }

    /**
     * Add a packet of byte in the right order to make a line
     * 
     * @param buffer
     *            to add on line until the eol marker
     * @param length
     *            of data of the buffer
     * @index indicate the sense of the read to find eol and how to add data on
     *        the line
     * 
     * @return true or false if the buffer contains a eol according to the
     *         encoding
     */
    boolean addByte(byte[] buffer, int length) {

        byte[] data;
        boolean stopReading;
        
        if (lastSequence.length != 0) {
            data = new byte[length + lastSequence.length];
            startMark = lastSequence.length;
            // Forward => add last sequence at the start of the data
            System.arraycopy(lastSequence, 0, data, 0, lastSequence.length);
            System.arraycopy(buffer, 0, data, lastSequence.length, length);
            // reset lastSequence
            lastSequence = new byte[0];
        } else {
            data = new byte[length];
            System.arraycopy(buffer, 0, data, 0, length);
        }
        // check EOL backward to find the start of the line
        endMark = length;
        stopReading = findNextEOL(data);
        line.add(Arrays.copyOfRange(data, startMark , endMark));
        return stopReading;
    }

    public byte[] getLine() {
        byte[] data = new byte[line.size() * FastRandomAccessFile.DEFAULT_CHAR_BUFFER_SIZE];
        int pos = 0;
        for (byte[] part : line) {
            System.arraycopy(part, 0, data, pos, part.length);
            pos += part.length;
        }
        return data;
    }

    void clear() {
        lastSequence = new byte[0];
        endMark = 0;
        line.clear();
    }

    public boolean findNextEOL(byte[] buffer) {
        boolean isEOL = false;
        for (byte[] eolSeq : newLineSequences) {
            isEOL = isForwardMatch(buffer, buffer.length, eolSeq);
            if (isEOL) {
                return isEOL;
            }
        }
        return isEOL;
    }

    public long getMark() {
        return endMark + 1;
    }

    public boolean isForwardMatch(byte[] array, int size, byte[] pattern) {
        byte[] probSeq = new byte[pattern.length];
        int k = 0;
        loop: for (int i = 0; i <= size; i++) {
            endMark = startMark + i;
            for (int j = 0; j < Math.min(size - i, pattern.length); j++) {
                if (array[i + j] != pattern[j]) {
                    k = 0;
                    continue loop;
                } else {
                    probSeq[k++] = array[i + j];
                }
            }
            if (endMark == 0 && line.isEmpty()) {
                startMark = i + 1;
                continue loop;
            }
            // If we found a larger sequence of probable eol
            // resize it and copy into lastSequence
            if (k != 0 && k > lastSequence.length) {
                lastSequence = Arrays.copyOf(probSeq, k);
            }
            return ( k >= lastSequence.length);
        }
        endMark = size;
        return false;
    }
}
