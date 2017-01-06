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

import static java.lang.String.format;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.swing.RowSorter;
import javax.swing.SortOrder;

public class ObjectTableSorter extends RowSorter<ObjectTableModel> {

    /**
     * View row with model mapping. All data relates to model.
     */
    public class Row {
        private int index;

        protected Row(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public Object getValue() {
            return getModel().getObjectList().get(getIndex());
        }

        public Object getValueAt(int column) {
            return getModel().getValueAt(getIndex(), column);
        }
    }

    protected class PreserveLastRowComparator implements Comparator<Row> {
        @Override
        public int compare(Row o1, Row o2) {
            int lastIndex = model.getRowCount() - 1;
            if (o1.getIndex() >= lastIndex || o2.getIndex() >= lastIndex) {
                return o1.getIndex() - o2.getIndex();
            }
            return 0;
        }
    }

    private ObjectTableModel model;
    private SortKey sortkey;

    private Comparator<Row> comparator  = null;
    private ArrayList<Row>  viewToModel = new ArrayList<>();
    private int[]           modelToView = new int[0];

    private Comparator<Row>  primaryComparator = null;
    private Comparator<?>[]  valueComparators;
    private Comparator<Row>  fallbackComparator;

    public ObjectTableSorter(ObjectTableModel model) {
        this.model = model;

        this.valueComparators = new Comparator<?>[this.model.getColumnCount()];
        IntStream.range(0, this.valueComparators.length).forEach(i -> this.setValueComparator(i, null));

        setFallbackComparator(null);
    }

    /**
     * Comparator used prior to sorted columns.
     */
    public Comparator<Row> getPrimaryComparator() {
        return primaryComparator;
    }

    /**
     * Comparator used on sorted columns.
     */
    public Comparator<?> getValueComparator(int column) {
        return valueComparators[column];
    }

    /**
     * Comparator if all sorted columns matches. Defaults to model index comparison.
     */
    public Comparator<Row> getFallbackComparator() {
        return fallbackComparator;
    }

    /**
     * Sets {@link #getPrimaryComparator() primary comparator} to one that don't sort last row.
     * @return <code>this</code>
     */
    public ObjectTableSorter fixLastRow() {
        primaryComparator = new PreserveLastRowComparator();
        return this;
    }

    /**
     * Assign comparator to given column, if <code>null</code> a {@link #getDefaultComparator(int) default one} is used instead.
     * @param column Model column index.
     * @param comparator Column value comparator.
     * @return <code>this</code>
     */
    public ObjectTableSorter setValueComparator(int column, Comparator<?> comparator) {
        if (comparator == null) {
            comparator = getDefaultComparator(column);
        }
        valueComparators[column] = comparator;
        return this;
    }

    /**
     * Builds a default comparator based on model column class. {@link Collator#getInstance()} for {@link String},
     * {@link Comparator#naturalOrder() natural order} for {@link Comparable}, no sort support for others.
     * @param column Model column index.
     */
    protected Comparator<?> getDefaultComparator(int column) {
        Class<?> columnClass = model.getColumnClass(column);
        if (columnClass == null) {
            return null;
        }
        if (columnClass == String.class) {
            return Comparator.nullsFirst(Collator.getInstance());
        }
        if (Comparable.class.isAssignableFrom(columnClass)) {
            return Comparator.nullsFirst(Comparator.naturalOrder());
        }
        return null;
    }

    /**
     * Sets a fallback comparator (defaults to model index comparison) if none {@link #getPrimaryComparator() primary}, neither {@link #getValueComparator(int) column value comparators} can make differences between two rows.
     * @return <code>this</code>
     */
    public ObjectTableSorter setFallbackComparator(Comparator<Row> comparator) {
        if (comparator == null) {
            comparator = Comparator.comparingInt(Row::getIndex);
        }
        fallbackComparator = comparator;
        return this;
    }

    @Override
    public ObjectTableModel getModel() {
        return model;
    }

    @Override
    public void toggleSortOrder(int column) {
        SortOrder newOrder =
                sortkey == null || sortkey.getColumn() != column || sortkey.getSortOrder() != SortOrder.ASCENDING
                    ? SortOrder.ASCENDING
                    : SortOrder.DESCENDING
        ;
        setSortKey(new SortKey(column, newOrder));
    }

