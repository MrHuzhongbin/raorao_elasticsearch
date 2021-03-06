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

        //????????????
        String[] split = StringUtils.split(book.getImage(), ',');
        if(!org.springframework.util.StringUtils.isEmpty(split)) {
            books.setImage(split[0]);
        }

        //???????????????????????????ID?????????List<Long>classification???
        List<BookClassification> bookClassifications = null;
        List<Long> classificationIds = null;
        try {
           bookClassifications = bookServiceClient.queryClassificationsByBid(books.getId());
           classificationIds = bookClassifications.stream().map(c -> c.getId()).collect(Collectors.toList());
           books.setClassification(classificationIds);
        }catch (Exception e){
            return books;
        }
        //???????????????????????????????????????data???
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
        //?????????????????????
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        //????????????
//        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id", "name", "info","image"}, null));
        //??????
        queryBuilder.withPageable(PageRequest.of(page, size));
        //????????????
        QueryBuilder basicQuery = buildBasicQuery(rRequest);
//        queryBuilder.withQuery(QueryBuilders.matchQuery("all", rRequest.getKey()));
        queryBuilder.withQuery(basicQuery);
        //????????????????????????
        String categoryAggName = "classification_agg";
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("classification"));
        String brandAggName = "press_agg";
        queryBuilder.addAggregation(AggregationBuilders.terms(brandAggName).field("pressId"));
        //??????
//        Page<Goods> goods = repository.search(queryBuilder.build());
        AggregatedPage<Books> goods = template.queryForPage(queryBuilder.build(),Books.class);
        //????????????
        long total = goods.getTotalElements();
        long pages = (long) goods.getTotalPages();
        List<Books> books = goods.getContent();
        //??????????????????
        Aggregations aggs = goods.getAggregations();
        List<BookClassification> categories = parseCategoryAgg(aggs.get(categoryAggName));
        List<BookPress> presses = parsePressAgg(aggs.get(brandAggName));
        return new SearchResult(total, pages, books,categories,presses);
    }

    private QueryBuilder buildBasicQuery(SearchRequest rRequest) {
        //??????????????????
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        //????????????
        queryBuilder.must(QueryBuilders.matchQuery("data", rRequest.getKey()));
        //????????????
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
            //client????????????????????????????????????
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
            //client?????????????????????????????????
            List<BookClassification> bookClassifications = bookServiceClient.queryClassificationsByIdList(ids);
            return bookClassifications;

        } catch (Exception e) {
            return null;
        }
    }

    public void createIndex(Long id) {
        BookLw bookLw = bookServiceClient.queryBookById(id);
        //?????????????????????
        if(bookRepository.existsById(id) && !bookLw.getSaleable()) {
            //?????????????????????????????????
            bookRepository.deleteById(id);
        }
        //??????
        if(bookLw.getSaleable()) {
            Books books = BuildBooks(bookLw);
            bookRepository.save(books);
        }

    }

    public void deleteIndex(Long id) {

    }

    public List<String> searchStr(String key) {
        //?????????????????????
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        //????????????
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"name"}, null));
        //????????????
        queryBuilder.withQuery(QueryBuilders.matchQuery("name", key));
        //??????
        List<Books> books = template.queryForList(queryBuilder.build(), Books.class);
        List<String> keys = books.stream().map(b -> b.getName()).collect(Collectors.toList());
        return keys;
    }
}
