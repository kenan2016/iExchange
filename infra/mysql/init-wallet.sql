-- 钱包表初始化
USE iexchange;

CREATE TABLE IF NOT EXISTS wallet_account (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  asset VARCHAR(32) NOT NULL COMMENT '资产类型',
  available_balance DECIMAL(32, 16) NOT NULL DEFAULT 0 COMMENT '可用余额',
  frozen_balance DECIMAL(32, 16) NOT NULL DEFAULT 0 COMMENT '冻结余额',
  total_balance DECIMAL(32, 16) NOT NULL DEFAULT 0 COMMENT '总余额',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_asset (user_id, asset)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包账户表';

CREATE TABLE IF NOT EXISTS wallet_flow (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  asset VARCHAR(32) NOT NULL COMMENT '资产类型',
  flow_type VARCHAR(32) NOT NULL COMMENT '流水类型',
  amount DECIMAL(32, 16) NOT NULL COMMENT '变动金额',
  balance_after DECIMAL(32, 16) NOT NULL COMMENT '变动后余额',
  business_id VARCHAR(64) NOT NULL COMMENT '幂等请求ID',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_flow_idempotent (user_id, asset, flow_type, business_id),
  KEY idx_user_asset (user_id, asset)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='钱包流水表';

CREATE TABLE IF NOT EXISTS wallet_chain_address (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  chain_name VARCHAR(32) NOT NULL COMMENT '链名称标识',
  address VARCHAR(128) NOT NULL COMMENT '链上地址',
  private_key VARCHAR(128) DEFAULT NULL COMMENT '链上地址私钥',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_chain (user_id, chain_name),
  UNIQUE KEY uk_chain_address (chain_name, address)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='链上充值地址表';

CREATE TABLE IF NOT EXISTS wallet_chain_withdraw (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  asset VARCHAR(32) NOT NULL COMMENT '资产类型',
  chain_name VARCHAR(32) NOT NULL COMMENT '链名称标识',
  amount DECIMAL(32, 16) NOT NULL COMMENT '提币数量',
  to_address VARCHAR(128) NOT NULL COMMENT '提币地址',
  request_id VARCHAR(64) NOT NULL COMMENT '提币请求ID',
  tx_hash VARCHAR(128) DEFAULT NULL COMMENT '链上交易哈希',
  status VARCHAR(32) NOT NULL COMMENT '状态',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_withdraw_request (request_id),
  KEY idx_user_chain (user_id, chain_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='链上提币记录';

CREATE TABLE IF NOT EXISTS wallet_chain_cursor (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  chain_name VARCHAR(32) NOT NULL COMMENT '链名称标识',
  token_address VARCHAR(128) NOT NULL COMMENT 'Token 合约地址',
  last_block BIGINT NOT NULL DEFAULT 0 COMMENT '已扫描到的区块高度',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_chain_token (chain_name, token_address)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='链上扫描游标';

CREATE TABLE IF NOT EXISTS wallet_chain_config (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  config_key VARCHAR(64) NOT NULL COMMENT '配置键',
  config_value TEXT NOT NULL COMMENT '配置值',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_chain_config (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='链上配置表';

CREATE TABLE IF NOT EXISTS otc_order (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  order_no VARCHAR(64) NOT NULL COMMENT '订单号',
  buyer_id BIGINT NOT NULL COMMENT '买方用户ID',
  seller_id BIGINT NOT NULL COMMENT '卖方用户ID',
  asset VARCHAR(32) NOT NULL COMMENT '资产类型',
  amount DECIMAL(32, 16) NOT NULL COMMENT '数量',
  status VARCHAR(32) NOT NULL COMMENT '订单状态',
  paid_at DATETIME DEFAULT NULL COMMENT '付款时间',
  released_at DATETIME DEFAULT NULL COMMENT '放币时间',
  canceled_at DATETIME DEFAULT NULL COMMENT '取消时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_otc_order_no (order_no),
  KEY idx_otc_buyer (buyer_id),
  KEY idx_otc_seller (seller_id),
  KEY idx_otc_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OTC 订单表';
