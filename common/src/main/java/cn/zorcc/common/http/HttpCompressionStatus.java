package cn.zorcc.common.http;

/**
 *   HttpCompressionStatus is an enum used in HttpResponse for the HttpEncoder to decide whether the content should be compressed
 */
public enum HttpCompressionStatus {
    NONE,
    GZIP,
    DEFLATE,
    BROTLI,
    ZSTD
}
