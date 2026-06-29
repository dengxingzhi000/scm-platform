package com.scmcloud.search.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(indexName = "products")
public class ProductDocument {

    @Id
    private String id;

    @Field(name = "product_id", type = FieldType.Keyword)
    private Long productId;

    @Field(name = "seller_id", type = FieldType.Keyword)
    private Long sellerId;

    @Field(name = "title", type = FieldType.Text, analyzer = "ik_max_word")
    private String title;

    @Field(name = "subtitle", type = FieldType.Text)
    private String subtitle;

    @Field(name = "description", type = FieldType.Text, analyzer = "ik_max_word")
    private String description;

    @Field(name = "category_id", type = FieldType.Keyword)
    private Long categoryId;

    @Field(name = "category_path", type = FieldType.Keyword)
    private String categoryPath;

    @Field(name = "brand_id", type = FieldType.Keyword)
    private Long brandId;

    @Field(name = "brand_name", type = FieldType.Keyword)
    private String brandName;

    @Field(name = "price", type = FieldType.Float)
    private BigDecimal price;

    @Field(name = "original_price", type = FieldType.Float)
    private BigDecimal originalPrice;

    @Field(name = "sales_count", type = FieldType.Integer)
    private Integer salesCount;

    @Field(name = "rating", type = FieldType.Float)
    private BigDecimal rating;

    @Field(name = "review_count", type = FieldType.Integer)
    private Integer reviewCount;

    @Field(name = "main_image", type = FieldType.Keyword)
    private String mainImage;

    @Field(name = "images", type = FieldType.Keyword)
    private List<String> images;

    @Field(name = "tags", type = FieldType.Keyword)
    private List<String> tags;

    @Field(name = "status", type = FieldType.Keyword)
    private String status;

    @Field(name = "created_at", type = FieldType.Date)
    private LocalDateTime createdAt;

    @Field(name = "updated_at", type = FieldType.Date)
    private LocalDateTime updatedAt;
}
