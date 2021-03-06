package com.easymargining.web.rest.dto.results;

import lombok.*;

import java.io.Serializable;

/**
 * Created by gmarchal on 22/02/2016.
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class LiquidationGroupSplitMarginResult implements Serializable {
    // Liquidation Group Split Name
    private String liquidationGroupSplitName;
    // IM Liquidation Group Split
    private Double imLiquidationGroupSplit;
    // Liquidity Risk Adjustment for a Liquidation Group Split
    private LiquidityRiskAdjustmentResult liquidityRiskAdjustmentResult;
    // Market Risk IM for a Liquidation Group Split
    private MarketRiskIMResult marketRiskIMResult;
}
