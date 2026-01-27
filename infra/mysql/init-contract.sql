git init-- 永续合约核心表结构

CREATE TABLE IF NOT EXISTS contract_symbol (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  symbol VARCHAR(32) NOT NULL COMMENT '合约交易对，例如 BTCUSDT-PERP',
  base_asset VARCHAR(16) NOT NULL COMMENT '基础资产',
  quote_asset VARCHAR(16) NOT NULL COMMENT '计价资产',
  price_scale INT NOT NULL COMMENT '价格精度',
  quantity_scale INT NOT NULL COMMENT '数量精度',
  max_leverage INT NOT NULL COMMENT '最大杠杆',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
  created_at DATETIME NOT NULL COMMENT '创建时间',
  updated_at DATETIME NOT NULL COMMENT '更新时间',
  UNIQUE KEY uk_symbol (symbol)
) COMMENT='合约交易对配置表';

CREATE TABLE IF NOT EXISTS contract_account (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  balance DECIMAL(18,8) NOT NULL DEFAULT 0 COMMENT '可用保证金余额',
  created_at DATETIME NOT NULL COMMENT '创建时间',
  updated_at DATETIME NOT NULL COMMENT '更新时间',
  UNIQUE KEY uk_user (user_id)
) COMMENT='合约保证金账户';

CREATE TABLE IF NOT EXISTS contract_position (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  symbol VARCHAR(32) NOT NULL COMMENT '合约交易对',
  side VARCHAR(8) NOT NULL COMMENT '方向：LONG/SHORT',
  margin_mode VARCHAR(16) NOT NULL COMMENT '保证金模式：CROSS/ISOLATED',
  leverage INT NOT NULL COMMENT '杠杆倍数',
  quantity DECIMAL(18,8) NOT NULL COMMENT '持仓数量',
  entry_price DECIMAL(18,8) NOT NULL COMMENT '开仓均价',
  margin DECIMAL(18,8) NOT NULL COMMENT '占用保证金',
  liquidation_price DECIMAL(18,8) NOT NULL COMMENT '预估强平价',
  created_at DATETIME NOT NULL COMMENT '创建时间',
  updated_at DATETIME NOT NULL COMMENT '更新时间',
  UNIQUE KEY uk_position (user_id, symbol, side, margin_mode)
) COMMENT='合约持仓表';

CREATE TABLE IF NOT EXISTS contract_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  symbol VARCHAR(32) NOT NULL COMMENT '合约交易对',
  action VARCHAR(8) NOT NULL COMMENT '开平：OPEN/CLOSE',
  side VARCHAR(8) NOT NULL COMMENT '方向：LONG/SHORT',
  type VARCHAR(8) NOT NULL COMMENT '类型：LIMIT/MARKET',
  price DECIMAL(18,8) NOT NULL DEFAULT 0 COMMENT '委托价格',
  quantity DECIMAL(18,8) NOT NULL COMMENT '委托数量',
  leverage INT NOT NULL COMMENT '杠杆倍数',
  margin_mode VARCHAR(16) NOT NULL COMMENT '保证金模式',
  status VARCHAR(16) NOT NULL COMMENT '订单状态',
  filled_price DECIMAL(18,8) NOT NULL DEFAULT 0 COMMENT '成交价格',
  created_at DATETIME NOT NULL COMMENT '创建时间',
  updated_at DATETIME NOT NULL COMMENT '更新时间',
  KEY idx_symbol (symbol),
  KEY idx_user (user_id)
) COMMENT='合约订单表';

