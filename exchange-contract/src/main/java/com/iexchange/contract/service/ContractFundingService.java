package com.iexchange.contract.service;

import com.iexchange.contract.dto.FundingRateRequest;
import com.iexchange.contract.dto.FundingRateResponse;
import com.iexchange.contract.dto.FundingSettleRequest;
import com.iexchange.contract.dto.FundingSettleResponse;

/**
 * 资金费率服务。
 */
public interface ContractFundingService {

    FundingRateResponse calculateRate(FundingRateRequest request);

    FundingRateResponse getLatestRate(String symbol);

    FundingSettleResponse settleFunding(FundingSettleRequest request);
}
