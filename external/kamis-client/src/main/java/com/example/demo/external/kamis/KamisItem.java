package com.example.demo.external.kamis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KamisItem(
        @JsonProperty("auctn_seq") String auctnSeq,
        @JsonProperty("scsbd_dt") String scsbdDt,
        @JsonProperty("trd_clcln_ymd") String trdClclnYmd,
        @JsonProperty("whsl_mrkt_cd") String whslMrktCd,
        @JsonProperty("whsl_mrkt_nm") String whslMrktNm,
        @JsonProperty("corp_cd") String corpCd,
        @JsonProperty("corp_nm") String corpNm,
        @JsonProperty("gds_lclsf_cd") String gdsLclsfCd,
        @JsonProperty("gds_lclsf_nm") String gdsLclsfNm,
        @JsonProperty("gds_mclsf_cd") String gdsMclsfCd,
        @JsonProperty("gds_mclsf_nm") String gdsMclsfNm,
        @JsonProperty("gds_sclsf_cd") String gdsSclsfCd,
        @JsonProperty("gds_sclsf_nm") String gdsSclsfNm,
        @JsonProperty("corp_gds_cd") String corpGdsCd,
        @JsonProperty("corp_gds_item_nm") String corpGdsItemNm,
        @JsonProperty("corp_gds_vrty_nm") String corpGdsVrtyNm,
        @JsonProperty("plor_cd") String plorCd,
        @JsonProperty("plor_nm") String plorNm,
        @JsonProperty("scsbd_prc") String scsbdPrc,
        String qty,
        @JsonProperty("unit_qty") String unitQty,
        @JsonProperty("unit_cd") String unitCd,
        @JsonProperty("unit_nm") String unitNm,
        @JsonProperty("pkg_cd") String pkgCd,
        @JsonProperty("pkg_nm") String pkgNm,
        @JsonProperty("spm_no") String spmNo,
        @JsonProperty("trd_se") String trdSe) {}
