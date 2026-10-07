-- 一批一仓一质量状态一行。后端启动时也会尝试加这把唯一键；重复行存在时两边都不会强行加。
ALTER TABLE batch_stocks
  ADD UNIQUE KEY uk_batch_stocks (drug_id, warehouse_id, batch_no, quality_status);
