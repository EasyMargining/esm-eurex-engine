package com.easymargining.web.rest.dto.results;

import lombok.*;

import java.io.Serializable;
import java.util.List;

/**
 * Created by gmarchal on 22/02/2016.
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class PortfolioMarginResult implements Serializable {
    // Eurex Portfolio Name : PEQ01 for Equity portfolio
    private String portfolioName;
    // Currency of IM Result
    private String currency;
    // Total IM for the portfolio
    private Double imResult;
    // IM for a Liquidation Group
    private List<LiquidationGroupMarginResult> liquidationGroupIMResults;
}
