package com.scmcloud.product.search.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 鍟嗗搧鎼滅储鏂囨。
 *
 * <p>鐢ㄤ簬 Elasticsearch 鍏ㄦ枃鎼滅储鐨勫晢鍝佹枃锟?
 *
 * <p>绱㈠紩璁捐锟?
 * - 浣跨敤 IK 鍒嗚瘝鍣ㄨ繘琛屼腑鏂囧垎锟?
 * - ik_max_word: 鏈€缁嗙矑搴﹀垎璇嶏紝鐢ㄤ簬鎼滅储瀛楁
 * - ik_smart: 绮楃矑搴﹀垎璇嶏紝鐢ㄤ簬鑱氬悎瀛楁
 * - 5 鍒嗙墖锟?鍓湰锛屾敮鎸佹按骞虫墿锟?
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Data
@Document(indexName = "scm_product")
@Setting(shards = 5)
public class ProductDocument {

    /**
     * SPU ID锛堜富閿級
     */
    @Id
    private String id;

    /**
     * SPU 缂栫爜
     */
    @Field(type = FieldType.Keyword)
    private String spuCode;

    /**
     * SPU 鍚嶇О锛堜娇锟絀K 鍒嗚瘝鍣紝鏀寔鍏ㄦ枃鎼滅储锟?
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_max_word")
    private String spuName;

    /**
     * 鍒嗙被 ID
     */
    @Field(type = FieldType.Keyword)
    private String categoryId;

    /**
     * 鍒嗙被鍚嶇О锛堢敤浜庢樉绀猴級
     */
    @Field(type = FieldType.Keyword)
    private String categoryName;

    /**
     * 鍝佺墝 ID
     */
    @Field(type = FieldType.Keyword)
    private String brandId;

    /**
     * 鍝佺墝鍚嶇О锛堢敤浜庢樉绀哄拰杩囨护锟?
     */
    @Field(type = FieldType.Keyword)
    private String brandName;

    /**
     * 鍟嗗搧鎻忚堪锛堟敮鎸佸叏鏂囨悳绱級
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String description;

    /**
     * 涓诲浘 URL
     */
    @Field(type = FieldType.Keyword, index = false)
    private String mainImage;

    /**
     * 鏈€浣庝环锟?
     */
    @Field(type = FieldType.Double)
    private BigDecimal minPrice;

    /**
     * 鏈€楂樹环锟?
     */
    @Field(type = FieldType.Double)
    private BigDecimal maxPrice;

    /**
     * 鎬诲簱锟?
     */
    @Field(type = FieldType.Integer)
    private Integer totalStock;

    /**
     * 鎬婚攢閲忥紙鐢ㄤ簬鎺掑簭锟?
     */
    @Field(type = FieldType.Integer)
    private Integer totalSales;

    /**
     * 鎺掑簭鏉冮噸
     */
    @Field(type = FieldType.Integer)
    private Integer sortOrder;

    /**
     * SEO 鏍囬
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String seoTitle;

    /**
     * SEO 鍏抽敭锟?
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String seoKeywords;

    /**
     * SEO 鎻忚堪
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String seoDescription;

    /**
     * 鐘讹拷 0-鑽夌, 1-涓婃灦, 2-涓嬫灦, 3-鍒犻櫎
     */
    @Field(type = FieldType.Integer)
    private Integer status;

    /**
     * 鍙戝竷鏃堕棿
     */
    @Field(type = FieldType.Date)
    private LocalDateTime publishedAt;

    /**
     * 鍒涘缓鏃堕棿
     */
    @Field(type = FieldType.Date)
    private LocalDateTime createTime;

    /**
     * 鏇存柊鏃堕棿锛堢敤浜庢帓搴忥級
     */
    @Field(type = FieldType.Date)
    private LocalDateTime updateTime;
}