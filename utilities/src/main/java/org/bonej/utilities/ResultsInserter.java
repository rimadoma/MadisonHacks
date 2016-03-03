package org.bonej.utilities;

import com.google.common.base.Strings;
import ij.measure.ResultsTable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A wrapper class for ResultsTable used to insert measurements according to the
 * following policy:
 * 1)   If there are no rows with the given label, then add a new row.
 * 2)   If there are rows with the given label, but there is not a column
 * with the given heading, then add a column, and set its value on the first row
 * with the label.
 * 3)   If there are rows with the given label, and there's a column with the given heading,
 * then find the first row which has no value in   the column (Double.NaN), and add the new value there.
 * If there are no such rows, then add a new row.
 *
 * By default the class uses the instance returned by
 * ResultsTable.getResultsTable()
 *
 * @author Richard Domander
 * @author Michael Doube
 * @todo add behaviour for headless mode: ResultsTable.saveAs...?
 */
public class ResultsInserter {
    private static final String DEFAULT_RESULTS_TABLE_TITLE = "Results";
    private ResultsTable resultsTable;

    public ResultsInserter() {
        setResultsTable(ResultsTable.getResultsTable());
    }

    public ResultsTable getResultsTable() {
        return resultsTable;
    }

    /**
     * Sets the ResultsTable the ResultsInserter uses
     *
     * @param resultsTable The table where the values are inserted
     * @throws NullPointerException if resultsTable == null
     */
    public void setResultsTable(ResultsTable resultsTable) throws NullPointerException {
        checkNotNull(resultsTable, "The ResultsTable in ResultsInserter must not be set null");

        this.resultsTable = resultsTable;
        this.resultsTable.setNaNEmptyCells(true);
    }

    /**
     * Adds new data to the underlying ResultsTable according to the policy
     * described in @see ResultsInserter
     *
     * @param rowLabel           The row label of the new data
     * @param measurementHeading The column heading of the new data
     * @param measurementValue   The value of the new data
     * @throws IllegalArgumentException if either String argument is null or empty
     */
    public void setMeasurementInFirstFreeRow(String rowLabel, String measurementHeading, double measurementValue)
            throws IllegalArgumentException {
        checkArgument(!Strings.isNullOrEmpty(rowLabel), "Row label must not be null or empty");
        checkArgument(!Strings.isNullOrEmpty(measurementHeading), "Measurement heading must not be null or empty");

        int rowNumber = rowOfLabel(rowLabel);
        if (rowNumber < 0) {
            addNewRow(rowLabel, measurementHeading, measurementValue);
            return;
        }

        int columnNumber = resultsTable.getColumnIndex(measurementHeading);
        if (columnNumber == ResultsTable.COLUMN_NOT_FOUND) {
            resultsTable.setValue(measurementHeading, rowNumber, measurementValue);
            return;
        }

        int firstFreeDataRow = rowOfLabelWithNoColumnData(rowLabel, measurementHeading);
        if (firstFreeDataRow < 0) {
            addNewRow(rowLabel, measurementHeading, measurementValue);
            return;
        }

        resultsTable.setValue(measurementHeading, firstFreeDataRow, measurementValue);
    }

    public void showTable() {
        resultsTable.show(DEFAULT_RESULTS_TABLE_TITLE);
    }

    //region -- Helper methods --
    private void addNewRow(String label, String measurementTitle, double measurementValue) {
        resultsTable.incrementCounter();
        resultsTable.addLabel(label);
        resultsTable.addValue(measurementTitle, measurementValue);
    }

    /**
     * Searches the first row, which has the given label.
     *
     * @param label The label to be searched
     * @return Index of the row, or -1 if none of rows has the given label.
     */
    private int rowOfLabel(String label) {
        final int rows = resultsTable.getCounter();
        for (int row = 0; row < rows; row++) {
            String rowLabel = resultsTable.getLabel(row);
            if (label.equals(rowLabel)) {
                return row;
            }
        }

        return -1;
    }

    /**
     * Returns the number of the first row which has the given label and no data
     * in the given column
     *
     * @param label   The label of the row
     * @param heading The heading of the column
     * @return Index of the first row with no data, or -1 if there are no such
     * rows
     *
     * No data means that the value in the column is Double.NaN
     */
    private int rowOfLabelWithNoColumnData(String label, String heading) {
        final int rows = resultsTable.getCounter();
        for (int row = 0; row < rows; row++) {
            String rowLabel = resultsTable.getLabel(row);
            double columnValue = resultsTable.getValue(heading, row);
            if (label.equals(rowLabel) && Double.isNaN(columnValue)) {
                return row;
            }
        }

        return -1;
    }
    //endregion
}
