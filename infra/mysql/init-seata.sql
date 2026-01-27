-- Seata 数据库初始化
CREATE DATABASE IF NOT EXISTS seata DEFAULT CHARACTER SET utf8mb4;

USE seata;

-- 全局事务表
CREATE TABLE IF NOT EXISTS global_table (
  xid VARCHAR(128) NOT NULL,
  transaction_id BIGINT,
  status TINYINT NOT NULL,
  application_id VARCHAR(32),
  transaction_service_group VARCHAR(32),
  transaction_name VARCHAR(128),
  timeout INT,
  begin_time BIGINT,
  application_data VARCHAR(2000),
  gmt_create DATETIME,
  gmt_modified DATETIME,
  PRIMARY KEY (xid),
  KEY idx_status_gmt_modified (status, gmt_modified),
  KEY idx_transaction_id (transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 分支事务表
CREATE TABLE IF NOT EXISTS branch_table (
  branch_id BIGINT NOT NULL,
  xid VARCHAR(128) NOT NULL,
  transaction_id BIGINT,
  resource_group_id VARCHAR(32),
  resource_id VARCHAR(256),
  branch_type VARCHAR(8),
  status TINYINT,
  client_id VARCHAR(64),
  application_data VARCHAR(2000),
  gmt_create DATETIME,
  gmt_modified DATETIME,
  PRIMARY KEY (branch_id),
  KEY idx_xid (xid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 锁表
CREATE TABLE IF NOT EXISTS lock_table (
  row_key VARCHAR(128) NOT NULL,
  xid VARCHAR(128),
  transaction_id BIGINT,
  branch_id BIGINT,
  resource_id VARCHAR(256),
  table_name VARCHAR(32),
  pk VARCHAR(36),
  gmt_create DATETIME,
  gmt_modified DATETIME,
  PRIMARY KEY (row_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 分布式锁表（Seata 1.7+ 需要）
CREATE TABLE IF NOT EXISTS distributed_lock (
  lock_key CHAR(20) NOT NULL COMMENT '锁键',
  lock_value VARCHAR(20) NOT NULL COMMENT '锁值',
  expire BIGINT NOT NULL COMMENT '过期时间',
  PRIMARY KEY (lock_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
