ALTER TABLE post
    MODIFY COLUMN status TINYINT NOT NULL DEFAULT 0
    COMMENT '状态：0待审核，1审核通过，2审核未通过，3封禁/下架';
