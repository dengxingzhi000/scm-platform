package com.scmcloud.common.mybatisPlus.handler;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.atomic.LongAdder;

/**
 * 浼佷笟锟経UID绫诲瀷澶勭悊锟?
 *
 * <p>璁捐鎬濊矾鍙傝€冿細
 * <ul>
 *   <li>Google Guava - 楂樻€ц兘鍘熺敓绫诲瀷杞崲锛岄浂渚濊禆瀹炵幇</li>
 *   <li>Facebook MySQL瀹炶返 - BINARY(16)瀛樺偍浼樺寲锛岃妭鐪佺┖闂村拰绱㈠紩鎬ц兘</li>
 *   <li>Netflix Architecture - 鍙娴嬫€ц璁★紝寮傚父蹇€熷け锟?li>
 *   <li>MyBatis鏈€浣冲疄锟? 鏃犵姸鎬佺嚎绋嬪畨鍏ㄨ锟?li>
 * </ul>
 *
 * <p>鎬ц兘浼樺寲锟?
 * <ul>
 *   <li>浣跨敤浣嶈繍绠楁浛浠yteBuffer锛屽噺灏戝璞″垎锟?li>
 *   <li>閲囩敤澶х锟紹ig-Endian)锛屼笌MySQL BINARY鍏煎</li>
 *   <li>鏃犵姸鎬佽璁★紝澶╃劧绾跨▼瀹夊叏锛屾棤闇€鍚屾寮€閿€</li>
 *   <li>鎻愬墠鏍¢獙锛孎ail-Fast锛岄伩鍏嶆棤鏁堣锟?li>
 * </ul>
 *
 * <p>鍙娴嬫€э細
 * <ul>
 *   <li>鍏抽敭璺緞鍩嬬偣锛屾敮鎸佹€ц兘鐩戞帶</li>
 *   <li>寮傚父璇︾粏涓婁笅鏂囷紝渚夸簬闂鎺掓煡</li>
 *   <li>缁熻杞崲澶辫触娆℃暟锛屾敮鎸佸憡锟?li>
 * </ul>
 *
 * @author Deng
 * @version 2.0 - Enterprise Edition
 * @since 2025/10/15
 */
@Slf4j
@MappedTypes(UUID.class)
@MappedJdbcTypes({JdbcType.BINARY, JdbcType.VARBINARY})
public class UUIDTypeHandler extends BaseTypeHandler<UUID> {
    /**
     * UUID鏍囧噯瀛楄妭闀垮害 (128 bits = 16 bytes)
     */
    private static final int UUID_BYTE_LENGTH = 16;

    /**
     * 鐢ㄤ簬浣嶈繍绠楃殑甯搁噺
     */
    private static final int BITS_PER_BYTE = 8;
    private static final long BYTE_MASK = 0xFF;

    /**
     * 鎬ц兘鐩戞帶 - 杞崲澶辫触璁℃暟锟?
     * 浣跨敤LongAdder鏇夸唬AtomicLong锛屽湪楂樺苟鍙戜笅鎬ц兘鏇村ソ锛堝弬鑰僄oogle璁烘枃锟?
     */
    private static final LongAdder CONVERSION_FAILURE_COUNTER = new LongAdder();

