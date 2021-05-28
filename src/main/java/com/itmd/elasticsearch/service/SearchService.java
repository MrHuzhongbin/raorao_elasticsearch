package com.itmd.elasticsearch.service;

import com.itmd.elasticsearch.client.BookServiceClient;
import com.itmd.elasticsearch.pojo.Books;
import com.itmd.elasticsearch.pojo.SearchRequest;
import com.itmd.elasticsearch.pojo.SearchResult;
import com.itmd.elasticsearch.repository.BookRepository;
import com.itmd.pojo.BookClassification;
import com.itmd.pojo.BookPress;
import com.itmd.vo.BookLw;
import com.itmd.vo.PageResult;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SearchService {
    @Autowired
    private BookServiceClient bookServiceClient;

    @Autowired
    private ElasticsearchTemplate template;

    @Autowired
    private BookRepository bookRepository;


    public Books BuildBooks(BookLw book){
        Books books = new Books();

        books.setAuthor(book.getAuthor());
        books.setElePrice(book.getElePrice());
        books.setId(book.getId());
        books.setInfo(book.getInfo());
        books.setPressId(book.getPress());
        books.setPressTime(book.getPressTime());
        books.setPrice(book.getPrice());
        books.setName(book.getName());

        StringBuffer buffer = new StringBuffer();
        buffer.append(book.getName()+" "+book.getPressName()+" "+book.getAuthor()+" "+book.getClassificationName()+" ");
        books.setData(buffer.toString());

        //图片截取
        String[] split = StringUtils.split(book.getImage(), ',');
        if(!org.springframework.util.StringUtils.isEmpty(split)) {
            books.setImage(split[0]);
        }

        //获取图书的直接分类ID添加到List<Long>classification中
        List<BookClassification> bookClassifications = null;
        List<Long> classificationIds = null;
        try {
           bookClassifications = bookServiceClient.queryClassificationsByBid(books.getId());
           classificationIds = bookClassifications.stream().map(c -> c.getId()).collect(Collectors.toList());
           books.setClassification(classificationIds);
        }catch (Exception e){
            return books;
        }
        //获取图书的父分类名称添加到data中
        while(true) {
            try {
                classificationIds = bookClassifications.stream().map(c -> c.getPreId()).collect(Collectors.toList());
                bookClassifications = bookServiceClient.queryClassificationsByIdList(classificationIds);
                List<String> names = bookClassifications.stream().map(c -> c.getCateName()).collect(Collectors.toList());
                buffer.append(StringUtils.join(names," "));
            } catch (Exception e) {
                break;
            }
        }

        books.setData(buffer.toString());
        return books;
    }
    public PageResult<Books> search(SearchRequest rRequest) {
        int page = rRequest.getPage() - 1;
        int size = rRequest.getSize();
        //创建查询构建器
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        //结果过滤
//        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id", "name", "info","image"}, null));
        //分页
        queryBuilder.withPageable(PageRequest.of(page, size));
        //搜索条件
        QueryBuilder basicQuery = buildBasicQuery(rRequest);
//        queryBuilder.withQuery(QueryBuilders.matchQuery("all", rRequest.getKey()));
        queryBuilder.withQuery(basicQuery);
        //聚合出版社和分类
        String categoryAggName = "classification_agg";
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("classification"));
        String brandAggName = "press_agg";
        queryBuilder.addAggregation(AggregationBuilders.terms(brandAggName).field("pressId"));
        //查询
//        Page<Goods> goods = repository.search(queryBuilder.build());
        AggregatedPage<Books> goods = template.queryForPage(queryBuilder.build(),Books.class);
        //解析结果
        long total = goods.getTotalElements();
        long pages = (long) goods.getTotalPages();
        List<Books> books = goods.getContent();
        //解析聚合结果
        Aggregations aggs = goods.getAggregations();
        List<BookClassification> categories = parseCategoryAgg(aggs.get(categoryAggName));
        List<BookPress> presses = parsePressAgg(aggs.get(brandAggName));
        return new SearchResult(total, pages, books,categories,presses);
    }

    private QueryBuilder buildBasicQuery(SearchRequest rRequest) {
        //创建布尔查询
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        //查询条件
        queryBuilder.must(QueryBuilders.matchQuery("data", rRequest.getKey()));
        //过滤条件
        Map<String, String> filter = rRequest.getFilter();
        for (Map.Entry<String, String> stringStringEntry : filter.entrySet()) {
            String value = stringStringEntry.getValue();
            String key = stringStringEntry.getKey();
            queryBuilder.filter(QueryBuilders.matchQuery(key, value));
        }
        return queryBuilder;
    }

    private List<BookPress> parsePressAgg(LongTerms terms) {
        try {
            List<Long> ids = terms.getBuckets()
                    .stream().map(b -> b.getKeyAsNumber().longValue())
                    .collect(Collectors.toList());
            //client调用接口查询出版社并返回
            List<BookPress> bookPresses = bookServiceClient.queryPressByIds(ids);
            return bookPresses;
        } catch (Exception e) {
            return null;
        }
    }

    private List<BookClassification> parseCategoryAgg(LongTerms terms) {
        try {
            List<Long> ids = terms.getBuckets()
                    .stream().map(b -> b.getKeyAsNumber().longValue())
                    .collect(Collectors.toList());
            //client调用接口查询分类并返回
            List<BookClassification> bookClassifications = bookServiceClient.queryClassificationsByIdList(ids);
            return bookClassifications;

        } catch (Exception e) {
            return null;
        }
    }

    public void createIndex(Long id) {
        BookLw bookLw = bookServiceClient.queryBookById(id);
        //判断上下架情况
        if(bookRepository.existsById(id) && !bookLw.getSaleable()) {
            //下架则删除索引库的数据
            bookRepository.deleteById(id);
        }
        //上架
        if(bookLw.getSaleable()) {
            Books books = BuildBooks(bookLw);
            bookRepository.save(books);
        }

    }

    public void deleteIndex(Long id) {

    }

    public List<String> searchStr(String key) {
        //创建查询构建器
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        //结果过滤
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"name"}, null));
        //搜索条件
        queryBuilder.withQuery(QueryBuilders.matchQuery("name", key));
        //查询
        List<Books> books = template.queryForList(queryBuilder.build(), Books.class);
        List<String> keys = books.stream().map(b -> b.getName()).collect(Collectors.toList());
        return keys;
    }
}
