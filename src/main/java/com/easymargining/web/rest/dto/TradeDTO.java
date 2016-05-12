package com.easymargining.web.rest.dto;

import lombok.*;

import java.io.Serializable;

/**
 * Created by Gilles Marchal on 10/04/2016.
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TradeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String _id;

    private String portfolioId;

    // Trade fields
    private String productId;
    private ContractMaturityDTO expiryDate;
    private String versionNumber;
    private String productSettlementType;
    private String optionType;  //Call or Put
    private Double exercisePrice;
    private String exerciseStyleFlag;
    private String instrumentType;
    private Double quantity;
}
