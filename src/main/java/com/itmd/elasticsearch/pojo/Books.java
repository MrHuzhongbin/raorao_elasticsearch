package com.itmd.elasticsearch.pojo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Data
@Document(indexName = "books", type = "docs", shards = 1, replicas = 0)
public class Books {

    @Id
    private Long id; //图书ID
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String data; //搜索过滤数据 包括书名，出版社，所有分类，作者
    @Field(type = FieldType.Keyword, index = false)
    private String info;//图书简介
    @Field(type = FieldType.Keyword, index = false)
    private String image; //图书封面
    @Field(type = FieldType.Keyword, index = false)
    private String pressTime;//出版时间

    private String name;
    private Long pressId;//出版社id
    private Float price;//实物价格
    private Float elePrice;//电子书价格
    private List<Long> classification;//所属分类ID
    private String author;//作者

}
