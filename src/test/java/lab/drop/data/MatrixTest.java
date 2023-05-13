package lab.drop.data;

import org.junit.jupiter.api.*;

import java.util.List;
import java.util.stream.Collectors;

class MatrixTest {

    @BeforeEach
    void beforeEach(TestInfo testInfo) {
        System.out.println(testInfo.getDisplayName());
    }

    @AfterEach
    void afterEach() {
        System.out.println();
    }

    private void assertData(String expected, Matrix<?> matrix) {
        System.out.println(matrix);
        Assertions.assertEquals(expected, matrix.toString(",", "|", "null", false));
        var columns = matrix.getColumns();
        if (!columns.isEmpty()) {
            int rows = matrix.size().getY();
            columns.forEach(column -> Assertions.assertEquals(rows, column.size()));
        }
    }

    @SafeVarargs
    private <T> void assertData(Matrix<T> matrix, T... expected) {
        var size = matrix.size();
        int i = 0;
        for (int y = 0; y < size.getY(); y++)
            for (int x = 0; x < size.getX(); x++)
                Assertions.assertEquals(expected[i++], matrix.get(x, y));
    }

    @SafeVarargs
    private <T> void assertData(List<T> list, T... expected) {
        Assertions.assertEquals(expected.length, list.size());
        for (int i = 0; i < list.size(); i++)
            Assertions.assertEquals(expected[i], list.get(i));
    }

    private void assertBadIndex(Runnable runnable) {
        try {
            runnable.run();
            Assertions.fail();
        } catch (IndexOutOfBoundsException e) {
            System.out.println(e.getMessage());
        }
    }

    private void assertUnmodifiable(Runnable runnable) {
        try {
            runnable.run();
            Assertions.fail("Modification succeeded");
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test
    void empty() {
        var matrix = new Matrix<Integer>();
        Assertions.assertTrue(matrix.isEmpty());
        Assertions.assertTrue(matrix.size().equals(0, 0));
        assertData("", matrix);
        Assertions.assertEquals("", matrix.toTableString());
        Assertions.assertTrue(matrix.getRows().isEmpty());
        Assertions.assertTrue(matrix.getColumns().isEmpty());
    }

    @Test
    void header() {
        Assertions.assertEquals("""
                Just a header
                -------------""", new Matrix<>().toTableString("Just a header"));
    }

    @Test
    void initSize() {
        var matrix = new Matrix<Integer>(3, 2);
        Assertions.assertFalse(matrix.isEmpty());
        Assertions.assertTrue(matrix.size().equals(3, 2));
        assertData("null,null,null|null,null,null", matrix);
    }

    @Test
    void firstAddEmptyRow() {
        var matrix = new Matrix<Integer>();
        matrix.addRow();
        Assertions.assertFalse(matrix.isEmpty());
        Assertions.assertTrue(matrix.size().equals(1, 1));
        assertData("null", matrix);
    }

    @Test
    void stretchByEmptyRow() {
        var matrix = new Matrix<Integer>();
        matrix.addRow();
        matrix.addRow();
        Assertions.assertTrue(matrix.size().equals(1, 2));
        assertData("null|null", matrix);
    }

    @Test
    void firstAddEmptyColumn() {
        var matrix = new Matrix<Integer>();
        matrix.addColumn();
        Assertions.assertFalse(matrix.isEmpty());
        Assertions.assertTrue(matrix.size().equals(1, 1));
        assertData("null", matrix);
    }

    @Test
    void stretchByEmptyColumn() {
        var matrix = new Matrix<Integer>();
        matrix.addColumn();
        matrix.addColumn();
        Assertions.assertTrue(matrix.size().equals(2, 1));
        assertData("null,null", matrix);
        assertData(matrix, null, null);
    }

    @Test
    void build2x2EmptyRows() {
        var matrix = new Matrix<Integer>();
        matrix.addRow();
        matrix.addRow();
        matrix.addColumn();
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("null,null|null,null", matrix);
    }

    @Test
    void stretch2x2EmptyRows() {
        var matrix = new Matrix<Integer>();
        matrix.addRow();
        matrix.addRowBefore(0);
        matrix.addColumn();
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("null,null|null,null", matrix);
    }

    @Test
    void stretchByData4x3() {
        var matrix = new Matrix<Integer>();
        matrix.addRow(1);
        matrix.addRow(2, 3);
        matrix.addRow(4, 5, 6);
        matrix.addColumn(7, 8);
        Assertions.assertTrue(matrix.size().equals(4, 3));
        assertData("1,null,null,7|2,3,null,8|4,5,6,null", matrix);
    }

    @Test
    void stretchByData4x4() {
        var matrix = new Matrix<Integer>();
        matrix.addRow(1);
        matrix.addRow(2, 3);
        matrix.addRow(4, 5, 6);
        matrix.addColumn(7, 8, 9, 10);
        Assertions.assertTrue(matrix.size().equals(4, 4));
        assertData("1,null,null,7|2,3,null,8|4,5,6,9|null,null,null,10", matrix);
    }

    @Test
    void stretchByData4x3Copy() {
        var matrix = new Matrix<Integer>();
        matrix.addRow(1);
        matrix.addRow(2, 3);
        matrix.addRow(4, 5, 6);
        matrix.addColumn(7, 8);
        var copy = new Matrix<>(matrix);
        Assertions.assertTrue(copy.size().equals(4, 3));
        assertData("1,null,null,7|2,3,null,8|4,5,6,null", copy);
    }

    @Test
    void stretchByData4x4Copy() {
        var matrix = new Matrix<Integer>();
        matrix.addRow(1);
        matrix.addRow(2, 3);
        matrix.addRow(4, 5, 6);
        matrix.addColumn(7, 8, 9, 10);
        var copy = new Matrix<>(matrix);
        Assertions.assertTrue(copy.size().equals(4, 4));
        assertData("1,null,null,7|2,3,null,8|4,5,6,9|null,null,null,10", copy);
    }

    @Test
    void build2x2EmptyColumns() {
        var matrix = new Matrix<Integer>();
        matrix.addColumn();
        matrix.addColumn();
        matrix.addRow();
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("null,null|null,null", matrix);
    }

    @Test
    void stretch2x2EmptyColumns() {
        var matrix = new Matrix<Integer>();
        matrix.addColumn();
        matrix.addColumnBefore(0);
        matrix.addRow();
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("null,null|null,null", matrix);
    }

    @Test
    void buildAndFill2x2() {
        var matrix = new Matrix<Character>(Matrix.Coordinates.of(2, 2));
        matrix.set(0, 0, 'a');
        matrix.set(1, 0, 'b');
        matrix.set(0, 1, 'c');
        matrix.set(1, 1, 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
    }

    @Test
    void buildRow1() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a');
        Assertions.assertTrue(matrix.size().equals(1, 1));
        assertData("a", matrix);
        Assertions.assertTrue(matrix.contains('a'));
    }

    @Test
    void buildRow2() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        Assertions.assertTrue(matrix.size().equals(2, 1));
        assertData("a,b", matrix);
    }

    @Test
    void build2x2Rows() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        assertData(matrix, 'a', 'b', 'c', 'd');
    }