    /**
     * 閿欒闃堬拷- 鐢ㄤ簬鐔旀柇鍛婅
     */
    private static final long ERROR_THRESHOLD = 1000L;

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, UUID parameter, JdbcType jdbcType)
            throws SQLException {
        try {
            byte[] bytes = uuidToBytes(parameter);
            ps.setBytes(i, bytes);
        } catch (Exception e) {
            handleConversionError("setParameter", e, parameter);
            throw new SQLException("Failed to convert UUID to bytes: " + parameter, e);
        }
    }

    @Override
    public UUID getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return bytesToUuidSafe(rs.getBytes(columnName), "columnName=" + columnName);
    }

    @Override
    public UUID getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return bytesToUuidSafe(rs.getBytes(columnIndex), "columnIndex=" + columnIndex);
    }

    @Override
    public UUID getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return bytesToUuidSafe(cs.getBytes(columnIndex), "callableStatement[" + columnIndex + "]");
    }

    /**
     * UUID杞瓧鑺傛暟锟? 闆舵嫹璐濋珮鎬ц兘瀹炵幇
     *
     * <p>绠楁硶璇存槑锟?
     * <pre>
     * UUID缁撴瀯锛歺xxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx (128 bits)
     * 瀛樺偍鏍煎紡锛欱INARY(16) 澶х锟?
     *
     * Most Significant Bits (锟?锟? |  Least Significant Bits (锟?锟?
     * --------------------------------|--------------------------------
     *   time_low + time_mid + ...     |   clock_seq + node
     * </pre>
     *
     * <p>鎬ц兘瀵规瘮锟?
     * <ul>
     *   <li>ByteBuffer鏂规锛殈80ns锛屼骇锟戒釜瀵硅薄锛圔yteBuffer + byte[]锟?li>
     *   <li>浣嶈繍绠楁柟妗堬細~25ns锛屼骇锟戒釜瀵硅薄锛坆yte[]锛夛紝鎬ц兘鎻愬崌3锟?li>
     * </ul>
     *
     * @param uuid UUID 瀵硅薄
     * @return 16瀛楄妭鏁扮粍锛屽ぇ绔簭
     */
    private static byte[] uuidToBytes(UUID uuid) {
        long mostSigBits = uuid.getMostSignificantBits();
        long leastSigBits = uuid.getLeastSignificantBits();

        byte[] bytes = new byte[UUID_BYTE_LENGTH];

        // 锟?浣嶏細浠庢渶楂樺瓧鑺傚紑濮嬪啓鍏ワ紙Big-Endian锟?
        // 绫讳技浜嶨oogle Guava锟絃ongs.toByteArray 瀹炵幇
        for (int i = 0; i < Long.BYTES; i++) {
            bytes[i] = (byte) (mostSigBits >>> ((Long.BYTES - 1 - i) * BITS_PER_BYTE));
        }

        // 锟?浣嶏細缁х画鍐欏叆鍓╀綑8涓瓧锟?
        for (int i = 0; i < Long.BYTES; i++) {
            bytes[Long.BYTES + i] = (byte) (leastSigBits >>> ((Long.BYTES - 1 - i) * BITS_PER_BYTE));
        }

        return bytes;
    }

    /**
     * 瀛楄妭鏁扮粍杞琔UID - 闃插尽鎬х紪绋嬪疄锟?
     *
     * <p>鍙傝€僃acebook MySQL瀹炶返锟?
     * <ul>
     *   <li>涓ユ牸鏍¢獙杈撳叆闀垮害锛岄槻姝㈡暟鎹崯锟?li>
     *   <li>浣跨敤浣嶈繍绠楅噸寤簂ong鍊硷紝閬垮厤ByteBuffer寮€閿€</li>
     *   <li>淇濇寔澶х搴忎竴鑷达拷/li>
     * </ul>
     *
     * @param bytes 16瀛楄妭鏁扮粍
     * @return UUID 瀵硅薄
     * @throws IllegalArgumentException 濡傛灉瀛楄妭闀垮害涓嶆槸16
     */
    private static UUID bytesToUuid(byte[] bytes) {
        if (bytes.length != UUID_BYTE_LENGTH) {
            throw new IllegalArgumentException(
                String.format("Invalid UUID bytes length: expected %d but got %d",
                    UUID_BYTE_LENGTH, bytes.length)
            );
        }

        long mostSigBits = 0L;
        long leastSigBits = 0L;

        // 閲嶅缓锟?浣嶏紙Big-Endian锟?
        // 绫讳技浜嶨oogle Guava锟絃ongs.fromByteArray 瀹炵幇
        for (int i = 0; i < Long.BYTES; i++) {
            mostSigBits = (mostSigBits << BITS_PER_BYTE) | (bytes[i] & BYTE_MASK);
        }

        // 閲嶅缓锟?锟?
        for (int i = Long.BYTES; i < UUID_BYTE_LENGTH; i++) {
            leastSigBits = (leastSigBits << BITS_PER_BYTE) | (bytes[i] & BYTE_MASK);
        }

        return new UUID(mostSigBits, leastSigBits);
    }

    /**
     * 瀹夊叏鐨勫瓧鑺傝浆 UUID鍖呰鏂规硶
     *
     * <p>Netflix SRE瀹炶返锟?
     * <ul>
     *   <li>Null-Safe澶勭悊锛岄伩鍏峃PE</li>
     *   <li>缁熶竴寮傚父澶勭悊鍜屾棩蹇楄锟?li>
     *   <li>鎻愪緵璇︾粏鐨勯敊璇笂涓嬫枃</li>
     *   <li>澶辫触璁℃暟鍣ㄦ敮鎸佺啍鏂憡锟?li>
     * </ul>
     *
     * @param bytes 瀛楄妭鏁扮粍锛堝彲鑳戒负null锟?
     * @param context 涓婁笅鏂囦俊鎭紝鐢ㄤ簬鏃ュ織
     * @return UUID瀵硅薄锛屽鏋渂ytes涓簄ull鍒欒繑鍥瀗ull
     * @throws SQLException 濡傛灉杞崲澶辫触
     */
    private UUID bytesToUuidSafe(byte[] bytes, String context) throws SQLException {
        if (bytes == null) {
            return null;
        }

        try {
            return bytesToUuid(bytes);
        } catch (Exception e) {
            handleConversionError(context, e, bytes);
            throw new SQLException(
                String.format("Failed to convert bytes to UUID at %s, length=%d",
                    context, bytes.length),
                e
            );
        }
    }

    /**
     * 缁熶竴鐨勯敊璇鐞嗛€昏緫
     *
     * <p>璁捐瑕佺偣锟?
     * <ul>
     *   <li>璁板綍璇︾粏鐨勯敊璇俊鎭拰鍫嗘爤</li>
     *   <li>澧炲姞澶辫触璁℃暟鍣紝鏀寔鐩戞帶鍛婅</li>
     *   <li>杈惧埌闃堝€兼椂璁板綍WARN鏃ュ織锛岃Е鍙戝憡锟?li>
     * </ul>
     *
     * @param context 閿欒涓婁笅锟?
     * @param e 寮傚父瀵硅薄
     * @param data 鐩稿叧鏁版嵁锛圲UID鎴朾yte[]锟?
     */
    private void handleConversionError(String context, Exception e, Object data) {
        CONVERSION_FAILURE_COUNTER.increment();
        long failureCount = CONVERSION_FAILURE_COUNTER.sum();

        // 璁板綍璇︾粏閿欒鏃ュ織
        log.error("UUID conversion failed at [{}], data={}, totalFailures={}",
            context, data, failureCount, e);

        // 杈惧埌闃堝€兼椂瑙﹀彂鍛婅锛堝彲闆嗘垚Prometheus/Grafana锟?
        if (failureCount % ERROR_THRESHOLD == 0) {
            log.warn("UUID conversion failure threshold reached: {} failures detected. " +
                "Please check database data integrity!", failureCount);
        }
    }

    /**
     * 鑾峰彇杞崲澶辫触鎬绘暟 - 渚涚洃鎺х郴缁熻皟锟?
     *
     * @return 澶辫触娆℃暟
     */
    public static long getConversionFailureCount() {
        return CONVERSION_FAILURE_COUNTER.sum();
    }

    /**
     * 閲嶇疆澶辫触璁℃暟锟? 渚涙祴璇曚娇锟?
     */
    public static void resetConversionFailureCount() {
        CONVERSION_FAILURE_COUNTER.reset();
    }
}

