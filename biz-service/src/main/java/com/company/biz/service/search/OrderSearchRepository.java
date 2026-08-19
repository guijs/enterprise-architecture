package com.company.biz.service.search;

import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.company.common.page.PageQuery;
import com.company.common.page.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;

/**
 * ES 查询封装：精确筛选用 filter（可缓存、不算分），全文相关性才用 must/should。
 * 深分页禁止 from+size 超过 1w，超大翻页用 search_after。
 */
@Repository
@RequiredArgsConstructor
public class OrderSearchRepository {

    private final ElasticsearchClient client;

    public PageResult<OrderDoc> search(OrderSearchReq req, PageQuery page) throws IOException {
        SearchResponse<OrderDoc> resp = client.search(s -> s
                .index("order_search")
                .query(q -> q.bool(b -> {
                    if (StrUtil.isNotBlank(req.getKeyword())) {
                        b.must(m -> m.match(t -> t.field("title").query(req.getKeyword())));
                    }
                    if (req.getStatus() != null) {
                        b.filter(f -> f.term(t -> t.field("status").value(req.getStatus())));
                    }
                    return b;
                }))
                .from((int) page.offset())
                .size((int) page.getPageSize())
                .sort(so -> so.field(f -> f.field("createTime").order(SortOrder.Desc))), OrderDoc.class);

        List<OrderDoc> list = resp.hits().hits().stream().map(Hit::source).toList();
        long total = resp.hits().total() == null ? 0 : resp.hits().total().value();
        return new PageResult<>(list, total, page.getPageNum(), page.getPageSize());
    }
}
