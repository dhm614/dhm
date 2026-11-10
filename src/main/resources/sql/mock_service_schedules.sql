USE pet_community;

INSERT INTO service_schedule
    (store_id, service_item_id, schedule_date, start_time, end_time,
     capacity, booked_count, status, remark)
SELECT
    store.id,
    service_item.id,
    DATE_ADD('2026-11-11', INTERVAL schedule_day.day_offset DAY),
    schedule_slot.start_time,
    schedule_slot.end_time,
    5,
    0,
    1,
    '系统生成的模拟排班'
FROM store
JOIN service_item ON service_item.store_id = store.id
CROSS JOIN (
    SELECT 0 AS day_offset
    UNION ALL SELECT 1
    UNION ALL SELECT 2
    UNION ALL SELECT 3
    UNION ALL SELECT 4
    UNION ALL SELECT 5
    UNION ALL SELECT 6
) schedule_day
CROSS JOIN (
    SELECT CAST('09:00:00' AS TIME) AS start_time, CAST('11:00:00' AS TIME) AS end_time
    UNION ALL
    SELECT CAST('14:00:00' AS TIME), CAST('16:00:00' AS TIME)
    UNION ALL
    SELECT CAST('18:00:00' AS TIME), CAST('20:00:00' AS TIME)
) schedule_slot
WHERE store.status = 1
  AND service_item.status = 1
  AND NOT EXISTS (
      SELECT 1
      FROM service_schedule existing_schedule
      WHERE existing_schedule.store_id = store.id
        AND existing_schedule.service_item_id = service_item.id
        AND existing_schedule.schedule_date = DATE_ADD('2026-11-11', INTERVAL schedule_day.day_offset DAY)
        AND existing_schedule.start_time = schedule_slot.start_time
        AND existing_schedule.end_time = schedule_slot.end_time
  );
