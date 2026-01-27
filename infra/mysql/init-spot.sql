-- 现货交易相关表结构

CREATE TABLE IF NOT EXISTS spot_symbol (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  symbol VARCHAR(32) NOT NULL COMMENT '交易对，例如 BTC_USDT',
  base_asset VARCHAR(16) NOT NULL COMMENT '基础资产',
  quote_asset VARCHAR(16) NOT NULL COMMENT '计价资产',
  price_scale INT NOT NULL COMMENT '价格精度',
  quantity_scale INT NOT NULL COMMENT '数量精度',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
  created_at DATETIME NOT NULL COMMENT '创建时间',
  updated_at DATETIME NOT NULL COMMENT '更新时间',
  UNIQUE KEY uk_symbol (symbol)
) COMMENT='现货交易对配置表';

CREATE TABLE IF NOT EXISTS spot_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '订单主键',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  symbol VARCHAR(32) NOT NULL COMMENT '交易对',
  side VARCHAR(8) NOT NULL COMMENT '方向：BUY/SELL',
  type VARCHAR(8) NOT NULL COMMENT '类型：LIMIT/MARKET',
  price DECIMAL(18,8) NOT NULL DEFAULT 0 COMMENT '委托价格，市价单可为0',
  quantity DECIMAL(18,8) NOT NULL COMMENT '委托数量',
  filled_quantity DECIMAL(18,8) NOT NULL DEFAULT 0 COMMENT '已成交数量',
  status VARCHAR(16) NOT NULL COMMENT '订单状态',
  created_at DATETIME NOT NULL COMMENT '创建时间',
  updated_at DATETIME NOT NULL COMMENT '更新时间',
  KEY idx_symbol_status (symbol, status),
  KEY idx_user (user_id)
) COMMENT='现货订单表';

CREATE TABLE IF NOT EXISTS spot_plan_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '计划单主键',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  symbol VARCHAR(32) NOT NULL COMMENT '交易对',
  side VARCHAR(8) NOT NULL COMMENT '方向：BUY/SELL',
  type VARCHAR(8) NOT NULL COMMENT '类型：LIMIT/MARKET',
  trigger_price DECIMAL(18,8) NOT NULL COMMENT '触发价格',
  order_price DECIMAL(18,8) NOT NULL DEFAULT 0 COMMENT '触发后委托价',
  quantity DECIMAL(18,8) NOT NULL COMMENT '委托数量',
  status VARCHAR(16) NOT NULL COMMENT '计划单状态',
  triggered_order_id BIGINT DEFAULT NULL COMMENT '触发后生成的订单ID',
  triggered_at DATETIME DEFAULT NULL COMMENT '触发时间',
  created_at DATETIME NOT NULL COMMENT '创建时间',
  updated_at DATETIME NOT NULL COMMENT '更新时间',
  KEY idx_plan_user_status (user_id, status),
  KEY idx_plan_symbol_status (symbol, status)
) COMMENT='现货计划委托表';

CREATE TABLE IF NOT EXISTS spot_trade (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '成交主键',
  symbol VARCHAR(32) NOT NULL COMMENT '交易对',
  buy_order_id BIGINT NOT NULL COMMENT '买单ID',
  sell_order_id BIGINT NOT NULL COMMENT '卖单ID',
  price DECIMAL(18,8) NOT NULL COMMENT '成交价格',
  quantity DECIMAL(18,8) NOT NULL COMMENT '成交数量',
  taker_side VARCHAR(8) NOT NULL COMMENT '主动方方向：BUY/SELL',
  created_at DATETIME NOT NULL COMMENT '成交时间',
  KEY idx_symbol (symbol)
) COMMENT='现货成交表';

CREATE TABLE IF NOT EXISTS spot_fee_flow (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  trade_id BIGINT NOT NULL COMMENT '成交ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  symbol VARCHAR(32) NOT NULL COMMENT '交易对',
  side VARCHAR(8) NOT NULL COMMENT '方向：BUY/SELL',
  fee_asset VARCHAR(16) NOT NULL COMMENT '手续费资产',
  fee_rate DECIMAL(18,8) NOT NULL COMMENT '手续费费率',
  fee_amount DECIMAL(18,8) NOT NULL COMMENT '手续费金额',
  created_at DATETIME NOT NULL COMMENT '创建时间',
  UNIQUE KEY uk_trade_user_side (trade_id, user_id, side),
  KEY idx_user (user_id)
) COMMENT='现货手续费流水表';

-- 初始化示例交易对
INSERT INTO spot_symbol (symbol, base_asset, quote_asset, price_scale, quantity_scale, status, created_at, updated_at)
VALUES
  ('BTC_USDT', 'BTC', 'USDT', 2, 6, 1, NOW(), NOW()),
  ('ETH_USDT', 'ETH', 'USDT', 2, 6, 1, NOW(), NOW()),
  ('SOL_USDT', 'SOL', 'USDT', 2, 6, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  base_asset = VALUES(base_asset),
  quote_asset = VALUES(quote_asset),
  price_scale = VALUES(price_scale),
  quantity_scale = VALUES(quantity_scale),
  status = VALUES(status),
  updated_at = VALUES(updated_at);
