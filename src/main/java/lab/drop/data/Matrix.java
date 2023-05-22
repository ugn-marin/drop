package lab.drop.data;

import lab.drop.Sugar;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A matrix of flexible size, supporting spreadsheet-like manipulation of cells, rows and columns.
 * @param <T> The type of elements in the matrix.
 */
public class Matrix<T> {
    private final List<List<T>> content;
    private final MatrixPrinter printer = MatrixPrinter.basic();

    /**
     * Constructs an empty matrix.
     */
    public Matrix() {
        this(0, 0);
    }

    /**
     * Constructs a matrix of the specified size.
     * @param size The matrix size, where X means columns and Y means rows.
     * @throws IndexOutOfBoundsException If a coordinate is negative, or only one of the coordinates is zero.
     */
    public Matrix(Coordinates size) {
        this(size.getX(), size.getY());
    }

    /**
     * Constructs a matrix of the specified size.
     * @param x The columns number.
     * @param y The rows number.
     * @throws IndexOutOfBoundsException If a coordinate is negative, or only one of the coordinates is zero.
     */
    public Matrix(int x, int y) {
        if (validateNegative(x) * validateNegative(y) == 0 && x != y)
            throw new IndexOutOfBoundsException("The matrix size can't be zero in one dimension.");
        content = Sugar.fill(x, () -> Sugar.fill(y));
    }

    /**
     * Constructs a matrix containing the data from the provided two-dimensional array.
     * @param data The matrix data.
     * @throws IndexOutOfBoundsException If rows length is zero.
     */
    public Matrix(T[][] data) {
        this();
        for (var row : data) {
            if (row.length == 0)
                throw new IndexOutOfBoundsException("The matrix size can't be zero in one dimension.");
            addRow(row);
        }
    }

    /**
     * Constructs a matrix containing the data from the provided matrix.
     * @param matrix The matrix.
     */
    public Matrix(Matrix<T> matrix) {
        this(new ArrayList<>(matrix.content.stream().map(ArrayList::new).toList()));
    }

    private Matrix(List<List<T>> content) {
        this.content = content;
    }

    /**
     * Returns an unmodifiable copy of the matrix.
     */
    public static <T> Matrix<T> unmodifiableCopy(Matrix<T> matrix) {
        return new Matrix<>(matrix.content.stream().map(Sugar::unmodifiableCopy).toList());
    }

    /**
     * Returns a matrix consisting of the results of applying the given function to the elements of this matrix.
     */
    public <O> Matrix<O> map(Function<T, O> function) {
        Objects.requireNonNull(function, "Function is null.");
        return new Matrix<>(new ArrayList<>(content.stream().map(column ->
                new ArrayList<>(column.stream().map(function).toList())).toList()));
    }

    /**
     * Returns true if the matrix size is [0, 0].
     */
    public boolean isEmpty() {
        return content.isEmpty();
    }

    /**
     * Returns the matrix size, where X means columns and Y means rows. Null cells, including whole rows/columns of
     * nulls, are included. A zero coordinate always means the other coordinate is also zero (matrix is empty).
     */
    public Coordinates size() {
        return new Coordinates(columns(), rows());
    }

    int rows() {
        return isEmpty() ? 0 : Sugar.first(content).size();
    }

    int columns() {
        return content.size();
    }

    private static int validateNegative(int index) {
        if (index < 0)
            throw new IndexOutOfBoundsException("Index out of bounds: " + index + ".");
        return index;
    }

    /**
     * Returns the cell at the coordinates provided.
     * @throws IndexOutOfBoundsException If a coordinate is out of bounds.
     */
    public T get(Coordinates coordinates) {
        return get(coordinates.getX(), coordinates.getY());
    }

    /**
     * Returns the cell at the coordinates provided.
     * @throws IndexOutOfBoundsException If a coordinate is out of bounds.
     */
    public T get(int x, int y) {
        return content.get(x).get(y);
    }

    /**
     * Updates the cell at the coordinates provided.
     * @return The replaced element.
     * @throws IndexOutOfBoundsException If a coordinate is out of bounds.
     */
    public T set(Coordinates coordinates, T element) {
        return set(coordinates.getX(), coordinates.getY(), element);
    }

    /**
     * Updates the cell at the coordinates provided.
     * @return The replaced element.
     * @throws IndexOutOfBoundsException If a coordinate is out of bounds.
     */
    public T set(int x, int y, T element) {
        return content.get(x).set(y, element);
    }

