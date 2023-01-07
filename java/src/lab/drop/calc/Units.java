package lab.drop.calc;

import lab.drop.Sugar;

import java.util.stream.Stream;

/**
 * Units converters and utilities.
 */
public abstract class Units {

    private Units() {}

    private interface Unit {

        long getValue();

        long getLimit();

        String name();

        default String describe(double value, Unit[] units) {
            return Stream.of(units).filter(unit -> Math.abs(value) <= unit.getLimit()).findFirst()
                    .orElse(Sugar.last(units)).describe(value, Sugar.first(units));
        }

        private String describe(double value, Unit smallestUnit) {
            double unitValue = Scale.getDefault().apply(value / getValue());
            return String.format("%s %s", this == smallestUnit ? Long.toString((long) unitValue) :
                    Double.toString(unitValue), name());
        }

        default double convert(double value, Unit targetUnit) {
            return value * ((double) getValue() / targetUnit.getValue());
        }
    }

    /**
     * Numeric values description tool.
     */
    public static abstract class Numeric {

        private enum NumericUnit implements Unit {
            numeric(1),
            K(numeric.getLimit()),
            M(K.getLimit()),
            B(M.getLimit());

            private final long value;

            NumericUnit(long value) {
                this.value = value;
            }

            @Override
            public long getValue() {
                return value;
            }

            @Override
            public long getLimit() {
                return value * 1000;
            }
        }

        private Numeric() {}

        /**
         * Describes the numeric value.
         */
        public static String describe(long value) {
            return Sugar.remove(NumericUnit.numeric.describe(value, NumericUnit.values()), " ",
                    NumericUnit.numeric.name());
        }
    }

    /**
     * Time units converters and utilities.
     */
    public static abstract class Time {

        enum TimeUnit implements Unit {
            milliseconds(1, 2000),
            seconds(1000, 120),
            minutes(60 * seconds.getValue(), 120),
            hours(60 * minutes.getValue(), 72),
            days(24 * hours.getValue(), 21),
            weeks(7 * days.getValue(), 1000);

            private final long value;
            private final long limit;

            TimeUnit(long value, long limitFactor) {
                this.value = value;
                limit = value * limitFactor;
            }

            @Override
            public long getValue() {
                return value;
            }

            @Override
            public long getLimit() {
                return limit;
            }
        }

        /**
         * One second in milliseconds.
         */
        public static final long SECOND = TimeUnit.seconds.getValue();
        /**
         * One minute in milliseconds.
         */
        public static final long MINUTE = TimeUnit.minutes.getValue();
        /**
         * One hour in milliseconds.
         */
        public static final long HOUR = TimeUnit.hours.getValue();
        /**
         * One day in milliseconds.
         */
        public static final long DAY = TimeUnit.days.getValue();
        /**
         * One week in milliseconds.
         */
        public static final long WEEK = TimeUnit.weeks.getValue();

        private Time() {}

        /**
         * Describes the time in appropriate units.
         * @param millis The time in milliseconds.
         * @return The time description.
         */
        public static String describe(long millis) {
            return TimeUnit.milliseconds.describe(millis, TimeUnit.values());
        }

        /**
         * Describes the time in appropriate units.
         * @param nano The time in nanoseconds.
         * @return The time description.
         */
        public static String describeNano(long nano) {
            return describe(convertNanoToMillis(nano));
        }

        /**
         * Returns the milliseconds passed since a timestamp: <code>System.currentTimeMillis() - startMillis</code>.
         */
        public static long since(long startMillis) {
            return System.currentTimeMillis() - startMillis;
        }

        /**
         * Returns the nanoseconds passed since a nano timestamp: <code>System.nanoTime() - startNano</code>.
         */
        public static long sinceNano(long startNano) {
            return System.nanoTime() - startNano;
        }

        /**
         * Describes the milliseconds passed since a timestamp in appropriate units.
         */
        public static String describeSince(long startMillis) {
            return describe(since(startMillis));
        }

        /**
         * Describes the nanoseconds passed since a nano timestamp in appropriate units.
         */
        public static String describeSinceNano(long startNano) {
            return describeNano(sinceNano(startNano));
        }

        /**
         * Converts nanoseconds to milliseconds.
         */
        public static long convertNanoToMillis(long nano) {
            return nano / 1000000L;
        }
    }

    /**
     * Size units converters and utilities.
     */
    public static abstract class Size {

        enum SizeUnit implements Unit {
            bytes(1),
            KB(bytes.getLimit()),
            MB(KB.getLimit()),
            GB(MB.getLimit()),
            TB(GB.getLimit()),
            PB(TB.getLimit());

            private final long value;

            SizeUnit(long value) {
                this.value = value;
            }

            @Override
            public long getValue() {
                return value;
            }

            @Override
            public long getLimit() {
                return value * 1024;
            }
        }

        /**
         * One KB in bytes.
         */
        public static final long KB = SizeUnit.KB.getValue();
        /**
         * One MB in bytes.
         */
        public static final long MB = SizeUnit.MB.getValue();
        /**
         * One GB in bytes.
         */
        public static final long GB = SizeUnit.GB.getValue();
        /**
         * One TB in bytes.
         */
        public static final long TB = SizeUnit.TB.getValue();
        /**
         * One PB in bytes.
         */
        public static final long PB = SizeUnit.PB.getValue();

        private Size() {}

        /**
         * Describes the size in appropriate units.
         * @param bytes The size in bytes.
         * @return The size description.
         */
        public static String describe(long bytes) {
            return SizeUnit.bytes.describe(bytes, SizeUnit.values());
        }

        /**
         * Converts bytes to KB.
         */
        public static double convertBytesToKB(long bytes) {
            return SizeUnit.bytes.convert(bytes, SizeUnit.KB);
        }

        /**
         * Converts bytes to MB.
         */
        public static double convertBytesToMB(long bytes) {
            return SizeUnit.bytes.convert(bytes, SizeUnit.MB);
        }

        /**
         * Converts MB to bytes.
         */
        public static double convertMBToBytes(double MB) {
            return SizeUnit.MB.convert(MB, SizeUnit.bytes);
        }
    }
}
