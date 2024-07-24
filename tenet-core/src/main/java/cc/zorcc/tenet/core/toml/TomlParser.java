package cc.zorcc.tenet.core.toml;

import cc.zorcc.tenet.core.*;
import jdk.incubator.vector.VectorOperators;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TomlParser {
    private static final String UNREACHED = "unreachable toml state";

    private static final byte NUT = (byte) '\0';
    private static final byte SPACE = (byte) ' ';
    private static final byte DEL = (byte) '\u007F';
    private static final byte LCB = (byte) '[';
    private static final byte RCB = (byte) ']';
    private static final byte COMMA = (byte) ',';
    private static final byte HASH = (byte) '#';
    private static final byte CR = (byte) '\r';
    private static final byte LF = (byte) '\n';
    private static final byte EQ = (byte) '=';
    /**
     *   Manually set parsing limitation
     */
    private static final int TOML_PARSING_LIMITATION = Integer.MAX_VALUE;

    /**
     *   Data structure for building an unmodified TomlTable
     */
    private static final class TomlConstructor {

        private final List<Map.Entry<String, TomlValue>> valueList = new ArrayList<>();
        private final List<Map.Entry<String, TomlTable>> tableList = new ArrayList<>();
        private String currentKey;

        private void constructKey(String key) {
            if(currentKey != null) {
                throw new TomlException("Key duplicates");
            }
            currentKey = key;
        }

        private void constructTable(String name) {

        }

        private void constructValue(TomlValue tomlValue) {

        }

        private void constructArrayOfTable(String name) {

        }
    }

    /**
     *   Toml parsing state enum
     */
    enum TomlState {
        /**
         *   When nothing in a newLine has been parsed, the state were default to initial
         */
        INITIAL,

        TABLE,

        ERR,

        /**
         *   When no more content could be extracted from the readBuffer, current toml table would be returned
         */
        FINISHED,
    }

    sealed interface TomlSearcher {
        /**
         *   Searching the next valuable byte in current toml document
         */
        SearchedResult searchNextValuableByte(ReadBuffer readBuffer);

        /**
         *   Searching the next CR or LF in current toml document
         */
        SearchedResult searchNextCRLF(ReadBuffer readBuffer);

        /**
         *   Searching the next RCB in current toml document
         */
        SearchedResult searchNextRCB(ReadBuffer readBuffer);

        /**
         *   Searching the next SPACE or EQ in current toml document
         */
        SearchedResult searchNextSpaceOrEq(ReadBuffer readBuffer);
    }

    /**
     *   Toml vector searcher based on SIMD operations
     */
    private static final class TomlVectorSearcher implements TomlSearcher {
        private static final VectorMatcher valuable_matcher = vec -> {
            var gt = vec.compare(VectorOperators.GT, SPACE);
            var lt = vec.compare(VectorOperators.LT, DEL);
            return gt.and(lt);
        };
        private static final VectorMatcher crlf_matcher = vec -> {
            var cr = vec.compare(VectorOperators.EQ, CR);
            var lf = vec.compare(VectorOperators.EQ, LF);
            return cr.or(lf);
        };
        private static final VectorMatcher rcb_matcher = vec -> vec.compare(VectorOperators.EQ, RCB);
        private static final VectorMatcher space_or_eq_matcher = vec -> {
            var sp = vec.compare(VectorOperators.EQ, SPACE);
            var eq = vec.compare(VectorOperators.EQ, EQ);
            return sp.or(eq);
        };

        @Override
        public SearchedResult searchNextValuableByte(ReadBuffer readBuffer) {
            return readBuffer.vectorSearch(valuable_matcher);
        }

        @Override
        public SearchedResult searchNextCRLF(ReadBuffer readBuffer) {
            return readBuffer.vectorSearch(crlf_matcher);
        }

        @Override
        public SearchedResult searchNextRCB(ReadBuffer readBuffer) {
            return readBuffer.vectorSearch(rcb_matcher);
        }

        @Override
        public SearchedResult searchNextSpaceOrEq(ReadBuffer readBuffer) {
            return readBuffer.vectorSearch(space_or_eq_matcher);
        }
    }

    /**
     *   Toml linear searcher, if the system doesn't support SIMD operations, this searcher would be used
     */
    private static final class TomlLinearSearcher implements TomlSearcher {
        private static final ByteMatcher valuable_matcher = b -> b > SPACE && b < DEL;
        private static final ByteMatcher crlf_matcher = b -> b == CR || b == LF;
        private static final ByteMatcher rcb_matcher = b -> b == RCB;
        private static final ByteMatcher space_or_eq_matcher = b -> b == SPACE || b == EQ;

        @Override
        public SearchedResult searchNextValuableByte(ReadBuffer readBuffer) {
            return readBuffer.linearSearch(valuable_matcher);
        }

        @Override
        public SearchedResult searchNextCRLF(ReadBuffer readBuffer) {
            return readBuffer.linearSearch(crlf_matcher);
        }

        @Override
        public SearchedResult searchNextRCB(ReadBuffer readBuffer) {
            return readBuffer.linearSearch(rcb_matcher);
        }

        @Override
        public SearchedResult searchNextSpaceOrEq(ReadBuffer readBuffer) {
            return readBuffer.linearSearch(space_or_eq_matcher);
        }
    }

    /**
     *   Global toml byte searcher
     */
    private static final TomlSearcher searcher = createTomlSearcher();

    /**
     *   Creating global toml byte searcher, using LinearSearcher as default strategy, using VectorSearcher if enabled
     */
    private static TomlSearcher createTomlSearcher() {
        if(Std.isVectorEnabled()) {
            return new TomlVectorSearcher();
        }else {
            return new TomlLinearSearcher();
        }
    }

    /**
     *   Peek the following byte, if not match, reset the current readIndex, throw an exception if the document is not sufficient
     */
    private static boolean peekByte(ReadBuffer readBuffer, ByteMatcher byteMatcher) {
        final long currentIndex = readBuffer.readIndex();
        if(readBuffer.size() - currentIndex >= Byte.SIZE) {
            if(byteMatcher.match(readBuffer.readByte())) {
                return true;
            }else {
                readBuffer.setIndex(currentIndex);
                return false;
            }
        }else {
            throw new TomlException("Toml document not complete");
        }
    }

    /**
     *  Keep peek the following bytes as long as they could match the target byteMatcher, return the first one not matching it
     */
    private static byte peekUntil(ReadBuffer readBuffer, ByteMatcher byteMatcher) {
        int len = Math.toIntExact(readBuffer.available());
        for(int i = 0; i < len; i++) {
            byte target = readBuffer.readByte();
            if(!byteMatcher.match(target)) {
                return target;
            }
        }
        throw new TomlException("Toml document not complete");
    }

    /**
     *   Toml parsing buffer, read-only
     */
    private final ReadBuffer readBuffer;
    /**
     *   Toml parsing result table
     */
    private final TomlConstructor constructor = new TomlConstructor();

    public TomlParser(ReadBuffer readBuffer) {
        this.readBuffer = readBuffer;
    }

    /**
     *   Start parsing current toml document
     */
    public void parse() {
        TomlState state = TomlState.INITIAL;
        for(int i = 0; i < TOML_PARSING_LIMITATION; i++) {
            state = transform(state);
            if(state == TomlState.FINISHED) {
                return ;
            }
        }
    }

    /**
     *   Transforming to next tomlState based on currentState
     */
    private TomlState transform(TomlState currentState) {
        return switch (currentState) {
            case null -> throw new TomlException(UNREACHED);
            case INITIAL -> parseInitial();
        };
    }

    /**
     *   Parsing initial byte value, could be table name, or global key-value pairs
     */
    private TomlState parseInitial() {
        return switch (searcher.searchNextValuableByte(readBuffer)) {
            case null -> throw new TomlException(UNREACHED);
            case SearchedResult.Value(byte b) -> switch (b) {
                case HASH -> parseComment();
                case LCB -> parseTable();
                default -> parseKey();
            };
            case SearchedResult.Empty() -> TomlState.FINISHED;
        };
    }

    /**
     *   Parsing comment expression
     *   Tenet toml parser支持两种不同的注释形式，即整行注释和行尾注释
     *   在toml规范中，要求Control characters other than tab (U+0000 to U+0008, U+000A to U+001F, U+007F) are not permitted in comments.
     *   不过在tenet提供的toml注释解析中不会对此进行严格要求
     *   parseComment是一个非常重要的方法，他使得我们按行解析toml文档，从而完全规避了栈溢出的风险
     */
    private TomlState parseComment() {
        return switch (searcher.searchNextCRLF(readBuffer)) {
            case null -> throw new TomlException(UNREACHED);
            case SearchedResult.Value(byte b) -> {
                switch (b) {
                    case CR -> {
                        if(readBuffer.available() > Byte.BYTES && readBuffer.readByte() == LF) {
                            yield TomlState.INITIAL;
                        }else {
                            throw new TomlException("CR were not followed by LF, illegal toml document");
                        }
                    }
                    case LF -> {
                        yield TomlState.INITIAL;
                    }
                    default -> throw new TomlException(UNREACHED);
                }
            }
            case SearchedResult.Empty() -> TomlState.FINISHED;
        };
    }

    /**
     *   解析key部分，读取到下一个空格或是等号处停止，如果先读到的是空格，那么继续解析至下一个等号处
     *   tenet toml解析器只支持Bare Keys，不支持Quoted keys，也就是说键不允许用双引号进行包裹，允许使用dotted keys
     */
    private TomlState parseKey() {
        long currentIndex = readBuffer.readIndex();
        return switch (searcher.searchNextSpaceOrEq(readBuffer)) {
            case null -> throw new TomlException(UNREACHED);
            case SearchedResult.Value(byte b) -> switch (b) {
                case EQ -> {
                    long readIndex = readBuffer.readIndex();
                    constructor.constructKey(new String(readBuffer.copy(currentIndex - Byte.BYTES, readIndex - Byte.BYTES)));
                    yield parseValue();
                }
                case SPACE -> {
                    long readIndex = readBuffer.readIndex();
                    if (!(peekUntil(readBuffer, target -> target == SPACE) == EQ)) {
                        throw new TomlException("separation between keys and values using EQ was not found");
                    }
                    constructor.constructKey(new String(readBuffer.copy(currentIndex - Byte.BYTES, readIndex - Byte.BYTES)));
                    yield parseValue();
                }
                default -> throw new TomlException(UNREACHED);
            };
            case SearchedResult.Empty() -> throw new TomlException("Key not valid");
        };
    }

    private TomlState parseValue() {

    }

    /**
     *   当遇到第一个LCB后，开始解析表名，可以是普通Table，或是Array of Tables
     */
    private TomlState parseTable() {
        if (peekByte(readBuffer, target -> target == LCB)) {
            return parseArrayOfTable();
        }
        long currentIndex = readBuffer.readIndex();
        return switch (searcher.searchNextRCB(readBuffer)) {
            case null -> throw new TomlException(UNREACHED);
            case SearchedResult.Value(byte _) -> {
                long readIndex = readBuffer.readIndex();
                constructor.constructTable(new String(readBuffer.copy(currentIndex, readIndex - Byte.BYTES), StandardCharsets.UTF_8));
                yield parseComment();
            }
            case SearchedResult.Empty() -> throw new TomlException("Table structure not complete");
        };
    }

    /**
     *   当连续遇到两个LCB后，开始解析表数组类型，在连续读取到两个RCB之后提交表数组内容
     */
    private TomlState parseArrayOfTable() {
        long currentIndex = readBuffer.readIndex();
        for(int i = 0; i < TOML_PARSING_LIMITATION; i++) {
            if(searcher.searchNextRCB(readBuffer) instanceof SearchedResult.Value _) {
                long readIndex = readBuffer.readIndex();
                if(peekByte(readBuffer, target -> target == RCB)) {
                    constructor.constructArrayOfTable(new String(readBuffer.copy(currentIndex, readIndex - Byte.BYTES), StandardCharsets.UTF_8));
                    return parseComment();
                }
            } else {
                throw new TomlException("RCB not found for current toml document");
            }
        }
        throw new TomlException("Iteration reaching limits");
    }

    /**
     *   Parsing key start situation
     *   There are two cases:
     *   1. Following byte is SPACE, then we will find the next EQ byte
     *   2. Following byte is EQ, then we start parsing value by finding the next valuable byte
     */
    private TomlState parseKeyStart(byte firstByte) {
        return switch (firstByte) {
            case SPACE -> TomlState.TABLE; // TODO
            default -> TomlState.ERR;
        };
    }
}