    /**
     * Returns the row at the specified index as an unmodifiable list.
     * @throws IndexOutOfBoundsException If the index is out of bounds.
     */
    public List<T> getRow(int y) {
        if (isEmpty())
            throw new IndexOutOfBoundsException("Matrix is empty, can't get row " + y + ".");
        return content.stream().map(column -> column.get(y)).toList();
    }

    /**
     * Returns the first row as an unmodifiable list.
     * @throws IndexOutOfBoundsException If the matrix is empty.
     */
    public List<T> getFirstRow() {
        return getRow(0);
    }

    /**
     * Returns the last row as an unmodifiable list.
     * @throws IndexOutOfBoundsException If the matrix is empty.
     */
    public List<T> getLastRow() {
        return getRow(rows() - 1);
    }

    /**
     * Returns the column at the specified index as an unmodifiable list.
     * @throws IndexOutOfBoundsException If the index is out of bounds.
     */
    public List<T> getColumn(int x) {
        return Sugar.unmodifiableCopy(content.get(x));
    }

    /**
     * Returns the first column as an unmodifiable list.
     * @throws IndexOutOfBoundsException If the matrix is empty.
     */
    public List<T> getFirstColumn() {
        return getColumn(0);
    }

    /**
     * Returns the last column as an unmodifiable list.
     * @throws IndexOutOfBoundsException If the matrix is empty.
     */
    public List<T> getLastColumn() {
        return getColumn(columns() - 1);
    }

    /**
     * Returns true if the matrix contains the element.
     */
    public boolean contains(T element) {
        return indexOf(element) != null;
    }

    /**
     * Returns the coordinates of the first occurrence of the element (smallest x and y), or null if not found. The
     * search is done by columns (column 0 from row 0 to Y, column 1 from row 0 etc.).
     */
    public Coordinates indexOf(T element) {
        for (int x = 0; x < columns(); x++) {
            int y = content.get(x).indexOf(element);
            if (y >= 0)
                return new Coordinates(x, y);
        }
        return null;
    }

    /**
     * Returns the coordinates of the last occurrence of the element (greatest x and y), or null if not found. The
     * search is done by columns (column X from row Y to 0, column X-1 from row Y etc.).
     */
    public Coordinates lastIndexOf(T element) {
        for (int x = columns() - 1; x >= 0; x--) {
            int y = content.get(x).lastIndexOf(element);
            if (y >= 0)
                return new Coordinates(x, y);
        }
        return null;
    }

    /**
     * Returns the matrix cells as an ordered, unmodifiable list of rows.
     */
    public List<List<T>> getRows() {
        return getRowsRange().stream().map(y -> content.stream().map(column -> column.get(y)).toList()).toList();
    }

    /**
     * Returns the matrix cells as an ordered, unmodifiable list of columns.
     */
    public List<List<T>> getColumns() {
        return content.stream().map(Sugar::unmodifiableCopy).toList();
    }

    /**
     * Returns a range of the matrix row indexes.
     */
    public Range getRowsRange() {
        return Range.of(0, rows());
    }

    /**
     * Returns a range of the matrix column indexes.
     */
    public Range getColumnsRange() {
        return Range.of(0, columns());
    }

    /**
     * Returns a block of the matrix.
     */
    public Block getBlock() {
        return new Block(new Coordinates(0, 0), size());
    }

    /**
     * Returns a flat stream of the matrix elements. The order is column 0 from row 0 to Y, column 1 from row 0 etc.
     */
    public Stream<T> stream() {
        return content.stream().flatMap(Collection::stream);
    }

    /**
     * Adds a row at the end of the matrix, stretching it by 1 row.
     * @param row Optional values of the new row. If empty or smaller than the number of columns in the matrix, the
     *            missing values are padded with nulls. If greater than the number of columns in the matrix, including
     *            if the matrix is empty, it is stretched to accommodate the new value(s) by adding columns, padded with
     *            nulls where necessary. If both the row and matrix are empty, the matrix is stretched to size [1, 1],
     *            containing null.
     */
    @SafeVarargs
    public final void addRow(T... row) {
        addRowBefore(rows(), row);
    }

