package lab.drop.data;

import lab.drop.Sugar;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A customizable matrix stringify function.
 */
public class MatrixPrinter implements Function<Matrix<?>, String> {
    private final String columnsDelimiter;
    private final String rowsDelimiter;
    private final String nullDefault;
    private final int cellLength;
    private final boolean hasHeaders;
    private final String[] headers;

    private MatrixPrinter(String columnsDelimiter, String rowsDelimiter, String nullDefault, int cellLength,
                          boolean hasHeaders, String... headers) {
        this.columnsDelimiter = Objects.requireNonNullElse(columnsDelimiter, " ");
        this.rowsDelimiter = Objects.requireNonNullElse(rowsDelimiter, "\n");
        this.nullDefault = Objects.requireNonNullElse(nullDefault, "");
        this.cellLength = cellLength;
        this.hasHeaders = hasHeaders;
        this.headers = headers;
    }

    /**
     * Creates a default printer of the matrix, equivalent to the <code>Matrix.toString()</code> method.
     */
    public static MatrixPrinter basic() {
        return new MatrixPrinter(null, null, null, -1, false);
    }

    /**
     * Creates a custom printer of the matrix, overriding the provided aspects of the prints.
     * @param columnsDelimiter The columns delimiter. If null, will use the default (space).
     * @param rowsDelimiter The rows delimiter. If null, will use the default (newline).
     * @param nullDefault The representation of null values. If null, will use the default (empty).
     * @param cellLength The cell length limit. Non-positive means no limit.
     * @return The matrix printer.
     */
    public static MatrixPrinter custom(String columnsDelimiter, String rowsDelimiter, String nullDefault,
                                       int cellLength) {
        return new MatrixPrinter(columnsDelimiter, rowsDelimiter, nullDefault, cellLength, false);
    }

    /**
     * Creates a table-like printer with optional headers.
     * @param headers The headers to use. If a matrix has more columns than the provided headers, the headers will be
     *                printed on the first columns. If a matrix has fewer columns than the provided headers, the
     *                additional headers will be printed with null columns.
     * @return The matrix printer.
     */
    public static MatrixPrinter table(String... headers) {
        return table(null, -1, headers);
    }

    /**
     * Creates a table-like printer with optional headers and cell length limit.
     * @param cellLength The cell length limit. Applies to the headers as well. Non-positive means no limit.
     * @param headers The headers to use. If a matrix has more columns than the provided headers, the headers will be
     *                printed on the first columns. If a matrix has fewer columns than the provided headers, the
     *                additional headers will be printed with null columns.
     * @return The matrix printer.
     */
    public static MatrixPrinter table(int cellLength, String... headers) {
        return table(null, cellLength, headers);
    }

    /**
     * Creates a table-like printer with optional headers and custom aspects of the prints.
     * @param nullDefault The representation of null values. Applies to the headers as well. If null, will use the
     *                    default (empty).
     * @param cellLength The cell length limit. Applies to the headers as well. Non-positive means no limit.
     * @param headers The headers to use. If a matrix has more columns than the provided headers, the headers will be
     *                printed on the first columns. If a matrix has fewer columns than the provided headers, the
     *                additional headers will be printed with null columns.
     * @return The matrix printer.
     */
    public static MatrixPrinter table(String nullDefault, int cellLength, String... headers) {
        return new MatrixPrinter(" | ", null, nullDefault, cellLength, true, headers);
    }

    @Override
    public String apply(Matrix<?> matrix) {
        if (hasHeaders) {
            var table = matrix.map(Sugar::cast);
            table.addRowBefore(0, headers);
            matrix = table;
        }
        Matrix<String> strings = new Matrix<>(matrix.size());
        Matrix<List<String>> additionalLines = new Matrix<>(matrix.size());
        boolean newlines = rowsDelimiter.contains("\n");
        int[] maxLength = new int[matrix.columns()];
        var matrixRef = matrix;
        matrix.getBlock().forEach((x, y) -> firstLines(strings, additionalLines, newlines, maxLength, matrixRef, x, y));
        if (newlines) {
            int addedTop = additionalLines(matrix, strings, additionalLines);
            strings.getBlock().forEach((x, y) -> {
                String string = strings.get(x, y);
                strings.set(x, y, string + " ".repeat(maxLength[x] - string.length()));
            });
            if (hasHeaders)
                strings.addRowAfter(addedTop, Arrays.stream(maxLength).mapToObj("-"::repeat).toArray(String[]::new));
        }
        var tableString =  strings.getRows().stream().map(row -> String.join(columnsDelimiter, row).stripTrailing())
                .collect(Collectors.joining(rowsDelimiter)).stripTrailing();
        if (tableString.isEmpty() || !hasHeaders)
            return tableString;
        return formatTable(tableString);
    }

    private void firstLines(Matrix<String> strings, Matrix<List<String>> additionalLines, boolean newlines,
                            int[] maxLength, Matrix<?> matrix, Integer x, Integer y) {
        String string = Objects.toString(matrix.get(x, y), nullDefault);
        var lines = new ArrayList<>((newlines ? string.lines() : Stream.of(string)).map(this::applyLength).toList());
        if (newlines)
            maxLength[x] = Math.max(maxLength[x], lines.stream().mapToInt(String::length).max().orElse(0));
        strings.set(x, y, lines.isEmpty() ? string : Sugar.removeFirst(lines));
        additionalLines.set(x, y, lines);
    }

    private String applyLength(String string) {
        if (string == null || cellLength <= 0 || string.length() <= cellLength)
            return string;
        if (cellLength < 4)
            return ".".repeat(cellLength);
        return string.substring(0, cellLength - 3) + "...";
    }

    private static int additionalLines(Matrix<?> matrix, Matrix<String> strings, Matrix<List<String>> additionalLines) {
        int addedRows = 0;
        int addedTop = 0;
        for (int y = 0; y < matrix.rows(); y++) {
            var additionalLinesRow = additionalLines.getRow(y);
            int linesToAdd = additionalLinesRow.stream().mapToInt(List::size).max().orElse(0);
            for (int i = 0; i < linesToAdd; i++) {
                int addedLine = i;
                strings.addRowAfter(y + addedRows++, additionalLinesRow.stream().map(lines ->
                        lines.size() > addedLine ? lines.get(addedLine) : "").toArray(String[]::new));
            }
            if (y == 0)
                addedTop = linesToAdd;
        }
        return addedTop;
    }

    private static String formatTable(String tableString) {
        var splitter = new StringBuilder();
        var splitters = new HashSet<String>();
        for (char c : tableString.toCharArray()) {
            if (c == '-' || c == ' ' || c == '|') {
                splitter.append(c);
                continue;
            }
            if (!splitter.isEmpty()) {
                splitters.add(splitter.toString());
                splitter = new StringBuilder();
            }
        }
        splitters.add(splitter.toString());
        var maxSplitter = splitters.stream().max(Comparator.comparing(String::length)).orElseThrow();
        return tableString.replace(maxSplitter, maxSplitter.replace("- |", "--|").replace("| -", "|--"));
    }
}
