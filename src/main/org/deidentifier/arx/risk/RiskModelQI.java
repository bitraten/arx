/*
 * ARX: Powerful Data Anonymization
 * Copyright 2012 - 2015 Florian Kohlmayer, Fabian Prasser
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.deidentifier.arx.risk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.deidentifier.arx.DataHandle;
import org.deidentifier.arx.DataHandleInput;
import org.deidentifier.arx.DataHandleOutput;
import org.deidentifier.arx.DataHandleSubset;


public class RiskModelQI {

    /** Fields */
    private final DataHandle handle;
    /** Fields */
    private final int[] cols;
    /** Fields */
    private int[][] data;
    /** Fields */
    private int[][] rows;

    /**
     * Creates a new instance
     * 
     * @param handle
     * @param identifiers
     */
    RiskModelQI(final DataHandle handle, Set<String> identifiers) {
        this.handle = handle;
        
        cols = new int[identifiers.size()];
        int index = 0;
        for (final String attribute : identifiers) {
        	cols[index++] = handle.getColumnIndexOf(attribute);
        }
        data = getData();
        rows = getCols(cols);
    }

    protected int[][] getData(){
    	int data[][] = new int[handle.getNumRows()][handle.getNumColumns()];
    	
    	if(handle instanceof DataHandleInput) {
    		data = ((DataHandleInput) handle).data;
    	}
    	else if( ((DataHandleSubset) handle).getSource() instanceof DataHandleInput) {
    		data = ((DataHandleInput) ((DataHandleSubset) handle).getSource()).data;
    	}
    	else if (((DataHandleSubset) handle).getSource() instanceof DataHandleOutput) {
    		data = ((DataHandleOutput) ((DataHandleSubset) handle).getSource()).outputGeneralized.getArray();
    	}
    	else {
    		data = null;
    	}
    	return data;
    }

    /**
     * Retrieve an Array of rows with only the given columns.
     *
     * @param cols
     * @return
     */
    protected int[][] getCols(int[] cols){
        int rows[][] = new int[handle.getNumRows()][cols.length];
        
        for(int i=0; i < handle.getNumRows(); i++){
            int[] row = new int[cols.length];
            for(int j=0; j < cols.length; j++) {
            		row[j] = data[i][cols[j]];
            }
            rows[i] = row;
        }
        return rows;
    }
    
    public double getAlphaDistinct() {
    	HashSet<ArrayList<Integer>> distinctRows = new HashSet<ArrayList<Integer>>();
    	
        // Convert to Set, so we get distinct values
    	for(int[] row : rows) {
    		ArrayList<Integer> rowList = new ArrayList<Integer>(row.length);
    		for(int i : row) {
    			rowList.add(i);
    		}
    		distinctRows.add(rowList);
    	}
        return (float) distinctRows.size()/handle.getNumRows();
    }
    
    public double getAlphaSeparation() {
    	int separatedRows = 0;
    	
    	// Total number of tuple pairs
    	int numTuples = (handle.getNumRows()*(handle.getNumRows()-1))/2;

        // Iterate over all rows
        for(int i = 0; i < rows.length; i++) {
        	
            // Compare row with each row below the current one.
            for(int j = i+1; j < rows.length; j++) {
            	for(int k = 0; k < rows[i].length; k++) {
            		if(rows[i][k] != rows[j][k]) {
            			separatedRows++;
            			break;
            		}
            	}
            }
        }
        return (float) separatedRows/numTuples;
    }
}
