package com.easymargining.domain;

import lombok.*;

import java.io.Serializable;

/**
 * Created by gmarchal on 23/02/2016.
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ContractMaturity implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer contractYear;

    private Integer contractMonth;

}