    @Override
    public int convertRowIndexToModel(int index) {
        if (viewToModel.isEmpty()) {
            return index;
        }
        return viewToModel.get(index).getIndex();
    }

    @Override
    public int convertRowIndexToView(int index) {
        if (modelToView.length == 0) {
            return index;
        }
        return modelToView[index];
    }

    @Override
    public void setSortKeys(List<? extends SortKey> keys) {
        switch (keys.size()) {
            case 0:
                setSortKey(null);
                break;
            case 1:
                setSortKey(keys.get(0));
                break;
            default:
                throw new IllegalArgumentException("Only one column can be sorted");
        }
    }

    public void setSortKey(SortKey sortkey) {
        if (Objects.equals(this.sortkey, sortkey)) {
            return;
        }

        if (sortkey != null) {
            int column = sortkey.getColumn();
            Comparator<?> comparator = valueComparators[column];
            if (comparator == null) {
                throw new IllegalArgumentException(format("Can't sort column %s, it is mapped to type %s and this one have no natural order. So an explicit one must be specified", column, model.getColumnClass(column)));
            }
        }
        this.sortkey    = sortkey;
        this.comparator = null;
        allRowsChanged();
    }

    @Override
    public List<? extends SortKey> getSortKeys() {
        return isSorted() ? Collections.singletonList(sortkey) : Collections.emptyList();
    }

    @Override
    public int getViewRowCount() {
        return getModelRowCount();
    }

    @Override
    public int getModelRowCount() {
        return model.getRowCount();
    }

    @Override
    public void modelStructureChanged() {
        setSortKey(null);
        allRowsChanged();
    }

    @Override
    public void allRowsChanged() {
        viewToModel.clear();
        modelToView = new int[0];
        if (isSorted()) {
            sort();
        }
    }

    @Override
    public void rowsInserted(int firstRow, int endRow) {
        rowsChanged(firstRow, endRow, false, true);
    }

    @Override
    public void rowsDeleted(int firstRow, int endRow) {
        rowsChanged(firstRow, endRow, true, false);
    }

    @Override
    public void rowsUpdated(int firstRow, int endRow) {
        rowsChanged(firstRow, endRow, true, true);
    }

    protected void rowsChanged(int firstRow, int endRow, boolean deleted, boolean inserted) {
        if (isSorted()) {
            sort();
        }
    }

    @Override
    public void rowsUpdated(int firstRow, int endRow, int column) {
        if (isSorted(column)) {
            rowsUpdated(firstRow, endRow);
        }
    }

    private boolean isSorted(int column) {
        return isSorted() && sortkey.getColumn() == column && sortkey.getSortOrder() != SortOrder.UNSORTED;
    }

    private boolean isSorted() {
        return sortkey != null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Comparator<Row> getComparatorFromSortKey(SortKey sortkey) {
        Comparator comparator = getValueComparator(sortkey.getColumn());
        if (sortkey.getSortOrder() == SortOrder.DESCENDING) {
            comparator = comparator.reversed();
        }
        Function<Row,Object> getValueAt = (Row row) -> row.getValueAt(sortkey.getColumn());
        return Comparator.comparing(getValueAt, comparator);
    }

    protected void sort() {
        if (comparator == null) {
            comparator = Stream.concat(
                    Stream.concat(
                            getPrimaryComparator() != null ? Stream.of(getPrimaryComparator()) : Stream.<Comparator<Row>>empty(),
                            getSortKeys().stream().filter(sk -> sk != null && sk.getSortOrder() != SortOrder.UNSORTED).map(this::getComparatorFromSortKey)
                    ),
                    Stream.of(getFallbackComparator())
            ).reduce(comparator, (result, current) -> result != null ? result.thenComparing(current) : current);
        }

        viewToModel.clear();
        viewToModel.ensureCapacity(model.getRowCount());
        IntStream.range(0, model.getRowCount()).mapToObj(i -> new Row(i)).forEach(viewToModel::add);
        Collections.sort(viewToModel, comparator);

        updateModelToView();
    }

    protected void updateModelToView() {
        modelToView = new int[viewToModel.size()];
        IntStream.range(0, viewToModel.size()).forEach(viewIndex -> modelToView[viewToModel.get(viewIndex).getIndex()] = viewIndex);
    }
}