    @Test
    void buildColumn1() {
        var matrix = new Matrix<Character>();
        matrix.addColumn('a');
        Assertions.assertTrue(matrix.size().equals(1, 1));
        assertData("a", matrix);
        Assertions.assertTrue(matrix.contains('a'));
    }

    @Test
    void buildColumn2() {
        var matrix = new Matrix<Character>();
        matrix.addColumn('a', 'c');
        Assertions.assertTrue(matrix.size().equals(1, 2));
        assertData("a|c", matrix);
    }

    @Test
    void build2x2Columns() {
        var matrix = new Matrix<Character>();
        matrix.addColumn('a', 'c');
        matrix.addColumn('b', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        assertData(matrix, 'a', 'b', 'c', 'd');
    }

    @Test
    void initData() {
        var matrix = new Matrix<>(new Character[][] {
                {'a', 'b'},
                {'c', 'd'}
        });
        Assertions.assertTrue(matrix.size().equals(2, 2));
        Assertions.assertEquals(matrix.size(), matrix.getBlock().size());
        assertData("a,b|c,d", matrix);
    }

    @Test
    void indexOf() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        Assertions.assertTrue(matrix.indexOf('a').equals(0, 0));
        Assertions.assertTrue(matrix.indexOf('b').equals(1, 0));
        Assertions.assertTrue(matrix.indexOf('c').equals(0, 1));
        Assertions.assertTrue(matrix.indexOf('d').equals(1, 1));
        Assertions.assertTrue(matrix.lastIndexOf('a').equals(0, 0));
        Assertions.assertTrue(matrix.lastIndexOf('b').equals(1, 0));
        Assertions.assertTrue(matrix.lastIndexOf('c').equals(0, 1));
        Assertions.assertTrue(matrix.lastIndexOf('d').equals(1, 1));
    }