CREATE TABLE IF NOT EXISTS contract_plan_order (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  symbol VARCHAR(32) NOT NULL COMMENT '合约交易对',
  action VARCHAR(8) NOT NULL COMMENT '开平：OPEN/CLOSE',
  side VARCHAR(8) NOT NULL COMMENT '方向：LONG/SHORT',
  type VARCHAR(8) NOT NULL COMMENT '类型：LIMIT/MARKET',
  trigger_price DECIMAL(18,8) NOT NULL COMMENT '触发价格',
  order_price DECIMAL(18,8) NOT NULL DEFAULT 0 COMMENT '触发后委托价',
  quantity DECIMAL(18,8) NOT NULL COMMENT '委托数量',
  leverage INT DEFAULT NULL COMMENT '杠杆倍数（开仓计划单）',
  margin_mode VARCHAR(16) DEFAULT NULL COMMENT '保证金模式（开仓/平仓）',
  status VARCHAR(16) NOT NULL COMMENT '计划单状态',
  triggered_order_id BIGINT DEFAULT NULL COMMENT '触发后生成的订单ID',
  triggered_at DATETIME DEFAULT NULL COMMENT '触发时间',
  created_at DATETIME NOT NULL COMMENT '创建时间',
  updated_at DATETIME NOT NULL COMMENT '更新时间',
  KEY idx_plan_user_status (user_id, status),
  KEY idx_plan_symbol_status (symbol, status)
) COMMENT='合约计划委托表';

CREATE TABLE IF NOT EXISTS contract_fee_flow (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  order_id BIGINT NOT NULL COMMENT '订单ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  symbol VARCHAR(32) NOT NULL COMMENT '合约交易对',
  action VARCHAR(8) NOT NULL COMMENT '开平：OPEN/CLOSE',
  side VARCHAR(8) NOT NULL COMMENT '方向：LONG/SHORT',
  fee_rate DECIMAL(18,8) NOT NULL COMMENT '手续费费率',
  fee_amount DECIMAL(18,8) NOT NULL COMMENT '手续费金额',
  created_at DATETIME NOT NULL COMMENT '创建时间',
  UNIQUE KEY uk_order (order_id),
  KEY idx_user (user_id)
) COMMENT='合约手续费流水表';

CREATE TABLE IF NOT EXISTS contract_funding_rate (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  symbol VARCHAR(32) NOT NULL COMMENT '合约交易对',
  rate DECIMAL(18,8) NOT NULL COMMENT '资金费率',
  mark_price DECIMAL(18,8) NOT NULL COMMENT '标记价格',
  index_price DECIMAL(18,8) NOT NULL COMMENT '指数价格',
  next_settle_time BIGINT NOT NULL COMMENT '下次结算时间（秒）',
  created_at DATETIME NOT NULL COMMENT '创建时间',
  KEY idx_symbol (symbol)
) COMMENT='资金费率记录';

CREATE TABLE IF NOT EXISTS contract_funding_settlement (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  position_id BIGINT NOT NULL COMMENT '持仓ID',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  symbol VARCHAR(32) NOT NULL COMMENT '合约交易对',
  side VARCHAR(8) NOT NULL COMMENT '方向：LONG/SHORT',
  rate DECIMAL(18,8) NOT NULL COMMENT '资金费率',
  mark_price DECIMAL(18,8) NOT NULL COMMENT '标记价格',
  funding_amount DECIMAL(18,8) NOT NULL COMMENT '资金费用（正收/负付）',
  settlement_time BIGINT NOT NULL COMMENT '结算时间（秒）',
  created_at DATETIME NOT NULL COMMENT '创建时间',
  KEY idx_user (user_id),
  KEY idx_symbol (symbol)
) COMMENT='资金费率结算记录';

-- 初始化示例合约
INSERT INTO contract_symbol (symbol, base_asset, quote_asset, price_scale, quantity_scale, max_leverage, status, created_at, updated_at)
VALUES
  ('BTCUSDT-PERP', 'BTC', 'USDT', 2, 6, 20, 1, NOW(), NOW()),
  ('ETHUSDT-PERP', 'ETH', 'USDT', 2, 6, 20, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  base_asset = VALUES(base_asset),
  quote_asset = VALUES(quote_asset),
  price_scale = VALUES(price_scale),
  quantity_scale = VALUES(quantity_scale),
  max_leverage = VALUES(max_leverage),
  status = VALUES(status),
  updated_at = VALUES(updated_at);
