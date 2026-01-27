package com.iexchange.contract.canal;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.iexchange.contract.es.ContractOrderDocument;
import com.iexchange.contract.es.ContractOrderEsMapper;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Canal 同步合约订单到 ES（演示版）。
 */
@Slf4j
@Component
public class ContractOrderCanalSync {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ContractCanalProperties properties;
    private final ContractOrderEsMapper esMapper;
    private volatile boolean running;
    private CanalConnector connector;
    private Thread worker;

    public ContractOrderCanalSync(ContractCanalProperties properties, ContractOrderEsMapper esMapper) {
        this.properties = properties;
        this.esMapper = esMapper;
    }

    /**
     * 安装好canel后，请手动去掉一下代码注释，
     */
//    @PostConstruct
//    public void start() {
//        if (!properties.isEnabled()) {
//            return;
//        }
//        running = true;
//        worker = new Thread(this::runLoop, "canal-contract-order-sync");
//        worker.setDaemon(true);
//        worker.start();
//    }

    @PreDestroy
    public void stop() {
        running = false;
        if (connector != null) {
            connector.disconnect();
        }
    }

    private void runLoop() {
        connector = CanalConnectors.newSingleConnector(
            new InetSocketAddress(properties.getHost(), properties.getPort()),
            properties.getDestination(),
            properties.getUsername(),
            properties.getPassword());
        // 建立 Canal 连接并订阅指定的表过滤规则
        connector.connect();
        connector.subscribe(properties.getFilter());
        connector.rollback();
        while (running) {
            // 拉取一批 Entry（不自动 ack，处理完成后手动 ack）
            Message message = connector.getWithoutAck(properties.getBatchSize());
            long batchId = message.getId();
            List<CanalEntry.Entry> entries = message.getEntries();
            if (batchId == -1 || entries.isEmpty()) {
                sleepQuietly(1000);
                continue;
            }
            try {
                // 解析 RowData 并同步到 ES
                handleEntries(entries);
                // 同步成功后提交 ack
                connector.ack(batchId);
            } catch (Exception ex) {
                log.warn("Canal 同步失败，batchId={}", batchId, ex);
                // 同步失败回滚，等待重试
                connector.rollback(batchId);
            }
        }
    }

    private void handleEntries(List<CanalEntry.Entry> entries) throws Exception {
        for (CanalEntry.Entry entry : entries) {
            // 只处理行级变更事件
            if (entry.getEntryType() != CanalEntry.EntryType.ROWDATA) {
                continue;
            }
            String table = entry.getHeader().getTableName();
            if (!"contract_order".equalsIgnoreCase(table)) {
                continue;
            }
            // 解析 RowChange，获取事件类型与变更数据
            CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            CanalEntry.EventType eventType = rowChange.getEventType();
            for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                if (eventType == CanalEntry.EventType.DELETE) {
                    String idValue = getColumnValue(rowData.getBeforeColumnsList(), "id");
                    if (idValue != null) {
                        // 删除事件：移除 ES 文档
                        esMapper.deleteById(Long.parseLong(idValue));
                    }
                    continue;
                }
                // 插入/更新事件：将列数据映射为 ES 文档
                ContractOrderDocument document = toDocument(rowData.getAfterColumnsList());
                if (document.getId() == null) {
                    continue;
                }
                if (eventType == CanalEntry.EventType.INSERT) {
                    // 插入事件：写入 ES
                    esMapper.insert(document);
                } else {
                    // 更新事件：更新 ES
                    esMapper.updateById(document);
                }
            }
        }
    }

    private ContractOrderDocument toDocument(List<CanalEntry.Column> columns) {
        ContractOrderDocument document = new ContractOrderDocument();
        document.setId(toLong(getColumnValue(columns, "id")));
        document.setUserId(toLong(getColumnValue(columns, "user_id")));
        document.setSymbol(getColumnValue(columns, "symbol"));
        document.setAction(getColumnValue(columns, "action"));
        document.setSide(getColumnValue(columns, "side"));
        document.setType(getColumnValue(columns, "type"));
        document.setPrice(toDecimal(getColumnValue(columns, "price")));
        document.setQuantity(toDecimal(getColumnValue(columns, "quantity")));
        document.setLeverage(toInteger(getColumnValue(columns, "leverage")));
        document.setMarginMode(getColumnValue(columns, "margin_mode"));
        document.setStatus(getColumnValue(columns, "status"));
        document.setFilledPrice(toDecimal(getColumnValue(columns, "filled_price")));
        document.setCreatedAt(toDateTime(getColumnValue(columns, "created_at")));
        document.setUpdatedAt(toDateTime(getColumnValue(columns, "updated_at")));
        return document;
    }

    private String getColumnValue(List<CanalEntry.Column> columns, String name) {
        for (CanalEntry.Column column : columns) {
            if (name.equalsIgnoreCase(column.getName())) {
                return column.getValue();
            }
        }
        return null;
    }

    private Long toLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.parseLong(value);
    }

    private Integer toInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Integer.parseInt(value);
    }

    private BigDecimal toDecimal(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value);
    }

    private LocalDateTime toDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value, TIME_FORMATTER);
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