    /**
     * Adds a row before the specified row, stretching the matrix by 1 row, effectively making the new one row y and
     * shifting subsequent rows (if any) by 1.
     * @param y The row index. May be equal to the rows number - the row will be added at the end of the matrix.
     * @param row Optional values of the new row. If empty or smaller than the number of columns in the matrix, the
     *            missing values are padded with nulls. If greater than the number of columns in the matrix, including
     *            if the matrix is empty, it is stretched to accommodate the new value(s) by adding columns, padded with
     *            nulls where necessary. If both the row and matrix are empty, the matrix is stretched to size [1, 1],
     *            containing null.
     * @throws IndexOutOfBoundsException If the index is out of bounds.
     */
    @SafeVarargs
    public final void addRowBefore(int y, T... row) {
        if (validateNegative(y) > rows())
            throw new IndexOutOfBoundsException("Row " + y + " can't be added having a total of " + rows() + ".");
        boolean wasEmpty = isEmpty();
        if (wasEmpty)
            content.add(Sugar.fill(1));
        Sugar.iterate(Math.max(row.length - columns(), 0), i -> addColumn());
        getColumnsRange().forEach(x -> content.get(x).add(y, x < row.length ? row[x] : null));
        if (wasEmpty)
            removeRow(y + 1);
    }

    /**
     * Adds a row after the specified row, stretching the matrix by 1 row, effectively making the new one row y + 1 and
     * shifting subsequent rows (if any) by 1.
     * @param y The row index.
     * @param row Optional values of the new row. If empty or smaller than the number of columns in the matrix, the
     *            missing values are padded with nulls. If greater than the number of columns in the matrix, including
     *            if the matrix is empty, it is stretched to accommodate the new value(s) by adding columns, padded with
     *            nulls where necessary.
     * @throws IndexOutOfBoundsException If the index is out of bounds.
     */
    @SafeVarargs
    public final void addRowAfter(int y, T... row) {
        addRowBefore(validateNegative(y) + 1, row);
    }

    /**
     * Adds a column at the end of the matrix, stretching it by 1 column.
     * @param column Optional values of the new column. If empty or smaller than the number of rows in the matrix, the
     *               missing values are padded with nulls. If greater than the number of rows in the matrix, including
     *               if the matrix is empty, it is stretched to accommodate the new value(s) by adding rows, padded with
     *               nulls where necessary. If both the column and matrix are empty, the matrix is stretched to size
     *               [1, 1], containing null.
     */
    @SafeVarargs
    public final void addColumn(T... column) {
        addColumnBefore(columns(), column);
    }

    /**
     * Adds a column before the specified column, stretching the matrix by 1 column, effectively making the new one
     * column x and shifting subsequent columns (if any) by 1.
     * @param x The column index. May be equal to the columns number - the column will be added at the end of the
     *          matrix.
     * @param column Optional values of the new column. If empty or smaller than the number of rows in the matrix, the
     *               missing values are padded with nulls. If greater than the number of rows in the matrix, including
     *               if the matrix is empty, it is stretched to accommodate the new value(s) by adding rows, padded with
     *               nulls where necessary. If both the column and matrix are empty, the matrix is stretched to size
     *               [1, 1], containing null.
     * @throws IndexOutOfBoundsException If the index is out of bounds.
     */
    @SafeVarargs
    public final void addColumnBefore(int x, T... column) {
        if (validateNegative(x) > columns())
            throw new IndexOutOfBoundsException("Column " + x + " can't be added having a total of " + columns() +
                    ".");
        boolean wasEmpty = isEmpty();
        Sugar.iterate(Math.max(column.length - rows(), 0), i -> addRow());
        List<T> columnContent = Sugar.fill(Math.max(rows(), 1));
        for (int y = 0; y < rows() && y < column.length; y++)
            columnContent.set(y, column[y]);
        content.add(x, columnContent);
        if (wasEmpty && column.length > 0)
            removeColumn(x + 1);
    }

    /**
     * Adds a column after the specified column, stretching the matrix by 1 column, effectively making the new one
     * column x + 1 and shifting subsequent columns (if any) by 1.
     * @param x The column index.
     * @param column Optional values of the new column. If empty or smaller than the number of rows in the matrix, the
     *               missing values are padded with nulls. If greater than the number of rows in the matrix, including
     *               if the matrix is empty, it is stretched to accommodate the new value(s) by adding rows, padded with
     *               nulls where necessary.
     * @throws IndexOutOfBoundsException If the index is out of bounds.
     */
    @SafeVarargs
    public final void addColumnAfter(int x, T... column) {
        addColumnBefore(validateNegative(x) + 1, column);
    }

    /**
     * Removes the row at the specified index, and shrinks the matrix by 1 row.
     * @param y The row index.
     * @return The removed row as an unmodifiable list.
     * @throws IndexOutOfBoundsException If the index is out of bounds.
     */
    public List<T> removeRow(int y) {
        if (validateNegative(y) >= rows())
            throw new IndexOutOfBoundsException("Row " + y + " doesn't exist in a total of " + rows() + ".");
        var row = content.stream().map(column -> column.remove(y)).toList();
        if (rows() == 0)
            clear();
        return row;
    }

