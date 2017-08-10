/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tudarmstadt.ukp.experiments.pipeline.utils;

import com.google.common.collect.Table;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ivan Habernal
 */
public class TableUtils
{
    /**
     * Converts Guava table to a CSV table
     *
     * @param table                   table
     * @param csvFormat               CSV format
     * @param missingValuePlaceholder print if a value is missing (empty string by default)
     * @param <T>                     object type (string)
     * @return table
     * @throws IOException exception
     */
    public static <T> String tableToCsv(Table<String, String, T> table, CSVFormat csvFormat,
            String missingValuePlaceholder)
            throws IOException
    {
        StringWriter sw = new StringWriter();
        CSVPrinter printer = new CSVPrinter(sw, csvFormat);

        List<String> firstRow = new ArrayList<>();
        firstRow.add(" ");
        firstRow.addAll(table.columnKeySet());
        printer.printRecord(firstRow);

        for (String rowKey : table.rowKeySet()) {
            printer.print(rowKey);
            for (String columnKey : table.columnKeySet()) {
                T value = table.get(rowKey, columnKey);

                if (value == null) {
                    printer.print(missingValuePlaceholder);
                }
                else {
                    printer.print(value);
                }
            }
            printer.println();
        }

        printer.close();

        return sw.toString();
    }

    /**
     * Converts Guava table to a CSV table
     *
     * @param table     table
     * @param csvFormat CSV format
     * @param <T>       object type (string)
     * @return table
     * @throws IOException exception
     */
    public static <T> String tableToCsv(Table<String, String, T> table, CSVFormat csvFormat)
            throws IOException
    {
        return tableToCsv(table, CSVFormat.TDF, null);
    }

    /**
     * Converts Guava table to a CSV table; default tab-separated
     *
     * @param table table
     * @param <T>   object type (string)
     * @return table
     * @throws IOException exception
     */
    public static <T> String tableToCsv(Table<String, String, T> table)
            throws IOException
    {
        return tableToCsv(table, CSVFormat.TDF);
    }

}
