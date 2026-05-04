package com.team31.financetracker.account.repository;

import com.team31.financetracker.account.model.AccountSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountSearchRepository extends ElasticsearchRepository<AccountSearchDocument, String> {

}