    /**
     * Removes the first row, and shrinks the matrix by 1 row.
     * @return The removed row.
     * @throws IndexOutOfBoundsException If the matrix is empty.
     */
    public List<T> removeFirstRow() {
        return removeRow(0);
    }

    /**
     * Removes the last row, and shrinks the matrix by 1 row.
     * @return The removed row.
     * @throws IndexOutOfBoundsException If the matrix is empty.
     */
    public List<T> removeLastRow() {
        return removeRow(rows() - 1);
    }

    /**
     * Removes the column at the specified index, and shrinks the matrix by 1 column.
     * @param x The column index.
     * @return The removed column.
     * @throws IndexOutOfBoundsException If the index is out of bounds.
     */
    public List<T> removeColumn(int x) {
        if (validateNegative(x) >= columns())
            throw new IndexOutOfBoundsException("Column " + x + " doesn't exist in a total of " + columns() + ".");
        return content.remove(x);
    }

    /**
     * Removes the first column, and shrinks the matrix by 1 column.
     * @return The removed column.
     * @throws IndexOutOfBoundsException If the matrix is empty.
     */
    public List<T> removeFirstColumn() {
        return removeColumn(0);
    }

    /**
     * Removes the last column, and shrinks the matrix by 1 column.
     * @return The removed column.
     * @throws IndexOutOfBoundsException If the matrix is empty.
     */
    public List<T> removeLastColumn() {
        return removeColumn(columns() - 1);
    }

    /**
     * Updates the row at the specified index.
     * @param y The row index.
     * @param row Optional values of the new row. If empty or smaller than the number of columns in the matrix, the
     *            missing values are padded with nulls. If greater than the number of columns in the matrix, it is
     *            stretched to accommodate the new value(s) by adding columns, padded with nulls where necessary.
     * @return The replaced row.
     * @throws IndexOutOfBoundsException If the index is out of bounds.
     */
    @SafeVarargs
    public final List<T> setRow(int y, T... row) {
        var previous = removeRow(y);
        addRowBefore(y, row);
        Sugar.iterate(Math.max(previous.size() - columns(), 0), i -> addColumn());
        return previous;
    }

    /**
     * Updates the column at the specified index.
     * @param x The column index.
     * @param column Optional values of the new column. If empty or smaller than the number of rows in the matrix, the
     *               missing values are padded with nulls. If greater than the number of rows in the matrix, it is
     *               stretched to accommodate the new value(s) by adding rows, padded with nulls where necessary.
     * @return The replaced column.
     * @throws IndexOutOfBoundsException If the index is out of bounds.
     */
    @SafeVarargs
    public final List<T> setColumn(int x, T... column) {
        var previous = removeColumn(x);
        addColumnBefore(x, column);
        Sugar.iterate(Math.max(previous.size() - rows(), 0), i -> addRow());
        return previous;
    }

    /**
     * Removes trailing rows and columns where all cells are null.
     */
    public void pack() {
        packRows();
        packColumns();
    }

    /**
     * Removes trailing rows where all cells are null.
     */
    public void packRows() {
        pack(this::getLastRow, this::removeLastRow);
    }

    /**
     * Removes trailing columns where all cells are null.
     */
    public void packColumns() {
        pack(this::getLastColumn, this::removeLastColumn);
    }

    private void pack(Supplier<List<T>> getLast, Runnable removeLast) {
        if (isEmpty())
            return;
        var last = getLast.get();
        while (last.stream().allMatch(Objects::isNull)) {
            removeLast.run();
            if (isEmpty())
                return;
            last = getLast.get();
        }
    }

    /**
     * Clears the matrix elements and shrinks it to [0, 0].
     */
    public void clear() {
        content.clear();
    }

    /**
     * Swaps between the two cells.
     * @throws IndexOutOfBoundsException If a coordinate is out of bounds.
     */
    public void swap(Coordinates coordinates1, Coordinates coordinates2) {
        swap(coordinates1.getX(), coordinates1.getY(), coordinates2.getX(), coordinates2.getY());
    }

    /**
     * Swaps between the two cells.
     * @throws IndexOutOfBoundsException If a coordinate is out of bounds.
     */
    public void swap(int x1, int y1, int x2, int y2) {
        T temp = get(x1, y1);
        set(x1, y1, get(x2, y2));
        set(x2, y2, temp);
    }

