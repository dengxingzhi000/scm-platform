package com.cognora.ecom.search.query;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;

/**
 * Shared query fragments for the product index. Kept separate so tests can assert the
 * tenant term without depending on the query builder internals.
 */
public final class SearchQueryFactory {

    private SearchQueryFactory() {
    }

    /**
     * Mandatory tenant filter. Every product query must include this term.
     */
    public static Query tenantTerm(String tenantId) {
        return Query.of(q -> q.term(t -> t.field("tenant_id").value(tenantId)));
    }
}
