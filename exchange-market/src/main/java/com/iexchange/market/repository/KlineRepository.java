package com.iexchange.market.repository;

import com.iexchange.market.document.KlineDocument;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * K 线仓库。
 */
public interface KlineRepository extends MongoRepository<KlineDocument, String> {

    List<KlineDocument> findBySymbolAndIntervalOrderByStartTimeDesc(String symbol, String interval, Pageable pageable);

    List<KlineDocument> findBySymbolAndIntervalAndStartTimeBetweenOrderByStartTimeAsc(
        String symbol, String interval, long startTime, long endTime);
}
