package com.atguigu.gmall.wms.vo;

import lombok.Data;

@Data
public class SkuLockVo {
    private Long skuId;
    private Integer count;

    //锁定成功与否的字段,锁定状态字段
    private Boolean lock;

    //锁定成功的库存Id，以方便将来解锁改仓库库存
    private Long wareSkuId;
}
