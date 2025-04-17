package winstone.cmdline;

/**
 * The compression schemes supported by the server. In the future, this list may be expanded as support for additional
 * compression schemes is added to Jetty upstream.
 */
public enum CompressionScheme {
    BROTLI,
    GZIP,
    ZSTD,
    NONE,
}
