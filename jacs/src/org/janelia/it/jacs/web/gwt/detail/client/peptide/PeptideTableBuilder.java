
package org.janelia.it.jacs.web.gwt.detail.client.peptide;

import org.janelia.it.jacs.web.gwt.common.client.ui.table.AbstractTableBuilder;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.SortableTable;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableCell;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.TableRow;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.NumericColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TableColumn;
import org.janelia.it.jacs.web.gwt.common.client.ui.table.columns.TextColumn;

import java.util.Iterator;
import java.util.List;

/**
 * Builds a SortableTable with the following standard columns:
 * <ol>
 * <li>Protein Accession</li>
 * <li>Protein Function</li>
 * <li>Gene Symbol</li>
 * <li>Gene Ontology</li>
 * <li>EC #</li>
 * <li>Length</li>
 * </ol>
 * Custom columns can be supplied to the constructor by the caller, and are inserted in the table after the
 * Protein Accession column.
 *
 * @author Michael Press
 */
public class PeptideTableBuilder extends AbstractTableBuilder {
    private static final String PROTEIN_ACC_HEADING = "Protein Accession";
    private static final String PROTEIN_FUNCTION_HEADING = "Protein Function";
    private static final String GENE_SYMBOL_HEADING = "Gene Symbol";
    private static final String GO_HEADING = "Gene Ontology";
    private static final String EC_NUM_HEADING = "EC #";
    private static final String LENGTH_HEADING = "Length";

    private static final int NUM_STANDARD_COLUMNS = 6;

    public PeptideTableBuilder(List<TableColumn> customCols) {
        super(customCols);
    }

    protected SortableTable createTable() {
        SortableTable table = new SortableTable();
        table.addColumn(new TextColumn(PROTEIN_ACC_HEADING));

        for (TableColumn col : getCustomColumns())
            table.addColumn(col);

        table.addColumn(new TextColumn(PROTEIN_FUNCTION_HEADING));
        table.addColumn(new TextColumn(GENE_SYMBOL_HEADING));
        table.addColumn(new TextColumn(GO_HEADING));
        table.addColumn(new TextColumn(EC_NUM_HEADING));
        table.addColumn(new NumericColumn(LENGTH_HEADING));

        return table;
    }

    public TableRow createTableRow(List<TableCell> stockCellList, List<TableCell> customCells) {
        Iterator<TableCell> stockCells = stockCellList.iterator();
        TableRow row = new TableRow();

        // first stock col
        int col = 0;
        row.setValue(col++, stockCells.next());

        // Set custom cols
        //TODO: verify that customCells.size() == numCustomCols()
        for (TableCell cell : customCells)
            row.setValue(col++, cell);

        // remainder stock cols
        while (stockCells.hasNext())
            row.setValue(col++, stockCells.next());

        return row;
    }

    public String[][] getSortOptions(String[][] customSortOptions) {
        int numCustomSortOptions = (customSortOptions == null) ? 0 : customSortOptions.length;
        String[][] sortOptions = new String[NUM_STANDARD_COLUMNS + numCustomSortOptions][];

        int i = 0;
        sortOptions[i++] = new String[]{"proteinAcc", PROTEIN_ACC_HEADING};
        if (customSortOptions != null)
            for (String[] customOption : customSortOptions)
                sortOptions[i++] = customOption;
        sortOptions[i++] = new String[]{"protein_function ", PROTEIN_FUNCTION_HEADING};
        sortOptions[i++] = new String[]{"gene_symbol", GENE_SYMBOL_HEADING};
        sortOptions[i++] = new String[]{"gene_ontology", GO_HEADING};
        sortOptions[i++] = new String[]{"enzyme_commission", EC_NUM_HEADING};
        sortOptions[i] = new String[]{"length", LENGTH_HEADING};

        return sortOptions;
    }
}