    /**
     * Swaps between the two rows.
     * @throws IndexOutOfBoundsException If an index is out of bounds.
     */
    public void swapRows(int y1, int y2) {
        getColumnsRange().forEach(x -> swap(x, y1, x, y2));
    }

    /**
     * Swaps between the two columns.
     * @throws IndexOutOfBoundsException If an index is out of bounds.
     */
    public void swapColumns(int x1, int x2) {
        var temp = content.get(x1);
        content.set(x1, content.get(x2));
        content.set(x2, temp);
    }

    /**
     * Reverses the order of the columns.
     */
    public void reverseX() {
        Collections.reverse(content);
    }

    /**
     * Reverses the order of the rows.
     */
    public void reverseY() {
        content.forEach(Collections::reverse);
    }

    /**
     * Changes the matrix rows into columns, effectively flipping it along the diagonal. Affects the size accordingly.
     */
    public void flip() {
        var rows = getRows();
        clear();
        rows.stream().map(ArrayList::new).forEach(content::add);
    }

    /**
     * Turns the matrix 90 degrees clockwise. Affects the size accordingly.
     */
    public void turnClockwise() {
        flip();
        reverseX();
    }

    /**
     * Turns the matrix 90 degrees counter-clockwise. Affects the size accordingly.
     */
    public void turnCounterClockwise() {
        flip();
        reverseY();
    }

    /**
     * Turns the matrix 180 degrees.
     */
    public void turnAround() {
        reverseX();
        reverseY();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Matrix<?> matrix = (Matrix<?>) o;
        return size().equals(matrix.size()) && content.equals(matrix.content);
    }

    @Override
    public String toString() {
        return printer.apply(this);
    }

    /**
     * X and Y coordinates.
     */
    public static final class Coordinates extends Couple<Integer> {

        private Coordinates(int x, int y) {
            super(x, y);
        }

        public static Coordinates of(int x, int y) {
            return new Coordinates(validateNegative(x), validateNegative(y));
        }

        public int getX() {
            return getFirst();
        }

        public int getY() {
            return getSecond();
        }
    }

    /**
     * A range of coordinates.
     */
    public static final class Block extends Couple<Coordinates> {

        private Block(Coordinates from, Coordinates to) {
            super(from, to);
        }

        public static Block of(int fromX, int fromY, int toX, int toY) {
            return of(Coordinates.of(fromX, fromY), Coordinates.of(toX, toY));
        }

        public static Block of(Coordinates from, Coordinates to) {
            if (from.getX() > to.getX() || from.getY() > to.getY())
                throw new IllegalArgumentException("Negative block.");
            return new Block(from, to);
        }

        public Coordinates getFrom() {
            return getFirst();
        }

        public Coordinates getTo() {
            return getSecond();
        }

        public Coordinates size() {
            return new Coordinates(getXRange().size(), getYRange().size());
        }

        /**
         * Returns the block X range.
         */
        public Range getXRange() {
            return Range.of(getFrom().getX(), getTo().getX());
        }

        /**
         * Returns the block Y range.
         */
        public Range getYRange() {
            return Range.of(getFrom().getY(), getTo().getY());
        }

        /**
         * Returns true if the block contains the coordinates.
         */
        public boolean contains(Coordinates coordinates) {
            return getXRange().contains(coordinates.getX()) && getYRange().contains(coordinates.getY());
        }

        /**
         * Returns a stream of the block coordinates, from <code>from</code> (inclusive) to <code>to</code> (exclusive).
         * If either X or Y range is empty, returns an empty stream.
         */
        public Stream<Coordinates> stream() {
            var yRange = getYRange();
            return getXRange().stream().flatMap(x -> yRange.stream().map(y -> Coordinates.of(x, y)));
        }

        /**
         * Performs an action for each cell in the block, from <code>from</code> (inclusive) to <code>to</code>
         * (exclusive). If either X or Y range is empty, does nothing.
         */
        public void forEach(BiConsumer<Integer, Integer> action) {
            Objects.requireNonNull(action, "Action is null.");
            forEach(coordinates -> action.accept(coordinates.getX(), coordinates.getY()));
        }

        /**
         * Performs an action for each cell in the block, from <code>from</code> (inclusive) to <code>to</code>
         * (exclusive). If either X or Y range is empty, does nothing. Equivalent to:
         * <pre>
         * stream().forEach(action)
         * </pre>
         */
        public void forEach(Consumer<Coordinates> action) {
            Objects.requireNonNull(action, "Action is null.");
            stream().forEach(action);
        }
    }
}
