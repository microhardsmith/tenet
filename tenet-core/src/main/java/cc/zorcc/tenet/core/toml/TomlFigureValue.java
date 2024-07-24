package cc.zorcc.tenet.core.toml;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/**
 *   TomlFigure is a numeric type of value, which might be Byte, Short, Integer, Long, Float, Double, Boolean, Offset Date-Time, Local Date-Time, Local Date, Local Time
 *   The actual numeric type of TomlFigure is determined by the consumer, specifying how to parse the node value themselves
 */
public record TomlFigureValue(
        byte[] value
) implements TomlValue {

    private static final byte POSITIVE = (byte) '+';
    private static final byte NEGATIVE = (byte) '-';
    private static final byte ZERO = (byte) '0';
    private static final byte ONE = (byte) '1';
    private static final byte SEVEN = (byte) '7';
    private static final byte NINE = (byte) '9';
    private static final byte x = (byte) 'x';
    private static final byte o = (byte) 'o';
    private static final byte a = (byte) 'a';
    private static final byte b = (byte) 'b';
    private static final byte f = (byte) 'f';
    private static final byte A = (byte) 'A';
    private static final byte F = (byte) 'F';
    private static final byte UNDER_SCORE = (byte) '_';

    public TomlFigureValue {
        if(value.length == 0) {
            throw new TomlException("No value given for current tomlFigure");
        }
    }

    /**
     *   Parsing current tomlFigureValue as a byte value, throw a TomlException if failed
     */
    public byte asByteValue() {
        long value = asLongValue();
        if(value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            return (byte) value;
        }else {
            throw new TomlException("Byte value overflow");
        }
    }

    /**
     *   Parsing current tomlFigureValue as a short value, throw a TomlException if failed
     */
    public short asShortValue() {
        long value = asLongValue();
        if(value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            return (short) value;
        }else {
            throw new TomlException("Short value overflow");
        }
    }

    /**
     *   Parsing current tomlFigureValue as an int value, throw a TomlException if failed
     */
    public int asIntValue() {
        long value = asLongValue();
        if(value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
            return (int) value;
        }else {
            throw new TomlException("Int value overflow");
        }
    }

    /**
     *   Parsing tomlFigureValue as a long value, throw a TomlException if failed
     */
    private long asLongValue() {
        return switch (value[0]) {
            case POSITIVE -> {
                long r = shiftDecimalValue(1);
                if(r == Long.MIN_VALUE) {
                    throw new TomlException("Long value overflow");
                }
                yield -r;
            }
            case NEGATIVE -> shiftDecimalValue(1);
            case ZERO -> switch (value.length) {
                case 1 -> 0L;
                case 2 -> throw new TomlException("No range value present in current tomlFigure");
                default -> switch (value[1]) {
                    case x -> shiftHexValue();
                    case o -> shiftOctValue();
                    case b -> shiftBinaryValue();
                    default -> throw new TomlException("Numeric value corrupted");
                };
            };
            default -> {
                long r = shiftDecimalValue(0);
                if(r == Long.MIN_VALUE) {
                    throw new TomlException("Long value overflow");
                }
                yield -r;
            }
        };
    }

    /**
     *   Parsing decimal value based on shifting
     *   Note that this function would always return a negative value for overflow control
     */
    private long shiftDecimalValue(int startIndex) {
        try {
            long r = 0L;
            for(int i = startIndex; i < value.length; i++) {
                byte b = value[i];
                if(b == UNDER_SCORE) {
                    continue ;
                }
                if(b >= ZERO && b <= NINE) {
                    r = Math.subtractExact(Math.multiplyExact(r, 10L), b - ZERO);
                } else {
                    throw new TomlException("Unrecognized numeric value : %d".formatted(b));
                }
            }
            return r;
        } catch (ArithmeticException e) {
            throw new TomlException("Arithmetic overflow for current value : %s".formatted(new String(value, StandardCharsets.UTF_8)));
        }
    }

    private long shiftHexValue() {
        if(value.length > 18) {
            throw new TomlException("Hex value overflow");
        }
        long r = 0L;
        for(int i = 2; i < value.length; i++) {
            byte b = value[i];
            if(b == UNDER_SCORE) {
                continue ;
            }
            r = (r << 4) | hexDump(b);
        }
        return r;
    }

    /**
     *   TODO could use switch after JDK23
     */
    private static int hexDump(byte b) {
        if(b >= ZERO && b <= NINE) {
            return b - ZERO;
        }else if(b >= a && b <= f) {
            return b - a + 10;
        }else if(b >= A && b <= F) {
            return b - A + 10;
        }else {
            throw new TomlException("Unrecognized hex digit : %d".formatted(b));
        }
    }

    private long shiftOctValue() {
        if(value.length > 23 && octDump(value[2]) > 1) {
            throw new TomlException("oct value overflow");
        }
        long r = 0L;
        for(int i = 2; i < value.length; i++) {
            byte b = value[i];
            if(b == UNDER_SCORE) {
                continue ;
            }
            r = (r << 3) | octDump(b);
        }
        return r;
    }

    private static int octDump(byte b) {
        if(b >= ZERO && b <= SEVEN) {
            return b - ZERO;
        }else {
            throw new TomlException("Unrecognized hex digit : %d".formatted(b));
        }
    }

    private long shiftBinaryValue() {
        if(value.length > 66) {
            throw new TomlException("binary value overflow");
        }
        long r = 0L;
        for(int i = 2; i < value.length; i++) {
            byte b = value[i];
            if(b == UNDER_SCORE) {
                continue ;
            }
            r = (r << 1) | binaryDump(b);
        }
        return r;
    }

    private static int binaryDump(byte b) {
        if(b >= ZERO && b <= ONE) {
            return b - ZERO;
        }else {
            throw new TomlException("Unrecognized hex digit : %d".formatted(b));
        }
    }

    /**
     *   Parsing tomlFigureValue as float value
     *   Using jdk provided methods for convenience
     */
    private float asFloatValue() {
        String str = new String(value, StandardCharsets.UTF_8);
        try {
            return Float.parseFloat(str);
        } catch (NumberFormatException e) {
            throw new TomlException("Unrecognized float value : %s, err : %s".formatted(str, e.getMessage()));
        }
    }

    /**
     *   Parsing tomlFigureValue as double value
     *   Using jdk provided methods for convenience
     */
    private double asDoubleValue() {
        String str = new String(value, StandardCharsets.UTF_8);
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            throw new TomlException("Unrecognized double value : %s, err : %s".formatted(str, e.getMessage()));
        }
    }

    /**
     *   Parsing tomlFigureValue as boolean value
     *   Using string comparison for convenience
     */
    private boolean asBooleanValue() {
        String str = new String(value, StandardCharsets.UTF_8);
        if(str.equals("true")) {
            return true;
        }else if(str.equals("false")) {
            return false;
        }else {
            throw new TomlException("Unrecognized boolean value : %s".formatted(str));
        }
    }

    /**
     *   Parsing tomlFigureValue as offsetDateTime value
     *   Using jdk provided methods for convenience
     */
    private OffsetDateTime asOffsetDateTime() {
        String str = new String(value, StandardCharsets.UTF_8);
        try {
            return OffsetDateTime.parse(str);
        }catch (DateTimeParseException e) {
            throw new TomlException("Unable to parse OffsetDateTime : %s, err :%s".formatted(str, e.getMessage()));
        }
    }

    /**
     *   Parsing tomlFigureValue as localDateTime value
     *   Using jdk provided methods for convenience
     */
    private LocalDateTime asLocalDateTime() {
        String str = new String(value, StandardCharsets.UTF_8);
        try {
            return LocalDateTime.parse(str);
        } catch (DateTimeParseException e) {
            throw new TomlException("Unable to parse LocalDateTime: %s, err: %s".formatted(str, e.getMessage()));
        }
    }

    /**
     *   Parsing tomlFigureValue as localDate value
     *   Using jdk provided methods for convenience
     */
    private LocalDate asLocalDate() {
        String str = new String(value, StandardCharsets.UTF_8);
        try {
            return LocalDate.parse(str);
        } catch (DateTimeParseException e) {
            throw new TomlException("Unable to parse LocalDate: %s, err: %s".formatted(str, e.getMessage()));
        }
    }

    /**
     *   Parsing tomlFigureValue as localTime value
     *   Using jdk provided methods for convenience
     */
    private LocalTime asLocalTime() {
        String str = new String(value, StandardCharsets.UTF_8);
        try {
            return LocalTime.parse(str);
        } catch (DateTimeParseException e) {
            throw new TomlException("Unable to parse LocalTime: %s, err: %s".formatted(str, e.getMessage()));
        }
    }
}
