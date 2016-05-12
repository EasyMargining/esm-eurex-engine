package com.easymargining.domain;

import lombok.*;

import java.io.Serializable;

/**
 * Created by Gilles Marchal on 19/01/2015.
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Trade implements Serializable {

    private static final long serialVersionUID = 1L;

    private String _id;

    private String portfolioId;

    // Trade fields
    private String productId;
    private ContractMaturity expiryDate;
    private String versionNumber;
    private String productSettlementType;
    private String optionType;  //Call or Put
    private Double exercisePrice;
    private String exerciseStyleFlag;
    private String instrumentType;
    private Double quantity;

}
