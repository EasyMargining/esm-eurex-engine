package com.easymargining.service;

import com.easymargining.domain.EurexMarketDataEnvironment;
import com.easymargining.domain.EurexTradeTransformer;
import com.easymargining.domain.Trade;
import com.easymargining.web.rest.dto.results.*;
import com.google.common.collect.Table;
import com.opengamma.margining.core.MarginEnvironment;
import com.opengamma.margining.core.request.MarginCalculator;
import com.opengamma.margining.core.request.PortfolioMeasure;
import com.opengamma.margining.core.request.TradeMeasure;
import com.opengamma.margining.core.result.MarginResults;
import com.opengamma.margining.core.trade.MarginPortfolio;
import com.opengamma.margining.core.util.CheckResults;
import com.opengamma.margining.core.util.OgmLinkResolver;
import com.opengamma.margining.core.util.PortfolioMeasureResultFormatter;
import com.opengamma.margining.core.util.TradeMeasureResultFormatter;
import com.opengamma.margining.eurex.prisma.replication.request.EurexPrismaReplicationRequest;
import com.opengamma.margining.eurex.prisma.replication.request.EurexPrismaReplicationRequests;
import com.opengamma.sesame.trade.TradeWrapper;
import com.opengamma.util.money.CurrencyAmount;
import com.opengamma.util.money.MultipleCurrencyAmount;
import com.opengamma.util.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.threeten.bp.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MarginService {

    private final Logger log = LoggerFactory.getLogger(MarginService.class);

    public MarginResult computeEtdMargin(List<Trade> trades) {

        // Load MarketData
        MarginEnvironment environment = EurexMarketDataEnvironment.getInstance().getMarginEnvironment();
        LocalDate s_valuationDate = LocalDate.of(
            EurexMarketDataEnvironment.getInstance().getValuationDate().getYear(),
            EurexMarketDataEnvironment.getInstance().getValuationDate().getMonthValue(),
            EurexMarketDataEnvironment.getInstance().getValuationDate().getDayOfMonth()
        );

        // Obtain portfolio, loaded from a trade file on the classpath
        OgmLinkResolver linkResolver = environment.getInjector().getInstance(OgmLinkResolver.class);
        EurexTradeTransformer transformer = new EurexTradeTransformer(linkResolver);
        MarginPortfolio portfolio = transformer.buildEurexPortfolioFromTrade(trades, s_valuationDate);

        log.info("Margin portfolio = " + portfolio);

        // Build PV calculation request
        EurexPrismaReplicationRequest pvRequest = getPvCalculationRequest(s_valuationDate);

        // Build IM calculation request
        EurexPrismaReplicationRequest imRequest = getImCalculationRequest(s_valuationDate);

        // Grab calculator
        MarginCalculator calculator = environment.getMarginCalculator();

        // Run PV request
        log.info("Running PV request");
        MarginResults pvResults = calculator.calculate(portfolio, pvRequest);

        // Print results
        Table<TradeWrapper<?>, TradeMeasure, Result<?>> tradePvResults = pvResults.getTradeResults().getResults();
        String stringPvTable = TradeMeasureResultFormatter.formatter()
            .truncateAfter(200)
            .format(tradePvResults);

        StringBuilder outResult = new StringBuilder();
        outResult.append("PV results:\n").append(stringPvTable);

        // Run IM request
        log.info("Running IM request");
        MarginResults imResults = calculator.calculate(portfolio, imRequest);


        // Print results
        String imResultTable = PortfolioMeasureResultFormatter.formatter()
            .format(imResults.getPortfolioResults());
        outResult.append("Portfolio results:\n").append(imResultTable);

        outResult.append("IM result: ").append(imResults.getPortfolioResults().getValues()
            .get("Total", EurexPrismaReplicationRequests.portfolioMeasures().im()));

        outResult.append("VM result: ").append(imResults.getPortfolioResults().getValues()
            .get("Total", EurexPrismaReplicationRequests.portfolioMeasures().vm()));

        outResult.append("Historical VAR result: ").append(imResults.getPortfolioResults().getValues()
            .get("Total", EurexPrismaReplicationRequests.portfolioMeasures().varEquity()));

        CheckResults.checkMarginResults(imResults);

        log.info("Result :  " + outResult.toString());


        // Build Output Result.
        MarginResult result = new MarginResult();
        result.setPortfolioMarginResults(new ArrayList());
        List<LiquidationGroupMarginResult> liquidationGroupMarginResults = null;
        CurrencyAmount[] currencyAmounts =
            imResults.getPortfolioResults().getValues().get(
                "Total",
                EurexPrismaReplicationRequests.portfolioMeasures().im()).getValue().getCurrencyAmounts();


        // Load Liquidation Groups & Liquidation Group Split
        Map<String, Set<String>> liquidationGroupDefinitions = EurexMarketDataEnvironment.getInstance().getLiquidationGroupSplit();
        List<String> liquidationGroups = new ArrayList(liquidationGroupDefinitions.keySet());

        log.info("List of liquidation groups : " + liquidationGroupDefinitions);


        for (int i=0 ; i< currencyAmounts.length; i++) {
            currencyAmounts[i].getCurrency();
            currencyAmounts[i].getAmount();

            // For Each Liquidation Group
            for (int j=0 ; j< liquidationGroups.size(); j++) {

                String liquidationGroupName = liquidationGroups.get(j);

                // Identify IM for Liquidation Group
                MultipleCurrencyAmount imgroup = null;

                log.info("Find IM Group Margin Result for Liquidation Group : " + liquidationGroupName);
                Result<MultipleCurrencyAmount> imgroupMarginResult =
                    imResults.getPortfolioResults().getValues()
                        .get("Total",
                            EurexPrismaReplicationRequests.portfolioMeasures()
                                .groupIm(liquidationGroupName));

                log.info("IM Group Margin Result : " + imgroupMarginResult);

                if (imgroupMarginResult!= null) {
                    imgroup = imgroupMarginResult.getValue();
                }

                if (imgroup != null) {

                    liquidationGroupMarginResults = new ArrayList();
                    result.getPortfolioMarginResults().add(
                        new PortfolioMarginResult(liquidationGroupName,
                            currencyAmounts[i].getCurrency().getCode(),
                            currencyAmounts[i].getAmount(),
                            liquidationGroupMarginResults));

                    List<LiquidationGroupSplitMarginResult> liquidationGroupSplitMarginResults = new ArrayList();
                    double value = imgroup.getAmount(currencyAmounts[i].getCurrency());
                    liquidationGroupMarginResults.add(new LiquidationGroupMarginResult(liquidationGroupName, value, liquidationGroupSplitMarginResults));

                    // For all Liquidation Group Split
                    List<String> liquidationGroupSplits = new ArrayList(liquidationGroupDefinitions.get(liquidationGroupName));
                    for (int k=0 ; k< liquidationGroupSplits.size(); k++) {
                        String liquidationGroupSplitName = liquidationGroupSplits.get(k);

                        MultipleCurrencyAmount imLGS = null;
                        MultipleCurrencyAmount liquidityAddon = null;
                        MultipleCurrencyAmount liquidityAddonEtd = null;
                        MultipleCurrencyAmount liquidityAddonOtc = null;
                        MultipleCurrencyAmount longOptionCredit = null;
                        MultipleCurrencyAmount var = null;

                        //Identify Market Risk IM for liquidation group
                        Result<MultipleCurrencyAmount> imLGSMarginResult = imResults.getPortfolioResults().getValues()
                            .get("Total",
                                EurexPrismaReplicationRequests.portfolioMeasures()
                                    .groupIm(liquidationGroupSplitName));
                        if (imLGSMarginResult!= null) {
                            imLGS = imLGSMarginResult.getValue();
                        }

                        Result<MultipleCurrencyAmount> liquidityAddonMarginResult =
                            imResults.getPortfolioResults().getValues()
                                .get("Total",
                                    EurexPrismaReplicationRequests.portfolioMeasures()
                                        .liquidityAddon(liquidationGroupSplitName));

                        if (liquidityAddonMarginResult!= null) {
                            liquidityAddon = liquidityAddonMarginResult.getValue();
                        }

                        Result<MultipleCurrencyAmount> liquidityAddonEtdMarginResult =
                            imResults.getPortfolioResults().getValues()
                                .get("Total",
                                    EurexPrismaReplicationRequests.portfolioMeasures()
                                        .liquidityAddonEtd(liquidationGroupSplitName));
                        if (liquidityAddonEtdMarginResult!= null) {
                            liquidityAddonEtd = liquidityAddonEtdMarginResult.getValue();
                        }

                        Result<MultipleCurrencyAmount> liquidityAddonOtcMarginResult =
                            imResults.getPortfolioResults().getValues()
                                .get("Total",
                                    EurexPrismaReplicationRequests.portfolioMeasures()
                                        .liquidityAddonOtc(liquidationGroupSplitName));

                        if (liquidityAddonOtcMarginResult!= null) {
                            liquidityAddonOtc = liquidityAddonOtcMarginResult.getValue();
                        }

                        Result<MultipleCurrencyAmount> longOptionCreditMarginResult =
                            imResults.getPortfolioResults().getValues()
                                .get("Total",
                                    EurexPrismaReplicationRequests.portfolioMeasures()
                                        .longOptionCredit(liquidationGroupSplitName));

                        if (longOptionCreditMarginResult!= null) {
                            longOptionCredit = longOptionCreditMarginResult.getValue();
                        }

                        Result<MultipleCurrencyAmount> varMarginResult =
                            imResults.getPortfolioResults().getValues()
                                .get("Total",
                                    EurexPrismaReplicationRequests.portfolioMeasures()
                                        .var(liquidationGroupSplitName));

                        if (varMarginResult!= null) {
                            var = varMarginResult.getValue();
                        }

                        LiquidityRiskAdjustmentResult liquidityRiskAdjustmentResult = new LiquidityRiskAdjustmentResult();
                        MarketRiskIMResult marketRiskIMResult = new MarketRiskIMResult();

                        if (liquidityAddon != null) {
                            liquidityRiskAdjustmentResult.setTotalLiquidityRiskAdjustmentAddOn(liquidityAddon.getAmount(currencyAmounts[i].getCurrency()));
                        }
                        if (liquidityAddonEtd != null) {
                            liquidityRiskAdjustmentResult.setEtdliquidityRiskAdjustmentAddOnPart(liquidityAddonEtd.getAmount(currencyAmounts[i].getCurrency()));
                        }
                        if (liquidityAddonOtc != null) {
                            liquidityRiskAdjustmentResult.setOtcliquidityRiskAdjustmentAddOnPart(liquidityAddonOtc.getAmount(currencyAmounts[i].getCurrency()));
                        }
                        if (longOptionCredit != null) {
                            liquidityRiskAdjustmentResult.setLongOptionCreditAddOn(longOptionCredit.getAmount(currencyAmounts[i].getCurrency()));
                        }

                        if (imLGS != null) {
                            Double valueImLGS = imLGS.getAmount(currencyAmounts[i].getCurrency());

                            liquidationGroupSplitMarginResults.add(new LiquidationGroupSplitMarginResult(liquidationGroupSplitName, valueImLGS, liquidityRiskAdjustmentResult, marketRiskIMResult ));
                        }

                    }
                }
            }
        }

        return result;
    }

    /**
     * Gets a trade PV calculation request.
     *
     * @return the calculation request
     */
    private static EurexPrismaReplicationRequest getPvCalculationRequest(LocalDate s_valuationDate) {

        TradeMeasure pv = EurexPrismaReplicationRequests.tradeMeasures()
            .pv()
            .build();

        return EurexPrismaReplicationRequests.request(s_valuationDate)
            .tradeMeasures(pv)
            .build();
    }

    /**
     * Gets a portfolio IM calculation request.
     *
     * @return the calculation request
     */
    private static EurexPrismaReplicationRequest getImCalculationRequest(LocalDate s_valuationDate) {

        PortfolioMeasure im = EurexPrismaReplicationRequests.portfolioMeasures().im();

        return EurexPrismaReplicationRequests.request(s_valuationDate)
            .portfolioMeasures(im)
            .crossMarginingEnabled(true)
            .build();

    }
}
