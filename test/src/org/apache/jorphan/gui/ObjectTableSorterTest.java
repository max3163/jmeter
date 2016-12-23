/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jorphan.gui;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;

import org.apache.jorphan.reflect.Functor;
import org.junit.Before;
import org.junit.Test;

public class ObjectTableSorterTest {
    ObjectTableModel  model;
    ObjectTableSorter sorter;



    @Test
    public void noSorting() {
        assertEquals(asList(b2(), a3(), d4(), c1()), actual());
    }

    @Test
    public void sortKeyAscending() {
        sorter.setSortKey(new SortKey(0, SortOrder.ASCENDING));
        assertEquals(asList(a3(), b2(), c1(), d4()), actual());
    }

    @Test
    public void sortKeyDescending() {
        sorter.setSortKey(new SortKey(0, SortOrder.DESCENDING));
        assertEquals(asList(d4(), c1(), b2(), a3()), actual());
    }

    @Test
    public void sortValueAscending() {
        sorter.setSortKey(new SortKey(1, SortOrder.ASCENDING));
        assertEquals(asList(c1(), b2(), a3(), d4()), actual());
    }

    @Test
    public void sortValueDescending() {
        sorter.setSortKey(new SortKey(1, SortOrder.DESCENDING));
        assertEquals(asList(d4(), a3(), b2(), c1()), actual());
    }


    @Before
    public void createModelAndSorter() {
        String[] headers         = { "key", "value" };
        Functor[] readFunctors   = { new Functor("getKey"), new Functor("getValue") };
        Functor[] writeFunctors  = { null, null };
        Class<?>[] editorClasses = { String.class, Integer.class };
        model                    = new ObjectTableModel(headers, readFunctors, writeFunctors, editorClasses);
        sorter                   = new ObjectTableSorter(model);
        List<Entry<String,Integer>> data = asList(b2(), a3(), d4(), c1());
        data.forEach(model::addRow);
    }



    @SuppressWarnings("unchecked")
    protected List<Entry<String,Integer>> actual() {
        return IntStream
                .range(0, sorter.getViewRowCount())
                .map(sorter::convertRowIndexToModel)
                .mapToObj(modelIndex -> (Entry<String,Integer>) sorter.getModel().getObjectList().get(modelIndex))
                .collect(Collectors.toList())
       ;
    }

    protected SimpleImmutableEntry<String, Integer> d4() {
        return new AbstractMap.SimpleImmutableEntry<>("d",  4);
    }

    protected SimpleImmutableEntry<String, Integer> c1() {
        return new AbstractMap.SimpleImmutableEntry<>("c",  1);
    }

    protected SimpleImmutableEntry<String, Integer> b2() {
        return new AbstractMap.SimpleImmutableEntry<>("b",  2);
    }

    protected SimpleImmutableEntry<String, Integer> a3() {
        return new AbstractMap.SimpleImmutableEntry<>("a",  3);
    }
}