    @Test
    void indexOfOrder() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('b', 'a');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|b,a", matrix);
        Assertions.assertTrue(matrix.indexOf('a').equals(0, 0));
        Assertions.assertTrue(matrix.indexOf('b').equals(0, 1));
        Assertions.assertTrue(matrix.lastIndexOf('a').equals(1, 1));
        Assertions.assertTrue(matrix.lastIndexOf('b').equals(1, 0));
    }

    @Test
    void getRows() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        assertData(matrix.getRow(0), 'a', 'b');
        assertData(matrix.getFirstRow(), 'a', 'b');
        assertData(matrix.getRow(1), 'c', 'd');
        assertData(matrix.getLastRow(), 'c', 'd');
        var rows = matrix.getRows();
        assertData(rows.get(0), 'a', 'b');
        assertData(rows.get(1), 'c', 'd');
    }

    @Test
    void getColumns() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        assertData(matrix.getColumn(0), 'a', 'c');
        assertData(matrix.getFirstColumn(), 'a', 'c');
        assertData(matrix.getColumn(1), 'b', 'd');
        assertData(matrix.getLastColumn(), 'b', 'd');
        var columns = matrix.getColumns();
        assertData(columns.get(0), 'a', 'c');
        assertData(columns.get(1), 'b', 'd');
    }

    @Test
    void insertInto2x2EmptyRow_0() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addRowBefore(0);
        Assertions.assertTrue(matrix.size().equals(2, 3));
        assertData("null,null|a,b|c,d", matrix);
    }

    @Test
    void insertInto2x2EmptyRow0_() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addRowAfter(0);
        Assertions.assertTrue(matrix.size().equals(2, 3));
        assertData("a,b|null,null|c,d", matrix);
    }

    @Test
    void insertInto2x2EmptyRow_1() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addRowBefore(1);
        Assertions.assertTrue(matrix.size().equals(2, 3));
        assertData("a,b|null,null|c,d", matrix);
    }

    @Test
    void insertInto2x2EmptyRow1_() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addRowAfter(1);
        Assertions.assertTrue(matrix.size().equals(2, 3));
        assertData("a,b|c,d|null,null", matrix);
    }

    @Test
    void insertInto2x2Row_0() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addRowBefore(0, 'X', 'Y');
        Assertions.assertTrue(matrix.size().equals(2, 3));
        assertData("X,Y|a,b|c,d", matrix);
    }

    @Test
    void insertInto2x2Row0_() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addRowAfter(0, 'X', 'Y');
        Assertions.assertTrue(matrix.size().equals(2, 3));
        assertData("a,b|X,Y|c,d", matrix);
    }

    @Test
    void insertInto2x2Row_1() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addRowBefore(1, 'X', 'Y');
        Assertions.assertTrue(matrix.size().equals(2, 3));
        assertData("a,b|X,Y|c,d", matrix);
    }

    @Test
    void insertInto2x2Row1_() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addRowAfter(1, 'X', 'Y');
        Assertions.assertTrue(matrix.size().equals(2, 3));
        assertData("a,b|c,d|X,Y", matrix);
    }

    @Test
    void insertInto2x2PartialRow_0() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addRowBefore(0, 'X');
        Assertions.assertTrue(matrix.size().equals(2, 3));
        assertData("X,null|a,b|c,d", matrix);
    }

    @Test
    void insertInto2x2PartialRow0_() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addRowAfter(0, 'X');
        Assertions.assertTrue(matrix.size().equals(2, 3));
        assertData("a,b|X,null|c,d", matrix);
    }

    @Test
    void insertInto2x2PartialRow_1() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addRowBefore(1, 'X');
        Assertions.assertTrue(matrix.size().equals(2, 3));
        assertData("a,b|X,null|c,d", matrix);
    }

    @Test
    void insertInto2x2PartialRow1_() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addRowAfter(1, 'X');
        Assertions.assertTrue(matrix.size().equals(2, 3));
        assertData("a,b|c,d|X,null", matrix);
    }

    @Test
    void insertInto2x2StretchRow_0() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addRowBefore(0, 'X', 'Y', 'Z');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("X,Y,Z|a,b,null|c,d,null", matrix);
    }

    @Test
    void insertInto2x2StretchRow0_() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addRowAfter(0, 'X', 'Y', 'Z');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,null|X,Y,Z|c,d,null", matrix);
    }

    @Test
    void insertInto2x2StretchRow_1() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addRowBefore(1, 'X', 'Y', 'Z');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,null|X,Y,Z|c,d,null", matrix);
    }

    @Test
    void insertInto2x2StretchRow1_() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addRowAfter(1, 'X', 'Y', 'Z');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,null|c,d,null|X,Y,Z", matrix);
    }

    @Test
    void insertInto2x2EmptyColumn_0() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addColumnBefore(0);
        Assertions.assertTrue(matrix.size().equals(3, 2));
        assertData("null,a,b|null,c,d", matrix);
    }

    @Test
    void insertInto2x2EmptyColumn0_() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addColumnAfter(0);
        Assertions.assertTrue(matrix.size().equals(3, 2));
        assertData("a,null,b|c,null,d", matrix);
    }

    @Test
    void insertInto2x2EmptyColumn_1() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addColumnBefore(1);
        Assertions.assertTrue(matrix.size().equals(3, 2));
        assertData("a,null,b|c,null,d", matrix);
    }

    @Test
    void insertInto2x2EmptyColumn1_() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addColumnAfter(1);
        Assertions.assertTrue(matrix.size().equals(3, 2));
        assertData("a,b,null|c,d,null", matrix);
    }

    @Test
    void insertInto2x2Column_0() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addColumnBefore(0, 'X', 'Y');
        Assertions.assertTrue(matrix.size().equals(3, 2));
        assertData("X,a,b|Y,c,d", matrix);
    }

    @Test
    void insertInto2x2Column0_() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addColumnAfter(0, 'X', 'Y');
        Assertions.assertTrue(matrix.size().equals(3, 2));
        assertData("a,X,b|c,Y,d", matrix);
    }

    @Test
    void insertInto2x2Column_1() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addColumnBefore(1, 'X', 'Y');
        Assertions.assertTrue(matrix.size().equals(3, 2));
        assertData("a,X,b|c,Y,d", matrix);
    }

    @Test
    void insertInto2x2Column1_() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addColumnAfter(1, 'X', 'Y');
        Assertions.assertTrue(matrix.size().equals(3, 2));
        assertData("a,b,X|c,d,Y", matrix);
    }

    @Test
    void insertInto2x2PartialColumn_0() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addColumnBefore(0, 'X');
        Assertions.assertTrue(matrix.size().equals(3, 2));
        assertData("X,a,b|null,c,d", matrix);
    }

    @Test
    void insertInto2x2PartialColumn0_() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addColumnAfter(0, 'X');
        Assertions.assertTrue(matrix.size().equals(3, 2));
        assertData("a,X,b|c,null,d", matrix);
    }

    @Test
    void insertInto2x2PartialColumn_1() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addColumnBefore(1, 'X');
        Assertions.assertTrue(matrix.size().equals(3, 2));
        assertData("a,X,b|c,null,d", matrix);
    }

    @Test
    void insertInto2x2PartialColumn1_() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addColumnAfter(1, 'X');
        Assertions.assertTrue(matrix.size().equals(3, 2));
        assertData("a,b,X|c,d,null", matrix);
    }

    @Test
    void insertInto2x2StretchColumn_0() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addColumnBefore(0, 'X', 'Y', 'Z');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("X,a,b|Y,c,d|Z,null,null", matrix);
    }

    @Test
    void insertInto2x2StretchColumn0_() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addColumnAfter(0, 'X', 'Y', 'Z');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,X,b|c,Y,d|null,Z,null", matrix);
    }

    @Test
    void insertInto2x2StretchColumn_1() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addColumnBefore(1, 'X', 'Y', 'Z');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,X,b|c,Y,d|null,Z,null", matrix);
    }

    @Test
    void insertInto2x2StretchColumn1_() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        matrix.addColumnAfter(1, 'X', 'Y', 'Z');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,X|c,d,Y|null,null,Z", matrix);
    }

    @Test
    void removeFrom3x3Row0() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        matrix.addRow('g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|g,h,i", matrix);
        assertData(matrix.removeFirstRow(), 'a', 'b', 'c');
        Assertions.assertTrue(matrix.size().equals(3, 2));
        assertData("d,e,f|g,h,i", matrix);
    }

    @Test
    void removeFrom3x3Row1() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        matrix.addRow('g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|g,h,i", matrix);
        assertData(matrix.removeRow(1), 'd', 'e', 'f');
        Assertions.assertTrue(matrix.size().equals(3, 2));
        assertData("a,b,c|g,h,i", matrix);
    }

    @Test
    void removeFrom3x3Row2() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        matrix.addRow('g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|g,h,i", matrix);
        assertData(matrix.removeLastRow(), 'g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 2));
        assertData("a,b,c|d,e,f", matrix);
    }

    @Test
    void removeFrom3x3Column0() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        matrix.addRow('g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|g,h,i", matrix);
        assertData(matrix.removeFirstColumn(), 'a', 'd', 'g');
        Assertions.assertTrue(matrix.size().equals(2, 3));
        assertData("b,c|e,f|h,i", matrix);
    }

    @Test
    void removeFrom3x3Column1() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        matrix.addRow('g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|g,h,i", matrix);
        assertData(matrix.removeColumn(1), 'b', 'e', 'h');
        Assertions.assertTrue(matrix.size().equals(2, 3));
        assertData("a,c|d,f|g,i", matrix);
    }

    @Test
    void removeFrom3x3Column2() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        matrix.addRow('g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|g,h,i", matrix);
        assertData(matrix.removeLastColumn(), 'c', 'f', 'i');
        Assertions.assertTrue(matrix.size().equals(2, 3));
        assertData("a,b|d,e|g,h", matrix);
    }

    @Test
    void update3x3Row0() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        matrix.addRow('g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|g,h,i", matrix);
        assertData(matrix.setRow(0, 'x', 'y', 'z'), 'a', 'b', 'c');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("x,y,z|d,e,f|g,h,i", matrix);
    }

    @Test
    void update3x3Row1() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        matrix.addRow('g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|g,h,i", matrix);
        assertData(matrix.setRow(1, 'x', 'y', 'z'), 'd', 'e', 'f');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|x,y,z|g,h,i", matrix);
    }

    @Test
    void update3x3Row2() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        matrix.addRow('g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|g,h,i", matrix);
        assertData(matrix.setRow(2, 'x', 'y', 'z'), 'g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|x,y,z", matrix);
    }

    @Test
    void update3x3Row0Empty() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        matrix.addRow('g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|g,h,i", matrix);
        assertData(matrix.setRow(0), 'a', 'b', 'c');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("null,null,null|d,e,f|g,h,i", matrix);
    }

    @Test
    void update3x3Row1Empty() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        matrix.addRow('g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|g,h,i", matrix);
        assertData(matrix.setRow(1), 'd', 'e', 'f');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|null,null,null|g,h,i", matrix);
    }

    @Test
    void update3x3Row2Empty() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        matrix.addRow('g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|g,h,i", matrix);
        assertData(matrix.setRow(2), 'g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|null,null,null", matrix);
    }

    @Test
    void update3x3Row0Partial() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        matrix.addRow('g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|g,h,i", matrix);
        assertData(matrix.setRow(0, 'x'), 'a', 'b', 'c');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("x,null,null|d,e,f|g,h,i", matrix);
    }

    @Test
    void update3x3Row1Partial() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        matrix.addRow('g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|g,h,i", matrix);
        assertData(matrix.setRow(1, 'x'), 'd', 'e', 'f');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|x,null,null|g,h,i", matrix);
    }

    @Test
    void update3x3Row2Partial() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        matrix.addRow('g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|g,h,i", matrix);
        assertData(matrix.setRow(2, 'x'), 'g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|x,null,null", matrix);
    }

    @Test
    void update3x3Row0Stretch() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        matrix.addRow('g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|g,h,i", matrix);
        assertData(matrix.setRow(0, 'x', 'y', 'z', '1'), 'a', 'b', 'c');
        Assertions.assertTrue(matrix.size().equals(4, 3));
        assertData("x,y,z,1|d,e,f,null|g,h,i,null", matrix);
    }

    @Test
    void update3x3Row1Stretch() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        matrix.addRow('g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|g,h,i", matrix);
        assertData(matrix.setRow(1, 'x', 'y', 'z', '1'), 'd', 'e', 'f');
        Assertions.assertTrue(matrix.size().equals(4, 3));
        assertData("a,b,c,null|x,y,z,1|g,h,i,null", matrix);
    }

    @Test
    void update3x3Row2Stretch() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        matrix.addRow('g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|g,h,i", matrix);
        assertData(matrix.setRow(2, 'x', 'y', 'z', '1'), 'g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(4, 3));
        assertData("a,b,c,null|d,e,f,null|x,y,z,1", matrix);
    }

    @Test
    void update3x3Column0() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        matrix.addRow('g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|g,h,i", matrix);
        assertData(matrix.setColumn(0, 'x', 'y', 'z'), 'a', 'd', 'g');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("x,b,c|y,e,f|z,h,i", matrix);
    }

    @Test
    void update3x3Column1() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        matrix.addRow('g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|g,h,i", matrix);
        assertData(matrix.setColumn(1, 'x', 'y', 'z'), 'b', 'e', 'h');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,x,c|d,y,f|g,z,i", matrix);
    }

    @Test
    void update3x3Column2() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        matrix.addRow('g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|g,h,i", matrix);
        assertData(matrix.setColumn(2, 'x', 'y', 'z'), 'c', 'f', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,x|d,e,y|g,h,z", matrix);
    }

    @Test
    void update3x3Column0Empty() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        matrix.addRow('g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|g,h,i", matrix);
        assertData(matrix.setColumn(0), 'a', 'd', 'g');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("null,b,c|null,e,f|null,h,i", matrix);
    }

    @Test
    void update3x3Column1Empty() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        matrix.addRow('g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|g,h,i", matrix);
        assertData(matrix.setColumn(1), 'b', 'e', 'h');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,null,c|d,null,f|g,null,i", matrix);
    }

    @Test
    void update3x3Column2Empty() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        matrix.addRow('g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|g,h,i", matrix);
        assertData(matrix.setColumn(2), 'c', 'f', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,null|d,e,null|g,h,null", matrix);
    }

    @Test
    void update3x3Column0Partial() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        matrix.addRow('g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|g,h,i", matrix);
        assertData(matrix.setColumn(0, 'x'), 'a', 'd', 'g');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("x,b,c|null,e,f|null,h,i", matrix);
    }

    @Test
    void update3x3Column1Partial() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        matrix.addRow('g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|g,h,i", matrix);
        assertData(matrix.setColumn(1, 'x'), 'b', 'e', 'h');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,x,c|d,null,f|g,null,i", matrix);
    }

    @Test
    void update3x3Column2Partial() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        matrix.addRow('g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|g,h,i", matrix);
        assertData(matrix.setColumn(2, 'x'), 'c', 'f', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,x|d,e,null|g,h,null", matrix);
    }

    @Test
    void update3x3Column0Stretch() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        matrix.addRow('g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|g,h,i", matrix);
        assertData(matrix.setColumn(0, 'x', 'y', 'z', '1'), 'a', 'd', 'g');
        Assertions.assertTrue(matrix.size().equals(3, 4));
        assertData("x,b,c|y,e,f|z,h,i|1,null,null", matrix);
    }

    @Test
    void update3x3Column1Stretch() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        matrix.addRow('g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|g,h,i", matrix);
        assertData(matrix.setColumn(1, 'x', 'y', 'z', '1'), 'b', 'e', 'h');
        Assertions.assertTrue(matrix.size().equals(3, 4));
        assertData("a,x,c|d,y,f|g,z,i|null,1,null", matrix);
    }

    @Test
    void update3x3Column2Stretch() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        matrix.addRow('g', 'h', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 3));
        assertData("a,b,c|d,e,f|g,h,i", matrix);
        assertData(matrix.setColumn(2, 'x', 'y', 'z', '1'), 'c', 'f', 'i');
        Assertions.assertTrue(matrix.size().equals(3, 4));
        assertData("a,b,x|d,e,y|g,h,z|null,null,1", matrix);
    }

    @Test
    void update1x1Row() {
        var matrix = new Matrix<Character>(1, 1);
        assertData(matrix.setRow(0, 'a'), (Character) null);
        Assertions.assertTrue(matrix.size().equals(1, 1));
        assertData("a", matrix);
        assertData(matrix.setRow(0), 'a');
        assertData("null", matrix);
        assertData(matrix.setRow(0, 'a', 'b'), (Character) null);
        Assertions.assertTrue(matrix.size().equals(2, 1));
        assertData("a,b", matrix);
        assertData(matrix.setRow(0, 'a'), 'a', 'b');
        Assertions.assertTrue(matrix.size().equals(2, 1));
        assertData("a,null", matrix);
        assertData(matrix.setRow(0, 'a', 'b'), 'a', null);
        Assertions.assertTrue(matrix.size().equals(2, 1));
        assertData("a,b", matrix);
        assertData(matrix.setRow(0), 'a', 'b');
        Assertions.assertTrue(matrix.size().equals(2, 1));
        assertData("null,null", matrix);
    }

    @Test
    void update1x1Column() {
        var matrix = new Matrix<Character>(1, 1);
        assertData(matrix.setColumn(0, 'a'), (Character) null);
        Assertions.assertTrue(matrix.size().equals(1, 1));
        assertData("a", matrix);
        assertData(matrix.setColumn(0), 'a');
        assertData("null", matrix);
        assertData(matrix.setColumn(0, 'a', 'b'), (Character) null);
        Assertions.assertTrue(matrix.size().equals(1, 2));
        assertData("a|b", matrix);
        assertData(matrix.setColumn(0, 'a'), 'a', 'b');
        Assertions.assertTrue(matrix.size().equals(1, 2));
        assertData("a|null", matrix);
        assertData(matrix.setColumn(0, 'a', 'b'), 'a', null);
        Assertions.assertTrue(matrix.size().equals(1, 2));
        assertData("a|b", matrix);
        assertData(matrix.setColumn(0), 'a', 'b');
        Assertions.assertTrue(matrix.size().equals(1, 2));
        assertData("null|null", matrix);
    }

    @Test
    void stream() {
        var matrix = new Matrix<String>();
        matrix.addRow("a", "b", "c");
        matrix.addRow("d", "e", "f");
        matrix.addRow("g", "h", "i");
        Assertions.assertEquals("a-d-g-b-e-h-c-f-i", matrix.stream().collect(Collectors.joining("-")));
    }

    @Test
    void contains() {
        var matrix = new Matrix<String>();
        matrix.addRow("a", "b", "c");
        matrix.addRow("d", "e", "f");
        matrix.addRow("g", "h", "i");
        Assertions.assertTrue(matrix.getBlock().contains(Matrix.Coordinates.of(0, 0)));
        Assertions.assertTrue(matrix.getBlock().contains(Matrix.Coordinates.of(2, 2)));
        Assertions.assertFalse(matrix.getBlock().contains(Matrix.Coordinates.of(3, 0)));
        Assertions.assertFalse(matrix.getBlock().contains(Matrix.Coordinates.of(0, 3)));
    }

    @Test
    void updates() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        Assertions.assertEquals('d', matrix.set(1, 1, 'a'));
        assertData("a,b|c,a", matrix);
        Assertions.assertTrue(matrix.indexOf('a').equals(0, 0));
        Assertions.assertTrue(matrix.lastIndexOf('a').equals(1, 1));
        Assertions.assertNull(matrix.indexOf('d'));
        Assertions.assertNull(matrix.lastIndexOf('d'));
        matrix.swap(matrix.indexOf('b'), matrix.indexOf('c'));
        assertData("a,c|b,a", matrix);
        matrix.swapRows(0, 1);
        assertData("b,a|a,c", matrix);
        matrix.swapColumns(0, 1);
        assertData("a,b|c,a", matrix);
    }

    @Test
    void packRows() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        matrix.addRow();
        matrix.addRow();
        matrix.addColumn();
        matrix.addColumn();
        Assertions.assertTrue(matrix.size().equals(4, 4));
        assertData("a,b,null,null|c,d,null,null|null,null,null,null|null,null,null,null", matrix);
        matrix.packRows();
        Assertions.assertTrue(matrix.size().equals(4, 2));
        assertData("a,b,null,null|c,d,null,null", matrix);
    }

    @Test
    void packColumns() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        matrix.addRow();
        matrix.addRow();
        matrix.addColumn();
        matrix.addColumn();
        Assertions.assertTrue(matrix.size().equals(4, 4));
        assertData("a,b,null,null|c,d,null,null|null,null,null,null|null,null,null,null", matrix);
        matrix.packColumns();
        Assertions.assertTrue(matrix.size().equals(2, 4));
        assertData("a,b|c,d|null,null|null,null", matrix);
    }

    @Test
    void pack() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        matrix.addRow();
        matrix.addRow();
        matrix.addColumn();
        matrix.addColumn();
        Assertions.assertTrue(matrix.size().equals(4, 4));
        assertData("a,b,null,null|c,d,null,null|null,null,null,null|null,null,null,null", matrix);
        matrix.pack();
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
    }

    @Test
    void packEmpty() {
        var matrix = new Matrix<Character>();
        matrix.addRow();
        matrix.addRow();
        matrix.addColumn();
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("null,null|null,null", matrix);
        matrix.pack();
        Assertions.assertTrue(matrix.isEmpty());
    }

    @Test
    void reverseX() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        matrix.reverseX();
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("b,a|d,c", matrix);
    }

    @Test
    void reverseY() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        matrix.reverseY();
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("c,d|a,b", matrix);
    }

    @Test
    void flip2x2() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        matrix.flip();
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,c|b,d", matrix);
    }

    @Test
    void flip2x3() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        matrix.addRow('e', 'f');
        Assertions.assertTrue(matrix.size().equals(2, 3));
        matrix.flip();
        Assertions.assertTrue(matrix.size().equals(3, 2));
        assertData("a,c,e|b,d,f", matrix);
    }

    @Test
    void flip3x2() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b', 'c');
        matrix.addRow('d', 'e', 'f');
        Assertions.assertTrue(matrix.size().equals(3, 2));
        matrix.flip();
        Assertions.assertTrue(matrix.size().equals(2, 3));
        assertData("a,d|b,e|c,f", matrix);
    }

    @Test
    void turnClockwise() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        matrix.turnClockwise();
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("c,a|d,b", matrix);
    }

    @Test
    void turnCounterClockwise() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        matrix.turnCounterClockwise();
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("b,d|a,c", matrix);
    }

    @Test
    void equals() {
        var matrix1 = new Matrix<Character>();
        matrix1.addRow('a', 'b');
        matrix1.addRow('c', 'd');
        var matrix2 = new Matrix<Character>();
        matrix2.addRow('a', 'b');
        matrix2.addRow('c', 'd');
        Assertions.assertEquals(matrix1, matrix2);
        Assertions.assertEquals('d', matrix2.set(1, 1, 'a'));
        Assertions.assertNotEquals(matrix1, matrix2);
    }

    @Test
    void unmodifiable() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        assertUnmodifiable(() -> matrix.getRow(0).add('X'));
        assertUnmodifiable(() -> matrix.getRows().get(0).add('X'));
        assertUnmodifiable(() -> matrix.getColumn(0).add('Y'));
        assertUnmodifiable(() -> matrix.getColumns().get(0).add('Y'));
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
    }

    @Test
    void unmodifiableCopy() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', null);
        matrix = Matrix.unmodifiableCopy(matrix);
        assertUnmodifiable(matrix::addRow);
        assertUnmodifiable(matrix::addColumn);
        assertUnmodifiable(matrix::removeFirstRow);
        assertUnmodifiable(matrix::removeLastRow);
        assertUnmodifiable(matrix::removeFirstColumn);
        assertUnmodifiable(matrix::removeLastColumn);
        assertUnmodifiable(matrix::clear);
        assertUnmodifiable(matrix::flip);
        assertUnmodifiable(matrix::reverseX);
        assertUnmodifiable(matrix::reverseY);
        assertUnmodifiable(matrix::turnClockwise);
        assertUnmodifiable(matrix::turnCounterClockwise);
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,null", matrix);
    }

    @Test
    void map() {
        var matrix = new Matrix<Character>();
        matrix.addRow('a', 'b');
        matrix.addRow('c', 'd');
        var upper = matrix.map(Character::toUpperCase);
        Assertions.assertTrue(upper.size().equals(2, 2));
        assertData("A,B|C,D", upper);
    }

    @Test
    void newlines() {
        var matrix = new Matrix<String>();
        matrix.addRow(null, "no-newline", "yes\nnewline");
        matrix.addRow("three\nlines\nhere", "100", "200");
        matrix.addRow("1\n2", "\n2\n3", null);
        Assertions.assertEquals("""
                      no-newline yes
                                 newline
                three 100        200
                lines
                here
                1
                2     2
                      3""", matrix.toString());
    }

    @Test
    void table() {
        var matrix = new Matrix<String>();
        matrix.addRow(null, "no-newline", "yes\nnewline");
        matrix.addRow("three\nlines\nhere", "100", "200");
        matrix.addRow("1\n2", "\n2\n3", null);
        Assertions.assertEquals("""
                #1    | Header     | Third
                      | 2          |
                ------|------------|--------
                      | no-newline | yes
                      |            | newline
                three | 100        | 200
                lines |            |
                here  |            |
                1     |            |
                2     | 2          |
                      | 3          |""", matrix.toTableString("#1", "Header\n2", "Third"));
    }

    @Test
    void badIndexes() {
        assertBadIndex(() -> new Matrix<>(0, 1));
        assertBadIndex(() -> new Matrix<>(1, 0));
        assertBadIndex(() -> new Matrix<>(-1, -1));
        assertBadIndex(() -> new Matrix<>(new Object[][] {{}}));
        var matrix = new Matrix<Character>();
        assertBadIndexesNegative(matrix);
        assertBadIndexesEmpty(matrix);
        matrix.addRow('a', 'b');
        Assertions.assertTrue(matrix.size().equals(2, 1));
        assertData("a,b", matrix);
        assertBadIndexesNegative(matrix);
        assertBadIndex(() -> matrix.get(2, 0));
        assertBadIndex(() -> matrix.get(0, 1));
        assertBadIndex(() -> matrix.getRow(1));
        assertBadIndex(() -> matrix.getColumn(2));
        assertBadIndex(() -> matrix.addRowAfter(1));
        assertBadIndex(() -> matrix.addColumnAfter(2));
        assertBadIndex(() -> matrix.addRowAfter(1, 'X'));
        assertBadIndex(() -> matrix.addColumnAfter(2, 'X'));
        assertBadIndex(() -> matrix.addRowBefore(2));
        assertBadIndex(() -> matrix.addColumnBefore(3));
        assertBadIndex(() -> matrix.addRowBefore(2, 'X'));
        assertBadIndex(() -> matrix.addColumnBefore(3, 'X'));
        assertBadIndex(() -> matrix.removeRow(1));
        assertBadIndex(() -> matrix.removeColumn(2));
        assertBadIndex(() -> matrix.setRow(1));
        assertBadIndex(() -> matrix.setRow(1, 'X'));
        assertBadIndex(() -> matrix.setColumn(2));
        assertBadIndex(() -> matrix.setColumn(2, 'X'));
        matrix.addRow('c', 'd');
        Assertions.assertTrue(matrix.size().equals(2, 2));
        assertData("a,b|c,d", matrix);
        assertBadIndexesNegative(matrix);
        assertBadIndex(() -> matrix.get(2, 2));
        assertBadIndex(() -> matrix.getRow(2));
        assertBadIndex(() -> matrix.addRowAfter(2));
        assertBadIndex(() -> matrix.addRowAfter(2, 'X'));
        assertBadIndex(() -> matrix.addRowBefore(3));
        assertBadIndex(() -> matrix.addRowBefore(3, 'X'));
        assertBadIndex(() -> matrix.removeRow(2));
        assertBadIndex(() -> matrix.setRow(2));
        assertBadIndex(() -> matrix.setRow(2, 'X'));
        assertBadIndex(() -> matrix.setColumn(2));
        assertBadIndex(() -> matrix.setColumn(2, 'X'));
        matrix.removeRow(1);
        Assertions.assertTrue(matrix.size().equals(2, 1));
        assertData("a,b", matrix);
        assertBadIndexesNegative(matrix);
        assertBadIndex(() -> matrix.get(2, 0));
        assertBadIndex(() -> matrix.get(0, 1));
        assertBadIndex(() -> matrix.getRow(1));
        assertBadIndex(() -> matrix.getColumn(2));
        assertBadIndex(() -> matrix.addRowAfter(1));
        assertBadIndex(() -> matrix.addColumnAfter(2));
        assertBadIndex(() -> matrix.addRowAfter(1, 'X', 'Y'));
        assertBadIndex(() -> matrix.addColumnAfter(2, 'X', 'Y'));
        assertBadIndex(() -> matrix.addRowBefore(2));
        assertBadIndex(() -> matrix.addColumnBefore(3));
        assertBadIndex(() -> matrix.addRowBefore(2, 'X', 'Y'));
        assertBadIndex(() -> matrix.addColumnBefore(3, 'X', 'Y'));
        assertBadIndex(() -> matrix.removeRow(1));
        assertBadIndex(() -> matrix.removeColumn(2));
        assertBadIndex(() -> matrix.setRow(1));
        assertBadIndex(() -> matrix.setRow(1, 'X'));
        assertBadIndex(() -> matrix.setColumn(2));
        assertBadIndex(() -> matrix.setColumn(2, 'X'));
        matrix.removeColumn(1);
        Assertions.assertTrue(matrix.size().equals(1, 1));
        assertData("a", matrix);
        assertBadIndexesNegative(matrix);
        assertBadIndex(() -> matrix.get(1, 0));
        assertBadIndex(() -> matrix.get(0, 1));
        assertBadIndex(() -> matrix.getRow(1));
        assertBadIndex(() -> matrix.getColumn(1));
        assertBadIndex(() -> matrix.addRowAfter(1));
        assertBadIndex(() -> matrix.addColumnAfter(1));
        assertBadIndex(() -> matrix.addRowAfter(1, 'X', 'Y'));
        assertBadIndex(() -> matrix.addColumnAfter(1, 'X', 'Y'));
        assertBadIndex(() -> matrix.addRowBefore(2));
        assertBadIndex(() -> matrix.addColumnBefore(2));
        assertBadIndex(() -> matrix.addRowBefore(2, 'X', 'Y'));
        assertBadIndex(() -> matrix.addColumnBefore(2, 'X', 'Y'));
        assertBadIndex(() -> matrix.removeRow(1));
        assertBadIndex(() -> matrix.removeColumn(1));
        assertBadIndex(() -> matrix.setRow(1));
        assertBadIndex(() -> matrix.setRow(1, 'X'));
        assertBadIndex(() -> matrix.setColumn(1));
        assertBadIndex(() -> matrix.setColumn(1, 'X'));
        matrix.clear();
        Assertions.assertTrue(matrix.size().equals(0, 0));
        assertData("", matrix);
        assertBadIndexesNegative(matrix);
        assertBadIndexesEmpty(matrix);
    }

    private void assertBadIndexesEmpty(Matrix<Character> matrix) {
        assertBadIndex(() -> matrix.get(0, 0));
        assertBadIndex(matrix::getFirstRow);
        assertBadIndex(matrix::getFirstColumn);
        assertBadIndex(() -> matrix.addRowAfter(0));
        assertBadIndex(() -> matrix.addColumnAfter(0));
        assertBadIndex(() -> matrix.addRowAfter(0, 'X'));
        assertBadIndex(() -> matrix.addColumnAfter(0, 'X'));
        assertBadIndex(() -> matrix.addRowBefore(1));
        assertBadIndex(() -> matrix.addColumnBefore(1));
        assertBadIndex(() -> matrix.addRowBefore(1, 'X'));
        assertBadIndex(() -> matrix.addColumnBefore(1, 'X'));
        assertBadIndex(matrix::removeFirstRow);
        assertBadIndex(matrix::removeFirstColumn);
        assertBadIndex(() -> matrix.setRow(0));
        assertBadIndex(() -> matrix.setRow(0, 'X'));
        assertBadIndex(() -> matrix.setColumn(0));
        assertBadIndex(() -> matrix.setColumn(0, 'X'));
    }

    private void assertBadIndexesNegative(Matrix<Character> matrix) {
        assertBadIndex(() -> matrix.get(-1, 0));
        assertBadIndex(() -> matrix.get(0, -1));
        assertBadIndex(() -> matrix.getRow(-1));
        assertBadIndex(() -> matrix.getColumn(-1));
        assertBadIndex(() -> matrix.addRowAfter(-1));
        assertBadIndex(() -> matrix.addColumnAfter(-1));
        assertBadIndex(() -> matrix.addRowAfter(-1, 'X'));
        assertBadIndex(() -> matrix.addColumnAfter(-1, 'X'));
        assertBadIndex(() -> matrix.addRowBefore(-1));
        assertBadIndex(() -> matrix.addColumnBefore(-1));
        assertBadIndex(() -> matrix.addRowBefore(-1, 'X'));
        assertBadIndex(() -> matrix.addColumnBefore(-1, 'X'));
        assertBadIndex(() -> matrix.removeRow(-1));
        assertBadIndex(() -> matrix.removeColumn(-1));
        assertBadIndex(() -> matrix.setRow(-1));
        assertBadIndex(() -> matrix.setRow(-1, 'X'));
        assertBadIndex(() -> matrix.setColumn(-1));
        assertBadIndex(() -> matrix.setColumn(-1, 'X'));
    }
}
